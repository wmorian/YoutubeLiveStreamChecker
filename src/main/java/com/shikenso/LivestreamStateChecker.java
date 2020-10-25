package com.shikenso;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LivestreamStateChecker {
    private HttpURLConnection connection;
    private BufferedReader reader;

    public void requestState(String channelUrl, ILiveStreamCallback callback) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(channelUrl);
            connection = (HttpURLConnection) url.openConnection();

            // setup request
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            String line;
            if (connection.getResponseCode() > 299)
                callback.run(LivestreamState.ERROR);
            else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                callback.run(getState(result.toString()));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
    }

    private static LivestreamState getState(String json) {
        JSONObject obj = new JSONObject(json);
        JSONObject pageInfo = obj.getJSONObject("pageInfo");
        int totalResult = pageInfo.getInt("totalResults");
        return totalResult == 0 ? LivestreamState.NOT_LIVE : LivestreamState.LIVE;
    }
}