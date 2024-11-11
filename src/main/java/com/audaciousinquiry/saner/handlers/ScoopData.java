package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.audaciousinquiry.saner.Utility;
import com.audaciousinquiry.saner.config.Oauth2Config;
import com.audaciousinquiry.saner.config.ScoopDataConfig;
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

public class ScoopData implements RequestHandler<Void, Job> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ScoopData.class);

    @Override
    public Job handleRequest(Void unused, Context context) {
        Job returnValue;

        log.info("ScoopData Lambda - Started");

        try {

            Region region = Region.of(System.getenv("AWS_REGION"));
            String authSecretName = System.getenv("API_AUTH_SECRET");
            String apiUrl = System.getenv("API_ENDPOINT");

            ScoopDataConfig config = ScoopDataConfig.fromEnvironment();

            String scoopDataBody = objectMapper.writeValueAsString(config);

            Oauth2Config oauth2Config = Oauth2Config.fromAwsSecret(region, authSecretName);
            log.info("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2Config);
            log.info("Access Token Obtained");

            log.info("Calling API to scoop data for Location: {}, Measure: {}", config.locationId(), config.measureId());

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
}
