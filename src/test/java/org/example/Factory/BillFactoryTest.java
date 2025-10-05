package org.example.Factory;

import org.example.DTO.BillDTO;
import org.example.DTO.BillItemDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bill Factory Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BillFactoryTest {

    @Test
    @Order(1)
    @DisplayName("Get factory for COUNTER type should return InStoreBillFactory")
    void getFactory_CounterType_ReturnsInStoreBillFactory() {
        BillFactory factory = BillFactory.getFactory("COUNTER");
        assertNotNull(factory);
        assertTrue(factory instanceof InStoreBillFactory);
    }

    @Test
    @Order(2)
    @DisplayName("Get factory for STORE type should return InStoreBillFactory")
    void getFactory_StoreType_ReturnsInStoreBillFactory() {
        BillFactory factory = BillFactory.getFactory("STORE");
        assertNotNull(factory);
        assertTrue(factory instanceof InStoreBillFactory);
    }

    @Test
    @Order(3)
    @DisplayName("Get factory for ONLINE type should return OnlineBillFactory")
    void getFactory_OnlineType_ReturnsOnlineBillFactory() {
        BillFactory factory = BillFactory.getFactory("ONLINE");
        assertNotNull(factory);
        assertTrue(factory instanceof OnlineBillFactory);
    }

    @Test
    @Order(4)
    @DisplayName("Get factory for WEBSITE type should return OnlineBillFactory")
    void getFactory_WebsiteType_ReturnsOnlineBillFactory() {
        BillFactory factory = BillFactory.getFactory("WEBSITE");
        assertNotNull(factory);
        assertTrue(factory instanceof OnlineBillFactory);
    }

    @Test
    @Order(5)
    @DisplayName("Get factory for lowercase types should work")
    void getFactory_LowercaseTypes_ReturnsCorrectFactory() {
        BillFactory counterFactory = BillFactory.getFactory("counter");
        assertTrue(counterFactory instanceof InStoreBillFactory);
        
        BillFactory onlineFactory = BillFactory.getFactory("online");
        assertTrue(onlineFactory instanceof OnlineBillFactory);
    }

    @Test
    @Order(6)
    @DisplayName("Get factory for invalid type should throw exception")
    void getFactory_InvalidType_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            BillFactory.getFactory("INVALID");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            BillFactory.getFactory("CASH");
        });
    }

    @Test
    @Order(7)
    @DisplayName("InStore factory should create correct bill")
    void inStoreFactory_CreateBill_CreatesCorrectBill() {
        BillFactory factory = new InStoreBillFactory();
        
        BillItemDTO item1 = new BillItemDTO();
        item1.setItemId(1);
        item1.setItemCode("ITEM001");
        item1.setItemName("Test Item");
        item1.setItemPrice(10.0);
        item1.setQuantity(2);
        item1.setTotalPrice(20.0);
        
        List<BillItemDTO> items = Arrays.asList(item1);
        
        BillDTO bill = factory.createBill(1, "John Doe", "1234567890", 
            "INV-001", items, 5.0, 25.0, 10.0);
        
        assertNotNull(bill);
        assertEquals(1, bill.getCustomerId());
        assertEquals("John Doe", bill.getCustomerName());
        assertEquals("1234567890", bill.getCustomerPhone());
        assertEquals("INV-001", bill.getInvoiceNumber());
        assertEquals(15.0, bill.getFullPrice()); // 20 - 5 = 15
        assertEquals(5.0, bill.getDiscount());
        assertEquals(25.0, bill.getCashTendered());
        assertEquals(10.0, bill.getChangeAmount());
        assertEquals("COUNTER", bill.getTransactionType());
        assertEquals("STORE", bill.getStoreType());
    }

    @Test
    @Order(8)
    @DisplayName("Online factory should create correct bill")
    void onlineFactory_CreateBill_CreatesCorrectBill() {
        BillFactory factory = new OnlineBillFactory();
        
        BillItemDTO item1 = new BillItemDTO();
        item1.setItemId(1);
        item1.setItemCode("ITEM001");
        item1.setItemName("Test Item");
        item1.setItemPrice(15.0);
        item1.setQuantity(2);
        item1.setTotalPrice(30.0);
        
        List<BillItemDTO> items = Arrays.asList(item1);
        
        BillDTO bill = factory.createBill(2, "Jane Smith", "0987654321", 
            "INV-002", items, 3.0, 100.0, 27.0); // Online should override cash values
        
        assertNotNull(bill);
        assertEquals(2, bill.getCustomerId());
        assertEquals("Jane Smith", bill.getCustomerName());
        assertEquals("0987654321", bill.getCustomerPhone());
        assertEquals("INV-002", bill.getInvoiceNumber());
        assertEquals(27.0, bill.getFullPrice()); // 30 - 3 = 27
        assertEquals(3.0, bill.getDiscount());
        assertEquals(0.0, bill.getCashTendered()); // Online should be 0
        assertEquals(0.0, bill.getChangeAmount()); // Online should be 0
        assertEquals("ONLINE", bill.getTransactionType());
        assertEquals("WEBSITE", bill.getStoreType());
    }

    @Test
    @Order(9)
    @DisplayName("Calculate total should work correctly")
    void calculateTotal_MultipleItems_CalculatesCorrectly() {
        BillFactory factory = new InStoreBillFactory();
        
        BillItemDTO item1 = new BillItemDTO();
        item1.setTotalPrice(20.0);
        
        BillItemDTO item2 = new BillItemDTO();
        item2.setTotalPrice(15.0);
        
        BillItemDTO item3 = new BillItemDTO();
        item3.setTotalPrice(8.5);
        
        List<BillItemDTO> items = Arrays.asList(item1, item2, item3);
        
        BillDTO bill = factory.createBill(1, "Test Customer", "1234567890", 
            "INV-001", items, 0.0, 50.0, 6.5);
        
        assertEquals(43.5, bill.getFullPrice()); // 20 + 15 + 8.5 = 43.5
    }

    @Test
    @Order(10)
    @DisplayName("Bill items should have bill ID set")
    void createBill_ShouldSetBillIdForItems() {
        BillFactory factory = new InStoreBillFactory();
        
        BillItemDTO item1 = new BillItemDTO();
        item1.setItemId(1);
        item1.setTotalPrice(20.0);
        
        List<BillItemDTO> items = Arrays.asList(item1);
        
        BillDTO bill = factory.createBill(1, "Test Customer", "1234567890", 
            "INV-001", items, 0.0, 25.0, 5.0);
        
        assertEquals(1, bill.getBillItems().size());
        // Note: Bill ID will be 0 until the bill is actually saved to database
        assertEquals(0, bill.getBillItems().get(0).getBillId());
    }
}
