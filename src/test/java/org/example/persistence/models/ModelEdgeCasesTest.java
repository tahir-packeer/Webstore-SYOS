package org.example.persistence.models;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModelEdgeCasesTest {

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ItemEdgeCaseTests {
        
        @Test
        @Order(1)
        void createItem_MinimumPrice_CreatesSuccessfully() {
            Item item = new Item("MIN001", "Minimum Price Item", 0.01);
            
            assertEquals("MIN001", item.getCode());
            assertEquals("Minimum Price Item", item.getName());
            assertEquals(0.01, item.getPrice(), 0.001);
        }

        @Test
        @Order(2)
        void createItem_ZeroPrice_CreatesSuccessfully() {
            Item item = new Item("ZERO001", "Zero Price Item", 0.0);
            
            assertEquals(0.0, item.getPrice(), 0.001);
        }

        @Test
        @Order(3)
        void createItem_VeryLongName_CreatesSuccessfully() {
            String longName = "A".repeat(255);
            Item item = new Item("LONG001", longName, 10.0);
            
            assertEquals(longName, item.getName());
        }

        @Test
        @Order(4)
        void createItem_SpecialCharactersInCode_CreatesSuccessfully() {
            Item item = new Item("SP@C!AL#001", "Special Code Item", 15.0);
            
            assertEquals("SP@C!AL#001", item.getCode());
        }

        @Test
        @Order(5)
        void createItem_VeryHighPrice_CreatesSuccessfully() {
            Item item = new Item("HIGH001", "High Price Item", 999999.99);
            
            assertEquals(999999.99, item.getPrice(), 0.001);
        }

        @Test
        @Order(6)
        void setPrice_NegativeValue_SetsSuccessfully() {
            Item item = new Item("NEG001", "Negative Price Item", 10.0);
            item.setPrice(-5.0);
            
            assertEquals(-5.0, item.getPrice(), 0.001);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CustomerEdgeCaseTests {

        @Test
        @Order(1)
        void createCustomer_EmptyName_CreatesSuccessfully() {
            Customer customer = new Customer("", "1234567890");
            
            assertEquals("", customer.getName());
        }

        @Test
        @Order(2)
        void createCustomer_EmptyContactNumber_CreatesSuccessfully() {
            Customer customer = new Customer("John Doe", "");
            
            assertEquals("", customer.getcontactNumber());
        }

        @Test
        @Order(3)
        void createCustomer_VeryLongName_CreatesSuccessfully() {
            String longName = "John " + "A".repeat(250) + " Doe";
            Customer customer = new Customer(longName, "1234567890");
            
            assertEquals(longName, customer.getName());
        }

        @Test
        @Order(4)
        void createCustomer_SpecialCharactersInName_CreatesSuccessfully() {
            Customer customer = new Customer("John O'Connor-Smith Jr.", "1234567890");
            
            assertEquals("John O'Connor-Smith Jr.", customer.getName());
        }

        @Test
        @Order(5)
        void createCustomer_InternationalPhoneNumber_CreatesSuccessfully() {
            Customer customer = new Customer("International Customer", "+44-20-7946-0958");
            
            assertEquals("+44-20-7946-0958", customer.getcontactNumber());
        }

        @Test
        @Order(6)
        void setContactNumber_DifferentFormats_SetsSuccessfully() {
            Customer customer = new Customer("Test Customer", "1234567890");
            
            customer.setcontactNumber("(123) 456-7890");
            assertEquals("(123) 456-7890", customer.getcontactNumber());
            
            customer.setcontactNumber("123.456.7890");
            assertEquals("123.456.7890", customer.getcontactNumber());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BillEdgeCaseTests {

        @Test
        @Order(1)
        void createBill_ZeroAmounts_CreatesSuccessfully() {
            Customer customer = new Customer("Zero Bill Customer", "1234567890");
            Bill bill = new Bill(customer, "ZERO001", 0.0, 0.0, 0.0, 0.0);
            
            assertEquals(0.0, bill.getFullPrice(), 0.001);
            assertEquals(0.0, bill.getDiscount(), 0.001);
            assertEquals(0.0, bill.getCashTendered(), 0.001);
            assertEquals(0.0, bill.getChangeAmount(), 0.001);
        }

        @Test
        @Order(2)
        void createBill_HighDiscount_CreatesSuccessfully() {
            Customer customer = new Customer("High Discount Customer", "1234567890");
            Bill bill = new Bill(customer, "DISC001", 100.0, 99.0, 1.0, 0.0);
            
            assertEquals(99.0, bill.getDiscount(), 0.001);
        }

        @Test
        @Order(3)
        void createBill_ExcessCashTendered_CreatesSuccessfully() {
            Customer customer = new Customer("Excess Cash Customer", "1234567890");
            Bill bill = new Bill(customer, "CASH001", 100.0, 10.0, 200.0, 110.0);
            
            assertEquals(200.0, bill.getCashTendered(), 0.001);
            assertEquals(110.0, bill.getChangeAmount(), 0.001);
        }

        @Test
        @Order(4)
        void createBill_VeryLongInvoiceNumber_CreatesSuccessfully() {
            String longInvoice = "INV" + "0".repeat(100);
            Customer customer = new Customer("Long Invoice Customer", "1234567890");
            Bill bill = new Bill(customer, longInvoice, 100.0, 0.0, 100.0, 0.0);
            
            assertEquals(longInvoice, bill.getInvoiceNumber());
        }

        @Test
        @Order(5)
        void createBill_WithTransactionAndStoreType_CreatesSuccessfully() {
            Customer customer = new Customer("Typed Transaction Customer", "1234567890");
            Bill bill = new Bill(customer, "TYPE001", 100.0, 0.0, 100.0, 0.0, "ONLINE", "WAREHOUSE");
            
            assertEquals("ONLINE", bill.getTransactionType());
            assertEquals("WAREHOUSE", bill.getStoreType());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BillItemEdgeCaseTests {

        @Test
        @Order(1)
        void createBillItem_ZeroQuantity_CreatesSuccessfully() {
            Item item = new Item("ZERO001", "Zero Quantity Item", 10.0);
            BillItem billItem = new BillItem(item, 0);
            
            assertEquals(0, billItem.getQuantity());
            assertEquals(0.0, billItem.getTotalPrice(), 0.001);
        }

        @Test
        @Order(2)
        void createBillItem_LargeQuantity_CreatesSuccessfully() {
            Item item = new Item("LARGE001", "Large Quantity Item", 1.0);
            BillItem billItem = new BillItem(item, 10000);
            
            assertEquals(10000, billItem.getQuantity());
            assertEquals(10000.0, billItem.getTotalPrice(), 0.001);
        }

        @Test
        @Order(3)
        void createBillItem_NegativeQuantity_CreatesSuccessfully() {
            Item item = new Item("NEG001", "Negative Quantity Item", 10.0);
            BillItem billItem = new BillItem(item, -5);
            
            assertEquals(-5, billItem.getQuantity());
            assertEquals(-50.0, billItem.getTotalPrice(), 0.001);
        }

        @Test
        @Order(4)
        void setQuantity_UpdatesTotalPrice() {
            Item item = new Item("UPDATE001", "Update Quantity Item", 25.0);
            BillItem billItem = new BillItem(item, 2);
            
            assertEquals(50.0, billItem.getTotalPrice(), 0.001);
            
            billItem.setQuantity(5);
            assertEquals(5, billItem.getQuantity());
            assertEquals(125.0, billItem.getTotalPrice(), 0.001);
        }

        @Test
        @Order(5)
        void setItem_UpdatesPriceAndTotal() {
            Item originalItem = new Item("ORIG001", "Original Item", 10.0);
            Item newItem = new Item("NEW001", "New Item", 20.0);
            BillItem billItem = new BillItem(originalItem, 3);
            
            assertEquals(30.0, billItem.getTotalPrice(), 0.001);
            
            billItem.setItem(newItem);
            assertEquals(newItem, billItem.getItem());
            assertEquals(20.0, billItem.getItemPrice(), 0.001);
            assertEquals(60.0, billItem.getTotalPrice(), 0.001);
        }
    }
}
