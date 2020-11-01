package com.shikenso;

import com.google.api.services.youtube.model.Video;
import com.shikenso.youtubeApi.YoutubeApiConfig;
import com.shikenso.youtubeApi.YoutubeDataService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.charset.Charset;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Hashtable;
import java.util.Iterator;

public class Program {
    private static String key;
    private static String baseUrl;
    private static int rate;
    private static int duration;
    private static Hashtable<String, String> channels = new Hashtable<>();

    public static void main(String[] args) throws InterruptedException, IOException, GeneralSecurityException {
        readConfigs();

        YoutubeDataService ytService = new YoutubeDataService(
                new YoutubeApiConfig("YoutubeLivestream", key));

        Iterator<Video> lives = ytService.getLiveStreams(channels.keySet().iterator(), "live").iterator();
        Iterator<Video> upcomings = ytService.getLiveStreams(channels.keySet().iterator(), "upcoming").iterator();

        System.out.println("==================================== LIVE ====================================");
        System.out.printf("%-30s %-30s %s%n","Start Time", "Channel ID", "Title");
        while (lives.hasNext()) {
            Video video = lives.next();
            String scheduledTime = video.getLiveStreamingDetails().getActualStartTime().toString();
            String videoTitle = video.getSnippet().getTitle();
            String channelID = video.getSnippet().getChannelId();

            System.out.printf("%-30s %-30s %s%n", scheduledTime, channelID, videoTitle);
        }

        System.out.println();
        System.out.println("==================================== UPCOMINGS ====================================");
        System.out.printf("%-30s %-30s %s%n","Start Time", "Channel ID", "Title");
        while (upcomings.hasNext()) {
            Video video = upcomings.next();
            String scheduledTime = video.getLiveStreamingDetails().getScheduledStartTime().toString();
            String videoTitle = video.getSnippet().getTitle();
            String channelID = video.getSnippet().getChannelId();

            System.out.printf("%-30s %-30s %s%n", scheduledTime, channelID, videoTitle);
        }

        // POLLING
        // schedule a task for each channel
//        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        channels.forEach((id, name) -> {
//            final LivestreamStateChecker livestreamStateChecker = new LivestreamStateChecker();
//            Runnable runnable = () -> {
//                String channelUrl = String.format("%s&channelId=%s&key=%s", baseUrl, id, key);
//                livestreamStateChecker.requestState(channelUrl, state -> {
//                    System.out.println(String.format("CHANNEL: %s NAME: %s STATE: %s", id, name, state));
//                });
//            };
//
//            scheduler.scheduleAtFixedRate(runnable, 0, rate, TimeUnit.SECONDS);
//        });
//
//        scheduler.awaitTermination(duration, TimeUnit.SECONDS);
//        scheduler.shutdownNow();
//        System.out.println("Shutdown complete");
    }

    private static void readConfigs() throws IOException {
        String dir = System.getProperty("user.dir");
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
