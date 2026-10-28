package ru.practicum.moviehub.validation;

import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class MovieValidationsTest {
    @Test
    public void validate_whenInvalid_returnsErrors() {
        Stream.of(null, "", "  ", "a".repeat(101)).forEach(title -> {
            assertEquals(1, MovieValidations.validate(title, 1999).size());
        });
        IntStream.of(1887, Year.now().getValue() + 2).forEach(year -> {
            assertEquals(1, MovieValidations.validate("Кино", year).size());
        });
    }

    @Test
    public void validate_whenBothFieldsInvalid_returnsBothErrors() {
        List<String> errors = MovieValidations.validate(null, 0);
        assertEquals(2, errors.size());
    }
}
