package org.example.ConcurrencyTests;

import org.example.presentation.controllers.OnlineController;
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
 * Comprehensive concurrency testing for online customer operations
 * Tests realistic customer scenarios with database under concurrent load
 */
public class OnlineCustomerConcurrencyTest {

    private OnlineController onlineController;
    private ExecutorService executorService;
    private Random random;

    // Test metrics
    private AtomicInteger successfulOperations;
    private AtomicInteger failedOperations;
    private AtomicLong totalResponseTime;
    private volatile boolean testCompleted;

    @BeforeEach
    void setUp() {
        onlineController = new OnlineController();
        executorService = Executors.newFixedThreadPool(15); // Conservative to avoid connection pool exhaustion
        random = new Random();
        successfulOperations = new AtomicInteger(0);
        failedOperations = new AtomicInteger(0);
        totalResponseTime = new AtomicLong(0);
        testCompleted = false;
    }

    /**
     * Test 1: Online Customer Registration Under Load
     * Tests if multiple customers can register simultaneously
     */
    @Test
    void testConcurrentOnlineCustomerRegistration() throws Exception {
        System.out.println("\n=== CONCURRENT ONLINE CUSTOMER REGISTRATION TEST ===");

        int numCustomers = 50; // Reduced for realistic testing
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numCustomers);

        System.out.println("Testing " + numCustomers + " concurrent online customer registrations...");

        long startTime = System.currentTimeMillis();

        // Create concurrent registration tasks
        for (int i = 0; i < numCustomers; i++) {
            final int customerId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    long operationStart = System.currentTimeMillis();

                    // Create unique customer data to avoid duplicates
                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                    Customer customer = new Customer("OnlineCustomer_" + customerId + "_" + uniqueId,
                            "071" + String.format("%07d", random.nextInt(9999999)));
                    String email = "customer" + customerId + "_" + uniqueId + "@test.com";
                    String address = "Address " + customerId + ", City " + uniqueId;

                    boolean success = onlineController.registerOnlineCustomer(customer, email, address);

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (success) {
                        successfulOperations.incrementAndGet();
                        System.out.println("Registration successful for customer " + customerId +
                                " (ID: " + customer.getId() + "), Response time: " + responseTime + " ms");
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println("Registration failed for customer " + customerId);
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Registration error for customer " + customerId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion with timeout
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Test should complete within 30 seconds");

        long totalTime = System.currentTimeMillis() - startTime;

        // Print results
        printTestResults("CONCURRENT ONLINE CUSTOMER REGISTRATION", totalTime, numCustomers);

        // Assert reasonable success rate (60% minimum considering connection limits)
        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numCustomers * 0.6),
                "At least 60% of online customer registrations should succeed. Success: " + successCount);
    }

    /**
     * Test 2: Website Inventory Browsing Under Load
     * Tests if multiple customers can browse inventory simultaneously
     */
    @Test
    void testConcurrentWebsiteInventoryBrowsing() throws Exception {
        System.out.println("\n=== CONCURRENT WEBSITE INVENTORY BROWSING TEST ===");

        resetCounters();
        int numCustomers = 100; // This should handle higher load as it's read-only
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numCustomers);

        System.out.println("Testing " + numCustomers + " concurrent customers browsing website inventory...");

        long startTime = System.currentTimeMillis();

        // Create concurrent browsing tasks
        for (int i = 0; i < numCustomers; i++) {
            final int customerId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();

                    List<Item> inventory = onlineController.getWebsiteInventory();

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (inventory != null && !inventory.isEmpty()) {
                        successfulOperations.incrementAndGet();
                        System.out.println("Customer " + customerId + " browsed inventory (" +
                                inventory.size() + " items), Response time: " + responseTime + " ms");
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println("Customer " + customerId + " failed to browse inventory");
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Inventory browsing error for customer " + customerId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Inventory browsing test should complete within 20 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("CONCURRENT WEBSITE INVENTORY BROWSING", totalTime, numCustomers);

        // Inventory browsing should have very high success rate
        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numCustomers * 0.95),
                "At least 95% of inventory browsing should succeed. Success: " + successCount);
    }

    /**
     * Test 3: Online Order Processing Under Load
     * Tests if multiple customers can place orders simultaneously
     */
    @Test
    void testConcurrentOnlineOrderProcessing() throws Exception {
        System.out.println("\n=== CONCURRENT ONLINE ORDER PROCESSING TEST ===");

        resetCounters();
        int numOrders = 30; // Conservative number to test order processing
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numOrders);

        System.out.println("Testing " + numOrders + " concurrent online order processing...");

        long startTime = System.currentTimeMillis();

        // Create concurrent order processing tasks
        for (int i = 0; i < numOrders; i++) {
            final int customerId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();

                    // Create a test customer for the order
                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                    Customer customer = new Customer("OrderCustomer_" + customerId + "_" + uniqueId,
                            "072" + String.format("%07d", random.nextInt(9999999)));
                    customer.setId(customerId + 1000); // Set a unique ID

                    // Create order items (small orders to avoid inventory depletion)
                    List<BillItem> orderItems = new ArrayList<>();

                    // Create a test item
                    Item testItem = new Item("TEST_ITEM_" + customerId, "Test Item " + customerId, 10.99);
                    testItem.setId(customerId + 1); // Unique item ID

                    // Add item to order
                    BillItem billItem = new BillItem(testItem, 1); // Order quantity of 1
                    orderItems.add(billItem);

                    // Process the order
                    Bill bill = onlineController.processOnlineOrder(orderItems, customer, 0.0);

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (bill != null && bill.getId() > 0) {
                        successfulOperations.incrementAndGet();
                        System.out.println("Customer " + customerId + " order processed (Bill ID: " +
                                bill.getId() + "), Response time: " + responseTime + " ms");
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println("Order processing failed for customer " + customerId);
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Order processing error for customer " + customerId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(45, TimeUnit.SECONDS);
        assertTrue(completed, "Order processing test should complete within 45 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("CONCURRENT ONLINE ORDER PROCESSING", totalTime, numOrders);

        // Order processing should have reasonable success rate
        int successCount = successfulOperations.get();
        assertTrue(successCount >= (numOrders * 0.5),
                "At least 50% of online orders should be processed successfully. Success: " + successCount);
    }

    /**
     * Test 4: Mixed Customer Activities
     * Tests realistic scenario with customers doing different activities
     */
    @Test
    void testMixedOnlineCustomerActivities() throws Exception {
        System.out.println("\n=== MIXED ONLINE CUSTOMER ACTIVITIES TEST ===");

        resetCounters();
        int totalCustomers = 60;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(totalCustomers);

        System.out.println("Testing " + totalCustomers + " customers with mixed online activities...");

        long startTime = System.currentTimeMillis();

        // Create mixed activity tasks
        for (int i = 0; i < totalCustomers; i++) {
            final int customerId = i;
            final int activityType = i % 3; // 0=browse, 1=register, 2=order

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long operationStart = System.currentTimeMillis();
                    boolean success = false;

                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

                    switch (activityType) {
                        case 0: // Browse inventory
                            List<Item> inventory = onlineController.getWebsiteInventory();
                            success = (inventory != null && !inventory.isEmpty());
                            if (success) {
                                System.out.println("Mixed activity " + customerId + " (browse) completed, " +
                                        inventory.size() + " items found");
                            }
                            break;

                        case 1: // Register customer
                            Customer customer = new Customer("MixedCustomer_" + customerId + "_" + uniqueId,
                                    "073" + String.format("%07d", random.nextInt(9999999)));
                            String email = "mixed" + customerId + "_" + uniqueId + "@test.com";
                            String address = "Mixed Address " + customerId + ", " + uniqueId;
                            success = onlineController.registerOnlineCustomer(customer, email, address);
                            if (success) {
                                System.out.println("Mixed activity " + customerId + " (register) completed, ID: " +
                                        customer.getId());
                            }
                            break;

                        case 2: // Place small order
                            Customer orderCustomer = new Customer("OrderMixed_" + customerId + "_" + uniqueId,
                                    "074" + String.format("%07d", random.nextInt(9999999)));
                            orderCustomer.setId(customerId + 2000);

                            List<BillItem> orderItems = new ArrayList<>();
                            Item item = new Item("MIX_ITEM_" + customerId, "Mixed Item " + customerId, 5.99);
                            item.setId(customerId + 100);
                            BillItem billItem = new BillItem(item, 1);
                            orderItems.add(billItem);

                            Bill bill = onlineController.processOnlineOrder(orderItems, orderCustomer, 0.0);
                            success = (bill != null && bill.getId() > 0);
                            if (success) {
                                System.out.println("Mixed activity " + customerId + " (order) completed, Bill ID: " +
                                        bill.getId());
                            }
                            break;
                    }

                    long responseTime = System.currentTimeMillis() - operationStart;
                    totalResponseTime.addAndGet(responseTime);

                    if (success) {
                        successfulOperations.incrementAndGet();
                    } else {
                        failedOperations.incrementAndGet();
                        System.out.println(
                                "Mixed activity failed for customer " + customerId + " (type " + activityType + ")");
                    }

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.out.println("Mixed activity error for customer " + customerId + ": " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "Mixed activities test should complete within 60 seconds");

        long totalTime = System.currentTimeMillis() - startTime;
        printTestResults("MIXED ONLINE CUSTOMER ACTIVITIES", totalTime, totalCustomers);

        // Mixed activities should have good success rate
        int successCount = successfulOperations.get();
        assertTrue(successCount >= (totalCustomers * 0.7),
                "At least 70% of mixed online activities should succeed. Success: " + successCount);
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
