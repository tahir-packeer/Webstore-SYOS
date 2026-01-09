package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class RapidRequestTestClient {
    private static final String BASE_URL = "http://localhost:8080/syos";
    private final HttpClient client;
    
    public RapidRequestTestClient() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }
    
    public void sendRapidRequests() {
        System.out.println("Starting rapid request test...");
        
        // Send 20 requests as fast as possible
        for (int i = 0; i < 20; i++) {
            final int requestId = i;
            
            CompletableFuture.runAsync(() -> {
                sendAsyncRequest(requestId);
            });
            
            // Very small delay between requests
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Wait a bit for all requests to complete
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Rapid request test completed!");
    }
    
    private void sendAsyncRequest(int requestId) {
        try {
            String json = String.format("""
                {
                    "name": "Rapid Test Customer %d",
                    "contactNumber": "077123%04d",
                    "email": "test%d@example.com",
                    "password": "password123"
                }
                """, requestId, 4000 + requestId, requestId);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/customers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Rapid Request " + requestId + ": " + response.statusCode() + " - " + response.body());
            
        } catch (Exception e) {
            System.out.println("Rapid Request " + requestId + " failed: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        RapidRequestTestClient client1 = new RapidRequestTestClient();
        RapidRequestTestClient client2 = new RapidRequestTestClient();
        
        // Run two clients simultaneously
        CompletableFuture<Void> client1Future = CompletableFuture.runAsync(client1::sendRapidRequests);
        CompletableFuture<Void> client2Future = CompletableFuture.runAsync(client2::sendRapidRequests);
        
        CompletableFuture.allOf(client1Future, client2Future).join();
        
        System.out.println("Both test clients completed!");
    }
}