package org.example.shared.interfaces;

import org.example.shared.dto.ItemDTO;
import java.sql.SQLException;
import java.util.List;

// Item service interface for managing store inventory
public interface ItemService {
    
    // Add multiple items to inventory
    void addItems(List<ItemDTO> items) throws SQLException, ClassNotFoundException;
    
    // Add single item to inventory  
    void addItem(ItemDTO item) throws SQLException, ClassNotFoundException;
    
    // Find item by product code
    ItemDTO getItemByCode(String code) throws SQLException, ClassNotFoundException;
    
    // Find item by database ID
    ItemDTO getItemById(int id) throws SQLException, ClassNotFoundException;
    
    // Get all items in inventory
    List<ItemDTO> getAllItems() throws SQLException, ClassNotFoundException;
    
    // Update item details
    void updateItem(ItemDTO item) throws SQLException, ClassNotFoundException;
    
    // Check if item code exists in system
    boolean itemExists(String code) throws SQLException, ClassNotFoundException;
}
