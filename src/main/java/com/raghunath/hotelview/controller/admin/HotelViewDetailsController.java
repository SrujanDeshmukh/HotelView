package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.service.admin.HotelViewDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/info")
public class HotelViewDetailsController {

    @GetMapping("/terms")
    public Map<String, String> getTerms() {
        Map<String, String> response = new HashMap<>();
        response.put("data", "Dummy Terms: 1. No refunds after 24 hours. 2. Respect hotel property. 3. Check-out is by 11 AM.");
        return response;
    }

    @GetMapping("/contact")
    public Map<String, String> getContact() {
        Map<String, String> response = new HashMap<>();
        response.put("email", "support@hotelview.com");
        response.put("phone", "+91 9876543210");
        response.put("address", "123, Hotel Street, Mumbai, India");
        return response;
    }

    @GetMapping("/about")
    public Map<String, String> getAbout() {
        Map<String, String> response = new HashMap<>();
        response.put("description", "HotelView is a premium Restaurant Management Software built to provide seamless dining experiences.");
        return response;
    }
}