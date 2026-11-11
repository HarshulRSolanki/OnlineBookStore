package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.Entity.User;
import com.example.demo.Repository.CartRepo;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private CartRepo cartRepo;

    @ModelAttribute("currentUserRole")
    public String currentUserRole(HttpSession session) {
        return authHelper.getEffectiveRole(session);
    }

    @ModelAttribute("isAdminSession")
    public boolean isAdminSession(HttpSession session) {
        return authHelper.isAdmin(session);
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        return authHelper.getLoggedInUser(session);
    }

    @ModelAttribute("currentCartCount")
    public int currentCartCount(HttpSession session) {
        if (authHelper.isAdmin(session)) {
            return 0;
        }
        User user = authHelper.getLoggedInUser(session);
        if (user == null || user.getUserid() == null) {
            return 0;
        }
        try {
            return cartRepo.findByUser(user).size();
        } catch (Exception e) {
            return 0;
        }
    }
}

