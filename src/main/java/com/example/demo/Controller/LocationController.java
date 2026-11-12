package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.IndiaLocationService;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private IndiaLocationService locationService;

    @GetMapping("/states")
    public List<String> getStates() {
        return locationService.getStates();
    }

    @GetMapping("/cities")
    public List<String> getCitiesByState(@RequestParam("state") String state) {
        return locationService.getCitiesForState(state);
    }

    @GetMapping("/all")
    public Map<String, List<String>> getAllStateCities() {
        return locationService.getStateCitiesMap();
    }
}

