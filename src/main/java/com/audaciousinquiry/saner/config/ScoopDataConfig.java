package com.audaciousinquiry.saner.config;

import com.audaciousinquiry.saner.Utility;

public record ScoopDataConfig(
        String periodStart,
        String periodEnd,
        String locationId,
        String measureId
) {
    public static ScoopDataConfig fromEnvironment() {
        String locationId = System.getenv("LOCATION_ID");
        String measureId = System.getenv("MEASURE_ID");

        return new ScoopDataConfig(
                Utility.getPeriodStart(DateAdjustConfig.fromEnvironmentStart()),
                Utility.getPeriodEnd(DateAdjustConfig.fromEnvironmentEnd()),
                locationId,
                measureId
        );
    }
}
