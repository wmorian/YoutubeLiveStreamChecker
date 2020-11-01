package com.shikenso.youtubeApi;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YoutubeDataService {
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private YouTube youtube;
    private long maxResults;
    private YoutubeApiConfig config;
    private NetHttpTransport httpTransport;

    public YoutubeDataService(YoutubeApiConfig config) throws GeneralSecurityException, IOException {
        this(config, 50);
    }

    public YoutubeDataService(YoutubeApiConfig config, long maxResults) throws GeneralSecurityException, IOException {
        this.config = config;
        this.maxResults = maxResults;
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.youtube = new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                .setApplicationName(this.config.getApplicationName())
                .build();
    }

    public List<Video> getLiveStreams(Iterator<String> channelIDs, String eventType) throws IOException {
        List<SearchResult> searchResults = new ArrayList<>();
        while (channelIDs.hasNext()) {
            searchResults.addAll(this.searchUpcomingLiveStreams(channelIDs.next(), eventType));
        }

        if (searchResults.isEmpty())
            return null;

        List<String> videoIDs = searchResults.stream()
                .map(r -> r.getId().getVideoId())
                .collect(Collectors.toList());

        List<Video> videos = this.getVideos(videoIDs);
        videos.sort(Comparator.comparing(video -> video.getLiveStreamingDetails().getScheduledStartTime().getValue()));
        return videos;
    }

    private List<SearchResult> searchUpcomingLiveStreams(String channelId, String eventType) throws IOException {
        YouTube.Search.List search = youtube.search().list("id,snippet");
        search.setChannelId(channelId);
        search.setEventType(eventType);
        search.setType("video");
        search.setKey(this.config.getKey());
        search.setMaxResults(this.maxResults);

        String nextPageToken = "";
        SearchListResponse response;
        List<SearchResult> result = new ArrayList<>();
        do {
            search.setPageToken(nextPageToken);
            response = search.execute();
            nextPageToken = response.getNextPageToken();
            result.addAll(response.getItems());
        } while (nextPageToken != null);

        return result;
    }

    private List<Video> getVideos(List<String> videoIDs) throws IOException {
        YouTube.Videos.List videos =  youtube.videos().list("liveStreamingDetails,contentDetails,snippet");
        videos.setKey(this.config.getKey());
        videos.setId(String.join(",", videoIDs));
        videos.setMaxResults(this.maxResults);

        String nextPageToken = "";
        VideoListResponse response;
        List<Video> result = new ArrayList<>();
        do {
            videos.setPageToken(nextPageToken);
            response = videos.execute();
            nextPageToken = response.getNextPageToken();
            result.addAll(response.getItems());
        } while (nextPageToken != null);

        return result;
    }
}
