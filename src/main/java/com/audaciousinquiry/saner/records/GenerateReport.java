package com.audaciousinquiry.saner.records;

import com.audaciousinquiry.saner.Utility;

import java.util.Optional;

public record GenerateReport(
        String periodStart,
        String periodEnd,
        String locationId,
        String measureId,
        boolean regenerate
) {
    public static GenerateReport fromEnvironment() {
        String locationId = System.getenv("LOCATION_ID");
        String measureId = System.getenv("MEASURE_ID");
        boolean regenerate = Optional.ofNullable(System.getenv("REGENERATE"))
                .map(Boolean::parseBoolean)
                .orElse(true);

        return new GenerateReport(
                Utility.getPeriodStart(DateAdjust.fromEnvironmentStart()),
                Utility.getPeriodEnd(DateAdjust.fromEnvironmentEnd()),
                locationId,
                measureId,
                regenerate
        );
    }
}
