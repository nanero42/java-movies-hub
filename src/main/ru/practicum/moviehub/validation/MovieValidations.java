package ru.practicum.moviehub.validation;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public final class MovieValidations {

    private MovieValidations() {
    }

    public static List<String> validate(String title, int year) {
        List<String> errors = new ArrayList<>();
        boolean isTitleEmpty = title == null || title.isBlank();
        boolean isTitleTooLong = title != null && title.trim().length() > 100;
        boolean isTitleInvalid = isTitleEmpty || isTitleTooLong;
        if (isTitleInvalid) {
            errors.add("Название должно содержать от 1 до 100 символов");
        }
        int maxYear = Year.now().getValue() + 1;
        boolean isYearInvalid = year < 1888 || year > maxYear;
        if (isYearInvalid) {
            errors.add("Год должен быть между 1888 и " + maxYear);
        }
        return errors;
    }
}
