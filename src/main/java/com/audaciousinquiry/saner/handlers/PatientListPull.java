package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.audaciousinquiry.saner.Utility;
import com.audaciousinquiry.saner.config.Oauth2Config;
import com.audaciousinquiry.saner.exceptions.SanerLambdaException;
import com.audaciousinquiry.saner.models.Job;
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
import java.time.Duration;

public class PatientListPull implements RequestHandler<Void, Job> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(PatientListPull.class);
    private static final String LOCATION_ID_PLACEHOLDER = "{locationId}";

    @Override
    public Job handleRequest(Void unused, Context context) {
        Job returnValue;

        log.info("PatientListPull Lambda - Started");

        try {

            Region region = Region.of(System.getenv("AWS_REGION"));
            String secretName = System.getenv("API_AUTH_SECRET");
            String baseUrl = System.getenv("API_ENDPOINT");
            String locationId = System.getenv("LOCATION_ID");
            String apiUrl = buildUrl(baseUrl, locationId);

            Oauth2Config oauth2Config = Oauth2Config.fromAwsSecret(region, secretName);
            log.info("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2Config);
            log.info("Access Token Obtained");

            log.info("Calling API to pull patient list for Location: {}", locationId);

            HttpResponse<String> response;
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .header("Authorization", accessToken.toAuthorizationHeader())
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());
            }
            returnValue = objectMapper.readValue(response.body(), Job.class);

            log.info("API Call Status: {}, Saner Job ID: {}",
                    response.statusCode(),
                    returnValue.getId()
            );

            log.info("PatientListPull Lambda - Completed");

        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new SanerLambdaException(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SanerLambdaException(ex.getMessage());
        }

        return returnValue;
    }

    private String buildUrl(String urlTemplate, String replacementValue) throws IllegalArgumentException {
        if (urlTemplate == null || replacementValue == null) {
            throw new IllegalArgumentException("UrlTemplate and ReplacementValue must not be null");
        }

        if (!urlTemplate.contains(LOCATION_ID_PLACEHOLDER)) {
            throw new IllegalArgumentException("UrlTemplate must contain '" + LOCATION_ID_PLACEHOLDER + "'");
        }

        return urlTemplate.replace(LOCATION_ID_PLACEHOLDER, replacementValue);
    }
}
