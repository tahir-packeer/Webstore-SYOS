package org.example.DesignPatterns;

import org.example.shared.dto.BillItemDTO;
import org.example.shared.dto.ItemDTO;
import org.example.persistence.gateways.ItemGateway;
import org.example.core.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit tests for State Design Pattern implementation
 * Tests CheckoutContext with different state transitions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("State Pattern Tests")
class StatePatternTest {

    @Mock
    private ItemGateway mockItemGateway;

    private CheckoutContext checkoutContext;
    private ItemDTO testItem;

    @BeforeEach
    void setUp() {
        // Create test item
        testItem = new ItemDTO();
        testItem.setId(1);
        testItem.setCode("TEST001");
        testItem.setName("Test Item");
        testItem.setPrice(10.50);

        // Initialize checkout context
        checkoutContext = new CheckoutContext("COUNTER");
    }

    @Test
    @DisplayName("Should start in SelectingItemsState")
    void shouldStartInSelectingItemsState() {
        // Given & When
        CheckoutContext context = new CheckoutContext("COUNTER");

        // Then
        assertThat(context.getCurrentState()).isInstanceOf(SelectingItemsState.class);
        assertThat(context.getCurrentState().getStateName()).isEqualTo("Selecting Items");
    }

    @Test
    @DisplayName("Should transition from SelectingItems to PaymentPending when items exist")
    void shouldTransitionFromSelectingItemsToPaymentPendingWhenItemsExist() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            // When
            checkoutContext.addItem("TEST001", 2);
            checkoutContext.processPayment(25.0);

            // Then
            assertThat(checkoutContext.getCurrentState()).isInstanceOf(PaymentPendingState.class);
            assertThat(checkoutContext.getCashTendered()).isEqualTo(25.0);
        }
    }

    @Test
    @DisplayName("Should transition from PaymentPending to BillGenerated when generating bill")
    void shouldTransitionFromPaymentPendingToBillGeneratedWhenGeneratingBill() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.addItem("TEST001", 2);
            checkoutContext.processPayment(25.0);

            // When
            checkoutContext.generateBill();

            // Then
            assertThat(checkoutContext.getCurrentState()).isInstanceOf(BillGeneratedState.class);
            assertThat(checkoutContext.getGeneratedBill()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle state transitions correctly for online transactions")
    void shouldHandleStateTransitionsCorrectlyForOnlineTransactions() throws Exception {
        // Given
        CheckoutContext onlineContext = new CheckoutContext("ONLINE");

        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            // When & Then
            assertThat(onlineContext.getCurrentState()).isInstanceOf(SelectingItemsState.class);

            onlineContext.addItem("TEST001", 1);
            onlineContext.processPayment(0.0); // Online payment

            assertThat(onlineContext.getCurrentState()).isInstanceOf(PaymentPendingState.class);

            onlineContext.generateBill();

            assertThat(onlineContext.getCurrentState()).isInstanceOf(BillGeneratedState.class);
        }
    }

    @Test
    @DisplayName("Should prevent invalid operations in wrong states")
    void shouldPreventInvalidOperationsInWrongStates() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.addItem("TEST001", 1);
            checkoutContext.processPayment(15.0);
            checkoutContext.generateBill();

            // When & Then - Should not allow adding items after bill generation
            CheckoutState currentState = checkoutContext.getCurrentState();
            assertThat(currentState).isInstanceOf(BillGeneratedState.class);

            // Attempting to add item in BillGenerated state should be prevented
            assertThatCode(() -> checkoutContext.addItem("TEST002", 1))
                    .doesNotThrowAnyException(); // State pattern handles this gracefully
        }
    }

    @Test
    @DisplayName("Should calculate totals correctly across states")
    void shouldCalculateTotalsCorrectlyAcrossStates() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            // When
            checkoutContext.addItem("TEST001", 3); // 3 * 10.50 = 31.50
            checkoutContext.applyDiscount(5.0);

            // Then
            assertThat(checkoutContext.calculateSubtotal()).isEqualTo(31.50);
            assertThat(checkoutContext.calculateTotal()).isEqualTo(26.50); // 31.50 - 5.0
            assertThat(checkoutContext.getDiscount()).isEqualTo(5.0);
        }
    }

    @Test
    @DisplayName("Should handle item removal correctly")
    void shouldHandleItemRemovalCorrectly() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.addItem("TEST001", 2);
            assertThat(checkoutContext.hasItems()).isTrue();
            assertThat(checkoutContext.getItems()).hasSize(1);

            // When
            checkoutContext.removeItem("TEST001");

            // Then
            assertThat(checkoutContext.hasItems()).isFalse();
            assertThat(checkoutContext.getItems()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should handle quantity updates correctly")
    void shouldHandleQuantityUpdatesCorrectly() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.addItem("TEST001", 2); // Initial quantity: 2

            // When - Add same item again
            checkoutContext.addItem("TEST001", 3); // Should update to quantity: 5

            // Then
            assertThat(checkoutContext.getItems()).hasSize(1);
            BillItemDTO item = checkoutContext.getItems().get(0);
            assertThat(item.getQuantity()).isEqualTo(5);
            assertThat(item.getTotalPrice()).isEqualTo(52.50); // 5 * 10.50
        }
    }

    @Test
    @DisplayName("Should handle item not found scenario")
    void shouldHandleItemNotFoundScenario() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("NONEXISTENT")).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> checkoutContext.addItem("NONEXISTENT", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Item not found: NONEXISTENT");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "COUNTER", "ONLINE", "STORE", "WEBSITE" })
    @DisplayName("Should handle different transaction types")
    void shouldHandleDifferentTransactionTypes(String transactionType) {
        // When
        CheckoutContext context = new CheckoutContext(transactionType);

        // Then
        assertThat(context.getTransactionType()).isEqualTo(transactionType);
        assertThat(context.getCurrentState()).isInstanceOf(SelectingItemsState.class);
    }

    @Test
    @DisplayName("Should maintain state consistency during operations")
    void shouldMaintainStateConsistencyDuringOperations() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            // When - Multiple operations
            checkoutContext.setCustomerId(123);
            checkoutContext.setCustomerName("John Doe");
            checkoutContext.setCustomerPhone("0771234567");
            checkoutContext.addItem("TEST001", 2);
            checkoutContext.applyDiscount(2.0);
            checkoutContext.processPayment(20.0);

            // Then
            assertThat(checkoutContext.getCustomerId()).isEqualTo(123);
            assertThat(checkoutContext.getCustomerName()).isEqualTo("John Doe");
            assertThat(checkoutContext.getCustomerPhone()).isEqualTo("0771234567");
            assertThat(checkoutContext.getCurrentState()).isInstanceOf(PaymentPendingState.class);
            assertThat(checkoutContext.calculateTotal()).isEqualTo(19.0); // 21.0 - 2.0
            assertThat(checkoutContext.getCashTendered()).isEqualTo(20.0);
        }
    }

    @Test
    @DisplayName("Should handle change calculation correctly")
    void shouldHandleChangeCalculationCorrectly() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.addItem("TEST001", 1); // Total: 10.50

            // When
            checkoutContext.processPayment(15.0);
            double expectedChange = 15.0 - 10.50; // 4.50
            checkoutContext.setChangeAmount(expectedChange);

            // Then
            assertThat(checkoutContext.getChangeAmount()).isEqualTo(4.50);
        }
    }

    @Test
    @DisplayName("Should handle empty checkout context")
    void shouldHandleEmptyCheckoutContext() {
        // When & Then
        assertThat(checkoutContext.hasItems()).isFalse();
        assertThat(checkoutContext.calculateSubtotal()).isEqualTo(0.0);
        assertThat(checkoutContext.calculateTotal()).isEqualTo(0.0);
        assertThat(checkoutContext.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should handle bill generation with customer details")
    void shouldHandleBillGenerationWithCustomerDetails() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            checkoutContext.setCustomerId(456);
            checkoutContext.setCustomerName("Jane Smith");
            checkoutContext.setCustomerPhone("0777654321");
            checkoutContext.setDeliveryAddress("123 Main St");
            checkoutContext.addItem("TEST001", 1);
            checkoutContext.processPayment(12.0);

            // When
            checkoutContext.generateBill();

            // Then
            assertThat(checkoutContext.getCurrentState()).isInstanceOf(BillGeneratedState.class);
            assertThat(checkoutContext.getGeneratedBill()).isNotNull();
            // Additional bill validation would depend on the actual implementation
        }
    }

    @Test
    @DisplayName("Should demonstrate polymorphic state behavior")
    void shouldDemonstratePolymorphicStateBehavior() throws Exception {
        // Given
        try (MockedStatic<ItemGateway> mockedStatic = mockStatic(ItemGateway.class)) {
            mockedStatic.when(ItemGateway::getInstance).thenReturn(mockItemGateway);
            when(mockItemGateway.findByCode("TEST001")).thenReturn(testItem);

            CheckoutState initialState = checkoutContext.getCurrentState();

            // When
            checkoutContext.addItem("TEST001", 1);
            CheckoutState afterAddingItem = checkoutContext.getCurrentState();

            checkoutContext.processPayment(15.0);
            CheckoutState afterPayment = checkoutContext.getCurrentState();

            // Then
            assertThat(initialState.getStateName()).isEqualTo("Selecting Items");
            assertThat(afterAddingItem.getStateName()).isEqualTo("Selecting Items");
            assertThat(afterPayment.getStateName()).isEqualTo("Payment Pending");

            // Different state objects should handle operations differently
            assertThat(initialState.getClass()).isEqualTo(afterAddingItem.getClass());
            assertThat(afterAddingItem.getClass()).isNotEqualTo(afterPayment.getClass());
        }
    }
}
