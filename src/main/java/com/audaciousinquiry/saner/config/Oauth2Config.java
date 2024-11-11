package com.audaciousinquiry.saner.config;

import com.audaciousinquiry.saner.Utility;
import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;

public record Oauth2Config(
        String tokenUrl,
        String clientId,
        String clientSecret,
        String username,
        String password,
        String scope,
        String credentialMode
) {
    public static Oauth2Config fromAwsSecret(Region region, String secretName) {
        JSONObject secret = Utility.getAwsSecret(region, secretName);

        return new Oauth2Config(
                secret.getString("token-url"),
                secret.getString("client-id"),
                secret.getString("client-secret"),
                secret.getString("username"),
                secret.getString("password"),
                secret.getString("scope"),
                secret.getString("credential-mode")
        );
    }
}
