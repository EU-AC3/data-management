package net.sparkworks.ac3.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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


public class HttpAmqpRequestLoggerServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpAmqpRequestLoggerServer.class);

    static final String HTTP_PORT = "HTTP_SERVER_PORT";
    static ObjectMapper objectMapper = new ObjectMapper();
    static Map<String, String> last = new HashMap<>();
    private static final String RABBIT_HOST = System.getenv("RABBIT_HOST");
    private static final int RABBIT_PORT = Integer.parseInt(System.getenv("RABBIT_PORT"));
    private static final String RABBIT_USERNAME = System.getenv("RABBIT_USERNAME");
    private static final String RABBIT_PASSWORD = System.getenv("RABBIT_PASSWORD");
    private static final String RABBIT_EXCHANGE = System.getenv("RABBIT_EXCHANGE");

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


            try {
                MyMessage mymessage = objectMapper.readValue(bodyStr, MyMessage.class);

                if (last.containsKey(mymessage.getTopic()) && last.get(mymessage.getTopic()).equals(mymessage.getPayload())) {
                    logger.debug("ignore");
                } else {
                    last.put(mymessage.getTopic(), mymessage.getPayload());
                    try {
                        ConnectionFactory factory = new ConnectionFactory();
                        factory.setHost(RABBIT_HOST);
                        factory.setPort(RABBIT_PORT);
                        factory.setUsername(RABBIT_USERNAME);
                        factory.setPassword(RABBIT_PASSWORD);
                        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
                            channel.basicPublish(RABBIT_EXCHANGE, mymessage.getTopic(), null, mymessage.getPayload().getBytes());
                        }
                    } catch (Exception e) {
                        logger.error("ERR:" + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("ERR:" + e.getMessage(), e);
            }
            exchange.sendResponseHeaders(200, -1);
        }
    }

}
