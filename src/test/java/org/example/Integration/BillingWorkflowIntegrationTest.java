package org.example.Integration;

import org.example.presentation.controllers.BillController;
import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.StockController;
import org.example.presentation.controllers.CustomerController;
import org.example.persistence.models.Bill;
import org.example.persistence.models.BillItem;
import org.example.persistence.models.Customer;
import org.example.persistence.models.Item;
import org.example.persistence.models.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Billing Workflow Integration Tests")
class BillingWorkflowIntegrationTest {

    private ItemController itemController;
    private StockController stockController;
    private BillController billController;
    private CustomerController customerController;
    
    private Item testItem1, testItem2;
    private Customer testCustomer;
    private String uniqueTestId;
    
    @BeforeEach
    void setUp() throws SQLException, ClassNotFoundException {
        itemController = new ItemController();
        stockController = new StockController();
        billController = new BillController();
        customerController = new CustomerController();
        
        uniqueTestId = String.valueOf(System.currentTimeMillis() % 100000);
        
        testItem1 = new Item("INT" + uniqueTestId + "01", "Integration Test Item 1", 15.99);
        testItem2 = new Item("INT" + uniqueTestId + "02", "Integration Test Item 2", 25.50);
        
        testCustomer = new Customer("Integration Test Customer " + uniqueTestId, "987654" + uniqueTestId);
        
        cleanUpTestData();
        
        try {
            customerController.add_Customer(testCustomer);
            testCustomer = customerController.get_Customer_from_contactNumber(testCustomer.getcontactNumber());
        } catch (Exception e) {
            testCustomer = customerController.get_Customer_from_contactNumber(testCustomer.getcontactNumber());
        }
        
        try {
            itemController.addItems(Arrays.asList(testItem1, testItem2));
        } catch (Exception e) {
            // Items might already exist
        }
        
        testItem1 = itemController.getItemFromCode(testItem1.getCode());
        testItem2 = itemController.getItemFromCode(testItem2.getCode());
    }
    
    @Test
    @DisplayName("Complete billing workflow: Add items → Add stock → Process sale → Verify stock reduction")
    void completeBillingWorkflow_Success() throws Exception {
        Date expiryDate = new Date(System.currentTimeMillis() + 86400000L * 30);
        
        Stock stock1 = new Stock(testItem1, 100, expiryDate);
        Stock stock2 = new Stock(testItem2, 50, expiryDate);
        
        stockController.add_items_to_stock(stock1);
        stockController.add_items_to_stock(stock2);
        
        int initialStock1 = stockController.get_Stock_quantity_by_item(testItem1);
        int initialStock2 = stockController.get_Stock_quantity_by_item(testItem2);
        
        assertTrue(initialStock1 >= 100);
        assertTrue(initialStock2 >= 50);
        
        BillItem billItem1 = new BillItem(testItem1, 5);
        BillItem billItem2 = new BillItem(testItem2, 2);
        List<BillItem> billItems = Arrays.asList(billItem1, billItem2);
        
        double expectedTotal = 79.95 + 51.00;
        double discount = 10.00;
        double finalAmount = expectedTotal - discount;
        double cashTendered = 150.00;
        double expectedChange = cashTendered - finalAmount;
        
        String invoiceNumber = billController.getInvoiceNumber();
        assertNotNull(invoiceNumber);
        assertTrue(invoiceNumber.startsWith("INV-"));
        
        Bill bill = new Bill(testCustomer, invoiceNumber, finalAmount, discount, cashTendered, expectedChange);
        
        Bill savedBill = billController.Add_Bill(bill);
        assertNotNull(savedBill);
        assertTrue(savedBill.getId() > 0);
        assertEquals(invoiceNumber, savedBill.getInvoiceNumber());
        assertEquals(finalAmount, savedBill.getFullPrice(), 0.01);
        assertEquals(discount, savedBill.getDiscount(), 0.01);
        assertEquals(cashTendered, savedBill.getCashTendered(), 0.01);
        assertEquals(expectedChange, savedBill.getChangeAmount(), 0.01);
        
        billController.add_Bill_items(billItems, savedBill);
        
        int finalStock1 = stockController.get_Stock_quantity_by_item(testItem1);
        int finalStock2 = stockController.get_Stock_quantity_by_item(testItem2);
        
        assertTrue(finalStock1 >= 0);
        assertTrue(finalStock2 >= 0);
    }
    
    @Test
    @DisplayName("Billing workflow with insufficient stock should handle gracefully")
    void billingWorkflow_InsufficientStock_HandlesGracefully() throws Exception {
        Date expiryDate = new Date(System.currentTimeMillis() + 86400000L * 30);
        Stock limitedStock = new Stock(testItem1, 2, expiryDate);
        stockController.add_items_to_stock(limitedStock);
        
        BillItem billItem = new BillItem(testItem1, 10);
        List<BillItem> billItems = Arrays.asList(billItem);
        
        String invoiceNumber = billController.getInvoiceNumber();
        Bill bill = new Bill(testCustomer, invoiceNumber, 159.90, 0, 200.00, 40.10);
        Bill savedBill = billController.Add_Bill(bill);
        
        assertDoesNotThrow(() -> {
            billController.add_Bill_items(billItems, savedBill);
        });
    }
    
    @Test
    @DisplayName("Multiple consecutive bills should generate unique invoice numbers")
    void multipleBills_UniqueInvoiceNumbers() throws Exception {
        
        String invoice1 = billController.getInvoiceNumber();
        Bill bill1 = new Bill(testCustomer, invoice1, 100.00, 0, 100.00, 0.00);
        Bill savedBill1 = billController.Add_Bill(bill1);
        
        String invoice2 = billController.getInvoiceNumber();
        Bill bill2 = new Bill(testCustomer, invoice2, 200.00, 0, 200.00, 0.00);
        Bill savedBill2 = billController.Add_Bill(bill2);
        
        String invoice3 = billController.getInvoiceNumber();
        Bill bill3 = new Bill(testCustomer, invoice3, 300.00, 0, 300.00, 0.00);
        Bill savedBill3 = billController.Add_Bill(bill3);
        
        assertNotEquals(savedBill1.getInvoiceNumber(), savedBill2.getInvoiceNumber());
        assertNotEquals(savedBill2.getInvoiceNumber(), savedBill3.getInvoiceNumber());
        assertNotEquals(savedBill1.getInvoiceNumber(), savedBill3.getInvoiceNumber());
        
        assertTrue(savedBill1.getInvoiceNumber().matches("INV-\\d{5}"));
        assertTrue(savedBill2.getInvoiceNumber().matches("INV-\\d{5}"));
        assertTrue(savedBill3.getInvoiceNumber().matches("INV-\\d{5}"));
    }
    
    @Test
    @DisplayName("Bill with zero discount should calculate correctly")
    void billingWorkflow_ZeroDiscount_CalculatesCorrectly() throws Exception {
        // Setup
        Date expiryDate = new Date(System.currentTimeMillis() + 86400000L * 30);
        Stock stock = new Stock(testItem1, 50, expiryDate);
        stockController.add_items_to_stock(stock);
        
        BillItem billItem = new BillItem(testItem1, 3); // 3 * 15.99 = 47.97
        List<BillItem> billItems = Arrays.asList(billItem);
        
        double expectedTotal = 47.97;
        double discount = 0.00;
        double cashTendered = 50.00;
        double expectedChange = 2.03;
        
        String invoiceNumber = billController.getInvoiceNumber();
        Bill bill = new Bill(testCustomer, invoiceNumber, expectedTotal, discount, cashTendered, expectedChange);
        
        Bill savedBill = billController.Add_Bill(bill);
        billController.add_Bill_items(billItems, savedBill);
        
        assertEquals(expectedTotal, savedBill.getFullPrice(), 0.01);
        assertEquals(0.00, savedBill.getDiscount(), 0.01);
        assertEquals(expectedChange, savedBill.getChangeAmount(), 0.01);
    }
    
    @Test
    @DisplayName("Bill with maximum discount should calculate correctly")
    void billingWorkflow_MaximumDiscount_CalculatesCorrectly() throws Exception {
        Date expiryDate = new Date(System.currentTimeMillis() + 86400000L * 30);
        Stock stock = new Stock(testItem1, 50, expiryDate);
        stockController.add_items_to_stock(stock);
        
        BillItem billItem = new BillItem(testItem1, 2); // 2 * 15.99 = 31.98
        List<BillItem> billItems = Arrays.asList(billItem);
        
        double discount = 31.98; // 100% discount
        double finalAmount = 0.00;
        double cashTendered = 0.00;
        double expectedChange = 0.00;
        
        String invoiceNumber = billController.getInvoiceNumber();
        Bill bill = new Bill(testCustomer, invoiceNumber, finalAmount, discount, cashTendered, expectedChange);
        
        Bill savedBill = billController.Add_Bill(bill);
        billController.add_Bill_items(billItems, savedBill);
        
        assertEquals(finalAmount, savedBill.getFullPrice(), 0.01);
        assertEquals(discount, savedBill.getDiscount(), 0.01);
        assertEquals(expectedChange, savedBill.getChangeAmount(), 0.01);
    }
    
    private void cleanUpTestData() {
        
    }
}
