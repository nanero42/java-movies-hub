package ru.practicum.moviehub.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ru.practicum.moviehub.model.Movie;

public class MoviesStore {

    private final HashMap<UUID, Movie> movies;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public void clear() {
        movies.clear();
    }

    public void add(Movie movie) {
        boolean has = movies.values().stream()
            .anyMatch(m -> {
                boolean isTitlesEqual = m.getTitle().equalsIgnoreCase(movie.getTitle());
                boolean isYearEqual = m.getYear() == movie.getYear();
                return isTitlesEqual && isYearEqual;
            });
        if (has) throw new IllegalArgumentException("Такое кино уже существует");
        movies.put(movie.getId(), movie);
    }
}