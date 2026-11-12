package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.DeliveryConfig;
import com.example.demo.Entity.DeliveryMethod;
import com.example.demo.Entity.StoreLocation;
import com.example.demo.Repository.DeliveryConfigRepo;
import com.example.demo.Repository.StoreLocationRepo;

@Service
public class StoreLocationService {

    @Autowired
    private StoreLocationRepo storeLocationRepo;

    @Autowired
    private DeliveryConfigRepo deliveryConfigRepo;

    @Autowired
    private IndiaLocationService locationService;

    public List<StoreLocation> getAllStoresForAdmin() {
        return storeLocationRepo.findAllByOrderByActiveDescStateAscCityAscStoreNameAsc();
    }

    public List<StoreLocation> getActiveStoresForCheckout() {
        return storeLocationRepo.findByActiveTrueOrderByStateAscCityAscStoreNameAsc();
    }

    public Optional<StoreLocation> getStoreById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return storeLocationRepo.findById(id);
    }

    public Optional<StoreLocation> getActiveStoreById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return storeLocationRepo.findById(id).filter(StoreLocation::isActive);
    }

    public long countActiveStores() {
        return storeLocationRepo.countByActiveTrue();
    }

    public String validateStoreFields(
            String storeName,
            String contactNumber,
            String country,
            String state,
            String city,
            String address,
            String pinCode,
            String openingHours) {

        if (storeName == null || storeName.trim().length() < 2) {
            return "Store name is required (minimum 2 characters).";
        }
        if (!locationService.isValidIndianMobile(contactNumber)) {
            return "Please enter a valid 10-digit Indian store contact number starting with 6, 7, 8, or 9.";
        }
        String resolvedCountry = (country == null || country.isBlank())
                ? IndiaLocationService.DEFAULT_COUNTRY
                : country.trim();
        if (!locationService.isValidCountry(resolvedCountry)) {
            return "Store locations are currently supported within India only.";
        }
        if (!locationService.isValidIndianState(state)) {
            return "Please select a valid Indian state or union territory.";
        }
        if (!locationService.isValidCityName(city)) {
            return "Please select or enter a valid city.";
        }
        if (address == null || address.trim().length() < 5) {
            return "Complete store address is required.";
        }
        if (!locationService.isValidIndianPinCode(pinCode)) {
            return "Please enter a valid 6-digit Indian PIN code.";
        }
        if (openingHours == null || openingHours.trim().isEmpty()) {
            return "Opening hours are required (e.g. 10:00 AM - 8:00 PM Daily).";
        }
        return null;
    }

    @Transactional
    public StoreLocation saveOrUpdateStore(
            Long storeId,
            String storeName,
            String contactNumber,
            String country,
            String state,
            String city,
            String address,
            String pinCode,
            String openingHours,
            boolean active) {

        String err = validateStoreFields(storeName, contactNumber, country, state, city, address, pinCode, openingHours);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        StoreLocation store = (storeId != null)
                ? storeLocationRepo.findById(storeId).orElseThrow(() -> new IllegalArgumentException("Store location not found."))
                : new StoreLocation();

        store.setStoreName(storeName.trim());
        store.setContactNumber(contactNumber.trim());
        store.setCountry(IndiaLocationService.DEFAULT_COUNTRY);
        store.setState(locationService.normalizeState(state));
        store.setCity(city.trim());
        store.setAddress(address.trim());
        store.setPinCode(pinCode.trim());
        store.setOpeningHours(openingHours.trim());
        store.setActive(active);

        return storeLocationRepo.save(store);
    }

    @Transactional
    public StoreLocation toggleStoreAvailability(Long storeId) {
        StoreLocation store = storeLocationRepo.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store location not found."));
        store.setActive(!store.isActive());
        return storeLocationRepo.save(store);
    }

    @Transactional
    public void deleteStore(Long storeId) {
        if (!storeLocationRepo.existsById(storeId)) {
            throw new IllegalArgumentException("Store location not found.");
        }
        storeLocationRepo.deleteById(storeId);
    }

    public DeliveryConfig getDeliveryConfig() {
        return deliveryConfigRepo.findById(DeliveryConfig.SINGLETON_ID)
                .orElseGet(() -> {
                    DeliveryConfig def = new DeliveryConfig();
                    def.setId(DeliveryConfig.SINGLETON_ID);
                    def.setHomeDeliveryCharge(0.0);
                    def.setStorePickupCharge(0.0);
                    def.setPickupInstructions(DeliveryConfig.DEFAULT_PICKUP_INSTRUCTIONS);
                    return deliveryConfigRepo.save(def);
                });
    }

    @Transactional
    public DeliveryConfig updateDeliveryConfig(double homeDeliveryCharge, double storePickupCharge, String pickupInstructions) {
        if (homeDeliveryCharge < 0 || storePickupCharge < 0) {
            throw new IllegalArgumentException("Delivery charges cannot be negative.");
        }
        DeliveryConfig cfg = getDeliveryConfig();
        cfg.setHomeDeliveryCharge(homeDeliveryCharge);
        cfg.setStorePickupCharge(storePickupCharge);
        cfg.setPickupInstructions(pickupInstructions != null && !pickupInstructions.isBlank()
                ? pickupInstructions.trim()
                : DeliveryConfig.DEFAULT_PICKUP_INSTRUCTIONS);
        return deliveryConfigRepo.save(cfg);
    }

    public double calculateDeliveryCharge(DeliveryMethod method) {
        if (method == null) {
            return 0.0;
        }
        DeliveryConfig cfg = getDeliveryConfig();
        return switch (method) {
            case HOME_DELIVERY -> cfg.getHomeDeliveryCharge();
            case STORE_PICKUP -> cfg.getStorePickupCharge();
        };
    }
}

