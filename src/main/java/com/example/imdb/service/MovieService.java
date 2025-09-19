package com.example.imdb.service;

import com.example.imdb.dto.MovieDto;
import com.example.imdb.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final OmdbClient omdbClient;
    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    public void testLogging() {
        log.info("This is an INFO log");
        log.warn("This is a WARN log");
        log.error("This is an ERROR log");
    }
    public MovieService(MovieRepository movieRepository, OmdbClient omdbClient) {
        this.movieRepository = movieRepository;
        this.omdbClient = omdbClient;
    }

    /**
     * Returns top movies by actor with plots fetched from OMDb.
     */
    public List<MovieDto> getTopMoviesWithPlot(String actor, int limit) {
        log.info("Fetching top {} movies for actor: {}", limit, actor);

        return movieRepository.findTopMoviesByActor(actor, limit)
                .stream()
                .map(row -> {
                    String title = (String) row.get("title");
                    double rating = (double) row.getOrDefault("rating", 0.0);
                    int votes = ((Number) row.getOrDefault("votes", 0)).intValue();
                    String imdbId = (String) row.get("tconst");

                    log.info("Processing movie: {} ({})", title, imdbId);

                    String plot = omdbClient.fetchPlotById(imdbId);

                    return new MovieDto(title, rating, votes, plot);
                })
                .collect(Collectors.toList());
    }

}
