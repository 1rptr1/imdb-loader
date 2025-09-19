package com.example.imdb.config;

import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final HikariDataSource dataSource;

    public DatabaseInitializer(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {

            List<TableMeta> tables = List.of(
                    new TableMeta("name_basics", getNameBasicsDDL(), getNameBasicsIndexes()),
                    new TableMeta("title_basics", getTitleBasicsDDL(), getTitleBasicsIndexes()),
                    new TableMeta("title_akas", getTitleAkasDDL(), getTitleAkasIndexes()),
                    new TableMeta("title_principals", getTitlePrincipalsDDL(), getTitlePrincipalsIndexes()),
                    new TableMeta("title_ratings", getTitleRatingsDDL(), getTitleRatingsIndexes()),
                    new TableMeta("title_crew", getTitleCrewDDL(), getTitleCrewIndexes()),
                    new TableMeta("title_episode", getTitleEpisodeDDL(), getTitleEpisodeIndexes())
                   // new TableMeta("title_plot", getTitlePlotDDL(), getTitlePlotIndexes())
            );

            for (TableMeta table : tables) {
                ensureTable(conn, table);

                // Load table only if empty
                try (Statement stmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table.name)) {
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        loadTable(conn, table);
                    } else {
                        System.out.println("Table " + table.name + " already has data.");
                    }
                }

                vacuumAnalyze(conn, table);
            }

            System.out.println("✅ All tables loaded successfully!");
        }
    }

    private void ensureTable(Connection conn, TableMeta table) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + table.name + " (" + table.ddl + ")");
            for (String idx : table.indexes) {
                stmt.execute(idx);
            }
        }
    }

    public void loadTable(Connection conn, TableMeta table) throws IOException, SQLException {
        Path rawFile = Paths.get("E:/database/" + table.name + ".tsv");
        if (!Files.exists(rawFile)) {
            throw new FileNotFoundException("File not found: " + rawFile);
        }

        // Pre-clean file before loading
        Path cleanFile = ImdbFileCleaner.cleanFile(rawFile, table.name);

        String copySql = switch (table.name) {
            case "name_basics" -> """
            COPY name_basics(nconst, primaryName, birthYear, deathYear, primaryProfession, knownForTitles)
            FROM STDIN WITH (FORMAT CSV, DELIMITER E'\\t', NULL '', HEADER true, QUOTE E'\\b')
        """;
            case "title_basics" -> """
            COPY title_basics(tconst, titleType, primaryTitle, originalTitle, isAdult, startYear, endYear, runtimeMinutes, genres)
            FROM STDIN WITH (FORMAT CSV, DELIMITER E'\\t', NULL '', HEADER true, QUOTE E'\\b')
        """;
            case "title_principals" -> """
            COPY title_principals(tconst, ordering, nconst, category, job, characters)
            FROM STDIN WITH (FORMAT CSV, DELIMITER E'\\t', NULL '', HEADER true, QUOTE E'\\b')
        """;
            case "title_ratings" -> """
            COPY title_ratings(tconst, averageRating, numVotes)
            FROM STDIN WITH (FORMAT CSV, DELIMITER E'\\t', NULL '', HEADER true, QUOTE E'\\b')
        """;
            default -> throw new IllegalArgumentException("Unknown table: " + table.name);
            case "title_akas" -> """
        COPY title_akas(titleId, ordering, title, region, language, types, attributes, isOriginalTitle)
        FROM STDIN WITH (FORMAT text, DELIMITER E'\\t', NULL '\\N')
        """;

            case "title_crew" -> """
        COPY title_crew(tconst, directors, writers)
        FROM STDIN WITH (FORMAT text, DELIMITER E'\\t', NULL '\\N')
        """;

            case "title_episode" -> """
        COPY title_episode(tconst, parentTconst, seasonNumber, episodeNumber)
        FROM STDIN WITH (FORMAT text, DELIMITER E'\\t', NULL '\\N')
        """;
        };

        BaseConnection pgConn = conn.unwrap(BaseConnection.class);

        try (BufferedReader reader = Files.newBufferedReader(cleanFile)) {
            CopyManager copyManager = new CopyManager(pgConn);
            long rows = copyManager.copyIn(copySql, reader);
            System.out.println("✅ Loaded " + table.name + " (" + rows + " rows)");
        }
    }


    private void vacuumAnalyze(Connection conn, TableMeta table) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("VACUUM ANALYZE " + table.name);
        }
    }

    // --- Table DDL + Indexes ---
    private String getNameBasicsDDL() {
        return """
                nconst TEXT PRIMARY KEY,
                primaryName TEXT,
                birthYear TEXT,
                deathYear TEXT,
                primaryProfession TEXT,
                knownForTitles TEXT
                """;
    }

    private String[] getNameBasicsIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_name_basics_primaryName ON name_basics(primaryName)"
        };
    }

    private String getTitleBasicsDDL() {
        return """
                tconst TEXT PRIMARY KEY,
                titleType TEXT,
                primaryTitle TEXT,
                originalTitle TEXT,
                isAdult TEXT,
                startYear TEXT,
                endYear TEXT,
                runtimeMinutes TEXT,
                genres TEXT
                """;
    }

    private String[] getTitleBasicsIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_title_basics_primaryTitle ON title_basics(primaryTitle)"
        };
    }

    private String getTitlePrincipalsDDL() {
        return """
                tconst TEXT,
                ordering INTEGER,
                nconst TEXT,
                category TEXT,
                job TEXT,
                characters TEXT
                """;
    }

    private String[] getTitlePrincipalsIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_title_principals_nconst ON title_principals(nconst)"
        };
    }

    private String getTitleRatingsDDL() {
        return """
                tconst TEXT PRIMARY KEY,
                averageRating FLOAT,
                numVotes INTEGER
                """;
    }

    private String[] getTitleRatingsIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_title_ratings_rating ON title_ratings(averageRating DESC)"
        };
    }

    private static class TableMeta {
        String name;
        String ddl;
        String[] indexes;

        public TableMeta(String name, String ddl, String[] indexes) {
            this.name = name;
            this.ddl = ddl;
            this.indexes = indexes;
        }
    }





    private String getTitleAkasDDL() {
        return """
            titleId TEXT,
            ordering INT,
            title TEXT,
            region TEXT,
            language TEXT,
            types TEXT,
            attributes TEXT,
            isOriginalTitle TEXT
        """;
    }
    private String[] getTitleAkasIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_titleAkas_title ON title_akas(title)",
                "CREATE INDEX IF NOT EXISTS idx_titleAkas_language ON title_akas(language)"
        };
    }





    private String getTitleCrewDDL() {
        return """
            tconst TEXT PRIMARY KEY,
            directors TEXT,
            writers TEXT
        """;
    }
    private String[] getTitleCrewIndexes() {
        return new String[0];
    }

    private String getTitleEpisodeDDL() {
        return """
            tconst TEXT PRIMARY KEY,
            parentTconst TEXT,
            seasonNumber TEXT,
            episodeNumber TEXT
        """;
    }
    private String[] getTitleEpisodeIndexes() {
        return new String[0];
    }

    private String getTitlePlotDDL() {
        return """
            tconst TEXT PRIMARY KEY,
            plot TEXT
        """;
    }
    private String[] getTitlePlotIndexes() {
        return new String[]{
                "CREATE INDEX IF NOT EXISTS idx_plot_text ON title_plot USING gin(to_tsvector('english', plot))"
        };
    }

}
