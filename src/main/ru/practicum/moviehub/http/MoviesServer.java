package ru.practicum.moviehub.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import ru.practicum.moviehub.store.MoviesStore;

public class MoviesServer {

    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore moviesStore, int i) {
        try {
            store = moviesStore;
            server = HttpServer.create(new InetSocketAddress(i), 0);
            server.createContext("/", new MoviesHttpHandler(store));
        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер");
            throw new RuntimeException(e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(2);
    }

    public MoviesStore getStore() {
        return store;
    }
}