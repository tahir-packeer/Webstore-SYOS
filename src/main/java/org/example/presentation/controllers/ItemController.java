package org.example.presentation.controllers;

import org.example.persistence.models.Item;
import org.example.shared.dto.ItemDTO;
import org.example.shared.interfaces.ItemService;
import org.example.business.services.ItemServiceImpl;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

// Item management controller using Service abstraction
public class ItemController {
    
    private final ItemService itemService;
    
    // Default constructor using Service implementation
    public ItemController() {
        this.itemService = new ItemServiceImpl();
    }
    
    // For dependency injection and testing
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }
    
    // Add multiple items with proper error handling
    public void addItems(List<Item> items) throws SQLException, ClassNotFoundException {
        try {
            List<ItemDTO> itemDTOs = new ArrayList<>();
            for (Item item : items) {
                validateItem(item);
                itemDTOs.add(convertToDTO(item));
            }
            itemService.addItems(itemDTOs);
            System.out.println("Successfully added " + items.size() + " items");
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while adding items: " + e.getMessage());
            throw e;
        }
    }
    
    // Add single item with proper error handling
    public void addItem(Item item) throws SQLException, ClassNotFoundException {
        try {
            validateItem(item);
            ItemDTO itemDTO = convertToDTO(item);
            itemService.addItem(itemDTO);
            System.out.println("Successfully added item: " + item.getName());
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while adding item: " + e.getMessage());
            throw e;
        }
    }
    
    // Get item by code with proper error handling
    public Item getItemFromCode(String code) throws SQLException, ClassNotFoundException {
        try {
            ItemDTO itemDTO = itemService.getItemByCode(code);
            return itemDTO != null ? convertToModel(itemDTO) : null;
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            return null;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while retrieving item: " + e.getMessage());
            throw e;
        }
    }
    
    // Get item by ID with proper error handling
    public Item getItemFromId(int id) throws SQLException, ClassNotFoundException {
        try {
            ItemDTO itemDTO = itemService.getItemById(id);
            return itemDTO != null ? convertToModel(itemDTO) : null;
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            return null;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while retrieving item: " + e.getMessage());
            throw e;
        }
    }
    
    // Get all items with proper error handling
    public List<Item> getAllItems() throws SQLException, ClassNotFoundException {
        try {
            List<ItemDTO> itemDTOs = itemService.getAllItems();
            List<Item> items = new ArrayList<>();
            for (ItemDTO itemDTO : itemDTOs) {
                items.add(convertToModel(itemDTO));
            }
            return items;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while retrieving items: " + e.getMessage());
            throw e;
        }
    }
    
    // Update item with proper error handling
    public void updateItem(Item item) throws SQLException, ClassNotFoundException {
        try {
            validateItem(item);
            ItemDTO itemDTO = convertToDTO(item);
            itemService.updateItem(itemDTO);
            System.out.println("Successfully updated item: " + item.getName());
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while updating item: " + e.getMessage());
            throw e;
        }
    }
    
    // Check if item exists with proper error handling
    public boolean itemExists(String code) throws SQLException, ClassNotFoundException {
        try {
            return itemService.itemExists(code);
        } catch (IllegalArgumentException e) {
            return false;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database error while checking item existence: " + e.getMessage());
            throw e;
        }
    }
    
    // Validation helper method
    private void validateItem(Item item) throws IllegalArgumentException {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        if (item.getCode() == null || item.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty");
        }
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (item.getPrice() < 0) {
            throw new IllegalArgumentException("Item price cannot be negative");
        }
    }
    
    // Convert Item model to ItemDTO
    private ItemDTO convertToDTO(Item item) {
        return new ItemDTO(item.getId(), item.getCode(), item.getName(), item.getPrice());
    }
    
    // Convert ItemDTO to Item model
    private Item convertToModel(ItemDTO itemDTO) {
        Item item = new Item(itemDTO.getCode(), itemDTO.getName(), itemDTO.getPrice());
        item.setId(itemDTO.getId());
        return item;
    }
}
