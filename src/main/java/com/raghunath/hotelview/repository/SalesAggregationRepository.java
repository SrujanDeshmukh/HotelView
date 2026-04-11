package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class SalesAggregationRepository {

    private final MongoTemplate mongoTemplate;

    // ✅ NEW: Today's analytics using checkoutDate + checkoutTime string fields
    public SalesAnalyticsDTO getTodayAnalytics(String hotelId) {
        String todayDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();

        Aggregation aggregation = newAggregation(
                // 1. Filter by hotelId and today's date string
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutDate").is(todayDate)),

                // 2. Extract hour from checkoutTime string "HH:mm:ss"
                // Add 1 for ceiling: 20:44 → hour=20 → ceilHour=21
                project("totalPayable")
                        .andExpression("toInt(substr(checkoutTime, 0, 2)) + 1").as("ceilHour"),

                // 3. Group by ceiling hour
                group("ceilHour")
                        .sum("totalPayable").as("revenue")
                        .count().as("orders"),

                // 4. Sort ascending
                sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(
                aggregation, "completed_orders", org.bson.Document.class);

        Map<String, SalesAnalyticsDTO.PeriodStats> chartData = new LinkedHashMap<>();
        double grandTotalRevenue = 0.0;
        long grandTotalOrders = 0L;

        for (org.bson.Document doc : results.getMappedResults()) {
            Object idValue = doc.get("_id");
            if (idValue == null) continue;

            String key = String.valueOf(((Number) idValue).intValue());
            double rev = ((Number) doc.get("revenue")).doubleValue();
            long ord = ((Number) doc.get("orders")).longValue();

            chartData.put(key, new SalesAnalyticsDTO.PeriodStats(rev, ord));
            grandTotalRevenue += rev;
            grandTotalOrders += ord;
        }

        return SalesAnalyticsDTO.builder()
                .totalRevenue(grandTotalRevenue)
                .totalOrders(grandTotalOrders)
                .chartData(chartData)
                .build();
    }

    // ✅ UNCHANGED: Week/Month/Year analytics
    public SalesAnalyticsDTO getSalesAnalytics(
            String hotelId,
            Instant start,
            Instant end,
            String groupType) {

        DateOperators.Timezone timezone = DateOperators.Timezone.valueOf("Asia/Kolkata");

        Aggregation aggregation = newAggregation(
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutAt").gte(start).lte(end)),

                project("totalPayable")
                        .and(DateOperators.Hour.hourOf("checkoutAt")
                                .withTimezone(timezone)).as("hour")
                        .and(DateOperators.DayOfMonth.dayOfMonth("checkoutAt")
                                .withTimezone(timezone)).as("dayOfMonth")
                        .and(DateOperators.Month.monthOf("checkoutAt")
                                .withTimezone(timezone)).as("month")
                        .and(DateOperators.DayOfWeek.dayOfWeek("checkoutAt")
                                .withTimezone(timezone)).as("dayOfWeek"),

                group(groupType)
                        .sum("totalPayable").as("revenue")
                        .count().as("orders"),

                sort(org.springframework.data.domain.Sort.Direction.ASC, "_id")
        );

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(
                aggregation, "completed_orders", org.bson.Document.class);

        Map<String, SalesAnalyticsDTO.PeriodStats> chartData = new LinkedHashMap<>();
        double grandTotalRevenue = 0.0;
        long grandTotalOrders = 0L;

        for (org.bson.Document doc : results.getMappedResults()) {
            Object idValue = doc.get("_id");
            if (idValue == null) continue;

            String key = formatKey(idValue, groupType);
            double rev = ((Number) doc.get("revenue")).doubleValue();
            long ord = ((Number) doc.get("orders")).longValue();

            chartData.put(key, new SalesAnalyticsDTO.PeriodStats(rev, ord));
            grandTotalRevenue += rev;
            grandTotalOrders += ord;
        }

        return SalesAnalyticsDTO.builder()
                .totalRevenue(grandTotalRevenue)
                .totalOrders(grandTotalOrders)
                .chartData(chartData)
                .build();
    }

    private String formatKey(Object idValue, String groupType) {
        int val = ((Number) idValue).intValue();
        return switch (groupType) {
            case "hour" -> String.valueOf(val);
            case "dayOfWeek" -> switch (val) {
                case 1 -> "SUNDAY";
                case 2 -> "MONDAY";
                case 3 -> "TUESDAY";
                case 4 -> "WEDNESDAY";
                case 5 -> "THURSDAY";
                case 6 -> "FRIDAY";
                case 7 -> "SATURDAY";
                default -> String.valueOf(val);
            };
            case "month" -> switch (val) {
                case 1 -> "JANUARY";
                case 2 -> "FEBRUARY";
                case 3 -> "MARCH";
                case 4 -> "APRIL";
                case 5 -> "MAY";
                case 6 -> "JUNE";
                case 7 -> "JULY";
                case 8 -> "AUGUST";
                case 9 -> "SEPTEMBER";
                case 10 -> "OCTOBER";
                case 11 -> "NOVEMBER";
                case 12 -> "DECEMBER";
                default -> String.valueOf(val);
            };
            default -> String.valueOf(val);
        };
    }
}