package net.sparkworks.mapper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final String MESSAGE_TEMPLATE = "%s,%f,%d";
    private static final String QUEUE_INPUT = "${rabbitmq.queue.input}";
    private static final String QUEUE_COMMAND = "${rabbitmq.queue.commands}";
    private static final String PROCESSING_TIMER_NAME = "mapper.mapped.latency";
    private static final String TAG_DEVICE = "device";
    private static final String TAG_SENSOR = "sensor";

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${rabbitmq.queue.output}")
    private String rabbitQueueOutput;

    @Value("${mapper.file.enabled:false}")
    private boolean fileWritingEnabled;

    @Value("${mapper.file.path:/srv/data.csv}")
    private String dataFilePath;

    private final RabbitTemplate rabbitTemplate;

    private final MeterRegistry meterRegistry;

    private Counter inputCounter, outputCounter;
    private AtomicInteger processingTime = new AtomicInteger(0);

    // Field name transformations: fieldName -> sensorName
    // Use this to map internal field names to external sensor names
    private final Map<String, String> fieldNameTransformations = new HashMap<>();

    @PostConstruct
    public void init() {
        inputCounter = Counter.builder("mapper.input.messages").description("input messages counter").register(meterRegistry);
        outputCounter = Counter.builder("mapper.output.messages").description("output messages counter").register(meterRegistry);
        Gauge.builder(PROCESSING_TIMER_NAME, processingTime, AtomicInteger::get).tags(TAG_DEVICE, "", TAG_SENSOR, "").register(meterRegistry);

        // Log file writing configuration
        if (fileWritingEnabled) {
            log.info("Mapper service initialized with file writing enabled, output path: {}", dataFilePath);
        } else {
            log.info("Mapper service initialized with file writing disabled");
        }

        // Initialize field name transformations
        // Map camelCase field names to sensor-specific names if needed
        fieldNameTransformations.put("iaqAccuracy", "iaq_accuracy");
        // Add more transformations here as needed
        // Example: fieldNameTransformations.put("co2", "co2_ppm");
    }

    public Collection<String> sendMeasurement(final String uri, final Integer reading, final long timestamp) {
        return sendMeasurement(uri, (double) reading, timestamp);
    }

    public Collection<String> sendMeasurement(final String uri, final Double reading, final long timestamp) {
        outputCounter.increment();
        final String message = String.format(MESSAGE_TEMPLATE, uri, reading, timestamp);

        if (fileWritingEnabled) {
            try {
                write2file(message);
            } catch (IOException e) {
                log.error("Failed to write measurement to file: path={}, error={}", dataFilePath, e.getMessage());
            }
        }

        log.debug("Sending measurement to queue: queue={}, routingKey={}, message={}", rabbitQueueOutput, rabbitQueueOutput, message);
        rabbitTemplate.send(rabbitQueueOutput, rabbitQueueOutput, new Message(message.getBytes(), new MessageProperties()));
        return Collections.singletonList(uri);
    }

    private void write2file(final String message) throws IOException {
        final File file = new File(dataFilePath);
        final FileWriter fr = new FileWriter(file, true);
        fr.write(message + "\n");
        fr.close();
    }

    //RabbitMQ listener for commands from SPARKS
    @RabbitListener(queues = QUEUE_COMMAND)
    public void receiveCommandFromSparks(final Message message) {
        final String command = new String(message.getBody());
        log.info("Received command from SPARKS: queue={}, command={}", QUEUE_COMMAND, command);
    }

    //RabbitMQ listener for data from IPN Mouse - data in format 3
    @RabbitListener(queues = QUEUE_INPUT)
    public void receiveFromSmartWorkV3(final Message message) {
        final String body = new String(message.getBody());
        final String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("Received message from input queue: queue={}, routingKey={}, body={}", QUEUE_INPUT, routingKey, body);

        try {
            inputCounter.increment();
            long start = System.nanoTime();

            // Parse as generic JSON
            final JsonNode jsonNode = mapper.readTree(body);
            log.debug("Parsed JSON message: queue={}, data={}", QUEUE_INPUT, jsonNode);

            final String baseUri = routingKey;
            long took = System.nanoTime() - start;
            extractAndSendReadings(took, baseUri, jsonNode);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON message: error={}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to process message: error={}", e.getMessage(), e);
        }
    }

    /**
     * Generic method to extract and send all measurements from JSON data.
     * Dynamically processes all JSON fields without requiring a predefined model class.
     * Supports field name transformations via fieldNameTransformations map.
     *
     * @param took     Processing time in nanoseconds
     * @param baseUri  Base URI for routing key
     * @param jsonNode The JSON node containing sensor readings
     */
    private void extractAndSendReadings(final long took, final String baseUri, final JsonNode jsonNode) {
        final String device = baseUri.split("/")[2];
        int measurementCount = 0;

        // Extract timestamp from JSON (required field)
        Long timestamp = null;
        if (jsonNode.has("timestamp")) {
            timestamp = jsonNode.get("timestamp").asLong();
        } else {
            log.warn("Missing timestamp field in message, using current system time");
            timestamp = System.currentTimeMillis();
        }

        // Iterate over all JSON fields
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            final String fieldName = entry.getKey();
            final JsonNode valueNode = entry.getValue();

            try {
                // Skip non-measurement fields
                if ("sensorid".equals(fieldName) || "timestamp".equals(fieldName)) {
                    log.debug("Skipping metadata field: fieldName={}", fieldName);
                    continue;
                }

                // Only process numeric values
                if (valueNode.isNumber()) {
                    Double measurement = valueNode.asDouble();

                    // Apply field name transformation if configured
                    String sensorName = transformFieldName(fieldName);

                    // Send measurement
                    sendMeasurement(baseUri + "/" + sensorName, measurement, timestamp);

                    // Record metrics using the transformed sensor name
                    Metrics.timer(PROCESSING_TIMER_NAME, TAG_DEVICE, device, TAG_SENSOR, sensorName)
                            .record(took, TimeUnit.NANOSECONDS);

                    measurementCount++;

                    log.debug("Processed measurement: fieldName={}, sensorName={}, value={}", fieldName, sensorName, measurement);
                } else {
                    log.debug("Skipping non-numeric field: fieldName={}, type={}", fieldName, valueNode.getNodeType());
                }

            } catch (Exception e) {
                log.error("Failed to process field: fieldName={}, error={}", fieldName, e.getMessage(), e);
            }
        }

        // Update output counter with actual measurement count
        outputCounter.increment(measurementCount);

        log.info("Successfully extracted and sent measurements: count={}, baseUri={}", measurementCount, baseUri);
    }

    /**
     * Apply custom field name transformations.
     * Transforms internal JSON field names to external sensor names.
     *
     * @param fieldName Original field name from JSON
     * @return Transformed sensor name
     */
    private String transformFieldName(final String fieldName) {
        String transformed = fieldNameTransformations.getOrDefault(fieldName, fieldName);
        if (!fieldName.equals(transformed)) {
            log.debug("Transformed field name: original={}, transformed={}", fieldName, transformed);
        }
        return transformed;
    }

}
