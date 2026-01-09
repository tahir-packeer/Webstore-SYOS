package org.example.business.services;

import org.example.shared.interfaces.ItemService;
import org.example.shared.dto.ItemDTO;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemServiceImplTest {

    private ItemService itemService;
    private ItemDTO testItem1;
    private ItemDTO testItem2;
    private String uniqueTestId;

    @BeforeEach
    void setUp() {
        uniqueTestId = String.valueOf(System.currentTimeMillis() % 100000);

        itemService = new ItemServiceImpl();
        testItem1 = new ItemDTO(0, "SVC" + uniqueTestId + "01", "Unit Test Item 1 " + uniqueTestId, 15.99);
        testItem2 = new ItemDTO(0, "SVC" + uniqueTestId + "02", "Unit Test Item 2 " + uniqueTestId, 25.99);
    }

    @Test
    @Order(1)
    @DisplayName("Should add single item successfully")
    void addItem_ValidItem_Success() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> itemService.addItem(testItem1));
    }

    @Test
    @Order(2)
    @DisplayName("Should add multiple items successfully")
    void addItems_ValidList_Success() {
        // Arrange
        List<ItemDTO> items = Arrays.asList(testItem2,
                new ItemDTO(0, "SVC" + uniqueTestId + "03", "Unit Test Item 3 " + uniqueTestId, 35.99));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> itemService.addItems(items));
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve item by code successfully")
    void getItemByCode_ExistingCode_ReturnsItem() {
        // Arrange - First add the item
        assertDoesNotThrow(() -> itemService.addItem(testItem1));

        // Act
        ItemDTO result = assertDoesNotThrow(() -> itemService.getItemByCode(testItem1.getCode()));

        // Assert
        assertNotNull(result);
        assertEquals(testItem1.getCode(), result.getCode());
        assertEquals("Unit Test Item 1 " + uniqueTestId, result.getName());
        assertEquals(15.99, result.getPrice(), 0.01);
    }

    @Test
    @Order(4)
    @DisplayName("Should return null for non-existing item code")
    void getItemByCode_NonExistingCode_ReturnsNull() {
        // Act
        ItemDTO result = assertDoesNotThrow(() -> itemService.getItemByCode("NONEXISTENT" + uniqueTestId));

        // Assert
        assertNull(result);
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve all items successfully")
    void getAllItems_ItemsExist_ReturnsAllItems() {
        // Arrange - Add test items
        String code1 = "SVC" + uniqueTestId + "GA1";
        String code2 = "SVC" + uniqueTestId + "GA2";
        assertDoesNotThrow(() -> {
            itemService.addItem(new ItemDTO(0, code1, "Get All Test 1 " + uniqueTestId, 10.99));
            itemService.addItem(new ItemDTO(0, code2, "Get All Test 2 " + uniqueTestId, 20.99));
        });

        // Act
        List<ItemDTO> result = assertDoesNotThrow(() -> itemService.getAllItems());

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 2); // At least our test items

        // Check if our test items are in the result
        boolean foundItem1 = result.stream().anyMatch(item -> code1.equals(item.getCode()));
        boolean foundItem2 = result.stream().anyMatch(item -> code2.equals(item.getCode()));
        assertTrue(foundItem1, "Should contain " + code1);
        assertTrue(foundItem2, "Should contain " + code2);
    }

    @Test
    @Order(6)
    @DisplayName("Should update item successfully")
    void updateItem_ValidItem_Success() {
        // Arrange - First add an item
        String updateCode = "UPDATE" + uniqueTestId;
        ItemDTO originalItem = new ItemDTO(0, updateCode, "Original Item " + uniqueTestId, 10.99);
        assertDoesNotThrow(() -> itemService.addItem(originalItem));

        // Get the item to get its generated ID
        ItemDTO retrievedItem = assertDoesNotThrow(() -> itemService.getItemByCode(updateCode));
        assertNotNull(retrievedItem);

        // Create updated version
        ItemDTO updatedItem = new ItemDTO(retrievedItem.getId(), updateCode, "Updated Item " + uniqueTestId, 19.99);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> itemService.updateItem(updatedItem));

        // Verify the update
        ItemDTO verifyItem = assertDoesNotThrow(() -> itemService.getItemByCode(updateCode));
        assertNotNull(verifyItem);
        assertEquals("Updated Item " + uniqueTestId, verifyItem.getName());
        assertEquals(19.99, verifyItem.getPrice(), 0.01);
    }

    @Test
    @Order(7)
    @DisplayName("Should check item existence correctly")
    void itemExists_ExistingItem_ReturnsTrue() {
        // Arrange - Add an item
        String existsCode = "EXISTS" + uniqueTestId;
        ItemDTO existsItem = new ItemDTO(0, existsCode, "Exists Test Item " + uniqueTestId, 5.99);
        assertDoesNotThrow(() -> itemService.addItem(existsItem));

        // Act
        boolean result = assertDoesNotThrow(() -> itemService.itemExists(existsCode));

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(8)
    @DisplayName("Should return false for non-existing item")
    void itemExists_NonExistingItem_ReturnsFalse() {
        // Act
        boolean result = assertDoesNotThrow(() -> itemService.itemExists("NOTEXISTS" + uniqueTestId));

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(9)
    @DisplayName("Should retrieve item by ID successfully")
    void getItemById_ExistingId_ReturnsItem() {
        // Arrange - Add an item and get its ID
        String byIdCode = "BYID" + uniqueTestId;
        ItemDTO byIdItem = new ItemDTO(0, byIdCode, "By ID Test Item " + uniqueTestId, 7.99);
        assertDoesNotThrow(() -> itemService.addItem(byIdItem));

        ItemDTO retrievedItem = assertDoesNotThrow(() -> itemService.getItemByCode(byIdCode));
        assertNotNull(retrievedItem);

        // Act
        ItemDTO result = assertDoesNotThrow(() -> itemService.getItemById(retrievedItem.getId()));

        // Assert
        assertNotNull(result);
        assertEquals(retrievedItem.getId(), result.getId());
        assertEquals(byIdCode, result.getCode());
        assertEquals("By ID Test Item " + uniqueTestId, result.getName());
    }

    @Test
    @Order(10)
    @DisplayName("Should return null for non-existing ID")
    void getItemById_NonExistingId_ReturnsNull() {
        // Act
        ItemDTO result = assertDoesNotThrow(() -> itemService.getItemById(99999));

        // Assert
        assertNull(result);
    }

}