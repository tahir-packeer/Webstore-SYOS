package org.example.shared.patterns.builders;

import org.example.shared.dto.ItemDTO;
import org.example.persistence.models.Item;
import java.util.concurrent.atomic.AtomicLong;

public class ItemBuilder {
    
    private static final AtomicLong counter = new AtomicLong(0);
    
    private int id = 0;
    private String code = "ITEM001";
    private String name = "Default Item";
    private double price = 10.0;
    
    public ItemBuilder withId(int id) {
        this.id = id;
        return this;
    }
    
    public ItemBuilder withCode(String code) {
        this.code = code;
        return this;
    }
    
    public ItemBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public ItemBuilder withPrice(double price) {
        this.price = price;
        return this;
    }
    
    public ItemBuilder withGeneratedCode() {
        this.code = "ITEM" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
        return this;
    }
    
    public ItemBuilder withHighPrice() {
        this.price = 999.99;
        return this;
    }
    
    public ItemBuilder withLowPrice() {
        this.price = 0.01;
        return this;
    }
    
    public ItemBuilder withZeroPrice() {
        this.price = 0.0;
        return this;
    }
    
    public ItemDTO buildDTO() {
        return new ItemDTO(id, code, name, price);
    }
    
    public Item buildModel() {
        Item item = new Item(code, name, price);
        item.setId(id);
        return item;
    }
    
    public static ItemBuilder createItem() {
        return new ItemBuilder();
    }
    
    public static ItemBuilder createStandardItem() {
        return new ItemBuilder()
            .withCode("STD001")
            .withName("Standard Item")
            .withPrice(15.99);
    }
    
    public static ItemBuilder createPremiumItem() {
        return new ItemBuilder()
            .withCode("PREM001")
            .withName("Premium Item")
            .withPrice(999.99);
    }
    
    public static ItemBuilder createDiscountItem() {
        return new ItemBuilder()
            .withCode("DISC001")
            .withName("Discount Item")
            .withPrice(1.99);
    }
}
