package com.raghunath.hotelview.service.admin;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class HotelViewDetailsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    // Fetches the Main Info (About, Metrics, Philosophy)
    public Document getAboutInfo() {
        Query query = new Query(Criteria.where("software_name").is("HotelView")
                .and("about_section").exists(true));
        return mongoTemplate.findOne(query, Document.class, "hotelview_details");
    }

    // Fetches Terms and Privacy Policy
    public Document getTermsAndPrivacy() {
        Query query = new Query(Criteria.where("parent_entity").is("Madhava Global"));
        return mongoTemplate.findOne(query, Document.class, "hotelview_details");
    }

    // Fetches Contact Section
    public Document getContactDetails() {
        Query query = new Query(Criteria.where("contact_section").exists(true));
        return mongoTemplate.findOne(query, Document.class, "hotelview_details");
    }

    public Document getPaymentInfo() {
        Query query = new Query(Criteria.where("payment_details").exists(true));
        return mongoTemplate.findOne(query, Document.class, "hotelview_details");
    }

    // ────────────────────────────────────────────────────────────────────
    // ⚡ READ: Caches the system greeting document globally in RAM memory
    // ────────────────────────────────────────────────────────────────────
    @Cacheable(value = "greetingCache", key = "'system-greeting'")
    public Document getGreetingMessage() {
        // Hits MongoDB exactly ONCE. Millions of customer device reads bypass DB completely.
        Query query = new Query(Criteria.where("greeting_config").exists(true));
        return mongoTemplate.findOne(query, Document.class, "hotelview_details");
    }

    // ────────────────────────────────────────────────────────────────────
    // 🛡️ WRITE: Overwrites configuration and atomically flushes old RAM cache
    // ────────────────────────────────────────────────────────────────────
    @CacheEvict(value = "greetingCache", key = "'system-greeting'")
    public Document updateGreetingMessage(Document newGreetingPayload) {
        Query query = new Query(Criteria.where("greeting_config").exists(true));

        Document existingDoc = mongoTemplate.findOne(query, Document.class, "hotelview_details");

        if (existingDoc == null) {
            existingDoc = new Document();
            existingDoc.put("software_name", "HotelView");
        }

        existingDoc.put("greeting_config", newGreetingPayload.get("greeting_config"));

        return mongoTemplate.save(existingDoc, "hotelview_details");
    }
}