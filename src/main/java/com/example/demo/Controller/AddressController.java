package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.AuthHelper;
import com.example.demo.Entity.User;
import com.example.demo.Entity.UserAddress;
import com.example.demo.service.AddressService;
import com.example.demo.service.IndiaLocationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/addresses")
public class AddressController {

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private AddressService addressService;

    private String sanitizeReturnUrl(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return "/profile#addresses-section";
        }
        String trimmed = returnTo.trim();
        if (trimmed.equals("/cart") || trimmed.startsWith("/checkout/")) {
            return trimmed;
        }
        if (trimmed.startsWith("/profile")) {
            return "/profile#addresses-section";
        }
        return "/profile#addresses-section";
    }

    @PostMapping("/save")
    public String saveAddress(
            @RequestParam(value = "addressId", required = false) Long addressId,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
            @RequestParam(value = "houseNo", required = false) String houseNo,
            @RequestParam(value = "buildingName", required = false) String buildingName,
            @RequestParam(value = "streetArea", required = false) String streetArea,
            @RequestParam(value = "landmark", required = false) String landmark,
            @RequestParam(value = "country", required = false, defaultValue = IndiaLocationService.DEFAULT_COUNTRY) String country,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "pinCode", required = false) String pinCode,
            @RequestParam(value = "addressType", required = false, defaultValue = "HOME") String addressType,
            @RequestParam(value = "defaultAddress", required = false, defaultValue = "false") boolean defaultAddress,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/dashboard";
        }

        String redirectTarget = sanitizeReturnUrl(returnTo);
        try {
            UserAddress saved = addressService.saveOrUpdateAddress(
                    currentUser, addressId, fullName, mobileNumber, houseNo, buildingName,
                    streetArea, landmark, country, state, city, pinCode, addressType, defaultAddress);
            redirectAttributes.addFlashAttribute("message",
                    (addressId != null ? "Delivery address updated for " : "New delivery address saved for ")
                            + saved.getFullName() + ".");
        } catch (SecurityException se) {
            return "redirect:/access-denied";
        } catch (IllegalArgumentException iae) {
            redirectAttributes.addFlashAttribute("error", iae.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not save delivery address: " + e.getMessage());
        }
        return "redirect:" + redirectTarget;
    }

    @PostMapping("/{addressId}/default")
    public String setDefaultAddress(
            @PathVariable Long addressId,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/dashboard";
        }

        String redirectTarget = sanitizeReturnUrl(returnTo);
        try {
            addressService.setDefaultAddress(addressId, currentUser);
            redirectAttributes.addFlashAttribute("message", "Default delivery address updated.");
        } catch (SecurityException se) {
            return "redirect:/access-denied";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update default address: " + e.getMessage());
        }
        return "redirect:" + redirectTarget;
    }

    @PostMapping("/{addressId}/delete")
    public String deleteAddress(
            @PathVariable Long addressId,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/dashboard";
        }

        String redirectTarget = sanitizeReturnUrl(returnTo);
        try {
            addressService.deleteOwnedAddress(addressId, currentUser);
            redirectAttributes.addFlashAttribute("message", "Saved delivery address removed.");
        } catch (SecurityException se) {
            return "redirect:/access-denied";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete address: " + e.getMessage());
        }
        return "redirect:" + redirectTarget;
    }
}

