package com.audaciousinquiry.saner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.audaciousinquiry.saner.config.Oauth2Config;
import com.audaciousinquiry.saner.exceptions.SanerLambdaException;
import com.audaciousinquiry.saner.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ExpungeData implements RequestHandler<Void, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(Void unused, Context context) {
        LambdaLogger logger = context.getLogger();
        String returnValue = "";

        logger.log("ExpungeData Lambda - Started");

        try {
            String secretName = System.getenv("API_AUTH_SECRET");
            Region region = Region.of(System.getenv("AWS_REGION"));
            String expungeApiUrl = System.getenv("API_ENDPOINT");

            Oauth2Config oauth2Config = Oauth2Config.fromAwsSecret(region, secretName);
            logger.log("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2Config);
            logger.log("Access Token Obtained");

            HttpResponse<String> response;
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(expungeApiUrl))
                        .header("Authorization", accessToken.toAuthorizationHeader())
                        .DELETE()
                        .build();

                response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());
            }

            Job job = objectMapper.readValue(response.body(), Job.class);

            logger.log(String.format(
                    "Expunge Call Status: %d, Saner Job ID: %s",
                    response.statusCode(),
                    job.getId()
                    )
            );

            logger.log("ExpungeData Lambda - Completed");
        } catch (URISyntaxException ex) {
            logger.log(
                    String.format("ExpungeData Lambda - URI Syntax Exception: %s", ex.getMessage()), LogLevel.ERROR
            );
            throw new SanerLambdaException(ex.getMessage());
        } catch (IOException ex) {
            logger.log(
                    String.format("ExpungeData Lambda - IO Exception: %s", ex.getMessage()), LogLevel.ERROR
            );
            throw new SanerLambdaException(ex.getMessage());
        } catch (ParseException ex) {
            logger.log(
                    String.format("ExpungeData Lambda - ParseException: %s", ex.getMessage()), LogLevel.ERROR
            );
            throw new SanerLambdaException(ex.getMessage());
        } catch (InterruptedException ex) {
            logger.log(
                    String.format("ExpungeData Lambda - InterruptedException: %s", ex.getMessage()), LogLevel.ERROR
            );
            Thread.currentThread().interrupt();
            throw new SanerLambdaException(ex.getMessage());
        }

        return returnValue;
    }
}
