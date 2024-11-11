package com.audaciousinquiry.saner.config;

import com.audaciousinquiry.saner.Utility;

import java.util.Optional;

public record GenerateReportConfig(
        String periodStart,
        String periodEnd,
        String locationId,
        String measureId,
        boolean regenerate
) {
    public static GenerateReportConfig fromEnvironment() {
        String locationId = System.getenv("LOCATION_ID");
        String measureId = System.getenv("MEASURE_ID");
        boolean regenerate = Optional.ofNullable(System.getenv("REGENERATE"))
                .map(Boolean::parseBoolean)
                .orElse(true);

        return new GenerateReportConfig(
                Utility.getPeriodStart(DateAdjustConfig.fromEnvironmentStart()),
                Utility.getPeriodEnd(DateAdjustConfig.fromEnvironmentEnd()),
                locationId,
                measureId,
                regenerate
        );
    }
}
