package ru.practicum.moviehub.model;

import java.util.Objects;
import java.util.UUID;

public class Movie {

    private final UUID id;
    private final String title;
    private final int year;

    public Movie(String title, int year) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.year = year;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public int getYear() { return year; }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.getId())
            && Objects.equals(title, movie.getTitle())
            && Objects.equals(year, movie.getYear());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, year);
    }
}