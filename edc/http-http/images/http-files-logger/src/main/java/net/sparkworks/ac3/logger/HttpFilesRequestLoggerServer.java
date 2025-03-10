package net.sparkworks.ac3.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class HttpFilesRequestLoggerServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpFilesRequestLoggerServer.class);

    static final String HTTP_PORT = "HTTP_SERVER_PORT";
    static ObjectMapper objectMapper = new ObjectMapper();
    static Map<String, String> last = new HashMap<>();
    private static final String FILES_DIR = System.getenv("FILES_DIR");

    public static void main(String[] args) {
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
            String bodyStr = new String(exchange.getRequestBody().readAllBytes());
            logger.info("Incoming message : {}", bodyStr);

            //need to store data to FILES_DIR

            exchange.sendResponseHeaders(200, -1);
        }
    }

}
