package com.shikenso.youtubeApi;

public class YoutubeApiConfig {
    private String applicationName;
    private String key;

    public YoutubeApiConfig(String applicationName, String key) {
        this.applicationName = applicationName;
        this.key = key;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getKey() {
        return key;
    }
}
