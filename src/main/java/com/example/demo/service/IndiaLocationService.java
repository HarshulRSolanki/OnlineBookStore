package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Centralized India-specific geographic taxonomy and validation service.
 * Designed extensibly to support additional Indian cities seamlessly.
 */
@Service
public class IndiaLocationService {

    public static final String DEFAULT_COUNTRY = "India";
    private static final Pattern INDIAN_MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern INDIAN_PIN_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private static final Map<String, List<String>> STATE_CITY_MAP;

    static {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put("Gujarat", List.of(
                "Ahmedabad", "Vadodara", "Surat", "Rajkot",
                "Gandhinagar", "Anand", "Bharuch", "Jamnagar",
                "Bhavnagar", "Junagadh", "Navsari", "Valsad", "Morbi", "Mehsana"));

        map.put("Maharashtra", List.of(
                "Mumbai", "Pune", "Nagpur", "Nashik",
                "Thane", "Chhatrapati Sambhajinagar", "Solapur", "Kolhapur", "Navi Mumbai", "Amravati"));

        map.put("Rajasthan", List.of(
                "Jaipur", "Udaipur", "Jodhpur", "Kota",
                "Ajmer", "Bikaner", "Alwar", "Bhilwara"));

        map.put("Madhya Pradesh", List.of(
                "Indore", "Bhopal", "Jabalpur", "Gwalior",
                "Ujjain", "Sagar", "Rewa", "Satna"));

        map.put("Delhi", List.of(
                "New Delhi", "Central Delhi", "South Delhi", "North Delhi",
                "East Delhi", "West Delhi", "Dwarka", "Rohini"));

        map.put("Uttar Pradesh", List.of(
                "Lucknow", "Noida", "Ghaziabad", "Kanpur",
                "Agra", "Varanasi", "Prayagraj", "Meerut", "Bareilly"));

        map.put("Karnataka", List.of(
                "Bengaluru", "Mysuru", "Mangaluru", "Hubballi", "Belagavi", "Kalaburagi"));

        map.put("Tamil Nadu", List.of(
                "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Vellore"));

        map.put("Telangana", List.of(
                "Hyderabad", "Warangal", "Nizamabad", "Khammam", "Karimnagar"));

        map.put("Andhra Pradesh", List.of(
                "Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Nellore"));

        map.put("West Bengal", List.of(
                "Kolkata", "Howrah", "Siliguri", "Durgapur", "Asansol"));

        map.put("Kerala", List.of(
                "Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Kannur"));

        map.put("Punjab", List.of(
                "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Mohali"));

        map.put("Haryana", List.of(
                "Gurugram", "Faridabad", "Panipat", "Ambala", "Karnal", "Panchkula"));

        map.put("Bihar", List.of(
                "Patna", "Gaya", "Muzaffarpur", "Bhagalpur", "Darbhanga"));

        map.put("Odisha", List.of(
                "Bhubaneswar", "Cuttack", "Rourkela", "Puri", "Sambalpur"));

        map.put("Assam", List.of(
                "Guwahati", "Silchar", "Dibrugarh", "Jorhat"));

        map.put("Jharkhand", List.of(
                "Ranchi", "Jamshedpur", "Dhanbad", "Bokaro"));

        map.put("Chhattisgarh", List.of(
                "Raipur", "Bhilai", "Bilaspur", "Korba"));

        map.put("Uttarakhand", List.of(
                "Dehradun", "Haridwar", "Haldwani", "Roorkee", "Rishikesh"));

        map.put("Himachal Pradesh", List.of(
                "Shimla", "Dharamshala", "Solan", "Mandi"));

        map.put("Goa", List.of(
                "Panaji", "Margao", "Vasco da Gama", "Mapusa"));

        map.put("Jammu and Kashmir", List.of(
                "Srinagar", "Jammu", "Anantnag", "Udhampur"));

        map.put("Chandigarh", List.of("Chandigarh"));
        map.put("Puducherry", List.of("Puducherry", "Karaikal"));
        map.put("Tripura", List.of("Agartala", "Udaipur"));
        map.put("Meghalaya", List.of("Shillong", "Tura"));
        map.put("Manipur", List.of("Imphal"));
        map.put("Nagaland", List.of("Kohima", "Dimapur"));
        map.put("Mizoram", List.of("Aizawl"));
        map.put("Arunachal Pradesh", List.of("Itanagar", "Naharlagun"));
        map.put("Sikkim", List.of("Gangtok", "Namchi"));
        map.put("Ladakh", List.of("Leh", "Kargil"));
        map.put("Andaman and Nicobar Islands", List.of("Sri Vijaya Puram"));
        map.put("Dadra and Nagar Haveli and Daman and Diu", List.of("Silvassa", "Daman", "Diu"));
        map.put("Lakshadweep", List.of("Kavaratti"));

        STATE_CITY_MAP = Collections.unmodifiableMap(map);
    }

    public String getDefaultCountry() {
        return DEFAULT_COUNTRY;
    }

    public List<String> getStates() {
        return new ArrayList<>(STATE_CITY_MAP.keySet());
    }

    public Map<String, List<String>> getStateCitiesMap() {
        return STATE_CITY_MAP;
    }

    public String normalizeState(String inputState) {
        if (inputState == null || inputState.isBlank()) {
            return null;
        }
        String needle = inputState.trim();
        for (String canonical : STATE_CITY_MAP.keySet()) {
            if (canonical.equalsIgnoreCase(needle)) {
                return canonical;
            }
        }
        return null;
    }

    public List<String> getCitiesForState(String state) {
        String canonical = normalizeState(state);
        if (canonical == null) {
            return Collections.emptyList();
        }
        return STATE_CITY_MAP.getOrDefault(canonical, Collections.emptyList());
    }

    public String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return DEFAULT_COUNTRY;
        }
        String trimmed = country.trim();
        return "India".equalsIgnoreCase(trimmed) ? DEFAULT_COUNTRY : trimmed;
    }

    public boolean isValidCountry(String country) {
        return country != null && DEFAULT_COUNTRY.equalsIgnoreCase(country.trim());
    }

    public boolean isValidIndianState(String state) {
        return normalizeState(state) != null;
    }

    public boolean isValidCityName(String city) {
        if (city == null || city.isBlank()) {
            return false;
        }
        String trimmed = city.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 80 && trimmed.matches("^[a-zA-Z0-9 .,'()/-]+$");
    }

    public boolean isValidIndianMobile(String mobile) {
        if (mobile == null) {
            return false;
        }
        return INDIAN_MOBILE_PATTERN.matcher(mobile.trim()).matches();
    }

    public boolean isValidIndianPinCode(String pinCode) {
        if (pinCode == null) {
            return false;
        }
        return INDIAN_PIN_PATTERN.matcher(pinCode.trim()).matches();
    }

    public Set<String> getAllowedAddressTypes() {
        return Set.of("HOME", "OFFICE", "OTHER");
    }
}

