package org.example.ConcurrencyTests;

import org.example.presentation.controllers.OnlineController;
import org.example.presentation.controllers.CustomerController;
import org.example.presentation.controllers.BillController;
import org.example.persistence.models.Customer;
import org.example.persistence.models.Item;
import org.example.persistence.models.BillItem;
import org.example.persistence.models.Bill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database Integration Tests for Online Customer Operations
 * Tests database consistency and transaction handling under load
 */
public class DatabaseIntegrationTest {

    private OnlineController onlineController;
    private CustomerController customerController;
    private BillController billController;
    private ExecutorService executorService;
    private Random random;

    // Test metrics
    private AtomicInteger successfulOperations;
    private AtomicInteger failedOperations;
    private AtomicLong totalResponseTime;

    @BeforeEach
    void setUp() {
        onlineController = new OnlineController();
        customerController = new CustomerController();
        billController = new BillController();
        executorService = Executors.newFixedThreadPool(10);
        random = new Random();
        successfulOperations = new AtomicInteger(0);
        failedOperations = new AtomicInteger(0);
        totalResponseTime = new AtomicLong(0);
    }

    /**
     * Test 1: Database Transaction Consistency
     * Tests if database maintains consistency under concurrent operations
     */
    @Test
    void testDatabaseTransactionConsistency() throws Exception {
        System.out.println("\n=== DATABASE TRANSACTION CONSISTENCY TEST ===");

        resetCounters();
        int numOperations = 40;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numOperations);

        System.out.println("Testing database consistency with " + numOperations + " concurrent operations...");

        long startTime = System.currentTimeMillis();

        // Mix of different database operations
        for (int i = 0; i < numOperations; i++) {
            final int operationId = i;
            final int operationType = i % 4; // 0=register online, 1=register store, 2=browse, 3=order

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();
                    boolean success = false;
                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

                    switch (operationType) {
                        case 0: // Online customer registration
                            Customer onlineCustomer = new Customer("DB_Online_" + operationId + "_" + uniqueId,
                                    "070" + String.format("%07d", random.nextInt(9999999)));
                            String email = "db_online" + operationId + "_" + uniqueId + "@test.com";
                            String address = "DB Address " + operationId;
                            success = onlineController.registerOnlineCustomer(onlineCustomer, email, address);
                            if (success) {
                                System.out.println("DB Test: Online customer " + operationId + " registered (ID: "
                                        + onlineCustomer.getId() + ")");
                            }
                            break;

                        case 1: // Store customer registration
                            Customer storeCustomer = new Customer("DB_Store_" + operationId + "_" + uniqueId,
                                    "071" + String.format("%07d", random.nextInt(9999999)));
                            try {
                                customerController.add_Customer(storeCustomer);
                                success = (storeCustomer.getId() > 0);
                                if (success) {
                                    System.out.println("DB Test: Store customer " + operationId + " registered (ID: "
                                            + storeCustomer.getId() + ")");
                                }
                            } catch (Exception e) {
                                success = false;
                                System.out.println("DB Test: Store customer registration failed: " + e.getMessage());
                            }
                            break;

                        case 2: // Inventory browsing
                            List<Item> inventory = onlineController.getWebsiteInventory();
                            success = (inventory != null);
                            if (success) {
                                System.out.println("DB Test: Operation " + operationId + " browsed " + inventory.size()
                                        + " items");
                            }
                            break;

                        case 3: // Order processing
                            Customer orderCustomer = new Customer("DB_Order_" + operationId + "_" + uniqueId,
                                    "072" + String.format("%07d", random.nextInt(9999999)));
                            orderCustomer.setId(operationId + 3000);

                            List<BillItem> orderItems = new ArrayList<>();
                            Item item = new Item("DB_ITEM_" + operationId, "DB Item " + operationId, 15.99);
                            item.setId(operationId + 200);
                            BillItem billItem = new BillItem(item, 1);
                            orderItems.add(billItem);

                            Bill bill = onlineController.processOnlineOrder(orderItems, orderCustomer, 0.0);
                            success = (bill != null && bill.getId() > 0);
                            if (success) {
                                System.out.println(
                                        "DB Test: Order " + operationId + " processed (Bill ID: " + bill.getId() + ")");
                            }
                            break;
                    }

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (success) {
                        successfulOperations.incrementAndGet();
                    } else {
                        failedOperations.incrementAndGet();
                        System.out
                                .println("DB Test: Operation " + operationId + " (type " + operationType + ") failed");
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("DB Test error for operation " + operationId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "Database consistency test should complete within 60 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("DATABASE TRANSACTION CONSISTENCY", totalTime, numOperations);

        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numOperations * 0.6),
                "At least 60% of database operations should succeed. Success: " + successCount);
    }

    /**
     * Test 2: Connection Pool Stress Test
     * Tests database connection handling under high load
     */
    @Test
    void testConnectionPoolStress() throws Exception {
        System.out.println("\n=== DATABASE CONNECTION POOL STRESS TEST ===");

        resetCounters();
        int numConnections = 25; // Test connection pool limits
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numConnections);

        System.out.println("Testing connection pool with " + numConnections + " concurrent database operations...");

        long startTime = System.currentTimeMillis();

        // Create operations that hold connections for different durations
        for (int i = 0; i < numConnections; i++) {
            final int connectionId = i;

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();

                    // Perform multiple database operations in sequence to stress connections
                    boolean success = true;

                    // Operation 1: Browse inventory
                    List<Item> inventory = onlineController.getWebsiteInventory();
                    if (inventory == null) {
                        success = false;
                    }

                    // Small delay to simulate processing time
                    Thread.sleep(50 + random.nextInt(100));

                    // Operation 2: Register a customer if inventory browsing succeeded
                    if (success) {
                        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                        Customer customer = new Customer("Conn_" + connectionId + "_" + uniqueId,
                                "075" + String.format("%07d", random.nextInt(9999999)));
                        String email = "conn" + connectionId + "_" + uniqueId + "@test.com";
                        String address = "Connection Test Address " + connectionId;

                        success = onlineController.registerOnlineCustomer(customer, email, address);
                        if (success) {
                            System.out.println("Connection " + connectionId + " completed sequence (Customer ID: "
                                    + customer.getId() + ")");
                        }
                    }

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (success) {
                        successfulOperations.incrementAndGet();
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println("Connection test " + connectionId + " failed");
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Connection error " + connectionId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(45, TimeUnit.SECONDS);
        assertTrue(completed, "Connection pool test should complete within 45 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("DATABASE CONNECTION POOL STRESS", totalTime, numConnections);

        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numConnections * 0.5),
                "At least 50% of connection pool operations should succeed. Success: " + successCount);
    }

    /**
     * Test 3: Invoice Generation Uniqueness Test
     * Tests if invoice numbers remain unique under concurrent load
     */
    @Test
    void testInvoiceGenerationUniqueness() throws Exception {
        System.out.println("\n=== INVOICE GENERATION UNIQUENESS TEST ===");

        resetCounters();
        int numOrders = 20; // Test invoice generation
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numOrders);
        List<String> generatedInvoices = new ArrayList<>();

        System.out.println("Testing invoice uniqueness with " + numOrders + " concurrent orders...");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numOrders; i++) {
            final int orderId = i;

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();

                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                    Customer customer = new Customer("Invoice_" + orderId + "_" + uniqueId,
                            "076" + String.format("%07d", random.nextInt(9999999)));
                    customer.setId(orderId + 4000);

                    List<BillItem> orderItems = new ArrayList<>();
                    Item item = new Item("INV_ITEM_" + orderId, "Invoice Item " + orderId, 20.99);
                    item.setId(orderId + 300);
                    BillItem billItem = new BillItem(item, 1);
                    orderItems.add(billItem);

                    Bill bill = onlineController.processOnlineOrder(orderItems, customer, 0.0);

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (bill != null && bill.getId() > 0) {
                        synchronized (generatedInvoices) {
                            generatedInvoices.add(bill.getInvoiceNumber());
                        }
                        successfulOperations.incrementAndGet();
                        System.out.println("Invoice test " + orderId + " generated: " + bill.getInvoiceNumber() +
                                " (Bill ID: " + bill.getId() + ")");
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println("Invoice generation failed for order " + orderId);
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Invoice generation error " + orderId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(40, TimeUnit.SECONDS);
        assertTrue(completed, "Invoice generation test should complete within 40 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("INVOICE GENERATION UNIQUENESS", totalTime, numOrders);

        // Check for duplicate invoices
        long uniqueInvoices = generatedInvoices.stream().distinct().count();
        System.out.println("Generated invoices: " + generatedInvoices.size());
        System.out.println("Unique invoices: " + uniqueInvoices);

        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numOrders * 0.4),
                "At least 40% of invoice generation should succeed. Success: " + successCount);

        if (successCount > 0) {
            assertTrue(uniqueInvoices == generatedInvoices.size(),
                    "All generated invoice numbers should be unique. Duplicates found!");
        }
    }

    /**
     * Reset counters for each test
     */
    private void resetCounters() {
        successfulOperations.set(0);
        failedOperations.set(0);
        totalResponseTime.set(0);
    }

    /**
     * Print detailed test results
     */
    private void printTestResults(String testName, long totalTime, int totalOperations) {
        int successCount = successfulOperations.get();
        int failureCount = failedOperations.get();
        double successRate = (successCount * 100.0) / totalOperations;
        double avgResponseTime = totalResponseTime.get() / (double) Math.max(successCount, 1);
        double throughput = (successCount * 1000.0) / totalTime;

        System.out.println("\n============================================================");
        System.out.println(testName + " TEST RESULTS");
        System.out.println("============================================================");
        System.out.println("Test Completed: YES");
        System.out.println(
                "Total Time: " + totalTime + " ms (" + String.format("%.2f", totalTime / 1000.0) + " seconds)");
        System.out.println("Total Operations: " + totalOperations);
        System.out.println("Successful Operations: " + successCount);
        System.out.println("Failed Operations: " + failureCount);
        System.out.println("Success Rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Average Response Time: " + String.format("%.2f", avgResponseTime) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        System.out.println("============================================================\n");
    }
}
