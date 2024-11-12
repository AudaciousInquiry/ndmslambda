package com.audaciousinquiry.saner;

public class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String LOCATION_ID_PLACEHOLDER = "{locationId}";
    public static final String PUBLISH_TYPE_PLACEHOLDER = "{publishType}";
}
