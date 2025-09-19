package com.example.imdb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class OmdbClient {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public OmdbClient(RestTemplate restTemplate, @Value("${omdb.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Fetch plot by IMDb ID (tconst)
     */
    public String fetchPlotById(String imdbId) {
        try {
            String url = String.format("https://www.omdbapi.com/?i=%s&apikey=%s&plot=full", imdbId, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && "True".equals(response.get("Response")) && response.containsKey("Plot")) {
                return response.get("Plot").toString();
            } else {
                System.out.println("OMDb response error for " + imdbId + ": " + response);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to fetch plot for IMDb ID " + imdbId + ": " + e.getMessage());
        }
        return "Plot not available";
    }
    public Map<String, Object> fetchMovieDetails(String imdbId) {
        try {
            String url = String.format("https://www.omdbapi.com/?i=%s&apikey=%s&plot=full", imdbId, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && "True".equals(response.get("Response"))) {
                return response;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch details for " + imdbId + ": " + e.getMessage());
        }
        return null;
    }

}
