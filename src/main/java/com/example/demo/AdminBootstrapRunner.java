package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepo;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.countByRoleIgnoreCase("ADMIN") > 0) {
            return;
        }
        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        String normalizedUsername = adminUsername.trim();
        User existing = userRepo.findByUsernameIgnoreCase(normalizedUsername);
        if (existing != null) {
            existing.setRole("ADMIN");
            existing.setIsregistered(true);
            existing.setEnabled(true);
            if (existing.getPassword() == null || existing.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(adminPassword.trim()));
            }
            userRepo.save(existing);
            return;
        }

        String resolvedEmail = (adminEmail != null && !adminEmail.isBlank())
                ? adminEmail.trim()
                : normalizedUsername.toLowerCase() + "@quickread.local";

        if (userRepo.findByEmailIgnoreCase(resolvedEmail) != null) {
            resolvedEmail = "admin-" + System.currentTimeMillis() + "@quickread.local";
        }

        User adminUser = new User();
        adminUser.setUsername(normalizedUsername);
        adminUser.setEmail(resolvedEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword.trim()));
        adminUser.setRole("ADMIN");
        adminUser.setIsregistered(true);
        adminUser.setEnabled(true);
        userRepo.save(adminUser);
    }
}

