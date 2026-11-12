package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entity.DeliveryConfig;
import com.example.demo.Entity.StoreLocation;
import com.example.demo.service.IndiaLocationService;
import com.example.demo.service.StoreLocationService;

@Controller
@RequestMapping("/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    @Autowired
    private StoreLocationService storeLocationService;

    @Autowired
    private IndiaLocationService locationService;

    @GetMapping
    public String listStores(
            @RequestParam(value = "editId", required = false) Long editId,
            Model model) {
        List<StoreLocation> stores = storeLocationService.getAllStoresForAdmin();
        DeliveryConfig config = storeLocationService.getDeliveryConfig();

        StoreLocation editStore = null;
        if (editId != null) {
            editStore = storeLocationService.getStoreById(editId).orElse(null);
        }

        long activeCount = stores.stream().filter(StoreLocation::isActive).count();

        model.addAttribute("stores", stores);
        model.addAttribute("deliveryConfig", config);
        model.addAttribute("editStore", editStore);
        model.addAttribute("activeStoreCount", activeCount);
        model.addAttribute("totalStoreCount", stores.size());
        model.addAttribute("indianStates", locationService.getStates());
        model.addAttribute("stateCitiesMap", locationService.getStateCitiesMap());
        model.addAttribute("defaultCountry", IndiaLocationService.DEFAULT_COUNTRY);
        return "Admin/stores";
    }

    @PostMapping("/save")
    public String saveOrUpdateStore(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("storeName") String storeName,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam(value = "country", required = false, defaultValue = IndiaLocationService.DEFAULT_COUNTRY) String country,
            @RequestParam("state") String state,
            @RequestParam("city") String city,
            @RequestParam("address") String address,
            @RequestParam("pinCode") String pinCode,
            @RequestParam("openingHours") String openingHours,
            @RequestParam(value = "active", required = false, defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {
        try {
            StoreLocation saved = storeLocationService.saveOrUpdateStore(
                    storeId, storeName, contactNumber, country, state, city, address, pinCode, openingHours, active);
            redirectAttributes.addFlashAttribute("message",
                    (storeId != null ? "Updated store location: '" : "Added store location: '")
                            + saved.getStoreName() + "' (" + saved.getCity() + ", " + saved.getState() + ").");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (storeId != null) {
                return "redirect:/admin/stores?editId=" + storeId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not save store location: " + e.getMessage());
        }
        return "redirect:/admin/stores";
    }

    @PostMapping("/{storeId}/toggle")
    public String toggleStoreAvailability(@PathVariable Long storeId, RedirectAttributes redirectAttributes) {
        try {
            StoreLocation toggled = storeLocationService.toggleStoreAvailability(storeId);
            redirectAttributes.addFlashAttribute("message",
                    "Pickup availability for '" + toggled.getStoreName() + "' is now "
                            + (toggled.isActive() ? "Active" : "Inactive") + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stores";
    }

    @PostMapping("/{storeId}/delete")
    public String deleteStore(@PathVariable Long storeId, RedirectAttributes redirectAttributes) {
        try {
            storeLocationService.deleteStore(storeId);
            redirectAttributes.addFlashAttribute("message", "Store location deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete store location: " + e.getMessage());
        }
        return "redirect:/admin/stores";
    }

    @PostMapping("/delivery-config")
    public String updateDeliveryConfig(
            @RequestParam(value = "homeDeliveryCharge", defaultValue = "0") double homeDeliveryCharge,
            @RequestParam(value = "storePickupCharge", defaultValue = "0") double storePickupCharge,
            @RequestParam(value = "pickupInstructions", required = false) String pickupInstructions,
            RedirectAttributes redirectAttributes) {
        try {
            storeLocationService.updateDeliveryConfig(homeDeliveryCharge, storePickupCharge, pickupInstructions);
            redirectAttributes.addFlashAttribute("message", "Delivery & pickup fee configuration updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update configuration: " + e.getMessage());
        }
        return "redirect:/admin/stores";
    }
}

