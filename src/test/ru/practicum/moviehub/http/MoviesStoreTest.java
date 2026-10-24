package ru.practicum.moviehub.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesStoreTest {

    private static MoviesStore store;

    @BeforeEach
    void beforeEach() {
        store = new MoviesStore();
    }
    
    @Test
    public void getMovies_whenEmpty_returnsEmptyList() {
        List<Movie> movies = store.getMovies();
        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
    }

    @Test
    public void getMovies_whenNotEmpty_returnsMoviesList() {
        Movie movie = new Movie("Star Wars: Episode I - The Phantom Menace", 1999);
        store.add(movie);
        List<Movie> movies = store.getMovies();
        assertFalse(movies.isEmpty(), "Список фильмов не должен быть пустым");
    }

    @Test
    public void clearMovies_whenEmptied_returnsEmptyList() {
        Movie movie = new Movie("Star Wars: Episode I - The Phantom Menace", 1999);
        Movie movie2 = new Movie("Star Wars: Episode II - Attack of the Clones", 2002);
        store.add(movie);
        store.add(movie2);
        assertTrue(store.getMovies().size() == 2, "В сторе должно находиться 2 фильма");
        store.clear();
        assertTrue(store.getMovies().isEmpty(), "В сторе не должно быть фильмов");
    }

    @Test
    public void restrictDups_whenAddDup_throwsException() {
        Movie movie = new Movie("Star Wars: Episode I - The Phantom Menace", 1999);
        Movie movie2 = new Movie("Star Wars: Episode I - The Phantom Menace", 1999);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            store.add(movie);
            store.add(movie2);
        });
        assertEquals("Такое кино уже существует", e.getMessage());
    }
}
