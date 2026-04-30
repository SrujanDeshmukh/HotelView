package com.raghunath.hotelview.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ISTUtil {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public static LocalDateTime nowIST() {
        return ZonedDateTime.now(IST).toLocalDateTime();
    }

    public static LocalDateTime nowISTplusDays(long days) {
        return ZonedDateTime.now(IST).plusDays(days).toLocalDateTime();
    }

    public static LocalDateTime nowISTplusMinutes(long minutes) {
        return ZonedDateTime.now(IST).plusMinutes(minutes).toLocalDateTime();
    }

    public static LocalDateTime nowISTplusHours(long hours) {
        return ZonedDateTime.now(IST).plusHours(hours).toLocalDateTime();
    }
}