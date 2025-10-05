package org.example.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

public class ConcurrentTestClient {
    private static final String BASE_URL = "http://localhost:8080/syos";
    private final HttpClient client;
    private final ExecutorService executor;
    
    public ConcurrentTestClient() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.executor = Executors.newFixedThreadPool(20);
    }
    
    public void runConcurrentStockTests(int numberOfRequests) {
        System.out.println("Starting concurrent stock test with " + numberOfRequests + " requests...");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                sendStockRequest(requestId);
            }, executor);
            futures.add(future);
        }
        
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allOf.get();
            System.out.println("All requests completed!");
        } catch (Exception e) {
            System.out.println("Some requests failed: " + e.getMessage());
        }
    }
    
    private void sendStockRequest(int requestId) {
        try {
            String json = String.format("""
                {
                    "code": "TEST%03d",
                    "quantity": %d,
                    "date_of_purchase": "2024-01-01",
                    "date_of_expiry": "2024-12-31"
                }
                """, requestId, 10 + requestId);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Request " + requestId + ": " + response.statusCode() + " - " + response.body());
            
        } catch (Exception e) {
            System.out.println("Request " + requestId + " failed: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        ConcurrentTestClient client = new ConcurrentTestClient();
        client.runConcurrentStockTests(10);
    }
}