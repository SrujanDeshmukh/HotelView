package com.raghunath.hotelview.service.admin;

public class HotelViewDetails {
    private String termsAndConditions;
    private String contactUs;
    private String aboutUs;

    public HotelViewDetails(String termsAndConditions, String contactUs, String aboutUs) {
        this.termsAndConditions = termsAndConditions;
        this.contactUs = contactUs;
        this.aboutUs = aboutUs;
    }

    // Getters
    public String getTermsAndConditions() { return termsAndConditions; }
    public String getContactUs() { return contactUs; }
    public String getAboutUs() { return aboutUs; }
}