package com.audaciousinquiry.saner;

import com.audaciousinquiry.saner.config.DateAdjustConfig;
import com.audaciousinquiry.saner.config.Oauth2Config;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Utility {
    private Utility() {
        throw new IllegalStateException("Utility class");
    }

    public static JSONObject getAwsSecret(Region region, String secretName) {
        GetSecretValueResponse getSecretValueResponse;
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .httpClient(ApacheHttpClient.create())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        }
        String secret = getSecretValueResponse.secretString();

        return new JSONObject(secret);
    }

    public static AccessToken getOauth2AccessToken(Oauth2Config oauth2Config) throws URISyntaxException, IOException, ParseException {
        TokenRequest request = new TokenRequest(
                new URI(oauth2Config.tokenUrl()),
                new ClientID(oauth2Config.clientId()),
                new ResourceOwnerPasswordCredentialsGrant(oauth2Config.username(), new Secret(oauth2Config.password())),
                new Scope(oauth2Config.scope())
        );

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        return response.toSuccessResponse().getTokens().getAccessToken();
    }

    public static String getPeriodStart(DateAdjustConfig dateAdjustConfig) {
        Calendar calendar = getCalendarForPeriod(dateAdjustConfig.adjustDays(), dateAdjustConfig.adjustMonths());

        if (dateAdjustConfig.adjustEdge()) {
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        }

        return new SimpleDateFormat(Constants.SIMPLE_DATE_MILLIS_FORMAT).format(calendar.getTime());
    }

    public static String getPeriodEnd(DateAdjustConfig dateAdjustConfig) {
        Calendar calendar = getCalendarForPeriod(dateAdjustConfig.adjustDays(), dateAdjustConfig.adjustMonths());

        if (dateAdjustConfig.adjustEdge()) {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        return new SimpleDateFormat(Constants.SIMPLE_DATE_MILLIS_FORMAT).format(calendar.getTime());
    }

    private static Calendar getCalendarForPeriod(int adjustDays, int adjustMonths) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, (adjustDays * 24));
        calendar.add(Calendar.MONTH, adjustMonths);
        return calendar;
    }

}
