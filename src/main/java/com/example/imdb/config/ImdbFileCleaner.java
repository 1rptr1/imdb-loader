package com.example.imdb.config;

import java.io.*;
import java.nio.file.*;

public class ImdbFileCleaner {

    public static Path cleanFile(Path inputFile, String tableName) throws IOException {
        Path cleanedFile = Paths.get(inputFile.toString() + ".cleaned");

        int expectedCols = switch (tableName) {
            case "name_basics" -> 6;
            case "title_basics" -> 9;
            case "title_principals" -> 6;
            case "title_ratings" -> 3;
            case "title_akas" -> 8;
            case "title_crew" -> 3;
            case "title_episode" -> 4;
            default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };

        try (BufferedReader reader = Files.newBufferedReader(inputFile);
             BufferedWriter writer = Files.newBufferedWriter(cleanedFile)) {

            String line;
            boolean first = true; // flag to detect header

            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false; // skip header
                    continue;
                }

                String[] cols = line.split("\t", -1);
                if (cols.length == expectedCols) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        return cleanedFile;
    }


}
