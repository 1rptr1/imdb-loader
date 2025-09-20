package com.example.imdb.controller;

import com.example.imdb.service.ImdbService;
import com.example.imdb.service.MovieService;
import com.example.imdb.service.OmdbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    private final ImdbService imdbService;
    private final MovieService movieService;
    private final OmdbClient omdbClient;

    public MovieController(ImdbService imdbService, MovieService movieService, OmdbClient omdbClient) {
        this.imdbService = imdbService;
        this.movieService = movieService;
        this.omdbClient = omdbClient;
    }

    @GetMapping("/top")
    public List<Map<String, Object>> getTopMoviesByActor(
            @RequestParam String actor,
            @RequestParam(defaultValue = "10") int limit) {
        return imdbService.getTopMoviesByActor(actor, limit);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchMoviesByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "10") int limit) {
        return imdbService.searchMoviesByTitle(title, limit);
    }

    @GetMapping("/top-rated")
    public List<Map<String, Object>> getTopRatedMovies(
            @RequestParam(defaultValue = "50") int limit) {
        return imdbService.getTopRatedMovies(limit);
    }

    @GetMapping("/filter")
    public List<Map<String, Object>> filterMovies(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String fromYear,
            @RequestParam(required = false) String toYear,
            @RequestParam(defaultValue = "50") int limit) {
        return imdbService.filterMovies(actor, genre, language, fromYear, toYear, limit);
    }

    @GetMapping("/top-with-plot")
    public List<Map<String, Object>> getTopMoviesWithPlot(
            @RequestParam String actor,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> movies = imdbService.getTopMoviesByActorWithPlot(actor, limit);

        // Process movies safely
        return movies.stream().map(m -> {
            Object tconstObj = m.get("tconst");

            if (tconstObj == null) {
                log.warn("Movie map missing 'tconst': {}", m);
                m.put("plot", "Plot not available");
                m.put("poster", "");
                return m;
            }

            String imdbId = Objects.toString(tconstObj, "");
            Map<String, Object> omdbData = omdbClient.fetchMovieDetails(imdbId);

            if (omdbData != null) {
                m.put("plot", omdbData.getOrDefault("Plot", "Plot not available"));
                m.put("poster", omdbData.getOrDefault("Poster", ""));
            } else {
                m.put("plot", "Plot not available");
                m.put("poster", "");
            }

            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "OK",
                "message", "IMDb API is running 🚀"
        );
    }
}
