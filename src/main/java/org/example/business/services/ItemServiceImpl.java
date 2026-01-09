package org.example.business.services;

import org.example.shared.interfaces.ItemService;
import org.example.persistence.gateways.ItemGateway;
import org.example.shared.dto.ItemDTO;
import java.sql.SQLException;
import java.util.List;

public class ItemServiceImpl implements ItemService {
    
    private final ItemGateway itemGateway;
    
    public ItemServiceImpl(ItemGateway itemGateway) {
        this.itemGateway = itemGateway;
    }
    
    public ItemServiceImpl() {
        this.itemGateway = ItemGateway.getInstance();
    }
    
    @Override
    public void addItems(List<ItemDTO> items) throws SQLException, ClassNotFoundException {
        itemGateway.insertBatch(items);
    }
    
    @Override
    public void addItem(ItemDTO item) throws SQLException, ClassNotFoundException {
        itemGateway.insert(item);
    }
    
    @Override
    public ItemDTO getItemByCode(String code) throws SQLException, ClassNotFoundException {
        return itemGateway.findByCode(code);
    }
    
    @Override
    public ItemDTO getItemById(int id) throws SQLException, ClassNotFoundException {
        return itemGateway.findById(id);
    }
    
    @Override
    public List<ItemDTO> getAllItems() throws SQLException, ClassNotFoundException {
        return itemGateway.findAll();
    }
    
    @Override
    public void updateItem(ItemDTO item) throws SQLException, ClassNotFoundException {
        itemGateway.update(item);
    }
    
    @Override
    public boolean itemExists(String code) throws SQLException, ClassNotFoundException {
        return itemGateway.exists(code);
    }
}
