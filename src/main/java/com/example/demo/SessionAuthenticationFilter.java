package com.example.demo;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.Entity.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean needsSync = existingAuth == null
                || !existingAuth.isAuthenticated()
                || (existingAuth instanceof AnonymousAuthenticationToken);

        if (needsSync) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object loggedIn = session.getAttribute("loggedInUser");
                if (loggedIn instanceof User user && user.isEnabled()) {
                    String role = user.isAdmin() ? "ADMIN" : "USER";
                    session.setAttribute("userRole", role);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else if ("Admin".equalsIgnoreCase(String.valueOf(loggedIn))
                        || "ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("userRole")))) {
                    session.setAttribute("userRole", "ADMIN");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            "Admin",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

