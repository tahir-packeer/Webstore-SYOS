package org.example.ConcurrencyTests;

import org.example.presentation.controllers.Authentication;
import org.example.presentation.controllers.CustomerController;
import org.example.persistence.database.DatabaseConnection;
import org.example.persistence.models.Customer;
import org.example.persistence.models.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Load Simulation Tests")
@Execution(ExecutionMode.CONCURRENT)
public class RealWorldUserLoadTest {

    private static final int SIMULTANEOUS_REGISTRATIONS = 100;
    private static final int SIMULTANEOUS_LOGINS = 200;

    private ExecutorService executorService;
    private DatabaseConnection databaseConnection;
    private Map<String, Long> responseTimeMetrics;
    private AtomicInteger totalRequests;
    private AtomicInteger successfulRequests;
    private AtomicInteger failedRequests;

    @BeforeEach
    void setUp() {
        executorService = Executors.newCachedThreadPool();
        databaseConnection = DatabaseConnection.getInstance();
        responseTimeMetrics = new ConcurrentHashMap<>();
        totalRequests = new AtomicInteger(0);
        successfulRequests = new AtomicInteger(0);
        failedRequests = new AtomicInteger(0);

        cleanupTestUsers();
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        cleanupTestUsers();
        printFinalMetrics();
    }

    @Test
    @Order(1)
    @DisplayName("Realistic Registration Rush - 100 Users Registering Simultaneously")
    void testRegistrationRushScenario() throws InterruptedException {
        System.out.println("\n🚀 TESTING REGISTRATION RUSH SCENARIO");
        System.out
                .println("Simulating " + SIMULTANEOUS_REGISTRATIONS + " users trying to register at the same time...");

        CountDownLatch readyLatch = new CountDownLatch(SIMULTANEOUS_REGISTRATIONS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(SIMULTANEOUS_REGISTRATIONS);

        List<String> successfulUsers = new CopyOnWriteArrayList<>();
        List<String> failedUsers = new CopyOnWriteArrayList<>();

        // Simulate users trying to register at exactly the same time
        for (int i = 0; i < SIMULTANEOUS_REGISTRATIONS; i++) {
            final int userId = i;
            final String username = "rushuser_" + userId + "_" + System.currentTimeMillis();
            final String phone = "070" + String.format("%07d", userId + 1000000);

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // All threads wait here until released simultaneously

                    long startTime = System.nanoTime();

                    CustomerController customerController = new CustomerController();
                    Customer customer = new Customer(username, phone);

                    customerController.add_Customer(customer);

                    long endTime = System.nanoTime();
                    long responseTimeMs = (endTime - startTime) / 1_000_000;

                    successfulUsers.add(username);
                    successfulRequests.incrementAndGet();
                    responseTimeMetrics.put("registration_" + userId, responseTimeMs);

                    System.out.printf("✅ User %s registered successfully in %d ms%n", username, responseTimeMs);

                } catch (Exception e) {
                    failedUsers.add(username);
                    failedRequests.incrementAndGet();
                    System.err.printf("❌ Registration failed for %s: %s%n", username, e.getMessage());
                } finally {
                    totalRequests.incrementAndGet();
                    finishLatch.countDown();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await();
        System.out.println("⏳ All " + SIMULTANEOUS_REGISTRATIONS + " registration threads ready... Starting NOW!");

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for completion with timeout
        boolean completed = finishLatch.await(60, TimeUnit.SECONDS);

        // Analyze results
        System.out.println("\n📊 REGISTRATION RUSH RESULTS:");
        System.out.println("✅ Successful registrations: " + successfulUsers.size());
        System.out.println("❌ Failed registrations: " + failedUsers.size());
        System.out.println("📈 Success rate: " + (successfulUsers.size() * 100.0 / SIMULTANEOUS_REGISTRATIONS) + "%");

        if (!failedUsers.isEmpty()) {
            System.out.println("⚠️ First few failures: " + failedUsers.subList(0, Math.min(5, failedUsers.size())));
        }

        assertTrue(completed, "Registration rush should complete within timeout");
        assertTrue(successfulUsers.size() >= SIMULTANEOUS_REGISTRATIONS * 0.8,
                "At least 80% of registrations should succeed during rush");
    }

    @Test
    @Order(2)
    @DisplayName("Peak Hour Login Storm - 200 Users Logging In Simultaneously")
    void testPeakHourLoginStorm() throws InterruptedException {
        System.out.println("\n⚡ TESTING PEAK HOUR LOGIN STORM");
        System.out.println("Simulating " + SIMULTANEOUS_LOGINS + " users trying to login during peak hours...");

        CountDownLatch readyLatch = new CountDownLatch(SIMULTANEOUS_LOGINS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(SIMULTANEOUS_LOGINS);

        AtomicInteger successfulLogins = new AtomicInteger(0);
        AtomicInteger failedLogins = new AtomicInteger(0);
        AtomicReference<String> slowestLogin = new AtomicReference<>("");
        AtomicReference<Long> slowestTime = new AtomicReference<>(0L);

        // Test with existing user credentials
        String[] testCredentials = {
                "cashier1:cash123",
                "manager1:store123",
                "admin:admin123"
        };

        for (int i = 0; i < SIMULTANEOUS_LOGINS; i++) {
            final int loginId = i;
            final String credential = testCredentials[i % testCredentials.length];
            final String[] parts = credential.split(":");
            final String username = parts[0];
            final String password = parts[1];

            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    long startTime = System.nanoTime();

                    Authentication auth = new Authentication();
                    User user = auth.authenticateUser(username, password);

                    long endTime = System.nanoTime();
                    long responseTimeMs = (endTime - startTime) / 1_000_000;

                    if (user != null) {
                        successfulLogins.incrementAndGet();
                        responseTimeMetrics.put("login_" + loginId, responseTimeMs);

                        // Track slowest login
                        if (responseTimeMs > slowestTime.get()) {
                            slowestTime.set(responseTimeMs);
                            slowestLogin.set(username + " (attempt " + loginId + ")");
                        }

                        if (loginId % 20 == 0) {
                            System.out.printf("✅ Login %d successful for %s in %d ms%n", loginId, username,
                                    responseTimeMs);
                        }
                    } else {
                        failedLogins.incrementAndGet();
                        System.err.printf("❌ Login %d failed for %s%n", loginId, username);
                    }

                } catch (Exception e) {
                    failedLogins.incrementAndGet();
                    System.err.printf("❌ Login exception for attempt %d (%s): %s%n", loginId, username, e.getMessage());
                } finally {
                    totalRequests.incrementAndGet();
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("⏳ All " + SIMULTANEOUS_LOGINS + " login threads ready... Starting NOW!");

        startLatch.countDown();
        boolean completed = finishLatch.await(90, TimeUnit.SECONDS);

        // Calculate average response time for logins
        double avgLoginTime = responseTimeMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("login_"))
                .mapToLong(Map.Entry::getValue)
                .average()
                .orElse(0.0);

        System.out.println("\n📊 PEAK HOUR LOGIN STORM RESULTS:");
        System.out.println("✅ Successful logins: " + successfulLogins.get());
        System.out.println("❌ Failed logins: " + failedLogins.get());
        System.out.println("📈 Success rate: " + (successfulLogins.get() * 100.0 / SIMULTANEOUS_LOGINS) + "%");
        System.out.printf("⏱️ Average login time: %.2f ms%n", avgLoginTime);
        System.out.printf("🐌 Slowest login: %s in %d ms%n", slowestLogin.get(), slowestTime.get());

        assertTrue(completed, "Login storm should complete within timeout");
        assertTrue(successfulLogins.get() >= SIMULTANEOUS_LOGINS * 0.9,
                "At least 90% of logins should succeed during peak hour");
        assertTrue(avgLoginTime < 5000, "Average login time should be under 5 seconds");
    }

    @Test
    @Order(3)
    @DisplayName("Database Connection Pool Exhaustion Test")
    void testDatabaseConnectionPoolExhaustion() throws InterruptedException {
        System.out.println("\n💾 TESTING DATABASE CONNECTION POOL UNDER PRESSURE");

        final int DB_OPERATIONS = 150;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(DB_OPERATIONS);

        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger failedConnections = new AtomicInteger(0);
        AtomicInteger activeConnections = new AtomicInteger(0);
        AtomicInteger maxConcurrentConnections = new AtomicInteger(0);

        for (int i = 0; i < DB_OPERATIONS; i++) {
            final int operationId = i;

            executorService.submit(() -> {
                Connection connection = null;
                try {
                    startLatch.await();

                    long startTime = System.nanoTime();
                    int currentActive = activeConnections.incrementAndGet();

                    // Update max concurrent connections
                    maxConcurrentConnections.updateAndGet(current -> Math.max(current, currentActive));

                    connection = databaseConnection.connect();

                    // Simulate database work
                    String query = "SELECT COUNT(*) FROM customers WHERE id > ?";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setInt(1, operationId % 100);
                        stmt.executeQuery();
                    }

                    // Hold connection for a bit to simulate real usage
                    Thread.sleep(100 + (operationId % 200)); // 100-300ms hold time

                    long endTime = System.nanoTime();
                    long responseTimeMs = (endTime - startTime) / 1_000_000;

                    successfulConnections.incrementAndGet();
                    responseTimeMetrics.put("db_op_" + operationId, responseTimeMs);

                    if (operationId % 15 == 0) {
                        System.out.printf("💾 DB Operation %d completed in %d ms (Active: %d)%n",
                                operationId, responseTimeMs, currentActive);
                    }

                } catch (Exception e) {
                    failedConnections.incrementAndGet();
                    System.err.printf("❌ DB Operation %d failed: %s%n", operationId, e.getMessage());
                } finally {
                    if (connection != null) {
                        databaseConnection.closeConnection(connection);
                    }
                    activeConnections.decrementAndGet();
                    totalRequests.incrementAndGet();
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(120, TimeUnit.SECONDS);

        double avgDbTime = responseTimeMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("db_op_"))
                .mapToLong(Map.Entry::getValue)
                .average()
                .orElse(0.0);

        System.out.println("\n📊 DATABASE CONNECTION POOL TEST RESULTS:");
        System.out.println("✅ Successful DB operations: " + successfulConnections.get());
        System.out.println("❌ Failed DB operations: " + failedConnections.get());
        System.out.printf("📈 Success rate: %.2f%%%n", (successfulConnections.get() * 100.0 / DB_OPERATIONS));
        System.out.printf("⏱️ Average DB operation time: %.2f ms%n", avgDbTime);
        System.out.println("🔗 Max concurrent connections: " + maxConcurrentConnections.get());

        assertTrue(completed, "DB operations should complete within timeout");
        assertTrue(successfulConnections.get() >= DB_OPERATIONS * 0.95,
                "At least 95% of DB operations should succeed");
    }

    @Test
    @Order(4)
    @DisplayName("Real-World Mixed Load Simulation - Peak Business Hours")
    void testRealWorldMixedLoadSimulation() throws InterruptedException {
        System.out.println("\n🌟 TESTING REAL-WORLD MIXED LOAD - PEAK BUSINESS HOURS");
        System.out.println("Simulating realistic business scenario with mixed operations...");

        final int REGISTRATION_USERS = 50;
        final int LOGIN_USERS = 150;
        final int DATA_ACCESS_USERS = 100;
        final int TOTAL_OPERATIONS = REGISTRATION_USERS + LOGIN_USERS + DATA_ACCESS_USERS;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(TOTAL_OPERATIONS);

        AtomicInteger registrations = new AtomicInteger(0);
        AtomicInteger logins = new AtomicInteger(0);
        AtomicInteger dataAccess = new AtomicInteger(0);

        // Registration operations
        for (int i = 0; i < REGISTRATION_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> performRegistration(userId, startLatch, finishLatch, registrations));
        }

        // Login operations
        for (int i = 0; i < LOGIN_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> performLogin(userId, startLatch, finishLatch, logins));
        }

        // Data access operations
        for (int i = 0; i < DATA_ACCESS_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> performDataAccess(userId, startLatch, finishLatch, dataAccess));
        }

        System.out.println("⏳ All " + TOTAL_OPERATIONS + " mixed operations ready... Starting NOW!");
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        boolean completed = finishLatch.await(150, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        double totalTimeSeconds = (endTime - startTime) / 1000.0;
        double throughput = totalRequests.get() / totalTimeSeconds;

        System.out.println("\n📊 REAL-WORLD MIXED LOAD RESULTS:");
        System.out.println("✅ Successful registrations: " + registrations.get() + "/" + REGISTRATION_USERS);
        System.out.println("✅ Successful logins: " + logins.get() + "/" + LOGIN_USERS);
        System.out.println("✅ Successful data access: " + dataAccess.get() + "/" + DATA_ACCESS_USERS);
        System.out.println("📊 Total successful operations: " + successfulRequests.get() + "/" + TOTAL_OPERATIONS);
        System.out.printf("📈 Overall success rate: %.2f%%%n", (successfulRequests.get() * 100.0 / TOTAL_OPERATIONS));
        System.out.printf("⚡ Throughput: %.2f operations/second%n", throughput);
        System.out.printf("⏱️ Total test time: %.2f seconds%n", totalTimeSeconds);

        assertTrue(completed, "Mixed load test should complete within timeout");
        assertTrue(successfulRequests.get() >= TOTAL_OPERATIONS * 0.85,
                "At least 85% of mixed operations should succeed");
    }

    private void performRegistration(int userId, CountDownLatch startLatch, CountDownLatch finishLatch,
            AtomicInteger counter) {
        try {
            startLatch.await();

            String username = "mixeduser_" + userId + "_" + System.currentTimeMillis();
            String phone = "071" + String.format("%07d", userId + 2000000);

            CustomerController customerController = new CustomerController();
            Customer customer = new Customer(username, phone);
            customerController.add_Customer(customer);

            counter.incrementAndGet();
            successfulRequests.incrementAndGet();

        } catch (Exception e) {
            failedRequests.incrementAndGet();
        } finally {
            totalRequests.incrementAndGet();
            finishLatch.countDown();
        }
    }

    private void performLogin(int userId, CountDownLatch startLatch, CountDownLatch finishLatch,
            AtomicInteger counter) {
        try {
            startLatch.await();

            Authentication auth = new Authentication();
            String[] credentials = { "cashier1:cash123", "manager1:store123", "admin:admin123" };
            String[] parts = credentials[userId % credentials.length].split(":");

            User user = auth.authenticateUser(parts[0], parts[1]);
            if (user != null) {
                counter.incrementAndGet();
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }

        } catch (Exception e) {
            failedRequests.incrementAndGet();
        } finally {
            totalRequests.incrementAndGet();
            finishLatch.countDown();
        }
    }

    private void performDataAccess(int userId, CountDownLatch startLatch, CountDownLatch finishLatch,
            AtomicInteger counter) {
        Connection connection = null;
        try {
            startLatch.await();

            connection = databaseConnection.connect();
            String[] queries = {
                    "SELECT COUNT(*) FROM customers",
                    "SELECT COUNT(*) FROM items",
                    "SELECT COUNT(*) FROM stock"
            };

            String query = queries[userId % queries.length];
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.executeQuery();
            }

            counter.incrementAndGet();
            successfulRequests.incrementAndGet();

        } catch (Exception e) {
            failedRequests.incrementAndGet();
        } finally {
            if (connection != null) {
                databaseConnection.closeConnection(connection);
            }
            totalRequests.incrementAndGet();
            finishLatch.countDown();
        }
    }

    private void cleanupTestUsers() {
        try {
            Connection connection = databaseConnection.connect();
            String[] cleanupQueries = {
                    "DELETE FROM customers WHERE name LIKE 'rushuser_%'",
                    "DELETE FROM customers WHERE name LIKE 'mixeduser_%'",
                    "DELETE FROM customers WHERE contactNumber LIKE '070%'",
                    "DELETE FROM customers WHERE contactNumber LIKE '071%'"
            };

            for (String query : cleanupQueries) {
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.executeUpdate();
                }
            }
            databaseConnection.closeConnection(connection);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void printFinalMetrics() {
        if (totalRequests.get() > 0) {
            System.out.println("\n🎯 FINAL TEST SESSION METRICS:");
            System.out.println("📊 Total requests: " + totalRequests.get());
            System.out.println("✅ Successful requests: " + successfulRequests.get());
            System.out.println("❌ Failed requests: " + failedRequests.get());
            System.out.printf("📈 Overall session success rate: %.2f%%%n",
                    (successfulRequests.get() * 100.0 / totalRequests.get()));
        }
    }
}
