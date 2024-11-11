package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.audaciousinquiry.saner.Utility;
import com.audaciousinquiry.saner.records.Oauth2;
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

public class ExpungeData implements RequestHandler<Void, Job> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ExpungeData.class);

    @Override
    public Job handleRequest(Void unused, Context context) {
        Job returnValue;

        log.info("ExpungeData Lambda - Started");

        try {
            String secretName = System.getenv("API_AUTH_SECRET");
            Region region = Region.of(System.getenv("AWS_REGION"));
            String apiUrl = System.getenv("API_ENDPOINT");

            Oauth2 oauth2 = Oauth2.fromAwsSecret(region, secretName);
            log.info("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2);
            log.info("Access Token Obtained");

            HttpResponse<String> response;
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .header("Authorization", accessToken.toAuthorizationHeader())
                        .DELETE()
                        .build();

                response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());
            }

            returnValue = objectMapper.readValue(response.body(), Job.class);

            log.info("API Call Status: {}, Saner Job ID: {}",
                    response.statusCode(),
                    returnValue.getId()
            );

            log.info("ExpungeData Lambda - Completed");
        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new SanerLambdaException(ex.getMessage());
        }   catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SanerLambdaException(ex.getMessage());
        }

        return returnValue;
    }
}
