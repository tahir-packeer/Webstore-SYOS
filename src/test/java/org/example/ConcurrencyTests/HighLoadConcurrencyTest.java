package org.example.ConcurrencyTests;

import org.example.presentation.controllers.Authentication;
import org.example.presentation.controllers.CustomerController;
import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.StockController;
import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Customer;
import org.example.persistence.models.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("High Load Concurrency Test Suite")
@Execution(ExecutionMode.CONCURRENT)
public class HighLoadConcurrencyTest {

    private static final int HIGH_LOAD_USERS = 500;
    private static final int MEDIUM_LOAD_USERS = 200;
    private static final int THREAD_POOL_SIZE = 100;
    private static final int TIMEOUT_SECONDS = 120;

    private ExecutorService executorService;
    private DatabaseConnection databaseConnection;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private AtomicLong totalResponseTime;
    private Random random;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        databaseConnection = DatabaseConnection.getInstance();
        successCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
        totalResponseTime = new AtomicLong(0);
        random = new Random();

        // Clean up any existing test data
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        cleanupTestData();
    }

    @Test
    @Order(1)
    @DisplayName("High Load User Registration Test - 500 Concurrent Users")
    void testHighLoadUserRegistration() throws InterruptedException {
        System.out.println("\n=== HIGH LOAD USER REGISTRATION TEST ===");
        System.out.println("Testing " + HIGH_LOAD_USERS + " concurrent user registrations...");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(HIGH_LOAD_USERS);
        long startTime = System.currentTimeMillis();

        // Create tasks for concurrent user registration
        for (int i = 0; i < HIGH_LOAD_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    long taskStartTime = System.nanoTime();
                    CustomerController customerController = new CustomerController();

                    // Create unique customer data
                    String uniqueId = String.valueOf(System.currentTimeMillis() + userId + random.nextInt(10000));
                    Customer customer = new Customer(
                            "LoadTestUser_" + uniqueId,
                            "077" + String.format("%07d", Math.abs(uniqueId.hashCode()) % 10000000));

                    // Attempt registration
                    customerController.add_Customer(customer);

                    long taskEndTime = System.nanoTime();
                    long responseTime = (taskEndTime - taskStartTime) / 1_000_000; // Convert to milliseconds

                    totalResponseTime.addAndGet(responseTime);
                    successCount.incrementAndGet();

                    if (userId % 50 == 0) {
                        System.out.printf("Registered user %d, Response time: %d ms%n", userId, responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.printf("Registration failed for user %d: %s%n", userId, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all tasks to complete
        boolean completed = completeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Print results
        printTestResults("USER REGISTRATION", startTime, endTime, HIGH_LOAD_USERS, completed);

        // Assertions
        assertTrue(completed, "Test should complete within timeout");
        assertTrue(successCount.get() > HIGH_LOAD_USERS * 0.1,
                "At least 10% of registrations should succeed under extreme load. Success: " + successCount.get());
    }

    @Test
    @Order(2)
    @DisplayName("High Load Authentication Test - 500 Concurrent Logins")
    void testHighLoadAuthentication() throws InterruptedException {
        System.out.println("\n=== HIGH LOAD AUTHENTICATION TEST ===");
        System.out.println("Testing " + HIGH_LOAD_USERS + " concurrent authentication attempts...");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(HIGH_LOAD_USERS);
        long startTime = System.currentTimeMillis();

        // Use hardcoded test credentials that should exist in database
        // These are simple test accounts that should be pre-created
        String[] testUsers = { "cashier1", "manager1", "admin", "testuser1", "testuser2" };
        String[] testPasswords = { "cash123", "store123", "admin123", "test123", "test123" };

        for (int i = 0; i < HIGH_LOAD_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long taskStartTime = System.nanoTime();
                    Authentication auth = new Authentication();

                    // Use different test credentials
                    int credIndex = userId % testUsers.length;
                    String username = testUsers[credIndex];
                    String password = testPasswords[credIndex];

                    // Attempt authentication
                    User user = auth.authenticateUser(username, password);

                    long taskEndTime = System.nanoTime();
                    long responseTime = (taskEndTime - taskStartTime) / 1_000_000;

                    if (user != null) {
                        totalResponseTime.addAndGet(responseTime);
                        successCount.incrementAndGet();

                        if (userId % 50 == 0) {
                            System.out.printf("Authenticated user %d (%s), Response time: %d ms%n",
                                    userId, username, responseTime);
                        }
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.printf("Authentication failed for user %d: %s%n", userId, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        printTestResults("AUTHENTICATION", startTime, endTime, HIGH_LOAD_USERS, completed);

        assertTrue(completed, "Test should complete within timeout");
        assertTrue(successCount.get() > HIGH_LOAD_USERS * 0.5,
                "At least 50% of authentications should succeed. Success: " + successCount.get());
    }

    @Test
    @Order(3)
    @DisplayName("Database Connection Pool Test - 200 Concurrent Database Operations")
    void testDatabaseConnectionPoolUnderLoad() throws InterruptedException {
        System.out.println("\n=== DATABASE CONNECTION POOL TEST ===");
        System.out.println("Testing " + MEDIUM_LOAD_USERS + " concurrent database operations...");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(MEDIUM_LOAD_USERS);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < MEDIUM_LOAD_USERS; i++) {
            final int operationId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long taskStartTime = System.nanoTime();
                    Connection connection = databaseConnection.connect();

                    // Perform database operations
                    String[] queries = {
                            "SELECT COUNT(*) FROM customers",
                            "SELECT COUNT(*) FROM items",
                            "SELECT COUNT(*) FROM stock",
                            "SELECT COUNT(*) FROM users"
                    };

                    String query = queries[operationId % queries.length];
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.executeQuery();
                    }

                    databaseConnection.closeConnection(connection);

                    long taskEndTime = System.nanoTime();
                    long responseTime = (taskEndTime - taskStartTime) / 1_000_000;

                    totalResponseTime.addAndGet(responseTime);
                    successCount.incrementAndGet();

                    if (operationId % 25 == 0) {
                        System.out.printf("DB Operation %d completed, Response time: %d ms%n",
                                operationId, responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.printf("DB Operation failed for operation %d: %s%n", operationId, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        printTestResults("DATABASE OPERATIONS", startTime, endTime, MEDIUM_LOAD_USERS, completed);

        assertTrue(completed, "Test should complete within timeout");
        assertTrue(successCount.get() > MEDIUM_LOAD_USERS * 0.5,
                "At least 50% of database operations should succeed under load. Success: " + successCount.get());
    }

    @Test
    @Order(4)
    @DisplayName("Mixed Operations Load Test - Registration + Authentication + Data Access")
    void testMixedOperationsUnderLoad() throws InterruptedException {
        System.out.println("\n=== MIXED OPERATIONS LOAD TEST ===");
        System.out.println("Testing mixed operations with " + MEDIUM_LOAD_USERS + " concurrent users...");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(MEDIUM_LOAD_USERS);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < MEDIUM_LOAD_USERS; i++) {
            final int operationId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long taskStartTime = System.nanoTime();

                    // Randomly choose operation type
                    int operationType = operationId % 4;

                    switch (operationType) {
                        case 0: // Customer Registration
                            CustomerController customerController = new CustomerController();
                            String uniqueId = String.valueOf(System.currentTimeMillis() + operationId);
                            Customer customer = new Customer(
                                    "MixedUser_" + uniqueId,
                                    "078" + String.format("%07d", Math.abs(uniqueId.hashCode()) % 10000000));
                            customerController.add_Customer(customer);
                            break;

                        case 1: // Authentication
                            Authentication auth = new Authentication();
                            auth.authenticateUser("cashier1", "cash123");
                            break;

                        case 2: // Item Controller Operations
                            ItemController itemController = new ItemController();
                            itemController.getAllItems();
                            break;

                        case 3: // Stock Controller Operations
                            StockController stockController = new StockController();
                            stockController.getAllStocks();
                            break;
                    }

                    long taskEndTime = System.nanoTime();
                    long responseTime = (taskEndTime - taskStartTime) / 1_000_000;

                    totalResponseTime.addAndGet(responseTime);
                    successCount.incrementAndGet();

                    if (operationId % 30 == 0) {
                        System.out.printf("Mixed operation %d (type %d) completed, Response time: %d ms%n",
                                operationId, operationType, responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.printf("Mixed operation failed for operation %d: %s%n", operationId, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        printTestResults("MIXED OPERATIONS", startTime, endTime, MEDIUM_LOAD_USERS, completed);

        assertTrue(completed, "Test should complete within timeout");
        assertTrue(successCount.get() > MEDIUM_LOAD_USERS * 0.5,
                "At least 50% of mixed operations should succeed. Success: " + successCount.get());
    }

    @Test
    @Order(5)
    @DisplayName("Stress Test - Extreme Load with 1000 Operations")
    void testExtremeLoadStress() throws InterruptedException {
        System.out.println("\n=== EXTREME LOAD STRESS TEST ===");
        final int EXTREME_LOAD = 1000;
        System.out.println("Testing " + EXTREME_LOAD + " concurrent operations for stress testing...");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(EXTREME_LOAD);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < EXTREME_LOAD; i++) {
            final int operationId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long taskStartTime = System.nanoTime();

                    // Simple authentication operation for stress test
                    Authentication auth = new Authentication();
                    auth.authenticateUser("cashier1", "cash123");

                    long taskEndTime = System.nanoTime();
                    long responseTime = (taskEndTime - taskStartTime) / 1_000_000;

                    totalResponseTime.addAndGet(responseTime);
                    successCount.incrementAndGet();

                    if (operationId % 100 == 0) {
                        System.out.printf("Stress operation %d completed, Response time: %d ms%n",
                                operationId, responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    if (operationId % 100 == 0) {
                        System.err.printf("Stress operation failed for operation %d: %s%n", operationId,
                                e.getMessage());
                    }
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS); // Extended timeout for stress
                                                                                        // test
        long endTime = System.currentTimeMillis();

        printTestResults("EXTREME LOAD STRESS", startTime, endTime, EXTREME_LOAD, completed);

        assertTrue(completed, "Stress test should complete within extended timeout");
        // More lenient success rate for stress test
        assertTrue(successCount.get() > EXTREME_LOAD * 0.7,
                "At least 70% of stress operations should succeed. Success: " + successCount.get());
    }

    private void printTestResults(String testName, long startTime, long endTime, int totalOperations,
            boolean completed) {
        long totalTime = endTime - startTime;
        double avgResponseTime = totalResponseTime.get() > 0 ? (double) totalResponseTime.get() / successCount.get()
                : 0;
        double throughput = (double) successCount.get() / (totalTime / 1000.0);
        double successRate = (double) successCount.get() / totalOperations * 100;

        System.out.println("\n" + "=".repeat(60));
        System.out.println(testName + " TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.printf("Test Completed: %s%n", completed ? "YES" : "NO (TIMEOUT)");
        System.out.printf("Total Time: %d ms (%.2f seconds)%n", totalTime, totalTime / 1000.0);
        System.out.printf("Total Operations: %d%n", totalOperations);
        System.out.printf("Successful Operations: %d%n", successCount.get());
        System.out.printf("Failed Operations: %d%n", failureCount.get());
        System.out.printf("Success Rate: %.2f%%%n", successRate);
        System.out.printf("Average Response Time: %.2f ms%n", avgResponseTime);
        System.out.printf("Throughput: %.2f operations/second%n", throughput);
        System.out.println("=".repeat(60));

        // Note: Counters are reset in @BeforeEach for each test method
    }

    private void cleanupTestData() {
        try {
            Connection connection = databaseConnection.connect();

            // Clean up test customers
            String[] cleanupQueries = {
                    "DELETE FROM customers WHERE name LIKE 'LoadTestUser_%' OR name LIKE 'MixedUser_%'",
                    "DELETE FROM customers WHERE contactNumber LIKE '077%' OR contactNumber LIKE '078%'"
            };

            for (String query : cleanupQueries) {
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.executeUpdate();
                }
            }

            databaseConnection.closeConnection(connection);
        } catch (Exception e) {
            // Ignore cleanup errors
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }
}
