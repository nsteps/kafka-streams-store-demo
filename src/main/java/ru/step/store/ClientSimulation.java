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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers;

public class ClientSimulation {
    private static final int CLIENT_NUM = 10;
    private static final String BASE_URl = "http://localhost:8080";


    public static void main(String[] args) {
        final var httpClient = HttpClient.newHttpClient();
        LongStream.range(0, CLIENT_NUM).forEach(i -> sendRequest(httpClient));
    }

    private static void sendRequest(HttpClient httpClient) {
        try {
            final HttpRequest request = createRequest(generateOrderRequest());
            final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            System.out.println(response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HttpRequest createRequest(OrderCreateRequest dto) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URl + "/orders"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(ofString(mapper.writeValueAsString(dto)))
                .build();
    }

    private static OrderCreateRequest generateOrderRequest() {
        final var random = ThreadLocalRandom.current();
        return new OrderCreateRequest(UUID.randomUUID(), randomProduct(random),
                randomQuantity(random), randomPrice(random));
    }

    private static Order.Product randomProduct(ThreadLocalRandom random) {
        Order.Product product;
        switch (random.nextInt(0, 3)) {
            case 0:
                product = Order.Product.SHOES;
                break;
            case 1:
                product = Order.Product.GUITAR;
                break;
            case 2:
                product = Order.Product.WATCH;
                break;
            default:
                throw new IllegalArgumentException("Illegal random number");
        }
        return product;
    }

    private static int randomQuantity(ThreadLocalRandom random) {
        int quantity;
        switch (random.nextInt(0, 3)) {
            case 0:
                quantity = random.nextInt(5, 10);
                break;
            case 1:
                quantity = random.nextInt(0, 5);
                break;
            case 2:
                quantity = random.nextInt(50, 100);
                break;
            default:
                throw new IllegalArgumentException("Illegal random number");
        }
        return quantity;
    }

    private static long randomPrice(ThreadLocalRandom random) {
        long price;
        switch (random.nextInt(0, 3)) {
            case 0:
                price = random.nextLong(10, 20);
                break;
            case 1:
                price = random.nextLong(50, 100);
                break;
            case 2:
                price = random.nextLong(101, 200);
                break;
            default:
                throw new IllegalArgumentException("Illegal random number");
        }
        return price;
    }
}
