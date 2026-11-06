package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OTPservice {

    @Autowired
    private EmailService emailService;

    private final Map<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_DURATION = 5 * 60 * 1000; // 5 minutes
    private static final long RESEND_COOLDOWN_DURATION = 60 * 1000;  // 60 seconds
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Check if user is eligible to request a new OTP (60s cooldown)
     */
    public boolean canResend(String email) {
        if (email == null) return false;
        OTPData data = otpStore.get(email.trim().toLowerCase());
        if (data == null) return true;
        return (System.currentTimeMillis() - data.getLastSentTimestamp()) >= RESEND_COOLDOWN_DURATION;
    }

    /**
     * Seconds remaining before user can resend
     */
    public long getCooldownRemainingSeconds(String email) {
        if (email == null) return 0;
        OTPData data = otpStore.get(email.trim().toLowerCase());
        if (data == null) return 0;
        long elapsed = System.currentTimeMillis() - data.getLastSentTimestamp();
        if (elapsed >= RESEND_COOLDOWN_DURATION) return 0;
        return (RESEND_COOLDOWN_DURATION - elapsed) / 1000;
    }

    /**
     * Generate cryptographically secure 6-digit OTP and store SHA-256 hash
     */
    public String generateOTP(String email) {
        if (email == null) return null;
        String normalizedEmail = email.trim().toLowerCase();

        // Generate 6-digit number [100000, 999999]
        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);

        String hashedOtp = hashOtp(otp);
        long now = System.currentTimeMillis();

        OTPData otpData = new OTPData(hashedOtp, now, now, 0);
        otpStore.put(normalizedEmail, otpData);

        return otp;
    }

    /**
     * Send OTP using branded HTML email template
     */
    public void sendOTP(String email, String otp) {
        emailService.sendOtpEmail(email, otp);
    }

    /**
     * Verify OTP with security hash, expiration, and brute-force attempt limits
     */
    public boolean verifyOTP(String email, String rawOtp) {
        if (email == null || rawOtp == null || rawOtp.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        OTPData otpData = otpStore.get(normalizedEmail);

        if (otpData == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        // 1. Expiration check (5 minutes)
        if ((currentTime - otpData.getCreatedAt()) > OTP_VALIDITY_DURATION) {
            otpStore.remove(normalizedEmail);
            return false;
        }

        // 2. Max attempts check
        if (otpData.getAttempts() >= MAX_FAILED_ATTEMPTS) {
            otpStore.remove(normalizedEmail);
            return false;
        }

        // 3. Cryptographic hash comparison
        String hashedAttempt = hashOtp(rawOtp.trim());
        if (otpData.getHashedOtp().equals(hashedAttempt)) {
            // Immediate invalidation on success
            otpStore.remove(normalizedEmail);
            return true;
        } else {
            // Increment failed attempt counter
            int attempts = otpData.incrementAttempts();
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Brute-force threshold exceeded: invalidate immediately
                otpStore.remove(normalizedEmail);
                System.err.println("Brute-force protection: Inactivated OTP for " + normalizedEmail + " after 5 failed attempts.");
            }
            return false;
        }
    }

    /**
     * SHA-256 hash helper
     */
    private String hashOtp(String plainOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainOtp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing in JVM", e);
        }
    }

    /**
     * Periodic scheduled cleanup of expired OTPs every 60 seconds
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredOTPs() {
        long currentTime = System.currentTimeMillis();
        otpStore.entrySet().removeIf(entry -> (currentTime - entry.getValue().getCreatedAt()) > OTP_VALIDITY_DURATION);
    }

    /**
     * Thread-safe OTP record
     */
    private static class OTPData {
        private final String hashedOtp;
        private final long createdAt;
        private final long lastSentTimestamp;
        private int attempts;

        public OTPData(String hashedOtp, long createdAt, long lastSentTimestamp, int attempts) {
            this.hashedOtp = hashedOtp;
            this.createdAt = createdAt;
            this.lastSentTimestamp = lastSentTimestamp;
            this.attempts = attempts;
        }

        public String getHashedOtp() {
            return hashedOtp;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastSentTimestamp() {
            return lastSentTimestamp;
        }

        public int getAttempts() {
            return attempts;
        }

        public synchronized int incrementAttempts() {
            this.attempts++;
            return this.attempts;
        }
    }
}
