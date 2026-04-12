package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class SalesAggregationRepository {

    private final MongoTemplate mongoTemplate;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ✅ TODAY - uses checkoutDate + checkoutTime strings
    public SalesAnalyticsDTO getTodayAnalytics(String hotelId) {
        String todayDate = LocalDate.now(IST).toString();

        Aggregation aggregation = newAggregation(
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutDate").is(todayDate)),

                project("totalPayable")
                        .andExpression("toInt(substr(checkoutTime, 0, 2)) + 1").as("ceilHour"),

                group("ceilHour")
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

    // ✅ WEEK - uses checkoutDate string range
    public SalesAnalyticsDTO getWeekAnalytics(String hotelId) {
        LocalDate todayIST = LocalDate.now(IST);
        // Get Monday of current week
        LocalDate monday = todayIST.with(java.time.DayOfWeek.MONDAY);

        // Build list of dates from Monday to today
        List<String> dateRange = monday.datesUntil(todayIST.plusDays(1))
                .map(LocalDate::toString)
                .toList();

        Aggregation aggregation = newAggregation(
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutDate").in(dateRange)),

                project("totalPayable", "checkoutDate"),

                group("checkoutDate")
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

            // Convert "2026-04-11" to "SATURDAY"
            String dateStr = idValue.toString();
            String dayName = LocalDate.parse(dateStr).getDayOfWeek().name();

            double rev = ((Number) doc.get("revenue")).doubleValue();
            long ord = ((Number) doc.get("orders")).longValue();
            chartData.put(dayName, new SalesAnalyticsDTO.PeriodStats(rev, ord));
            grandTotalRevenue += rev;
            grandTotalOrders += ord;
        }

        return SalesAnalyticsDTO.builder()
                .totalRevenue(grandTotalRevenue)
                .totalOrders(grandTotalOrders)
                .chartData(chartData)
                .build();
    }

    // ✅ MONTH - uses checkoutDate string range
    public SalesAnalyticsDTO getMonthAnalytics(String hotelId) {
        LocalDate todayIST = LocalDate.now(IST);
        LocalDate firstOfMonth = todayIST.withDayOfMonth(1);

        List<String> dateRange = firstOfMonth.datesUntil(todayIST.plusDays(1))
                .map(LocalDate::toString)
                .toList();

        Aggregation aggregation = newAggregation(
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutDate").in(dateRange)),

                project("totalPayable", "checkoutDate"),

                group("checkoutDate")
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
            // Key is "2026-04-11" format
            String key = idValue.toString();
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

    // ✅ YEAR - uses checkoutDate string range
    public SalesAnalyticsDTO getYearAnalytics(String hotelId) {
        LocalDate todayIST = LocalDate.now(IST);
        LocalDate firstOfYear = todayIST.withDayOfYear(1);

        Aggregation aggregation = newAggregation(
                match(Criteria.where("hotelId").is(hotelId)
                        .and("checkoutDate").gte(firstOfYear.toString())
                        .lte(todayIST.toString())),

                project("totalPayable")
                        .andExpression("substr(checkoutDate, 0, 7)").as("yearMonth"),

                group("yearMonth")
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
            // Convert "2026-04" to "APRIL"
            String yearMonth = idValue.toString();
            int monthNum = Integer.parseInt(yearMonth.split("-")[1]);
            String monthName = java.time.Month.of(monthNum).name();

            double rev = ((Number) doc.get("revenue")).doubleValue();
            long ord = ((Number) doc.get("orders")).longValue();
            chartData.put(monthName, new SalesAnalyticsDTO.PeriodStats(rev, ord));
            grandTotalRevenue += rev;
            grandTotalOrders += ord;
        }

        return SalesAnalyticsDTO.builder()
                .totalRevenue(grandTotalRevenue)
                .totalOrders(grandTotalOrders)
                .chartData(chartData)
                .build();
    }
}