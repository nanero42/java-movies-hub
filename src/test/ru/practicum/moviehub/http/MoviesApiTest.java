package ru.practicum.moviehub.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.utils.UuidTypeAdapter;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    public static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(UUID.class, new UuidTypeAdapter())
            .create();
    }

    @BeforeEach
    public void beforeEach() {
        server.getStore().clear();
    }

    @AfterAll
    public static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/movies"))
            .header("Content-Type", "application/json; charset=UTF-8")
            .GET()
            .build();

        HttpResponse<String> res = client.send(
            req,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, res.statusCode(), "Ответ должен быть успешным");

        String contentType = res.headers()
            .firstValue("content-type")
            .orElse("unknown");
        assertEquals("application/json; charset=UTF-8", contentType);
        assertEquals("[]", res.body().trim());
    }

    @Test
    public void getMovies_whenNotEmpty_returnsMoviesArray() throws Exception {
        server.getStore().add(new Movie("Star Wars: Episode I - The Phantom Menace", 1999));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/movies"))
            .header("Content-Type", "application/json; charset=UTF-8")
            .GET()
            .build();

        HttpResponse<String> res = client.send(
            req,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        int code = res.statusCode();

        assertEquals(200, code, "Ответ должен быть успешным");

        String contentType = res.headers().firstValue("content-type").orElse("unknown");

        assertEquals("application/json; charset=UTF-8", contentType);

        JsonElement root = JsonParser.parseString(res.body());
        boolean isJsonArray = root.isJsonArray();

        assertTrue(isJsonArray, "Ответ должен быть в виде массива");

        JsonArray jsonArray = root.getAsJsonArray();
        boolean isEmpty = jsonArray.size() == 0;

        assertFalse(isEmpty, "Должен вернуться хотя бы один элемент");

        JsonObject movie = jsonArray.get(0).getAsJsonObject();

        assertTrue(movie.has("id"), "В ответе отсутствует поле \"id\"");
        JsonElement id = movie.get("id");
        assertDoesNotThrow(() -> gson.fromJson(id, UUID.class), "Поле \"id\" должно быть типа UUID");

        assertTrue(movie.has("title"), "В ответе отсутствует поле поле \"title\"");
        JsonElement title = movie.get("title");
        assertTrue(title.getAsJsonPrimitive().isString(), "Поле \"title\" должно быть типа СТРОКОЙ");

        assertTrue(movie.has("year"), "В ответе отсутствует поле \"year\"");
        JsonElement year = movie.get("year");
        assertTrue(year.getAsJsonPrimitive().isNumber(), "Поле \"year\" должно быть типа ЧИСЛОМ");
    }
}