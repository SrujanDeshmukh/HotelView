package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.service.admin.HotelViewDetailsService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/info")
public class HotelViewDetailsController {

    @Autowired
    private HotelViewDetailsService detailsService;

    @GetMapping("/about")
    public ResponseEntity<Document> getAbout() {
        Document data = detailsService.getAboutInfo();
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }

    @GetMapping("/terms")
    public ResponseEntity<Document> getTerms() {
        Document data = detailsService.getTermsAndPrivacy();
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }

    @GetMapping("/greeting")
    public ResponseEntity<Document> getGreeting() {
        Document data = detailsService.getGreetingMessage();
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }

    @PutMapping("/greeting")
    public ResponseEntity<Document> updateGreeting(@RequestBody Document newGreetingPayload) {
        try {
            Document updatedData = detailsService.updateGreetingMessage(newGreetingPayload);
            return ResponseEntity.ok(updatedData);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/contact")
    public ResponseEntity<Document> getContact() {
        Document data = detailsService.getContactDetails();
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }
}