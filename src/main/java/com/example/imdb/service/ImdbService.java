package com.example.imdb.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ArrayUtils.toArray;

@Service
public class ImdbService {

    private final JdbcTemplate jdbcTemplate;
    private final OmdbClient omdbClient;

    public ImdbService(JdbcTemplate jdbcTemplate, OmdbClient omdbClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.omdbClient = omdbClient;
    }

    // 🔹 Utility method to add plot + poster
    private List<Map<String, Object>> enrichWithOmdb(List<Map<String, Object>> movies) {
        movies.forEach(m -> {
            try {
                Object tconst = m.get("tconst");
                if (tconst == null) {
                    m.put("plot", "Plot not available");
                    m.put("poster", "");
                    return;
                }

                String imdbId = tconst.toString();
                Map<String, Object> omdbData = omdbClient.fetchMovieDetails(imdbId);

                if (omdbData != null) {
                    m.put("plot", omdbData.getOrDefault("Plot", "Plot not available"));
                    m.put("poster", omdbData.getOrDefault("Poster", ""));
                } else {
                    m.put("plot", "Plot not available");
                    m.put("poster", "");
                }
            } catch (Exception e) {
                m.put("plot", "Plot not available");
                m.put("poster", "");
            }
        });
        return movies;
    }

    // 🔹 Top movies by actor
    public List<Map<String, Object>> getTopMoviesByActor(String actor, int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres,
                   n.primaryName AS actorName, r.averageRating
            FROM title_basics t
            JOIN title_principals p ON t.tconst = p.tconst
            JOIN name_basics n ON p.nconst = n.nconst
            JOIN title_ratings r ON t.tconst = r.tconst
            WHERE n.primaryName ILIKE ?
            ORDER BY r.averageRating DESC
            LIMIT ?
            """;

        List<Map<String, Object>> movies = jdbcTemplate.queryForList(sql, "%" + actor + "%", limit);
        return enrichWithOmdb(movies);
    }

    // 🔹 Search by movie title
    public List<Map<String, Object>> searchMoviesByTitle(String title, int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres, r.averageRating
            FROM title_basics t
            JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.primaryTitle ILIKE ?
            ORDER BY r.averageRating DESC
            LIMIT ?
            """;

        List<Map<String, Object>> movies = jdbcTemplate.queryForList(sql, "%" + title + "%", limit);
        return enrichWithOmdb(movies);
    }

    // 🔹 Top rated movies
    public List<Map<String, Object>> getTopRatedMovies(int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres, r.averageRating
            FROM title_basics t
            JOIN title_ratings r ON t.tconst = r.tconst
            ORDER BY r.averageRating DESC
            LIMIT ?
            """;

        List<Map<String, Object>> movies = jdbcTemplate.queryForList(sql, limit);
        return enrichWithOmdb(movies);
    }

    // 🔹 Filter movies
    public List<Map<String, Object>> filterMovies(String actor,
                                                  String genre,
                                                  String language,
                                                  String fromYear,
                                                  String toYear,
                                                  int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres, r.averageRating
            FROM title_basics t
            JOIN title_ratings r ON t.tconst = r.tconst
            """);

        if (actor != null && !actor.isEmpty()) {
            sql.append("JOIN title_principals p ON t.tconst = p.tconst ")
                    .append("JOIN name_basics n ON p.nconst = n.nconst ");
        }

        sql.append("WHERE 1=1 ");

        new Object() {
            void addCondition(StringBuilder s, String column, Object value) {
                if (value != null && !value.toString().isEmpty()) {
                    s.append("AND ").append(column).append(" ILIKE ? ");
                }
            }
        }.addCondition(sql, "t.genres", genre);

        if (actor != null && !actor.isEmpty()) {
            sql.append("AND n.primaryName ILIKE ? ");
        }
        if (language != null && !language.isEmpty()) {
            sql.append("AND t.languages ILIKE ? ");
        }
        if (fromYear != null && !fromYear.isEmpty()) {
            sql.append("AND t.startYear >= ? ");
        }
        if (toYear != null && !toYear.isEmpty()) {
            sql.append("AND t.startYear <= ? ");
        }

        sql.append("ORDER BY r.averageRating DESC LIMIT ?");

        // Collect parameters in order
        new java.util.ArrayList<Object>() {{
            if (genre != null && !genre.isEmpty()) add("%" + genre + "%");
            if (actor != null && !actor.isEmpty()) add("%" + actor + "%");
            if (language != null && !language.isEmpty()) add("%" + language + "%");
            if (fromYear != null && !fromYear.isEmpty()) add(fromYear);
            if (toYear != null && !toYear.isEmpty()) add(toYear);
            add(limit);
        }};

        Object[] params = toArray();

        List<Map<String, Object>> movies = jdbcTemplate.queryForList(sql.toString(), params);
        return enrichWithOmdb(movies);
    }
    public List<Map<String, Object>> getTopMoviesByActorWithPlot(String actor, int limit) {
        String sql = """
               SELECT t.tconst, t.primaryTitle, t.startYear, t.genres,
                                                                                                                        n.primaryName AS actorName, r.averageRating
                                                                                                                 FROM title_basics t
                                                                                                                 JOIN title_principals p ON t.tconst = p.tconst
                                                                                                                 JOIN name_basics n ON p.nconst = n.nconst
                                                                                                                 JOIN title_ratings r ON t.tconst = r.tconst
                                                                                                                 WHERE n.primaryName ILIKE ?
                                                                                                                 ORDER BY r.averageRating DESC
                                                                                                                 LIMIT ?
                
                
        """;
        String actorParam = "%" + actor + "%";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, actorParam, limit);

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
