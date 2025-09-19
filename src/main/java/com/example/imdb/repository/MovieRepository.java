package com.example.imdb.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MovieRepository {

    private final JdbcTemplate jdbcTemplate;

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Find top movies for an actor.
     */
    public List<Map<String, Object>> findTopMoviesByActor(String actor, int limit) {
        String sql = """
            SELECT tb.primarytitle AS title,
                   tr.averagerating AS rating,
                   tr.numvotes AS votes
            FROM title_principals tp
            JOIN name_basics nb ON tp.nconst = nb.nconst
            JOIN title_basics tb ON tp.tconst = tb.tconst
            LEFT JOIN title_ratings tr ON tb.tconst = tr.tconst
            WHERE LOWER(nb.primaryname) LIKE LOWER(?)
              AND tb.titletype = 'movie'
            ORDER BY tr.averagerating DESC NULLS LAST
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, "%" + actor + "%", limit);
    }
}
