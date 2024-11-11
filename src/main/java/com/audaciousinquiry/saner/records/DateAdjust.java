package com.audaciousinquiry.saner.records;

import java.util.Optional;

public record DateAdjust(
        int adjustDays,
        int adjustMonths,
        boolean adjustEdge
) {
    public static DateAdjust fromEnvironmentStart() {
        int startAdjustDays = Integer.getInteger("START_ADJUST_DAYS", 0);
        int startAdjustMonths = Integer.getInteger("START_ADJUST_MONTHS", 0);
        boolean startDateEdge = Optional.ofNullable(System.getenv("START_DATE_EDGE"))
                .map(Boolean::parseBoolean)
                .orElse(true);

        return new DateAdjust(startAdjustDays, startAdjustMonths, startDateEdge);
    }

    public static DateAdjust fromEnvironmentEnd() {
        int endAdjustDays = Integer.getInteger("END_ADJUST_DAYS", 0);
        int endAdjustMonths = Integer.getInteger("END_ADJUST_MONTHS", 0);
        boolean endDateEdge = Optional.ofNullable(System.getenv("END_DATE_EDGE"))
                .map(Boolean::parseBoolean)
                .orElse(true);

        return new DateAdjust(endAdjustDays, endAdjustMonths, endDateEdge);
    }
}
