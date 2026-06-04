package com.raghunath.hotelview.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching // 👈 Crucial: Activates Spring's caching engine globally across your system
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // Explicitly register your high-traffic isolation memory buckets
        cacheManager.setCacheNames(Arrays.asList("menuCache", "menuSummaryCache","greetingCache","orderCache"));

        return cacheManager;
    }
}