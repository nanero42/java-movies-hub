package ru.practicum.moviehub.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.utils.UuidTypeAdapter;

public class MoviesHttpHandler implements HttpHandler {

    private final MoviesStore store;
    private final Gson gson;

    public MoviesHttpHandler(MoviesStore store) {
        this.store = store;
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(UUID.class, new UuidTypeAdapter())
            .create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET":
                handleGetMovies(exchange);
                break;
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        String res = gson.toJson(store.getMovies());
        sendResponse(exchange, 200, res);
    }

    private void sendResponse(
        HttpExchange exchange,
        int statusCode,
        String responseText
    ) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
