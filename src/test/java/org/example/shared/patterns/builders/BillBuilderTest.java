package org.example.shared.patterns.builders;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bill Builder Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BillBuilderTest {

    private BillBuilder billBuilder;

    @BeforeEach
    void setUp() {
        billBuilder = new BillBuilder();
    }

    @Test
    @Order(1)
    @DisplayName("New builder should create empty bill")
    void newBuilder_CreatesEmptyBill() {
        BillBuilder builder = BillBuilder.newBuilder();
        assertNotNull(builder);
    }

    @Test
    @Order(2)
    @DisplayName("Set customer information should work correctly")
    void setCustomer_ValidInformation_SetsCorrectly() {
        BillDTO bill = billBuilder
            .setCustomer(1, "John Doe", "1234567890")
            .setInvoiceNumber("INV-001")
            .addItem(1, "ITEM001", "Test Item", 10.0, 2)
            .build();

        assertEquals(1, bill.getCustomerId());
        assertEquals("John Doe", bill.getCustomerName());
        assertEquals("1234567890", bill.getCustomerPhone());
    }

    @Test
    @Order(3)
    @DisplayName("Set invoice number should work correctly")
    void setInvoiceNumber_ValidNumber_SetsCorrectly() {
        BillDTO bill = billBuilder
            .setCustomer(1, "John Doe", "1234567890")
            .setInvoiceNumber("INV-001")
            .addItem(1, "ITEM001", "Test Item", 10.0, 2)
            .build();

        assertEquals("INV-001", bill.getInvoiceNumber());
    }

    @Test
    @Order(4)
    @DisplayName("Add item should calculate total price correctly")
    void addItem_ValidItem_CalculatesTotalCorrectly() {
        BillDTO bill = billBuilder
            .setCustomer(1, "John Doe", "1234567890")
            .setInvoiceNumber("INV-001")
            .addItem(1, "ITEM001", "Test Item", 10.0, 2)
            .build();

        assertEquals(1, bill.getBillItems().size());
        BillItemDTO item = bill.getBillItems().get(0);
        assertEquals(1, item.getItemId());
        assertEquals("ITEM001", item.getItemCode());
        assertEquals("Test Item", item.getItemName());
        assertEquals(10.0, item.getItemPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(20.0, item.getTotalPrice());
    }

    @Test
    @Order(5)
    @DisplayName("Add multiple items should work correctly")
    void addMultipleItems_ValidItems_AddsCorrectly() {
        BillDTO bill = billBuilder
            .setCustomer(1, "John Doe", "1234567890")
            .setInvoiceNumber("INV-001")
            .addItem(1, "ITEM001", "Test Item 1", 10.0, 2)
            .addItem(2, "ITEM002", "Test Item 2", 15.0, 1)
            .build();

        assertEquals(2, bill.getBillItems().size());
        assertEquals(35.0, bill.getFullPrice()); // 20 + 15 = 35
    }
}
