package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    private static final Gson GSON = new Gson();
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    public static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
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
    public void getMovies_whenEmpty_returnsEmptyArray() {
        HttpResponse<String> response = request("GET", "/movies", null, null);
        assertEquals(200, response.statusCode());
        assertEquals(0, JsonParser.parseString(response.body()).getAsJsonArray().size());
    }

    @Test
    public void getMovies_whenNotEmpty_returnsMoviesArray() {
        Movie movie = server.getStore().add("Звёздные войны", 1999);
        HttpResponse<String> response = request("GET", "/movies", null, null);
        assertEquals(200, response.statusCode());
        JsonArray movies = JsonParser.parseString(response.body()).getAsJsonArray();
        assertEquals(1, movies.size());
        assertEquals(GSON.toJsonTree(movie), movies.get(0));
    }

    @Test
    public void createMovie_whenCorrectData_returnsCreatedMovie() {
        Stream.of(
            "application/json",
            "application/json; charset=UTF-8",
            "Application/JSON; charset=utf-8"
        ).forEach(contentType -> {
            String body = GSON.toJson(Map.of("title", "Кино", "year", 1999));
            HttpResponse<String> response = request("POST", "/movies", body, contentType);
            assertEquals(201, response.statusCode());
            JsonObject movie = JsonParser.parseString(response.body()).getAsJsonObject();
            assertTrue(movie.get("id").getAsJsonPrimitive().isNumber());
            assertTrue(movie.get("id").getAsLong() > 0);
            assertEquals("Кино", movie.get("title").getAsString());
            assertEquals(1999, movie.get("year").getAsInt());
            HttpResponse<String> found = request(
                "GET",
                "/movies/" + movie.get("id").getAsLong(),
                null, null
            );
            assertEquals(200, found.statusCode());
            assertEquals(movie, JsonParser.parseString(found.body()));
        });
    }

    @Test
    public void createMovie_whenTitleBlank_returnsValidationError() {
        Stream.of("", "  ", "\t\n").forEach(title -> {
            String body = GSON.toJson(Map.of("title", title, "year", 1999));
            assertValidationError(body, 1);
        });
    }

    @Test
    public void createMovie_whenTitleTooLong_returnsValidationError() {
        String body = GSON.toJson(Map.of("title", "a".repeat(101), "year", 1999));
        assertValidationError(body, 1);
    }

    @Test
    public void createMovie_whenYearOutsideRange_returnsValidationError() {
        String tooEarlyBody = GSON.toJson(Map.of("title", "Кино", "year", 1887));
        assertValidationError(tooEarlyBody, 1);
        String tooLateBody = GSON.toJson(Map.of(
            "title", "Кино",
            "year", Year.now().getValue() + 2
        ));
        assertValidationError(tooLateBody, 1);
    }

    @Test
    public void createMovie_whenBoundaryValues_returnsCreated() {
        IntStream.of(1888, Year.now().getValue() + 1).forEach(year -> {
            String body = GSON.toJson(Map.of("title", "a".repeat(100), "year", year));
            assertEquals(201, post(body).statusCode());
        });
    }

    @Test
    public void createMovie_whenBothFieldsInvalid_returnsAllDetails() {
        String body = GSON.toJson(Map.of("title", "", "year", 0));
        assertValidationError(body, 2);
    }

    @Test
    public void createMovie_whenFieldsMissing_returnsValidationError() {
        Stream.of("{}", "{\"title\":null,\"year\":null}").forEach(body -> {
            assertValidationError(body, 2);
        });
    }

    @Test
    public void createMovie_whenContentTypeWrong_returnsUnsupportedMediaType() {
        Stream.of("text/plain", "application/xml", "application/jsonp", "").forEach(contentType -> {
            String body = GSON.toJson(Map.of("title", "Кино", "year", 1999));
            HttpResponse<String> response = request("POST", "/movies", body, contentType);
            assertError(
                response,
                415
            );
            assertTrue(server.getStore().getMovies().isEmpty());
        });
    }

    @Test
    public void createMovie_whenContentTypeAbsent_returnsUnsupportedMediaType() {
        String body = GSON.toJson(Map.of("title", "Кино", "year", 1999));
        HttpResponse<String> response = request("POST", "/movies", body, null);
        assertError(response, 415);
    }

    @Test
    public void createMovie_whenJsonInvalid_returnsBadRequest() {
        Stream.of(
            "",
            "null",
            "[]",
            "42",
            "{",
            "{\"title\":\"Кино\",\"year\":1999",
            "{\"title\":\"Кино\",\"year\":1999} {}",
            "{\"title\":[],\"year\":1999}",
            "{\"title\":\"Кино\",\"year\":true}"
        ).forEach(body -> {
            server.getStore().clear();
            assertError(post(body), 400);
            assertTrue(server.getStore().getMovies().isEmpty());
            String nextMovieBody = GSON.toJson(Map.of("title", "Следующий фильм", "year", 2000));
            assertEquals(201, post(nextMovieBody).statusCode());
        });
    }

    @Test
    public void createMovie_whenMoviesAdded_assignsDifferentIds() {
        String firstBody = GSON.toJson(Map.of("title", "Первый", "year", 1999));
        HttpResponse<String> first = post(firstBody);
        String secondBody = GSON.toJson(Map.of("title", "Второй", "year", 2000));
        HttpResponse<String> second = post(secondBody);
        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertNotEquals(
            JsonParser.parseString(first.body()).getAsJsonObject().get("id"),
            JsonParser.parseString(second.body()).getAsJsonObject().get("id")
        );
        assertEquals(2, server.getStore().getMovies().size());
    }

    @Test
    public void getMovie_whenPresent_returnsMovie() {
        Movie movie = server.getStore().add("Кино", 1999);
        HttpResponse<String> response = request("GET", "/movies/" + movie.getId(), null, null);
        assertEquals(200, response.statusCode());
        assertEquals(GSON.toJsonTree(movie), JsonParser.parseString(response.body()));
    }

    @Test
    public void movie_whenMissing_returnsNotFound() {
        Stream.of("GET", "DELETE").forEach(method -> {
            HttpResponse<String> response = request(method, "/movies/999999", null, null);
            assertError(response, 404);
        });
    }

    @Test
    public void getMovie_whenIdInvalid_returnsBadRequest() {
        Stream.of("abc", "1.5", "999999999999999999999999").forEach(id -> {
            HttpResponse<String> response = request("GET", "/movies/" + id, null, null);
            assertError(response, 400);
        });
    }

    @Test
    public void deleteMovie_whenIdInvalid_returnsBadRequest() {
        Stream.of("abc", "1.5", "999999999999999999999999").forEach(id -> {
            HttpResponse<String> response = request("DELETE", "/movies/" + id, null, null);
            assertError(response, 400);
        });
    }

    @Test
    public void deleteMovie_whenPresent_removesOnlySelectedMovie() {
        Movie removed = server.getStore().add("Первый", 1999);
        Movie retained = server.getStore().add("Второй", 2000);
        String path = "/movies/" + removed.getId();
        HttpResponse<String> response = request("DELETE", path, null, null);
        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        HttpResponse<String> missingMovieResponse = request("GET", path, null, null);
        assertError(missingMovieResponse, 404);
        HttpResponse<String> repeatedDeleteResponse = request("DELETE", path, null, null);
        assertError(repeatedDeleteResponse, 404);
        HttpResponse<String> retainedMovieResponse = request(
            "GET",
            "/movies/" + retained.getId(),
            null, null
        );
        assertEquals(
            200,
            retainedMovieResponse.statusCode()
        );
    }

    @Test
    public void getMovies_whenYearGiven_returnsOnlyMatchingMovies() {
        Movie first = server.getStore().add("Первый", 1999);
        Movie second = server.getStore().add("Второй", 1999);
        server.getStore().add("Третий", 2000);
        HttpResponse<String> response = request("GET", "/movies?year=1999", null, null);
        assertEquals(200, response.statusCode());
        JsonArray movies = JsonParser.parseString(response.body()).getAsJsonArray();
        assertEquals(2, movies.size());
        assertTrue(movies.contains(GSON.toJsonTree(first)));
        assertTrue(movies.contains(GSON.toJsonTree(second)));
    }

    @Test
    public void getMovies_whenYearHasNoMatches_returnsEmptyArray() {
        server.getStore().add("Кино", 1999);
        HttpResponse<String> response = request("GET", "/movies?year=2001", null, null);
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body().trim());
    }

    @Test
    public void getMovies_whenYearInvalid_returnsBadRequest() {
        Stream.of(
            "year=abc",
            "year=",
            "year",
            "year=1.5",
            "year=999999999999"
        ).forEach(query -> {
            HttpResponse<String> response = request("GET", "/movies?" + query, null, null);
            assertError(response, 400);
        });
    }

    @Test
    public void putMovies_whenMethodNotSupported_returnsNotAllowed() {
        Stream.of("/movies", "/movies/1").forEach(path -> {
            HttpResponse<String> response = request("PUT", path, null, null);
            assertError(response, 405);
        });
    }

    @Test
    public void patchMovies_whenMethodNotSupported_returnsNotAllowed() {
        Stream.of("/movies", "/movies/1").forEach(path -> {
            HttpResponse<String> response = request("PATCH", path, null, null);
            assertError(response, 405);
        });
    }

    @Test
    public void optionsMovies_whenMethodNotSupported_returnsNotAllowed() {
        Stream.of("/movies", "/movies/1").forEach(path -> {
            HttpResponse<String> response = request("OPTIONS", path, null, null);
            assertError(response, 405);
        });
    }

    @Test
    public void methods_whenUsedOnWrongResource_returnNotAllowed() {
        HttpResponse<String> response = request("DELETE", "/movies", null, null);
        assertError(response, 405);
        String body = GSON.toJson(Map.of("title", "Кино", "year", 1999));
        HttpResponse<String> response2 = request("POST", "/movies/1", body, CONTENT_TYPE);
        assertError(
            response2,
            405
        );
    }

    @Test
    public void getMovies_whenPathUnknown_returnsJsonNotFound() {
        Stream.of("/", "/unknown", "/moviesXYZ", "/movies/1/extra", "/movies/").forEach(path -> {
            HttpResponse<String> response = request("GET", path, null, null);
            assertError(response, 404);
        });
    }

    private void assertValidationError(String body, int count) {
        HttpResponse<String> response = post(body);
        assertError(response, 422);
        JsonArray details = JsonParser.parseString(response.body())
            .getAsJsonObject()
            .getAsJsonArray("details");
        assertEquals(count, details.size());
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    private void assertError(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode());
        JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(error.get("error").getAsJsonPrimitive().isString());
        assertFalse(error.get("error").getAsString().isBlank());
    }

    private HttpResponse<String> post(String body) {
        return request("POST", "/movies", body, CONTENT_TYPE);
    }

    private HttpResponse<String> request(
        String method,
        String path,
        String body,
        String contentType
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE + path))
            .timeout(Duration.ofSeconds(3))
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body)
            );
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        HttpResponse<String> response = assertDoesNotThrow(() -> client.send(
            builder.build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ));
        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
        return response;
    }

}
