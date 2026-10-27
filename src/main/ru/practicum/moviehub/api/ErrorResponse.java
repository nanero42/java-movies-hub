package ru.practicum.moviehub.api;

import java.util.List;

public record ErrorResponse(String error, List<String> details) {}
