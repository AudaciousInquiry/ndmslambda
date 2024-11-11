package com.audaciousinquiry.saner;

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
}
