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

    public enum OtpStatus {
        VALID,
        EXPIRED,
        INVALID,
        MAX_ATTEMPTS,
        NOT_FOUND
    }

    @Autowired
    private EmailService emailService;

    private final Map<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_DURATION = 5 * 60 * 1000L; // 5 minutes
    private static final long RESEND_COOLDOWN_DURATION = 60 * 1000L;  // 60 seconds
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    public static String profileUpdateKey(Long userId, String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        return "profile:" + (userId != null ? userId : "anon") + ":" + normalizedEmail;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    /**
     * Check if user is eligible to request a new OTP (60s cooldown while an active OTP exists)
     */
    public boolean canResend(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeKey(key);
        OTPData data = otpStore.get(normalized);
        if (data == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        // If already expired, allow generating a fresh OTP immediately
        if ((now - data.getCreatedAt()) > OTP_VALIDITY_DURATION) {
            return true;
        }
        return (now - data.getLastSentTimestamp()) >= RESEND_COOLDOWN_DURATION;
    }

    /**
     * Seconds remaining before user can resend
     */
    public long getCooldownRemainingSeconds(String key) {
        if (key == null || key.trim().isEmpty()) {
            return 0;
        }
        String normalized = normalizeKey(key);
        OTPData data = otpStore.get(normalized);
        if (data == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if ((now - data.getCreatedAt()) > OTP_VALIDITY_DURATION) {
            return 0;
        }
        long elapsed = now - data.getLastSentTimestamp();
        if (elapsed >= RESEND_COOLDOWN_DURATION) {
            return 0;
        }
        return Math.max(1L, (RESEND_COOLDOWN_DURATION - elapsed + 999L) / 1000L);
    }

    public boolean hasActiveOTP(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        OTPData data = otpStore.get(normalizeKey(key));
        if (data == null) {
            return false;
        }
        return (System.currentTimeMillis() - data.getCreatedAt()) <= OTP_VALIDITY_DURATION
                && data.getAttempts() < MAX_FAILED_ATTEMPTS;
    }

    public int getRemainingAttempts(String key) {
        if (key == null || key.trim().isEmpty()) {
            return 0;
        }
        OTPData data = otpStore.get(normalizeKey(key));
        if (data == null) {
            return 0;
        }
        return Math.max(0, MAX_FAILED_ATTEMPTS - data.getAttempts());
    }

    public void invalidateOTP(String key) {
        if (key != null && !key.trim().isEmpty()) {
            otpStore.remove(normalizeKey(key));
        }
    }

    /**
     * Test/diagnostic helper to expire an active OTP deterministically without waiting 5 minutes.
     */
    public void expireOtpForTesting(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        String normalized = normalizeKey(key);
        OTPData data = otpStore.get(normalized);
        if (data != null) {
            long expiredCreatedAt = System.currentTimeMillis() - OTP_VALIDITY_DURATION - 5000L;
            otpStore.put(normalized, new OTPData(data.getHashedOtp(), expiredCreatedAt, data.getLastSentTimestamp(), data.getAttempts()));
        }
    }

    /**
     * Generate cryptographically secure 6-digit OTP and store SHA-256 hash
     */
    public String generateOTP(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String normalizedKey = normalizeKey(key);

        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);

        String hashedOtp = hashOtp(otp);
        long now = System.currentTimeMillis();

        OTPData otpData = new OTPData(hashedOtp, now, now, 0);
        otpStore.put(normalizedKey, otpData);

        return otp;
    }

    /**
     * Send OTP using branded HTML email template
     */
    public void sendOTP(String email, String otp) {
        emailService.sendOtpEmail(email, otp);
    }

    public void sendProfileUpdateOTP(String email, String otp) {
        emailService.sendOtpEmail(
                email,
                otp,
                "Quickread Profile Update Verification Code",
                "Verify Your Profile Update",
                "Use the 6-digit security code below to confirm and save changes to your Quickread profile.");
    }

    /**
     * Verify OTP returning detailed status (VALID, EXPIRED, INVALID, MAX_ATTEMPTS, NOT_FOUND).
     * Consumes OTP atomically upon VALID verification or invalidation.
     */
    public OtpStatus verifyOTPWithStatus(String key, String rawOtp) {
        if (key == null || key.trim().isEmpty()) {
            return OtpStatus.NOT_FOUND;
        }
        if (rawOtp == null || rawOtp.trim().isEmpty()) {
            return OtpStatus.INVALID;
        }

        String normalizedKey = normalizeKey(key);
        OTPData otpData = otpStore.get(normalizedKey);
        if (otpData == null) {
            return OtpStatus.NOT_FOUND;
        }

        synchronized (otpData) {
            if (otpStore.get(normalizedKey) != otpData) {
                return OtpStatus.NOT_FOUND;
            }

            long currentTime = System.currentTimeMillis();

            // 1. Expiration check (5 minutes)
            if ((currentTime - otpData.getCreatedAt()) > OTP_VALIDITY_DURATION) {
                otpStore.remove(normalizedKey, otpData);
                return OtpStatus.EXPIRED;
            }

            // 2. Max attempts check
            if (otpData.getAttempts() >= MAX_FAILED_ATTEMPTS) {
                otpStore.remove(normalizedKey, otpData);
                return OtpStatus.MAX_ATTEMPTS;
            }

            // 3. Cryptographic hash comparison
            String hashedAttempt = hashOtp(rawOtp.trim());
            if (otpData.getHashedOtp().equals(hashedAttempt)) {
                otpStore.remove(normalizedKey, otpData);
                return OtpStatus.VALID;
            } else {
                int attempts = otpData.incrementAttempts();
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    otpStore.remove(normalizedKey, otpData);
                    System.err.println("Brute-force protection: Invalidated OTP for " + normalizedKey + " after 5 failed attempts.");
                    return OtpStatus.MAX_ATTEMPTS;
                }
                return OtpStatus.INVALID;
            }
        }
    }

    /**
     * Backward-compatible boolean OTP verification
     */
    public boolean verifyOTP(String key, String rawOtp) {
        return verifyOTPWithStatus(key, rawOtp) == OtpStatus.VALID;
    }

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
