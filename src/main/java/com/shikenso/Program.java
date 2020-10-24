package com.shikenso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.charset.Charset;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Program {
    private static String key;
    private static String url;
    private static int rate;
    private static int duration;
    private static Hashtable<String, String> channels = new Hashtable<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        readConfigs();

        // schedule a task for each channel
        channels.forEach((id, name) -> {
            final LiveStreamChecker checkLifeStream = new LiveStreamChecker();
            checkLifeStream.init(url, name, id, key);
            scheduler.scheduleAtFixedRate(checkLifeStream, 0, rate, TimeUnit.SECONDS);
        });

        scheduler.awaitTermination(duration, TimeUnit.SECONDS);
        scheduler.shutdownNow();
        System.out.println("Shutdown complete");
    }

    private static void readConfigs() throws IOException {
        String dir = System.getProperty("user.dir");
        String configFile = readFile(dir + "/config/config.json", StandardCharsets.UTF_8);
        JSONObject config = new JSONObject(configFile);
        key = config.getString("key");
        url = config.getString("url");
        rate = config.getInt("request_rate_in_sec");
        duration = config.getInt("duration_in_sec");

        JSONArray jsonArray = config.getJSONArray("channels");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject channel = jsonArray.getJSONObject(i);
            channels.put(channel.getString("channel"), channel.getString("name"));
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, encoding);
    }

    static class LiveStreamChecker implements Runnable {
        private String channelName;
        private String channelId;
        private String channelUrl;
        private HttpURLConnection connection;

        public void init(String url, String channelName, String channelId, String publicKey) {
            this.channelName = channelName;
            this.channelId = channelId;
            this.channelUrl = String.format("%s&channelId=%s&key=%s", url, channelId, publicKey);
        }

        @Override
        public void run() {
            StringBuilder result = new StringBuilder();
            BufferedReader reader;
            try {
                URL url = new URL(channelUrl);
                connection = (HttpURLConnection) url.openConnection();

                // setup request
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                String line;
                if (connection.getResponseCode() > 299) {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                    String errorMessage = getErrorMessage(result.toString());
                    System.out.println(String.format("CHANNEL: %s ERROR: %s", channelId, errorMessage));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();
                    String state = getState(result.toString());
                    System.out.println(String.format("CHANNEL: %s NAME: %s STATE: %s", channelId, channelName, state));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }

        private static String getState(String json) {
            JSONObject obj = new JSONObject(json);
            JSONObject pageInfo = obj.getJSONObject("pageInfo");
            int totalResult = pageInfo.getInt("totalResults");
            return totalResult == 0 ? "not live" : "live";
        }

        private static String getErrorMessage(String json) {
            JSONObject obj = new JSONObject(json);
            JSONObject error = obj.getJSONObject("error");
            return error.getString("message");
        }
    }
}
