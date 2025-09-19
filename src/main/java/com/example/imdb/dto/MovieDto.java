package com.example.imdb.dto;

public class MovieDto {
    private String title;
    private double rating;
    private int votes;
    private String plot;

    public MovieDto(String title, double rating, int votes, String plot) {
        this.title = title;
        this.rating = rating;
        this.votes = votes;
        this.plot = plot;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }
    public String getPlot() { return plot; }
    public void setPlot(String plot) { this.plot = plot; }
}
