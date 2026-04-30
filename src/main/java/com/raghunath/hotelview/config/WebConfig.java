package com.raghunath.hotelview.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the URL path to the physical folder
        registry.addResourceHandler("/receipts/**")
                .addResourceLocations("file:src/main/resources/static/receipts/");
    }
}