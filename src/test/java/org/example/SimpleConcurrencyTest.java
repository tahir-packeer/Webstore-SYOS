package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Concurrency Test - Demonstrates actual concurrent user simulation
 * Tests basic operations that would happen when multiple users access the
 * system
 */
public class SimpleConcurrencyTest {

    private ExecutorService executorService;
    private AtomicInteger successCounter;
    private AtomicInteger failureCounter;
    private List<Long> responseTimes;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(50);
        successCounter = new AtomicInteger(0);
        failureCounter = new AtomicInteger(0);
        responseTimes = Collections.synchronizedList(new ArrayList<>());
    }

    @Test
    @DisplayName("Test 100 Concurrent User Registrations")
    void testConcurrentUserRegistrations() throws InterruptedException {
        System.out.println("=".repeat(60));
        System.out.println("STARTING: 100 Concurrent User Registration Test");
        System.out.println("=".repeat(60));

        int numberOfUsers = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfUsers);

        long testStartTime = System.currentTimeMillis();

        // Submit all registration tasks
        for (int i = 1; i <= numberOfUsers; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Simulate user registration
                    simulateUserRegistration(userId);

                } catch (Exception e) {
                    System.err.println("Error in user " + userId + ": " + e.getMessage());
                    failureCounter.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        System.out.println("All threads prepared. Starting concurrent execution...");

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all to complete (max 30 seconds)
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);

        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        // Calculate results
        int successful = successCounter.get();
        int failed = failureCounter.get();
        double successRate = (successful * 100.0) / numberOfUsers;
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        double throughput = (successful * 1000.0) / totalTestTime;

        // Print detailed results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONCURRENT USER REGISTRATION TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Test Completed: " + (completed ? "YES" : "TIMEOUT"));
        System.out.println("Total Test Time: " + totalTestTime + " ms (" + String.format("%.2f", totalTestTime / 1000.0)
                + " seconds)");
        System.out.println("Total Users: " + numberOfUsers);
        System.out.println("Successful Registrations: " + successful);
        System.out.println("Failed Registrations: " + failed);
        System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Average Response Time: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " registrations/second");
        System.out.println("=".repeat(60));

        // Assertions for test validation
        assertTrue(completed, "Test should complete within timeout");
        assertTrue(successRate >= 50, "At least 50% of registrations should succeed. Actual: " + successRate + "%");

        executorService.shutdown();
    }

    @Test
    @DisplayName("Test 50 Concurrent User Logins")
    void testConcurrentUserLogins() throws InterruptedException {
        System.out.println("=".repeat(60));
        System.out.println("STARTING: 50 Concurrent User Login Test");
        System.out.println("=".repeat(60));

        int numberOfUsers = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfUsers);

        long testStartTime = System.currentTimeMillis();

        // Submit all login tasks
        for (int i = 1; i <= numberOfUsers; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    simulateUserLogin(userId);
                } catch (Exception e) {
                    System.err.println("Login error for user " + userId + ": " + e.getMessage());
                    failureCounter.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        System.out.println("All login threads prepared. Starting concurrent execution...");

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        boolean completed = completeLatch.await(20, TimeUnit.SECONDS);

        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        // Calculate results
        int successful = successCounter.get();
        int failed = failureCounter.get();
        double successRate = (successful * 100.0) / numberOfUsers;
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        double throughput = (successful * 1000.0) / totalTestTime;

        // Print results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONCURRENT USER LOGIN TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Test Completed: " + (completed ? "YES" : "TIMEOUT"));
        System.out.println("Total Test Time: " + totalTestTime + " ms (" + String.format("%.2f", totalTestTime / 1000.0)
                + " seconds)");
        System.out.println("Total Login Attempts: " + numberOfUsers);
        System.out.println("Successful Logins: " + successful);
        System.out.println("Failed Logins: " + failed);
        System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Average Response Time: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " logins/second");
        System.out.println("=".repeat(60));

        assertTrue(completed, "Login test should complete within timeout");
        assertTrue(successRate >= 70, "At least 70% of logins should succeed. Actual: " + successRate + "%");

        executorService.shutdown();
    }

    @Test
    @DisplayName("Test Mixed Operations - 30 Users Doing Different Actions")
    void testMixedOperations() throws InterruptedException {
        System.out.println("=".repeat(60));
        System.out.println("STARTING: Mixed Operations Test (30 Users)");
        System.out.println("=".repeat(60));

        int numberOfOperations = 30;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfOperations);

        long testStartTime = System.currentTimeMillis();

        // Submit mixed operations
        for (int i = 1; i <= numberOfOperations; i++) {
            final int operationId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // Randomly choose operation type
                    int operationType = operationId % 3;
                    switch (operationType) {
                        case 0:
                            simulateUserRegistration(operationId);
                            break;
                        case 1:
                            simulateUserLogin(operationId);
                            break;
                        case 2:
                            simulateProductBrowsing(operationId);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Mixed operation " + operationId + " failed: " + e.getMessage());
                    failureCounter.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        System.out.println("All mixed operation threads prepared. Starting execution...");

        startLatch.countDown();
        boolean completed = completeLatch.await(15, TimeUnit.SECONDS);

        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        int successful = successCounter.get();
        int failed = failureCounter.get();
        double successRate = (successful * 100.0) / numberOfOperations;
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        double throughput = (successful * 1000.0) / totalTestTime;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("MIXED OPERATIONS TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Test Completed: " + (completed ? "YES" : "TIMEOUT"));
        System.out.println("Total Test Time: " + totalTestTime + " ms (" + String.format("%.2f", totalTestTime / 1000.0)
                + " seconds)");
        System.out.println("Total Operations: " + numberOfOperations);
        System.out.println("Successful Operations: " + successful);
        System.out.println("Failed Operations: " + failed);
        System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Average Response Time: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        System.out.println("=".repeat(60));

        assertTrue(completed, "Mixed operations test should complete within timeout");
        assertTrue(successRate >= 60, "At least 60% of mixed operations should succeed. Actual: " + successRate + "%");

        executorService.shutdown();
    }

    /**
     * Simulates user registration process
     */
    private void simulateUserRegistration(int userId) {
        long startTime = System.currentTimeMillis();

        try {
            // Simulate registration validation (50-150ms)
            Thread.sleep(50 + (int) (Math.random() * 100));

            // Simulate database insertion (100-300ms)
            Thread.sleep(100 + (int) (Math.random() * 200));

            // Simulate email sending (50-100ms)
            Thread.sleep(50 + (int) (Math.random() * 50));

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            responseTimes.add(responseTime);

            successCounter.incrementAndGet();
            System.out.println("✅ User " + userId + " registered successfully (Response time: " + responseTime + "ms)");

        } catch (InterruptedException e) {
            failureCounter.incrementAndGet();
            System.err.println("❌ User " + userId + " registration failed: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulates user login process
     */
    private void simulateUserLogin(int userId) {
        long startTime = System.currentTimeMillis();

        try {
            // Simulate credential validation (30-80ms)
            Thread.sleep(30 + (int) (Math.random() * 50));

            // Simulate session creation (20-50ms)
            Thread.sleep(20 + (int) (Math.random() * 30));

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            responseTimes.add(responseTime);

            successCounter.incrementAndGet();
            System.out.println("🔑 User " + userId + " logged in successfully (Response time: " + responseTime + "ms)");

        } catch (InterruptedException e) {
            failureCounter.incrementAndGet();
            System.err.println("❌ User " + userId + " login failed: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulates product browsing
     */
    private void simulateProductBrowsing(int userId) {
        long startTime = System.currentTimeMillis();

        try {
            // Simulate product catalog loading (40-120ms)
            Thread.sleep(40 + (int) (Math.random() * 80));

            // Simulate image loading (30-70ms)
            Thread.sleep(30 + (int) (Math.random() * 40));

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            responseTimes.add(responseTime);

            successCounter.incrementAndGet();
            System.out.println(
                    "🛍️ User " + userId + " browsed products successfully (Response time: " + responseTime + "ms)");

        } catch (InterruptedException e) {
            failureCounter.incrementAndGet();
            System.err.println("❌ User " + userId + " browsing failed: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
