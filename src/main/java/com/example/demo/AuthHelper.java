package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepo;

import jakarta.servlet.http.HttpSession;

@Component
public class AuthHelper {

    @Autowired
    private UserRepo userRepo;

    public User getLoggedInUser(HttpSession session) {
        if (session != null) {
            Object sessionObj = session.getAttribute("loggedInUser");
            if (sessionObj instanceof User u) {
                if (u.getUserid() != null) {
                    return userRepo.findById(u.getUserid()).orElse(u);
                }
                return u;
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User u) {
                return u;
            }
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                User found = userRepo.findByUsernameIgnoreCase(name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public boolean isAdmin(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()))) {
            return true;
        }
        if (session != null) {
            Object roleAttr = session.getAttribute("userRole");
            if ("ADMIN".equalsIgnoreCase(String.valueOf(roleAttr))) {
                return true;
            }
            Object sessionObj = session.getAttribute("loggedInUser");
            if (sessionObj instanceof User u) {
                return u.isAdmin();
            }
            if ("Admin".equalsIgnoreCase(String.valueOf(sessionObj))) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthenticated(HttpSession session) {
        if (isAdmin(session)) {
            return true;
        }
        if (getLoggedInUser(session) != null) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    public String getEffectiveRole(HttpSession session) {
        if (isAdmin(session)) {
            return "ADMIN";
        }
        if (isAuthenticated(session)) {
            return "USER";
        }
        return "GUEST";
    }

    public void syncSessionAuth(HttpSession session, User user) {
        if (session == null || user == null) {
            return;
        }
        String role = user.isAdmin() ? "ADMIN" : "USER";
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userRole", role);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    public void clearAuth(HttpSession session) {
        SecurityContextHolder.clearContext();
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
            }
        }
    }
}

