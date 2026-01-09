package org.example.DesignPatterns;

import org.example.shared.patterns.builders.BillBuilder;
import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

/**
 * Comprehensive JUnit tests for Builder Design Pattern implementation
 * Tests BillBuilder with fluent interface and validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Builder Pattern Tests")
class BuilderPatternTest {

    @Test
    @DisplayName("Should build basic bill with required fields")
    void shouldBuildBasicBillWithRequiredFields() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-001")
                .setCustomer(1, "John Doe", "0771234567")
                .addItem(1, "ITEM001", "Test Item", 10.00, 2)
                .build();

        // Then
        assertThat(bill).isNotNull();
        assertThat(bill.getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(bill.getCustomerId()).isEqualTo(1);
        assertThat(bill.getCustomerName()).isEqualTo("John Doe");
        assertThat(bill.getCustomerPhone()).isEqualTo("0771234567");
        assertThat(bill.getBillItems()).hasSize(1);
        assertThat(bill.getFullPrice()).isEqualTo(20.00);
    }

    @Test
    @DisplayName("Should build bill with fluent interface chaining")
    void shouldBuildBillWithFluentInterfaceChaining() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-002")
                .setCustomer(2, "Jane Smith", "0777654321")
                .addItem(1, "ITEM001", "Product 1", 15.50, 1)
                .addItem(2, "ITEM002", "Product 2", 25.75, 2)
                .setDiscount(5.00)
                .setPayment(60.00, 3.25)
                .calculateTotals()
                .build();

        // Then
        assertThat(bill.getInvoiceNumber()).isEqualTo("INV-002");
        assertThat(bill.getBillItems()).hasSize(2);
        assertThat(bill.getDiscount()).isEqualTo(5.00);
        assertThat(bill.getCashTendered()).isEqualTo(60.00);
        assertThat(bill.getChangeAmount()).isEqualTo(3.25);
        assertThat(bill.getFullPrice()).isEqualTo(62.00); // 15.50 + 51.50 - 5.00 = 62.00
    }

    @Test
    @DisplayName("Should throw exception when building bill without invoice number")
    void shouldThrowExceptionWhenBuildingBillWithoutInvoiceNumber() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            BillBuilder.newBuilder()
                    .setCustomer(1, "John Doe", "0771234567")
                    .addItem(1, "ITEM001", "Test Item", 10.00, 1)
                    .build();
        }, "Should throw exception when invoice number is missing");
    }

    @Test
    @DisplayName("Should throw exception when building bill without items")
    void shouldThrowExceptionWhenBuildingBillWithoutItems() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            BillBuilder.newBuilder()
                    .setInvoiceNumber("INV-003")
                    .setCustomer(1, "John Doe", "0771234567")
                    .build();
        }, "Should throw exception when no items are added");
    }

    @Test
    @DisplayName("Should calculate totals automatically when building")
    void shouldCalculateTotalsAutomaticallyWhenBuilding() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-004")
                .setCustomer(1, "Test Customer", "0771111111")
                .addItem(1, "ITEM001", "Item 1", 12.50, 2)
                .addItem(2, "ITEM002", "Item 2", 8.75, 3)
                .setDiscount(3.00)
                .build();

        // Then
        assertThat(bill.getFullPrice()).isEqualTo(48.25); // (25.00 + 26.25) - 3.00
    }

    @Test
    @DisplayName("Should handle multiple items with different quantities")
    void shouldHandleMultipleItemsWithDifferentQuantities() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-005")
                .setCustomer(1, "Multi Customer", "0772222222")
                .addItem(1, "BULK001", "Bulk Item", 1.99, 100)
                .addItem(2, "SINGLE001", "Single Item", 50.00, 1)
                .addItem(3, "REGULAR001", "Regular Item", 12.50, 5)
                .build();

        // Then
        assertThat(bill.getBillItems()).hasSize(3);
        assertThat(bill.getFullPrice()).isEqualTo(311.50); // 199 + 50 + 62.5 = 311.50
    }

    @Test
    @DisplayName("Should set current date as default bill date")
    void shouldSetCurrentDateAsDefaultBillDate() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-006")
                .setCustomer(1, "Date Customer", "0773333333")
                .addItem(1, "ITEM001", "Test Item", 10.00, 1)
                .build();

        // Then
        assertThat(bill.getBillDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Should allow custom bill date")
    void shouldAllowCustomBillDate() {
        // Given
        LocalDate customDate = LocalDate.of(2024, 12, 25);

        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("INV-007")
                .setCustomer(1, "Christmas Customer", "0774444444")
                .setBillDate(customDate)
                .addItem(1, "GIFT001", "Christmas Gift", 25.00, 1)
                .build();

        // Then
        assertThat(bill.getBillDate()).isEqualTo(customDate);
    }

    @Test
    @DisplayName("Should create in-store bill with convenience method")
    void shouldCreateInStoreBillWithConvenienceMethod() {
        // When
        BillDTO bill = BillBuilder.forInStoreSale()
                .setInvoiceNumber("STORE-001")
                .setCustomer(1, "Store Customer", "0775555555")
                .addItem(1, "STORE001", "Store Item", 15.00, 2)
                .setPayment(35.00, 5.00)
                .build();

        // Then
        assertThat(bill.getTransactionType()).isEqualTo("COUNTER");
        assertThat(bill.getStoreType()).isEqualTo("STORE");
        assertThat(bill.getCashTendered()).isEqualTo(35.00);
        assertThat(bill.getChangeAmount()).isEqualTo(5.00);
    }

    @Test
    @DisplayName("Should create online bill with convenience method")
    void shouldCreateOnlineBillWithConvenienceMethod() {
        // When
        BillDTO bill = BillBuilder.forOnlineSale()
                .setInvoiceNumber("ONLINE-001")
                .setCustomer(1, "Online Customer", "0776666666")
                .addItem(1, "WEB001", "Web Item", 20.00, 1)
                .build();

        // Then
        assertThat(bill.getTransactionType()).isEqualTo("ONLINE");
        assertThat(bill.getStoreType()).isEqualTo("WEBSITE");
        assertThat(bill.getCashTendered()).isEqualTo(0.0);
        assertThat(bill.getChangeAmount()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should add BillItemDTO directly")
    void shouldAddBillItemDTODirectly() {
        // Given
        BillItemDTO customItem = new BillItemDTO();
        customItem.setItemId(99);
        customItem.setItemCode("CUSTOM001");
        customItem.setItemName("Custom Item");
        customItem.setItemPrice(33.33);
        customItem.setQuantity(3);
        customItem.setTotalPrice(99.99);

        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("CUSTOM-001")
                .setCustomer(1, "Custom Customer", "0777777777")
                .addItem(customItem)
                .build();

        // Then
        assertThat(bill.getBillItems()).hasSize(1);
        BillItemDTO addedItem = bill.getBillItems().get(0);
        assertThat(addedItem.getItemCode()).isEqualTo("CUSTOM001");
        assertThat(addedItem.getTotalPrice()).isEqualTo(99.99);
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 10.00, 90.00",
            "50.50, 5.50, 45.00",
            "200.00, 0.00, 200.00",
            "75.25, 25.25, 50.00"
    })
    @DisplayName("Should calculate correct totals with various discounts")
    void shouldCalculateCorrectTotalsWithVariousDiscounts(double itemTotal, double discount, double expectedTotal) {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("DISCOUNT-TEST")
                .setCustomer(1, "Discount Customer", "0778888888")
                .addItem(1, "DISC001", "Discount Item", itemTotal, 1)
                .setDiscount(discount)
                .build();

        // Then
        assertThat(bill.getFullPrice()).isEqualTo(expectedTotal);
        assertThat(bill.getDiscount()).isEqualTo(discount);
    }

    @Test
    @DisplayName("Should handle zero quantity items correctly")
    void shouldHandleZeroQuantityItemsCorrectly() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("ZERO-QTY")
                .setCustomer(1, "Zero Customer", "0779999999")
                .addItem(1, "ZERO001", "Zero Quantity Item", 10.00, 0)
                .addItem(2, "NORMAL001", "Normal Item", 15.00, 1)
                .build();

        // Then
        assertThat(bill.getBillItems()).hasSize(2);
        assertThat(bill.getFullPrice()).isEqualTo(15.00); // Only the normal item
    }

    @Test
    @DisplayName("Should maintain immutability of built bill")
    void shouldMaintainImmutabilityOfBuiltBill() {
        // Given
        BillBuilder builder1 = BillBuilder.newBuilder()
                .setInvoiceNumber("IMMUT-001")
                .setCustomer(1, "Immutable Customer", "0770000000")
                .addItem(1, "IMMUT001", "Immutable Item", 10.00, 1);

        BillBuilder builder2 = BillBuilder.newBuilder()
                .setInvoiceNumber("IMMUT-002")
                .setCustomer(1, "Immutable Customer", "0770000000")
                .addItem(1, "IMMUT001", "Immutable Item", 10.00, 1);

        // When
        BillDTO bill1 = builder1.build();
        BillDTO bill2 = builder2.addItem(2, "EXTRA001", "Extra Item", 5.00, 1).build();

        // Then
        assertThat(bill1.getBillItems()).hasSize(1);
        assertThat(bill2.getBillItems()).hasSize(2); // This builder has two items
        assertThat(bill1.getFullPrice()).isEqualTo(10.00);
        assertThat(bill2.getFullPrice()).isEqualTo(15.00); // Should have both items
    }

    @Test
    @DisplayName("Should handle large numbers correctly")
    void shouldHandleLargeNumbersCorrectly() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("LARGE-001")
                .setCustomer(999999, "Large Customer", "0771111111")
                .addItem(1, "EXPENSIVE001", "Expensive Item", 999999.99, 1)
                .setDiscount(9999.99)
                .setPayment(1000000.00, 10000.00)
                .build();

        // Then
        assertThat(bill.getCustomerId()).isEqualTo(999999);
        assertThat(bill.getFullPrice()).isEqualTo(990000.00);
        assertThat(bill.getCashTendered()).isEqualTo(1000000.00);
    }

    @Test
    @DisplayName("Should handle decimal precision correctly")
    void shouldHandleDecimalPrecisionCorrectly() {
        // When
        BillDTO bill = BillBuilder.newBuilder()
                .setInvoiceNumber("PRECISION-001")
                .setCustomer(1, "Precise Customer", "0772222222")
                .addItem(1, "PREC001", "Precise Item", 3.333, 3)
                .setDiscount(0.001)
                .build();

        // Then
        assertThat(bill.getFullPrice()).isCloseTo(9.998, offset(0.001)); // Use offset for floating point comparison
    }
}
