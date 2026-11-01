package com.example.demo.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OTPservice
{
	@Autowired
    private JavaMailSender mailSender;

    private final Map<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_DURATION = 5 * 60 * 1000; // 5 minutes

    // Generate and store OTP for a user
    public String generateOTP(String email) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // Generate 6-digit OTP
        OTPData otpData = new OTPData(otp, System.currentTimeMillis());

        otpStore.put(email, otpData); // Store only the latest OTP for each user
        return otp;
    }

    // Send OTP via email
    public void sendOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP for verification is: " + otp + "\nThis OTP is valid for 5 minutes.");
        mailSender.send(message);
    }

    // Verify OTP for a user
    public boolean verifyOTP(String email, String otp) {
        OTPData otpData = otpStore.get(email);

        if (otpData == null) {
            return false; // No OTP found for the user
        }

        long currentTime = System.currentTimeMillis();
        if ((currentTime - otpData.getTimestamp()) > OTP_VALIDITY_DURATION) {
            otpStore.remove(email); // Remove expired OTP
            return false;
        }

        if (otpData.getOtp().equals(otp)) {
            otpStore.remove(email); // Remove OTP after successful verification
            return true;
        }

        long elapsedTimeSeconds = (currentTime - otpData.getTimestamp()) / 1000; // Convert to seconds
        if (elapsedTimeSeconds > 300) { // 300 seconds = 5 minutes
            otpStore.remove(email); // Remove expired OTP
            return false;
        }

        
        return false; // Invalid OTP
    }

    // Scheduled task to remove expired OTPs every minute
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void cleanupExpiredOTPs() {
        long currentTime = System.currentTimeMillis();
        otpStore.entrySet().removeIf(entry -> (currentTime - entry.getValue().getTimestamp()) > OTP_VALIDITY_DURATION);
    }

    // Inner class to store OTP data
    private static class OTPData {
        private final String otp;
        private final long timestamp;

        public OTPData(String otp, long timestamp) {
            this.otp = otp;
            this.timestamp = timestamp;
        }

        public String getOtp() {
            return otp;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
