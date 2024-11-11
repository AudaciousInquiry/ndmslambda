package com.audaciousinquiry.saner.records;

import com.audaciousinquiry.saner.Utility;

public record ScoopData(
        String periodStart,
        String periodEnd,
        String locationId,
        String measureId
) {
    public static ScoopData fromEnvironment() {
        String locationId = System.getenv("LOCATION_ID");
        String measureId = System.getenv("MEASURE_ID");

        return new ScoopData(
                Utility.getPeriodStart(DateAdjust.fromEnvironmentStart()),
                Utility.getPeriodEnd(DateAdjust.fromEnvironmentEnd()),
                locationId,
                measureId
        );
    }
}
