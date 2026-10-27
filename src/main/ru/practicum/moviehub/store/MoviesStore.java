package ru.practicum.moviehub.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.practicum.moviehub.model.Movie;

public class MoviesStore {

    private final HashMap<Long, Movie> movies;
    private long nextId = 1;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public List<Movie> getMovies() {
        List<Movie> moviesList = new ArrayList<>(movies.values());
        return moviesList.subList(0, Math.min(moviesList.size(), 10));
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
            .filter(movie -> movie.getYear() == year)
            .toList();
    }

    public Movie get(long id) {
        return movies.get(id);
    }

    public boolean remove(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
    }

    public Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }
}
