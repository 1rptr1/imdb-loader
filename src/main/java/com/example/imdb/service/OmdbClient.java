package com.example.imdb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    }private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    public void testLogging() {
        log.info("This is an INFO log");
        log.warn("This is a WARN log");
        log.error("This is an ERROR log");
    }

    /**
     * Fetch plot by IMDb ID (tconst)
     */
    public String fetchPlotById(String imdbId) {
        if (imdbId == null || imdbId.isEmpty()) {
            log.warn("IMDb ID is null or empty");
            return "Plot not available";
        }

        try {
            String url = String.format("https://www.omdbapi.com/?i=%s&apikey=%s&plot=full", imdbId, apiKey);
            log.info("Fetching OMDb plot from URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.info("OMDb response: {}", response);

            if (response != null && "True".equals(response.get("Response"))) {
                return response.getOrDefault("Plot", "Plot not available").toString();
            } else {
                log.warn("OMDb returned no data for IMDb ID: {}", imdbId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch plot for IMDb ID {}: {}", imdbId, e.getMessage());
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
