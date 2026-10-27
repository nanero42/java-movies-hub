package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {

    private final long id;
    private final String title;
    private final int year;

    public Movie(long id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        boolean isDifferentType = o == null || o.getClass() != getClass();
        if (isDifferentType) return false;
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