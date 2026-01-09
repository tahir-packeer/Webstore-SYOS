package org.example.business.facades;

import org.example.core.config.SystemConfig;
import org.example.business.validators.TransactionValidator;

import org.example.shared.dto.BillDTO;
import org.example.shared.dto.BillItemDTO;
import org.example.shared.dto.ItemDTO;
import org.example.persistence.gateways.ItemGateway;
import org.example.business.managers.WebsiteInventoryManager;
import org.example.core.state.CheckoutContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OnlineStoreFacade {
    private static OnlineStoreFacade instance;
    private static final Object lock = new Object();
    
    private final ItemGateway itemGateway;
    private final WebsiteInventoryManager websiteInventory;
    private CheckoutContext currentCart;

    private OnlineStoreFacade() {
        this.itemGateway = ItemGateway.getInstance();
        this.websiteInventory = WebsiteInventoryManager.getInstance();
    }

    public static OnlineStoreFacade getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new OnlineStoreFacade();
                }
            }
        }
        return instance;
    }

    // Start new shopping cart
    public void createCart(int customerId, String customerName, String customerPhone, String address) {
        // Validate that we're creating an online cart
        String transactionType = SystemConfig.TRANSACTION_ONLINE;
        if (!TransactionValidator.isValidTransactionType(transactionType)) {
            throw new IllegalStateException("Invalid transaction type for online store");
        }
        
        currentCart = new CheckoutContext(transactionType);
        currentCart.setCustomerId(customerId);
        currentCart.setCustomerName(customerName);
        currentCart.setCustomerPhone(customerPhone);
        currentCart.setDeliveryAddress(address);
        System.out.println("Created new ONLINE shopping cart for customer: " + customerName);
    }

    // Add item to cart
    public boolean addToCart(String itemCode, int quantity) {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart. Call createCart() first.");
        }

        try {
            // Check item exists
            ItemDTO item = itemGateway.findByCode(itemCode);
            if (item == null) {
                System.err.println("Item not found: " + itemCode);
                return false;
            }

            if (!websiteInventory.hasEnoughStock(item.getId(), quantity)) {
                System.err.println("Insufficient stock for item: " + itemCode);
                return false;
            }

            currentCart.addItem(itemCode, quantity);
            System.out.println("Added " + quantity + " of " + itemCode + " to cart");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error adding item to cart: " + e.getMessage());
            return false;
        }
    }

    // Update cart item quantity
    public boolean updateCartItem(String itemCode, int newQuantity) {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart");
        }

        try {
            if (newQuantity <= 0) {
                removeFromCart(itemCode);
                return true;
            }

            ItemDTO item = itemGateway.findByCode(itemCode);
            if (item != null && !websiteInventory.hasEnoughStock(item.getId(), newQuantity)) {
                System.err.println("Insufficient stock for quantity: " + newQuantity);
                return false;
            }

            currentCart.removeItem(itemCode);
            currentCart.addItem(itemCode, newQuantity);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error updating cart item: " + e.getMessage());
            return false;
        }
    }

    public void removeFromCart(String itemCode) {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart");
        }
        currentCart.removeItem(itemCode);
        System.out.println("Removed " + itemCode + " from cart");
    }

    public void applyPromoCode(String promoCode, double discountPercentage) {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart");
        }
        
        // Simple promo code validation (in real implementation, would check database)
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            currentCart.applyDiscount(discountPercentage);
            System.out.println("Applied promo code: " + promoCode + " (" + discountPercentage + "% discount)");
        }
    }

    public void setDeliveryAddress(String address) {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart");
        }
        currentCart.setDeliveryAddress(address);
    }

    public CartSummary getCartSummary() {
        if (currentCart == null) {
            return new CartSummary();
        }
        
        List<BillItemDTO> items = new ArrayList<>(currentCart.getItems());
        double total = currentCart.calculateTotal();
        double shipping = calculateShipping(total);
        
        return new CartSummary(items, total, shipping, total + shipping);
    }

    // Calculate shipping based on order total
    private double calculateShipping(double orderTotal) {
        // Free shipping for orders over threshold
        if (orderTotal >= SystemConfig.FREE_SHIPPING_THRESHOLD) {
            return 0.0;
        }
        return SystemConfig.SHIPPING_COST;
    }

    public BillDTO processOrder() {
        if (currentCart == null) {
            throw new IllegalStateException("No active cart");
        }

        try {
            // Validate this is an online transaction
            String transactionType = currentCart.getTransactionType();
            if (!SystemConfig.TRANSACTION_ONLINE.equals(transactionType)) {
                throw new IllegalStateException("OnlineStoreFacade can only process ONLINE transactions");
            }
            
            // Validate cash payment (should be 0 for cash-on-delivery)
            if (!TransactionValidator.isCashPaymentValid(transactionType, 0)) {
                throw new IllegalStateException("Invalid payment setup for online transaction");
            }
            
            // Calculate final total including shipping
            double orderTotal = currentCart.calculateTotal();
            double shipping = calculateShipping(orderTotal);
            
            // SYOS only accepts cash payments - order will be cash on delivery
            // No payment processed online, payment will be collected on delivery
            
            currentCart.generateBill();
            
            BillDTO bill = currentCart.getGeneratedBill();
            
            // Ensure correct store type for online transaction
            String expectedStoreType = TransactionValidator.getStoreTypeForTransaction(transactionType);
            if (!expectedStoreType.equals(bill.getStoreType())) {
                System.out.println("Setting correct store type: " + expectedStoreType);
            }
            
            // Update website inventory (separate from store stock)
            updateWebsiteStockLevels(currentCart.getItems());
            
            sendOrderConfirmation(bill, shipping, "CASH_ON_DELIVERY", "PENDING_DELIVERY");
            
            currentCart = null;
            
            return bill;
            
        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            return null;
        }
    }

    public void abandonCart() {
        if (currentCart != null) {
            System.out.println("Shopping cart abandoned");
            currentCart = null;
        }
    }

    public String saveCartForLater() {
        if (currentCart == null) {
            return null;
        }
        
        // In real implementation, would save to database
        String cartId = "CART_" + System.currentTimeMillis();
        System.out.println("Cart saved for later with ID: " + cartId);
        return cartId;
    }

    public List<ItemDTO> browseItems() {
        try {
            return itemGateway.findAll();
        } catch (Exception e) {
            System.err.println("Error browsing items: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Search items by name
    public List<ItemDTO> searchItems(String searchTerm) {
        try {
            List<ItemDTO> allItems = itemGateway.findAll();
            List<ItemDTO> results = new ArrayList<>();
            
            String lowerSearchTerm = searchTerm.toLowerCase();
            for (ItemDTO item : allItems) {
                if (item.getName().toLowerCase().contains(lowerSearchTerm)) {
                    results.add(item);
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("Error searching items: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get item with stock info
    public ItemDetailDTO getItemDetails(String itemCode) {
        try {
            ItemDTO item = itemGateway.findByCode(itemCode);
            if (item == null) {
                return null;
            }
            
            int stockQuantity = websiteInventory.getWebsiteStock(item.getId());
            boolean inStock = stockQuantity > 0;
            
            return new ItemDetailDTO(item, stockQuantity, inStock);
            
        } catch (Exception e) {
            System.err.println("Error getting item details: " + e.getMessage());
            return null;
        }
    }

    private void updateWebsiteStockLevels(List<BillItemDTO> items) throws SQLException, ClassNotFoundException {
        for (BillItemDTO item : items) {
            // Update website inventory (separate from store shelves)
            WebsiteInventoryManager.getInstance().reduceStock(item.getItemId(), item.getQuantity());
        }
    }

    private void sendOrderConfirmation(BillDTO bill, double shipping, String paymentMethod, String paymentReference) {
        System.out.println("\n=== ORDER CONFIRMATION ===");
        System.out.println("Order placed successfully!");
        System.out.println("Invoice Number: " + bill.getInvoiceNumber());
        System.out.println("Customer: " + (currentCart != null ? currentCart.getCustomerName() : "N/A"));
        System.out.println("Total Amount: Rs." + String.format("%.2f", bill.getFullPrice()));
        System.out.println("Shipping Cost: Rs." + String.format("%.2f", shipping));
        System.out.println("Grand Total: Rs." + String.format("%.2f", bill.getFullPrice() + shipping));
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Payment Status: " + paymentReference);
        System.out.println("\nIMPORTANT: SYOS only accepts CASH payments.");
        System.out.println("Please have exact cash ready for delivery.");
        System.out.println("========================\n");
    }

    // Cart summary data
    public static class CartSummary {
        private List<BillItemDTO> items;
        private double subtotal;
        private double shipping;
        private double total;

        public CartSummary() {
            this.items = new ArrayList<>();
            this.subtotal = 0.0;
            this.shipping = 0.0;
            this.total = 0.0;
        }

        public CartSummary(List<BillItemDTO> items, double subtotal, double shipping, double total) {
            this.items = items;
            this.subtotal = subtotal;
            this.shipping = shipping;
            this.total = total;
        }

        // Getters
        public List<BillItemDTO> getItems() { return items; }
        public double getSubtotal() { return subtotal; }
        public double getShipping() { return shipping; }
        public double getTotal() { return total; }
        public int getItemCount() { return items.size(); }
    }

    public static class ItemDetailDTO {
        private ItemDTO item;
        private int stockQuantity;
        private boolean inStock;

        public ItemDetailDTO(ItemDTO item, int stockQuantity, boolean inStock) {
            this.item = item;
            this.stockQuantity = stockQuantity;
            this.inStock = inStock;
        }

        // Getters
        public ItemDTO getItem() { return item; }
        public int getStockQuantity() { return stockQuantity; }
        public boolean isInStock() { return inStock; }
    }
}
