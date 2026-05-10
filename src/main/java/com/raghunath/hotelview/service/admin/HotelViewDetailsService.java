package com.raghunath.hotelview.service.admin;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
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
}