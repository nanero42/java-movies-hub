package ru.practicum.moviehub.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesStoreTest {
    private MoviesStore store;

    @BeforeEach
    public void beforeEach() {
        store = new MoviesStore();
    }

    @Test
    public void getMovies_whenEmpty_returnsEmptyList() {
        assertTrue(store.getMovies().isEmpty());
    }

    @Test
    public void getMovies_whenNotEmpty_returnsMoviesList() {
        Movie movie = store.add("Кино", 1999);
        assertEquals(List.of(movie), store.getMovies());
        assertEquals(movie, store.get(movie.getId()));
    }

    @Test
    public void clearMovies_whenEmptied_returnsEmptyList() {
        Movie first = store.add("Первый", 1999);
        store.add("Второй", 2000);
        store.clear();
        assertTrue(store.getMovies().isEmpty());
        assertNull(store.get(first.getId()));
    }

    @Test
    public void remove_whenMovieExists_deletesOnlyRequestedMovie() {
        Movie first = store.add("Первый", 1999);
        Movie second = store.add("Второй", 2000);
        assertTrue(store.remove(first.getId()));
        assertNull(store.get(first.getId()));
        assertFalse(store.remove(first.getId()));
        assertEquals(List.of(second), store.getMovies());
    }

    @Test
    public void getMoviesByYear_whenYearGiven_returnsOnlyMatchingMovies() {
        Movie first = store.add("Первый", 1999);
        Movie second = store.add("Второй", 1999);
        store.add("Третий", 2000);
        List<Movie> movies = store.getMoviesByYear(1999);
        assertEquals(2, movies.size());
        assertTrue(movies.containsAll(List.of(first, second)));
        assertTrue(store.getMoviesByYear(2001).isEmpty());
        assertEquals(3, store.getMovies().size());
    }

}
