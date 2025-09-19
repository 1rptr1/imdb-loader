package com.example.imdb.controller;

import com.example.imdb.service.ImdbService;
import com.example.imdb.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final ImdbService imdbService;
    private final MovieService movieService;
    public MovieController(ImdbService imdbService, MovieService movieService) {
        this.imdbService = imdbService;
        this.movieService = movieService;
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
    public List<Map<String, Object>> getTopMoviesWithPlot(@RequestParam String actor,
                                                          @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> movies = imdbService.getTopMoviesByActor(actor, limit);
        movies.forEach(m -> m.put("plot", movieService.getTopMoviesWithPlot(m.get("primaryTitle").toString(),1)));
        return movies;
    }


    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of("status", "OK", "message", "IMDb API is running 🚀");
    }
}
