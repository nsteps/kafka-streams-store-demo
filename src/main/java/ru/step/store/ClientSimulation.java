package ru.step.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.step.store.common.model.Order;
import ru.step.store.order.model.OrderCreateRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static java.net.http.HttpResponse.BodyHandlers;

public class ClientSimulation {
    private static final int CLIENT_NUM = 10;
    private static final String BASE_URl = "http://localhost:8080";


    public static void main(String[] args) throws Exception {
        final var httpClient = HttpClient.newHttpClient();
        final HttpRequest request = orderCreateRequest();
        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    }

    private static HttpRequest orderCreateRequest() throws Exception {
        final var orderCreateRequest = new OrderCreateRequest(UUID.randomUUID(),
                Order.Product.SHOES, 2, 2);
        ObjectMapper mapper = new ObjectMapper();
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URl + "/orders"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(orderCreateRequest)))
                .build();
    }
}
