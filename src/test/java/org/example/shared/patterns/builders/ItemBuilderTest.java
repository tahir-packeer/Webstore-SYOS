package org.example.shared.patterns.builders;

import org.example.shared.dto.ItemDTO;
import org.example.persistence.models.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Item Builder Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ItemBuilderTest {

    @Test
    @Order(1)
    @DisplayName("Build DTO with default values should work")
    void buildDTO_DefaultValues_CreatesCorrectly() {
        ItemBuilder builder = new ItemBuilder();
        ItemDTO dto = builder.buildDTO();
        
        assertNotNull(dto);
        assertEquals(0, dto.getId());
        assertEquals("ITEM001", dto.getCode());
        assertEquals("Default Item", dto.getName());
        assertEquals(10.0, dto.getPrice());
    }

    @Test
    @Order(2)
    @DisplayName("Build model with default values should work")
    void buildModel_DefaultValues_CreatesCorrectly() {
        ItemBuilder builder = new ItemBuilder();
        Item item = builder.buildModel();
        
        assertNotNull(item);
        assertEquals(0, item.getId());
        assertEquals("ITEM001", item.getCode());
        assertEquals("Default Item", item.getName());
        assertEquals(10.0, item.getPrice());
    }

    @Test
    @Order(3)
    @DisplayName("Set custom values should work correctly")
    void setCustomValues_ValidValues_SetsCorrectly() {
        ItemDTO dto = new ItemBuilder()
            .withId(123)
            .withCode("CUSTOM001")
            .withName("Custom Item")
            .withPrice(25.99)
            .buildDTO();
        
        assertEquals(123, dto.getId());
        assertEquals("CUSTOM001", dto.getCode());
        assertEquals("Custom Item", dto.getName());
        assertEquals(25.99, dto.getPrice());
    }

    @Test
    @Order(4)
    @DisplayName("Generated code should create unique codes")
    void withGeneratedCode_CreatesUniqueCode() {
        ItemBuilder builder1 = new ItemBuilder().withGeneratedCode();
        ItemBuilder builder2 = new ItemBuilder().withGeneratedCode();
        
        Item item1 = builder1.buildModel();
        Item item2 = builder2.buildModel();
        
        assertNotEquals(item1.getCode(), item2.getCode());
        assertTrue(item1.getCode().startsWith("ITEM"));
        assertTrue(item2.getCode().startsWith("ITEM"));
    }

    @Test
    @Order(5)
    @DisplayName("High price preset should set correct price")
    void withHighPrice_SetsHighPrice() {
        Item item = new ItemBuilder()
            .withHighPrice()
            .buildModel();
        
        assertTrue(item.getPrice() >= 100.0);
    }

    @Test
    @Order(6)
    @DisplayName("Low price preset should set correct price")
    void withLowPrice_SetsLowPrice() {
        Item item = new ItemBuilder()
            .withLowPrice()
            .buildModel();
        
        assertTrue(item.getPrice() <= 5.0);
        assertTrue(item.getPrice() > 0.0);
    }

    @Test
    @Order(7)
    @DisplayName("Zero price preset should set zero price")
    void withZeroPrice_SetsZeroPrice() {
        Item item = new ItemBuilder()
            .withZeroPrice()
            .buildModel();
        
        assertEquals(0.0, item.getPrice());
    }

    @Test
    @Order(8)
    @DisplayName("Create item factory method should work")
    void createItem_FactoryMethod_CreatesBuilder() {
        ItemBuilder builder = ItemBuilder.createItem();
        assertNotNull(builder);
        
        Item item = builder.buildModel();
        assertNotNull(item);
    }

    @Test
    @Order(9)
    @DisplayName("Create standard item factory method should work")
    void createStandardItem_FactoryMethod_CreatesStandardItem() {
        Item item = ItemBuilder.createStandardItem().buildModel();
        
        assertEquals("STD001", item.getCode());
        assertEquals("Standard Item", item.getName());
        assertEquals(15.99, item.getPrice());
    }

    @Test
    @Order(10)
    @DisplayName("Create premium item factory method should work")
    void createPremiumItem_FactoryMethod_CreatesPremiumItem() {
        Item item = ItemBuilder.createPremiumItem().buildModel();
        
        assertEquals("PREM001", item.getCode());
        assertEquals("Premium Item", item.getName());
        assertEquals(999.99, item.getPrice());
    }

    @Test
    @Order(11)
    @DisplayName("Create discount item factory method should work")
    void createDiscountItem_FactoryMethod_CreatesDiscountItem() {
        Item item = ItemBuilder.createDiscountItem().buildModel();
        
        assertEquals("DISC001", item.getCode());
        assertEquals("Discount Item", item.getName());
        assertEquals(1.99, item.getPrice());
    }

    @Test
    @Order(12)
    @DisplayName("Method chaining should work correctly")
    void methodChaining_MultipleOperations_WorksCorrectly() {
        Item item = ItemBuilder.createItem()
            .withCode("CHAIN001")
            .withName("Chained Item")
            .withPrice(33.33)
            .withId(999)
            .buildModel();
        
        assertEquals(999, item.getId());
        assertEquals("CHAIN001", item.getCode());
        assertEquals("Chained Item", item.getName());
        assertEquals(33.33, item.getPrice());
    }

    @Test
    @Order(13)
    @DisplayName("DTO and Model should have same values")
    void buildDTOAndModel_SameBuilder_HaveSameValues() {
        ItemBuilder builder = new ItemBuilder()
            .withId(100)
            .withCode("SAME001")
            .withName("Same Item")
            .withPrice(50.0);
        
        ItemDTO dto = builder.buildDTO();
        Item model = builder.buildModel();
        
        assertEquals(dto.getId(), model.getId());
        assertEquals(dto.getCode(), model.getCode());
        assertEquals(dto.getName(), model.getName());
        assertEquals(dto.getPrice(), model.getPrice());
    }
}
