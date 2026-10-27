package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.validation.MovieValidations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MoviesHttpHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson;

    public MoviesHttpHandler(MoviesStore store) {
        this.store = store;
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            boolean isMoviesPath = path.equals("/movies");
            boolean isMoviePath = path.startsWith("/movies/")
                && path.length() > "/movies/".length()
                && path.indexOf('/', "/movies/".length()) == -1;
            if (isMoviesPath) {
                handleMovies(exchange);
            } else if (isMoviePath) {
                handleMovie(exchange, path.substring("/movies/".length()));
            } else {
                sendError(exchange, 404, "Эндпоинт не найден");
            }
        } finally {
            exchange.close();
        }
    }

    private void handleMovies(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET":
                handleGetMovies(exchange);
                break;
            case "POST":
                handlePostMovies(exchange);
                break;
            default:
                sendMethodNotAllowed(exchange);
        }
    }

    private void handleMovie(HttpExchange exchange, String rawId) throws IOException {
        String method = exchange.getRequestMethod();
        boolean isMethodAllowed = method.equals("GET") || method.equals("DELETE");
        if (!isMethodAllowed) {
            sendMethodNotAllowed(exchange);
            return;
        }
        long id;
        try {
            id = Math.abs(Long.parseLong(rawId));
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }
        Movie movie = store.get(id);
        if (movie == null) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }
        if (method.equals("DELETE")) {
            store.remove(id);
            sendNoContent(exchange);
        } else {
            sendJson(exchange, 200, gson.toJson(movie));
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Integer year;
        try {
            year = parseYearQuery(exchange.getRequestURI().getRawQuery());
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }
        List<Movie> movies;
        if (year == null) {
            movies = store.getMovies();
        } else {
            movies = store.getMoviesByYear(year);
        }
        sendJson(exchange, 200, gson.toJson(movies));
    }

    private Integer parseYearQuery(String query) {
        if (query == null) {
            return null;
        }
        String[] yearParameter = Arrays.stream(query.split("&"))
            .map(parameter -> parameter.split("=", 2))
            .filter(parameter -> parameter[0].equals("year"))
            .findFirst()
            .orElse(null);
        if (yearParameter == null) {
            return null;
        }
        if (yearParameter.length < 2) {
            throw new NumberFormatException();
        }
        return Integer.valueOf(yearParameter[1]);
    }

    private void handlePostMovies(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        boolean isJsonContentType = mediaType.equalsIgnoreCase("application/json");
        if (!isJsonContentType) {
            sendError(exchange, 415, "Неподдерживаемый Content-Type");
            return;
        }
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );
        String title;
        int year;
        try {
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            title = object.has("title")
                && !object.get("title").isJsonNull()
                ? object.get("title").getAsString()
                : null;
            year = object.has("year")
                && !object.get("year").isJsonNull()
                ? object.get("year").getAsInt()
                : 0;
        } catch (
            JsonParseException | NumberFormatException
            | IllegalStateException | UnsupportedOperationException e
        ) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }
        List<String> errors = MovieValidations.validate(title, year);
        if (!errors.isEmpty()) {
            sendJson(
                exchange, 422,
                gson.toJson(new ErrorResponse("Ошибка валидации", errors))
            );
            return;
        }
        Movie movie = store.add(title, year);
        sendJson(exchange, 201, gson.toJson(movie));
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendError(exchange, 405, "Метод не поддерживается");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, gson.toJson(new ErrorResponse(message, null)));
    }
}
