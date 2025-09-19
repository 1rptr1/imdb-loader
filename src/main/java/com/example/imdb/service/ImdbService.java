package com.example.imdb.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ImdbService {

    private final JdbcTemplate jdbcTemplate;
    private final OmdbClient omdbClient;
    public ImdbService(JdbcTemplate jdbcTemplate, OmdbClient omdbClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.omdbClient = omdbClient;
    }

    public List<Map<String, Object>> getTopMoviesByActor(String actor, int limit) {
        String sql = """
            SELECT tb.primaryTitle, tb.startYear, tb.genres,
                   nr.primaryName AS actorName,
                   tr.averageRating, tp.plot
            FROM title_basics tb
            JOIN title_principals tp2 ON tb.tconst = tp2.tconst
            JOIN name_basics nr ON tp2.nconst = nr.nconst
            LEFT JOIN title_ratings tr ON tb.tconst = tr.tconst
            LEFT JOIN title_plot tp ON tb.tconst = tp.tconst
            WHERE nr.primaryName ILIKE ?
            ORDER BY tr.averageRating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + actor + "%", limit);
    }

    public List<Map<String, Object>> searchMoviesByTitle(String title, int limit) {
        String sql = """
            SELECT tb.primaryTitle, tb.startYear, tb.genres,
                   tr.averageRating, tp.plot
            FROM title_basics tb
            LEFT JOIN title_ratings tr ON tb.tconst = tr.tconst
            LEFT JOIN title_plot tp ON tb.tconst = tp.tconst
            WHERE tb.primaryTitle ILIKE ?
            ORDER BY tr.averageRating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + title + "%", limit);
    }

    public List<Map<String, Object>> getTopRatedMovies(int limit) {
        String sql = """
            SELECT tb.primaryTitle, tb.startYear, tb.genres,
                   tr.averageRating, tp.plot
            FROM title_basics tb
            JOIN title_ratings tr ON tb.tconst = tr.tconst
            LEFT JOIN title_plot tp ON tb.tconst = tp.tconst
            ORDER BY tr.averageRating DESC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    public List<Map<String, Object>> filterMovies(String actor, String genre,
                                                  String language, String fromYear, String toYear, int limit) {
        String sql = """
            SELECT tb.primaryTitle, tb.startYear, tb.genres,
                   nr.primaryName AS actorName,
                   ta.language, tr.averageRating, tp.plot
            FROM title_basics tb
            JOIN title_principals tp2 ON tb.tconst = tp2.tconst
            JOIN name_basics nr ON tp2.nconst = nr.nconst
            LEFT JOIN title_akas ta ON tb.tconst = ta.titleId
            LEFT JOIN title_ratings tr ON tb.tconst = tr.tconst
            LEFT JOIN title_plot tp ON tb.tconst = tp.tconst
            WHERE (? IS NULL OR nr.primaryName ILIKE ?)
              AND (? IS NULL OR tb.genres ILIKE ?)
              AND (? IS NULL OR ta.language ILIKE ?)
              AND tb.startYear BETWEEN ? AND ?
            ORDER BY tr.averageRating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql,
                actor, actor != null ? "%" + actor + "%" : null,
                genre, genre != null ? "%" + genre + "%" : null,
                language, language != null ? "%" + language + "%" : null,
                fromYear != null ? fromYear : "1900",
                toYear != null ? toYear : "2100",
                limit
        );
    }
    public List<Map<String, Object>> getTopMoviesByActorWithPlot(String actor, int limit) {
        String sql = """
            SELECT tb.tconst, tb.primaryTitle, tb.startYear, tr.averageRating, tr.numVotes
            FROM title_basics tb
            JOIN title_principals tp ON tb.tconst = tp.tconst
            JOIN name_basics nb ON tp.nconst = nb.nconst
            JOIN title_ratings tr ON tb.tconst = tr.tconst
            WHERE nb.primaryName ILIKE ?
            ORDER BY tr.averageRating DESC
            LIMIT ?
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, actor, limit);

        // 🔹 Enrich each result with OMDb plot
        results.forEach(movie -> {
            String imdbId = (String) movie.get("tconst");
            try {
                Map<String, Object> omdbData = omdbClient.fetchMovieDetails(imdbId);
                if (omdbData != null && omdbData.containsKey("Plot")) {
                    movie.put("plot", omdbData.get("Plot"));
                    movie.put("poster", omdbData.get("Poster"));
                }
            } catch (Exception e) {
                movie.put("plot", "Plot not available");
            }
        });

        return results;
    }
}
