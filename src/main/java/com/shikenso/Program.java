package com.shikenso;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static String baseUrl;
    private static int rate;
    private static int duration;
    private static Hashtable<String, String> channels = new Hashtable<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        readConfigs();

        // schedule a task for each channel
        channels.forEach((id, name) -> {
            final LivestreamStateChecker livestreamStateChecker = new LivestreamStateChecker();
            Runnable runnable = () -> {
                String channelUrl = String.format("%s&channelId=%s&key=%s", baseUrl, id, key);
                livestreamStateChecker.requestState(channelUrl, state -> {
                    System.out.println(String.format("CHANNEL: %s NAME: %s STATE: %s", id, name, state));
                });
            };

            scheduler.scheduleAtFixedRate(runnable, 0, rate, TimeUnit.SECONDS);
        });

        scheduler.awaitTermination(duration, TimeUnit.SECONDS);
        scheduler.shutdownNow();
        System.out.println("Shutdown complete");
    }

    private static void readConfigs() throws IOException {
        String dir = System.getProperty("user.dir");
        String temp = FileSystems.getDefault().getPath(".").toAbsolutePath().toString();
        String configFile = readFile(dir + "/config/config.json", StandardCharsets.UTF_8);
        JSONObject config = new JSONObject(configFile);
        key = config.getString("key");
        baseUrl = config.getString("url");
        rate = config.getInt("request_rate_in_sec");
        duration = config.getInt("duration_in_sec");

        JSONArray jsonArray = config.getJSONArray("channels");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject channel = jsonArray.getJSONObject(i);
            channels.put(channel.getString("channel"), channel.getString("name"));
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, encoding);
    }
}
