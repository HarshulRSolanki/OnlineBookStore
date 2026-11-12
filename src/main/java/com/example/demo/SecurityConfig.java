package com.example.demo;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private SessionAuthenticationFilter sessionAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico",
                    "/error", "/error/**", "/access-denied", "/errorpage"
                ).permitAll()
                .requestMatchers(
                    "/", "/index", "/books", "/books/view/**", "/categories", "/search",
                    "/about", "/contact", "/viewpdf/**", "/api/locations/**"
                ).permitAll()
                .requestMatchers(
                    "/login", "/logout", "/userreg", "/forgetpwd",
                    "/verify-otp", "/resend-otp", "/newpass"
                ).permitAll()
                .requestMatchers(
                    "/api/webhook/stripe", "/success", "/payment-success", "/cancel"
                ).permitAll()
                .requestMatchers(
                    "/admin/**", "/admindash", "/manage-books", "/addBooks",
                    "/displayBooks", "/add-category", "/deleteBooks",
                    "/updateBooks/**", "/update/**", "/delete-category",
                    "/delete-user", "/delete-inquiry", "/books/report/pdf"
                ).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader("Accept");
                    String uri = request.getRequestURI();
                    if ((accept != null && accept.contains("application/json"))
                            || (uri != null && (uri.startsWith("/api/") || uri.contains("/analytics")))) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String accept = request.getHeader("Accept");
                    String uri = request.getRequestURI();
                    if ((accept != null && accept.contains("application/json"))
                            || (uri != null && (uri.startsWith("/api/") || uri.contains("/analytics") || uri.contains("/export/")))) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        request.setAttribute("jakarta.servlet.error.status_code", HttpServletResponse.SC_FORBIDDEN);
                        request.getRequestDispatcher("/access-denied").forward(request, response);
                    }
                })
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void init() {
        File uploadDir = new File("uploads/books/");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            System.out.println("Book storage folder created at: " + uploadDir.getAbsolutePath());
        }
    }
}
