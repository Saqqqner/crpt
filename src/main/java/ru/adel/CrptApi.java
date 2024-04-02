package ru.adel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final ObjectMapper objectMapper;
    private final HttpClient client;
    private final RingBuffer ringBuffer;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        objectMapper = om;

        client = HttpClient.newHttpClient();

        ringBuffer = new RingBuffer(requestLimit, timeUnit);
    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);
        for (int i = 0; i < 12; i++) {
            HttpResponse<String> response = api.sendRequest(API_URL, api.createDocument(), UUID.randomUUID().toString());
            System.out.println(response.statusCode());
        }
    }

    private HttpResponse<String> sendRequest(String url, Object body, String signature) throws URISyntaxException, IOException, InterruptedException {
        ringBuffer.push();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Signature", signature)
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private Document createDocument() {
        Description description = new Description("string");

        Product product = new Product.ProductBuilder()
                .certificateDocument("string")
                .certificateDocumentDate(LocalDate.of(2020, Month.JANUARY, 23))
                .certificateDocumentNumber("string")
                .ownerInn("string")
                .producerInn("string")
                .productionDate(LocalDate.of(2020, Month.JANUARY, 23))
                .tnvedCode("string")
                .uitCode("string")
                .uituCode("string")
                .build();

        List<Product> products = new ArrayList<>();
        products.add(product);

        return new Document.DocumentBuilder()
                .description(description)
                .docId("string")
                .docStatus("string")
                .docType("LP_INTRODUCE_GOODS")
                .importRequest(true)
                .ownerInn("string")
                .participantInn("string")
                .producerInn("string")
                .productionDate(LocalDate.of(2020, Month.JANUARY, 23))
                .productionType("string")
                .products(products)
                .regDate(LocalDate.of(2020, Month.JANUARY, 23))
                .regNumber("string")
                .build();
    }

    private static class RingBuffer {
        private final LinkedList<Long> buffer;
        private final int capacity;
        private final TimeUnit timeUnit;

        public RingBuffer(int capacity, TimeUnit timeUnit) {
            this.capacity = capacity;
            this.timeUnit = timeUnit;
            this.buffer = new LinkedList<>();
        }

        public synchronized void push() throws InterruptedException {
            Long eldest = buffer.peek();

            while (buffer.size() == capacity && calcWaitTime(eldest) < 0L) {
                Thread.sleep(-calcWaitTime(eldest));
            }

            buffer.add(Instant.now().toEpochMilli());
            if (buffer.size() > capacity) {
                buffer.removeFirst();
            }
        }

        private long calcWaitTime(long eldest) {
            return Duration.between(Instant.ofEpochMilli(eldest), Instant.now()).toMillis() - timeUnit.toMillis(1L);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Description {
        private String participantInn;
    }

    @Builder
    @Getter
    public static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("certificate_document_date")
        private LocalDate certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }

    @Builder
    @Getter
    public static class Document {
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        private Collection<Product> products;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("reg_date")
        private LocalDate regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }
}

