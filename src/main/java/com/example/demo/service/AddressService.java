package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.User;
import com.example.demo.Entity.UserAddress;
import com.example.demo.Repository.UserAddressRepo;

@Service
public class AddressService {

    @Autowired
    private UserAddressRepo userAddressRepo;

    @Autowired
    private IndiaLocationService locationService;

    public List<UserAddress> getAddressesForUser(User user) {
        if (user == null || user.getUserid() == null) {
            return List.of();
        }
        return userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(user);
    }

    public Optional<UserAddress> getOwnedAddress(Long addressId, User user) {
        if (addressId == null || user == null || user.getUserid() == null) {
            return Optional.empty();
        }
        return userAddressRepo.findByIdAndUser(addressId, user);
    }

    public String validateAddressFields(
            String fullName,
            String mobileNumber,
            String houseNo,
            String buildingName,
            String streetArea,
            String country,
            String state,
            String city,
            String pinCode,
            String addressType) {

        if (fullName == null || fullName.trim().length() < 2) {
            return "Full name is required (minimum 2 characters).";
        }
        if (!locationService.isValidIndianMobile(mobileNumber)) {
            return "Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.";
        }
        if (houseNo == null || houseNo.trim().isEmpty()) {
            return "House / Flat Number is required.";
        }
        if (buildingName == null || buildingName.trim().isEmpty()) {
            return "Building / Apartment Name is required.";
        }
        if (streetArea == null || streetArea.trim().isEmpty()) {
            return "Street / Area is required.";
        }
        String resolvedCountry = (country == null || country.isBlank())
                ? IndiaLocationService.DEFAULT_COUNTRY
                : country.trim();
        if (!locationService.isValidCountry(resolvedCountry)) {
            return "Delivery addresses are currently supported within India only.";
        }
        if (!locationService.isValidIndianState(state)) {
            return "Please select a valid Indian state or union territory.";
        }
        if (!locationService.isValidCityName(city)) {
            return "Please select or enter a valid city.";
        }
        if (!locationService.isValidIndianPinCode(pinCode)) {
            return "Please enter a valid 6-digit Indian PIN code (e.g. 380009).";
        }
        String normalizedType = addressType != null ? addressType.trim().toUpperCase() : "HOME";
        if (!locationService.getAllowedAddressTypes().contains(normalizedType)) {
            return "Address type must be Home, Office, or Other.";
        }
        return null;
    }

    @Transactional
    public UserAddress saveOrUpdateAddress(
            User user,
            Long addressId,
            String fullName,
            String mobileNumber,
            String houseNo,
            String buildingName,
            String streetArea,
            String landmark,
            String country,
            String state,
            String city,
            String pinCode,
            String addressType,
            boolean setAsDefault) {

        if (user == null || user.getUserid() == null) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        String validationError = validateAddressFields(
                fullName, mobileNumber, houseNo, buildingName, streetArea,
                country, state, city, pinCode, addressType);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        long existingCount = userAddressRepo.countByUser(user);
        boolean shouldBeDefault = setAsDefault || existingCount == 0;
        if (shouldBeDefault) {
            userAddressRepo.clearDefaultForUser(user);
        }

        UserAddress target;
        if (addressId != null) {
            target = userAddressRepo.findByIdAndUser(addressId, user)
                    .orElseThrow(() -> new SecurityException("Address not found or you do not have permission to edit it."));
        } else {
            target = new UserAddress();
            target.setUser(user);
        }

        target.setFullName(fullName.trim());
        target.setMobileNumber(mobileNumber.trim());
        target.setHouseNo(houseNo.trim());
        target.setBuildingName(buildingName.trim());
        target.setStreetArea(streetArea.trim());
        target.setLandmark(landmark != null && !landmark.trim().isEmpty() ? landmark.trim() : null);
        target.setCountry(IndiaLocationService.DEFAULT_COUNTRY);
        target.setState(locationService.normalizeState(state));
        target.setCity(city.trim());
        target.setPinCode(pinCode.trim());
        target.setAddressType(addressType != null ? addressType.trim().toUpperCase() : "HOME");

        if (shouldBeDefault) {
            target.setDefaultAddress(true);
        } else if (addressId == null) {
            target.setDefaultAddress(false);
        }

        return userAddressRepo.save(target);
    }

    @Transactional
    public void setDefaultAddress(Long addressId, User user) {
        if (userAddressRepo.findByIdAndUser(addressId, user).isEmpty()) {
            throw new SecurityException("Address not found or does not belong to your account.");
        }
        userAddressRepo.clearDefaultForUser(user);
        UserAddress refreshed = userAddressRepo.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new SecurityException("Address not found or does not belong to your account."));
        refreshed.setDefaultAddress(true);
        userAddressRepo.save(refreshed);
    }

    @Transactional
    public void deleteOwnedAddress(Long addressId, User user) {
        UserAddress target = userAddressRepo.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new SecurityException("Address not found or does not belong to your account."));
        boolean wasDefault = target.isDefaultAddress();
        userAddressRepo.delete(target);
        userAddressRepo.flush();

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(user);
            if (!remaining.isEmpty()) {
                UserAddress next = remaining.get(0);
                next.setDefaultAddress(true);
                userAddressRepo.save(next);
            }
        }
    }
}

