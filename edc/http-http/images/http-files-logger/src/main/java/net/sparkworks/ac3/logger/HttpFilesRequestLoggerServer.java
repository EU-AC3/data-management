package net.sparkworks.ac3.logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


public class HttpFilesRequestLoggerServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpFilesRequestLoggerServer.class);

    static final String HTTP_PORT = "HTTP_SERVER_PORT";
    private static String FILES_DIR = System.getenv("FILES_DIR");

    public static void main(String[] args) {
        if (FILES_DIR == null) {
            FILES_DIR = "files";
        }
        int port = Integer.parseInt(Optional.ofNullable(System.getenv(HTTP_PORT)).orElse("4000"));
        try {
            var server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new ReceiverHandler());
            server.setExecutor(null);
            server.start();

            logger.info("HTTP request server logger started at {}", port);
        } catch (IOException e) {
            throw new RuntimeException("Unable to start server at port " + port, e);
        }
    }

    private static class ReceiverHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            try {
                Path path = Paths.get("%s/%d.data".formatted(FILES_DIR, System.currentTimeMillis()));
                Files.write(path, bytes);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            exchange.sendResponseHeaders(200, -1);
        }
    }

}
