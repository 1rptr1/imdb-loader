package com.example.imdb.service;

import com.example.imdb.dto.MovieDto;
import com.example.imdb.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final OmdbClient omdbClient;

    public MovieService(MovieRepository movieRepository, OmdbClient omdbClient) {
        this.movieRepository = movieRepository;
        this.omdbClient = omdbClient;
    }

    /**
     * Returns top movies by actor with plots fetched from OMDb.
     */
    public List<MovieDto> getTopMoviesWithPlot(String actor, int limit) {
        return movieRepository.findTopMoviesByActor(actor, limit)
                .stream()
                .map(row -> {
                    // Ensure keys match your repository query
                    String title = (String) row.get("primarytitle"); // adjust if your key differs
                    String imdbId = (String) row.get("tconst");
                    double rating = ((Number) row.getOrDefault("averagerating", 0)).doubleValue();
                    int votes = ((Number) row.getOrDefault("numvotes", 0)).intValue();

                    System.out.println("Fetching plot for: " + title + " (" + imdbId + ")");

                    String plot = omdbClient.fetchPlotById(imdbId);
                    System.out.println("Plot fetched: " + plot);

                    return new MovieDto(title, rating, votes, plot);
                })
                .collect(Collectors.toList());
    }
}
