package com.audaciousinquiry.saner.models;

import lombok.Getter;

@Getter
public class ScoopDataModel {
    private final String periodStart;
    private final String periodEnd;
    private final String locationId;
    private final String measureId;

    public ScoopDataModel(String periodStart, String periodEnd, String locationId, String measureId) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.locationId = locationId;
        this.measureId = measureId;
    }
}
