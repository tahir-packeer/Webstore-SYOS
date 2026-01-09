package org.example.Integration;

import org.example.presentation.controllers.ItemController;
import org.example.persistence.models.Item;
import org.example.shared.patterns.builders.ItemBuilder;
import org.example.core.di.DIContainer;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemIntegrationTest {
    
    private ItemController itemController;
    private String uniqueTestId;
    
    @BeforeEach
    void setUp() {
        uniqueTestId = String.valueOf(System.currentTimeMillis() % 100000);
        
        DIContainer container = DIContainer.getInstance();
        itemController = container.createItemController();
    }
    
    @Test
    @Order(1)
    @DisplayName("Integration: Controller and Service should work together for adding items")
    void addItem_ControllerAndService_Integration() {
        String itemCode = "ITM" + uniqueTestId + "01";
        Item testItem = ItemBuilder.createStandardItem()
            .withCode(itemCode)
            .withName("Integration Test Item " + uniqueTestId)
            .withPrice(25.99)
            .buildModel();
        
        // Act & Assert
        assertDoesNotThrow(() -> itemController.addItem(testItem));
        
        // Verify through retrieval
        Item retrievedItem = assertDoesNotThrow(() -> itemController.getItemFromCode(itemCode));
        assertNotNull(retrievedItem);
        assertEquals("Integration Test Item " + uniqueTestId, retrievedItem.getName());
    }
    
    @Test
    @Order(2)
    @DisplayName("Integration: Batch operations should work correctly")
    void addMultipleItems_BatchOperation_Integration() {
        String baseCode = "ITM" + uniqueTestId;
        List<Item> items = Arrays.asList(
            ItemBuilder.createItem()
                .withCode(baseCode + "B1")
                .withName("Bulk Item 1 " + uniqueTestId)
                .withPrice(10.50)
                .buildModel(),
            ItemBuilder.createItem()
                .withCode(baseCode + "B2")
                .withName("Bulk Item 2 " + uniqueTestId)
                .withPrice(20.99)
                .buildModel(),
            ItemBuilder.createItem()
                .withCode(baseCode + "B3")
                .withName("Bulk Item 3 " + uniqueTestId)
                .withPrice(15.75)
                .buildModel()
        );
        
        assertDoesNotThrow(() -> itemController.addItems(items));
        
        // Verify all items were added
        Item item1 = assertDoesNotThrow(() -> itemController.getItemFromCode(baseCode + "B1"));
        Item item2 = assertDoesNotThrow(() -> itemController.getItemFromCode(baseCode + "B2"));
        Item item3 = assertDoesNotThrow(() -> itemController.getItemFromCode(baseCode + "B3"));
        
        assertNotNull(item1);
        assertNotNull(item2);
        assertNotNull(item3);
        assertEquals("Bulk Item 1 " + uniqueTestId, item1.getName());
        assertEquals("Bulk Item 2 " + uniqueTestId, item2.getName());
        assertEquals("Bulk Item 3 " + uniqueTestId, item3.getName());
    }
    
    @Test
    @Order(3)
    @DisplayName("Integration: Update operations should persist correctly")
    void updateItem_PersistenceCheck_Integration() {
        // Arrange - Add item first
        String itemCode = "ITM" + uniqueTestId + "U1";
        Item originalItem = ItemBuilder.createStandardItem()
            .withCode(itemCode)
            .withName("Original Item " + uniqueTestId)
            .withPrice(10.99)
            .buildModel();
        
        assertDoesNotThrow(() -> itemController.addItem(originalItem));
        
        // Get the item to obtain its ID
        Item retrievedItem = assertDoesNotThrow(() -> itemController.getItemFromCode(itemCode));
        assertNotNull(retrievedItem);
        
        // Modify the item
        retrievedItem.setName("Updated Item Name " + uniqueTestId);
        retrievedItem.setPrice(19.99);
        
        // Update the item
        assertDoesNotThrow(() -> itemController.updateItem(retrievedItem));
        
        // Verify the update persisted
        Item updatedItem = assertDoesNotThrow(() -> itemController.getItemFromCode(itemCode));
        assertNotNull(updatedItem);
        assertEquals("Updated Item Name " + uniqueTestId, updatedItem.getName());
        assertEquals(19.99, updatedItem.getPrice(), 0.01);
    }
    
    @Test
    @Order(4)
    @DisplayName("Integration: Existence checks should work across layers")
    void itemExists_CrossLayer_Integration() {
        // Arrange
        String itemCode = "ITM" + uniqueTestId + "E1";
        Item testItem = ItemBuilder.createItem()
            .withCode(itemCode)
            .withName("Item to Check Existence " + uniqueTestId)
            .withPrice(5.99)
            .buildModel();
        
        // Verify doesn't exist initially
        boolean existsBefore = assertDoesNotThrow(() -> itemController.itemExists(itemCode));
        assertFalse(existsBefore);
        
        // Add the item
        assertDoesNotThrow(() -> itemController.addItem(testItem));
        
        boolean existsAfter = assertDoesNotThrow(() -> itemController.itemExists(itemCode));
        assertTrue(existsAfter);
    }
    
    @Test
    @Order(5)
    @DisplayName("Integration: Error handling should work consistently across layers")
    void errorHandling_ConsistentBehavior_Integration() {
        // Test null handling
        assertDoesNotThrow(() -> {
            Item result = itemController.getItemFromCode("NONEXISTENT" + uniqueTestId);
            assertNull(result);
        });
        
        // Test ID-based retrieval for non-existent item
        assertDoesNotThrow(() -> {
            Item result = itemController.getItemFromId(99999);
            assertNull(result);
        });
        
        // Test existence check for non-existent item
        boolean exists = assertDoesNotThrow(() -> itemController.itemExists("NONEXISTENT" + uniqueTestId));
        assertFalse(exists);
    }
    
    @Test
    @Order(6)
    @DisplayName("Integration: Complex scenario with multiple operations")
    void complexScenario_MultipleOperations_Integration() {
        String itemCode = "ITM" + uniqueTestId + "C1";
        Item complexItem = ItemBuilder.createItem()
            .withCode(itemCode)
            .withName("Complex Scenario Item " + uniqueTestId)
            .withPrice(45.99)
            .buildModel();
        
        assertDoesNotThrow(() -> itemController.addItem(complexItem));
        
        Item retrievedItem = assertDoesNotThrow(() -> itemController.getItemFromCode(itemCode));
        assertNotNull(retrievedItem);
        assertEquals("Complex Scenario Item " + uniqueTestId, retrievedItem.getName());
        
        retrievedItem.setName("Updated Complex Item " + uniqueTestId);
        retrievedItem.setPrice(55.99);
        assertDoesNotThrow(() -> itemController.updateItem(retrievedItem));
        
        Item updatedItem = assertDoesNotThrow(() -> itemController.getItemFromCode(itemCode));
        assertNotNull(updatedItem);
        assertEquals("Updated Complex Item " + uniqueTestId, updatedItem.getName());
        assertEquals(55.99, updatedItem.getPrice(), 0.01);
        
        boolean exists = assertDoesNotThrow(() -> itemController.itemExists(itemCode));
        assertTrue(exists);
        
        List<Item> allItems = assertDoesNotThrow(() -> itemController.getAllItems());
        boolean foundInAll = allItems.stream()
            .anyMatch(item -> itemCode.equals(item.getCode()));
        assertTrue(foundInAll, "Complex item should be found in getAllItems");
    }
}
