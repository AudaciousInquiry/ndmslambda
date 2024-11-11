package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.audaciousinquiry.saner.Utility;
import com.audaciousinquiry.saner.config.Oauth2Config;
import com.audaciousinquiry.saner.exceptions.SanerLambdaException;
import com.audaciousinquiry.saner.model.Job;
import com.audaciousinquiry.saner.models.ScoopDataModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.TimeZone;

public class ScoopData implements RequestHandler<Void, Job> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ScoopData.class);
    public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    @Override
    public Job handleRequest(Void unused, Context context) {
        Job returnValue;

        log.info("ScoopData Lambda - Started");

        try {

            Region region = Region.of(System.getenv("AWS_REGION"));
            String authSecretName = System.getenv("API_AUTH_SECRET");
            String apiUrl = System.getenv("API_ENDPOINT");
            String locationId = System.getenv("LOCATION_ID");
            String measureId = System.getenv("MEASURE_ID");

            int startAdjustDays = Integer.getInteger("START_ADJUST_DAYS", 0);
            int startAdjustMonths = Integer.getInteger("START_ADJUST_MONTHS", 0);
            boolean startDateEdge = Optional.ofNullable(System.getenv("START_DATE_EDGE"))
                    .map(Boolean::parseBoolean)
                    .orElse(true);

            int endAdjustDays = Integer.getInteger("END_ADJUST_DAYS", 0);
            int endAdjustMonths = Integer.getInteger("END_ADJUST_MONTHS", 0);
            boolean endDateEdge = Optional.ofNullable(System.getenv("END_DATE_EDGE"))
                    .map(Boolean::parseBoolean)
                    .orElse(true);

            ScoopDataModel scoopDataCall = new ScoopDataModel(
                    getPeriodStart(startAdjustDays, startAdjustMonths, startDateEdge),
                    getPeriodEnd(endAdjustDays, endAdjustMonths, endDateEdge),
                    locationId,
                    measureId
            );
            String scoopDataBody = objectMapper.writeValueAsString(scoopDataCall);

            Oauth2Config oauth2Config = Oauth2Config.fromAwsSecret(region, authSecretName);
            log.info("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2Config);
            log.info("Access Token Obtained");

            log.info("Calling API to scoop data for Location: {}, Measure: {}", locationId, measureId);

            HttpResponse<String> response;
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .header("Authorization", accessToken.toAuthorizationHeader())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(scoopDataBody))
                        .build();

                response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());
            }
            returnValue = objectMapper.readValue(response.body(), Job.class);

            log.info("API Call Status: {}, Saner Job ID: {}",
                    response.statusCode(),
                    returnValue.getId()
            );

            log.info("ScoopData Lambda - Completed");

        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new SanerLambdaException(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SanerLambdaException(ex.getMessage());
        }

        return returnValue;
    }

    private String getPeriodStart(int adjustDays, int adjustMonths, boolean dateEdge) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, (adjustDays * 24));
        calendar.add(Calendar.MONTH, adjustMonths);

        if (dateEdge) {
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }

        return new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT).format(calendar.getTime());
    }

    private String getPeriodEnd(int adjustDays, int adjustMonths, boolean dateEdge) {
        Calendar calendar = getCalendarForPeriod(adjustDays, adjustMonths);

        if (dateEdge) {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        return new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT).format(calendar.getTime());
    }

    private Calendar getCalendarForPeriod(int adjustDays, int adjustMonths) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, (adjustDays * 24));
        calendar.add(Calendar.MONTH, adjustMonths);
        return calendar;
    }
}
