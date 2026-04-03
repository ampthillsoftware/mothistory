package com.ampsoft.MOTHistory.data.api;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("expires_in")
    private long expiresInSeconds;

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
