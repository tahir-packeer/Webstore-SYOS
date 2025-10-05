// Customer Cart Page JavaScript
let cartItems = [];
let appliedDiscount = 0;

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    checkUserStatus();
    loadCart();
});

function checkUserStatus() {
    const user = SessionManager.getUser();
    if (user && SessionManager.checkSession()) {
        document.getElementById('userInfo').textContent = `Welcome, ${user.name || user.email || 'Customer'}`;
        document.getElementById('authButton').textContent = 'Logout';
        document.getElementById('authButton').onclick = logout;
    } else {
        document.getElementById('userInfo').textContent = 'Guest';
        document.getElementById('authButton').textContent = 'Login';
        document.getElementById('authButton').onclick = () => Router.navigate('/syos/pages/customer/login.html');
    }
    
    CartManager.updateCartCount();
}

function loadCart() {
    cartItems = CartManager.getCart();
    
    if (cartItems.length === 0) {
        showEmptyCart();
        return;
    }
    
    displayCartItems();
    updateSummary();
}

function showEmptyCart() {
    // Hide the main cart content
    const cartContent = document.getElementById('cartContent');
    if (cartContent) {
        cartContent.style.display = 'none';
    }
    
    // Show the empty cart state
    const emptyState = document.getElementById('emptyCartState');
    if (emptyState) {
        emptyState.style.display = 'block';
    }
    
    // Update the page header to reflect empty state
    document.querySelector('.row.mb-4 h1').textContent = 'Your Cart is Empty';
    document.querySelector('.row.mb-4 p').textContent = 'Browse our catalog to add items to your cart';
    
    // Disable pay button and update summary
    document.getElementById('payBtn').disabled = true;
    document.getElementById('subtotal').textContent = 'LKR 0.00';
    document.getElementById('discount').textContent = '-LKR 0.00';
    document.getElementById('total').textContent = 'LKR 0.00';
}

function displayCartItems() {
    const container = document.getElementById('cartItems');
    
    if (cartItems.length === 0) {
        showEmptyCart();
        return;
    }
    
    // Show the main cart content and hide empty state
    const cartContent = document.getElementById('cartContent');
    const emptyState = document.getElementById('emptyCartState');
    
    if (cartContent) {
        cartContent.style.display = 'flex';
    }
    if (emptyState) {
        emptyState.style.display = 'none';
    }
    
    // Restore normal page header
    document.querySelector('.row.mb-4 h1').textContent = 'Shopping Cart';
    document.querySelector('.row.mb-4 p').textContent = 'Review your items before payment';
    
    container.innerHTML = '';
    
    cartItems.forEach((item, index) => {
        const cartItem = createCartItemElement(item, index);
        container.appendChild(cartItem);
    });
}

function createCartItemElement(item, index) {
    const itemElement = document.createElement('div');
    itemElement.className = 'cart-item mb-3 p-3';
    itemElement.style.cssText = `
        border: 1px solid var(--border-color);
        border-radius: var(--border-radius);
        background: white;
    `;
    
    itemElement.innerHTML = `
        <div class="row align-items-center">
            <div class="col-6">
                <h5>${item.name}</h5>
                <p class="text-muted">Code: ${item.code}</p>
                <p class="text-primary">${UIUtils.formatCurrency(item.price)}</p>
            </div>
            <div class="col-3">
                <div class="d-flex align-items-center gap-2">
                    <button class="btn btn-sm btn-outline" onclick="updateQuantity(${index}, ${item.quantity - 1})">-</button>
                    <input type="number" class="form-control text-center" style="width: 80px;" 
                           value="${item.quantity}" min="1" 
                           onchange="updateQuantity(${index}, parseInt(this.value))">
                    <button class="btn btn-sm btn-outline" onclick="updateQuantity(${index}, ${item.quantity + 1})">+</button>
                </div>
            </div>
            <div class="col-2 text-center">
                <strong>${UIUtils.formatCurrency(item.price * item.quantity)}</strong>
            </div>
            <div class="col-1 text-center">
                <button class="btn btn-sm btn-danger" onclick="removeItem(${index})" title="Remove item">
                    ×
                </button>
            </div>
        </div>
    `;
    
    return itemElement;
}

function updateQuantity(index, newQuantity) {
    if (newQuantity <= 0) {
        removeItem(index);
        return;
    }
    
    cartItems[index].quantity = newQuantity;
    CartManager.updateCartItem(cartItems[index].code, newQuantity);
    
    displayCartItems();
    updateSummary();
    CartManager.updateCartCount();
}

function removeItem(index) {
    const item = cartItems[index];
    if (confirm(`Remove ${item.name} from cart?`)) {
        CartManager.removeFromCart(item.code);
        cartItems = CartManager.getCart();
        
        if (cartItems.length === 0) {
            // Clear any applied discounts when cart becomes empty
            if (appliedDiscount > 0) {
                appliedDiscount = 0;
                // Reset discount UI
                document.getElementById('discountCode').disabled = false;
                document.getElementById('discountCode').value = '';
                const applyBtn = document.querySelector('.btn[onclick="removeDiscount()"]') || 
                                document.querySelector('.btn[onclick="applyDiscount()"]');
                if (applyBtn) {
                    applyBtn.textContent = 'Apply';
                    applyBtn.onclick = applyDiscount;
                    applyBtn.className = 'btn btn-secondary';
                }
            }
            showEmptyCart();
        } else {
            displayCartItems();
            updateSummary();
        }
        
        CartManager.updateCartCount();
        UIUtils.showAlert(`${item.name} removed from cart`, 'info', 2000);
    }
}

function clearCart() {
    if (cartItems.length === 0) return;
    
    if (confirm('Are you sure you want to clear your entire cart?')) {
        CartManager.clearCart();
        cartItems = [];
        
        // Clear any applied discounts
        if (appliedDiscount > 0) {
            appliedDiscount = 0;
            // Reset discount UI
            document.getElementById('discountCode').disabled = false;
            document.getElementById('discountCode').value = '';
            const applyBtn = document.querySelector('.btn[onclick="removeDiscount()"]') || 
                            document.querySelector('.btn[onclick="applyDiscount()"]');
            if (applyBtn) {
                applyBtn.textContent = 'Apply';
                applyBtn.onclick = applyDiscount;
                applyBtn.className = 'btn btn-secondary';
            }
        }
        
        showEmptyCart();
        CartManager.updateCartCount();
        UIUtils.showAlert('Cart cleared', 'info', 2000);
    }
}

function updateSummary() {
    const subtotal = cartItems.reduce((total, item) => total + (item.price * item.quantity), 0);
    const discountAmount = appliedDiscount; // appliedDiscount is now the actual discount amount in Rs
    const total = subtotal - discountAmount;
    
    document.getElementById('subtotal').textContent = UIUtils.formatCurrency(subtotal);
    document.getElementById('discount').textContent = `-${UIUtils.formatCurrency(discountAmount)}`;
    document.getElementById('total').textContent = UIUtils.formatCurrency(total);
    
    // Enable/disable pay button
    const payBtn = document.getElementById('payBtn');
    payBtn.disabled = cartItems.length === 0;
}

function applyDiscount() {
    const discountCode = document.getElementById('discountCode').value.trim().toUpperCase();
    
    if (!discountCode) {
        UIUtils.showAlert('Please enter a discount code', 'warning');
        return;
    }
    
    if (cartItems.length === 0) {
        UIUtils.showAlert('Please add items to cart before applying discount', 'warning');
        return;
    }
    
    // Call the discount validation API
    fetch(`/syos/api/discount-codes/validate?code=${encodeURIComponent(discountCode)}`)
        .then(response => response.json())
        .then(data => {
            if (data.success && data.discount > 0) {
                appliedDiscount = data.discount;
                updateSummary();
                
                // Disable the input and show remove option
                document.getElementById('discountCode').disabled = true;
                const applyBtn = document.querySelector('.btn[onclick="applyDiscount()"]');
                applyBtn.textContent = 'Remove';
                applyBtn.onclick = removeDiscount;
                applyBtn.className = 'btn btn-outline-secondary';
                
                UIUtils.showAlert(`Discount applied: Rs ${data.discount.toFixed(2)} off!`, 'success');
            } else {
                UIUtils.showAlert(data.message || 'Invalid discount code', 'error');
            }
        })
        .catch(error => {
            UIUtils.showAlert('Error validating discount code. Please try again.', 'error');
        });
}

function removeDiscount() {
    appliedDiscount = 0;
    updateSummary();
    
    // Re-enable the input and show apply option
    document.getElementById('discountCode').disabled = false;
    document.getElementById('discountCode').value = '';
    const applyBtn = document.querySelector('.btn[onclick="removeDiscount()"]');
    applyBtn.textContent = 'Apply';
    applyBtn.onclick = applyDiscount;
    applyBtn.className = 'btn btn-secondary';
    
    UIUtils.showAlert('Discount removed', 'info');
}

async function payNow() {            
    if (cartItems.length === 0) {
        UIUtils.showAlert('Your cart is empty', 'warning');
        return;
    }
    
    const user = SessionManager.getUser();
    
    if (!user || !SessionManager.checkSession()) {
        if (confirm('You need to be logged in to place an order. Would you like to login now?')) {
            window.location.href = 'login.html';
        }
        return;
    }
    
    try {
        // Disable pay button during processing
        const payBtn = document.getElementById('payBtn');
        payBtn.disabled = true;
        payBtn.textContent = 'Processing...';
        
        // Calculate totals
        const subtotal = cartItems.reduce((total, item) => total + (item.price * item.quantity), 0);
        const discountAmount = appliedDiscount; 
        const total = subtotal - discountAmount;
        
        // Prepare order data
        const orderPayload = {
            customerId: user.contactNumber || user.phone || user.id || 1, 
            items: cartItems.map(item => ({
                code: item.code,
                quantity: item.quantity,
                price: item.price
            })),
            subtotal: subtotal,
            discount: discountAmount,
            discountCode: discountAmount > 0 ? document.getElementById('discountCode').value.trim().toUpperCase() : null,
            total: total,
            paymentMethod: 'online',
            customerInfo: {
                name: user.name || user.email,
                phone: user.phone || user.contactNumber,
                email: user.email
            }
        };
        
        const response = await APIClient.post(API_ENDPOINTS.SALES_ONLINE, orderPayload);
        
        CartManager.clearCart();
        CartManager.updateCartCount();
        
        // Show bill popup with payment details
        showBillPopup(response, orderPayload, user);
        
        // Refresh cart display
        loadCart();
        
    } catch (error) {
        UIUtils.showAlert('Payment failed. Please try again or contact support.', 'danger');
    } finally {
        // Re-enable pay button
        const payBtn = document.getElementById('payBtn');
        payBtn.disabled = false;
        payBtn.textContent = 'Pay Now';
    }
}

function handleAuth() {
    const user = SessionManager.getUser();
    if (user && SessionManager.checkSession()) {
        logout();
    } else {
        Router.navigate('/syos/pages/customer/login.html');
    }
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        SessionManager.clearSession();
        UIUtils.showAlert('Logged out successfully', 'info');
        setTimeout(() => {
            Router.navigate('/syos/index.html');
        }, 1000);
    }
}

function showBillPopup(response, orderPayload, user) {
    // Create bill popup content
    const billContent = `
        <div class="bill-popup-overlay" id="billPopup" style="
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.7);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 1000;
        ">
            <div class="bill-container" style="
                background: white;
                max-width: 500px;
                width: 90%;
                max-height: 90%;
                border-radius: 10px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
                overflow-y: auto;
                position: relative;
            ">
                <div class="bill-header" style="
                    background: #3b82f6;
                    color: white;
                    padding: 20px;
                    text-align: center;
                    border-radius: 10px 10px 0 0;
                ">
                    <h2 style="margin: 0; font-size: 24px;">Payment Receipt</h2>
                    <button onclick="closeBillPopup()" style="
                        position: absolute;
                        top: 15px;
                        right: 15px;
                        background: none;
                        border: none;
                        color: white;
                        font-size: 24px;
                        cursor: pointer;
                        width: 30px;
                        height: 30px;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    ">&times;</button>
                </div>
                <div class="bill-body" style="padding: 20px;">
                    <div class="bill-info" style="margin-bottom: 20px;">
                        <h3 style="color: #1f2937; margin-bottom: 15px; text-align: center;">SYOS Retail Management</h3>
                        <div style="text-align: center; margin-bottom: 20px; color: #6b7280;">
                            <p style="margin: 5px 0;">Invoice: ${response.invoiceNumber || 'N/A'}</p>
                            <p style="margin: 5px 0;">Date: ${new Date().toLocaleDateString()}</p>
                            <p style="margin: 5px 0;">Time: ${new Date().toLocaleTimeString()}</p>
                        </div>
                    </div>
                    
                    <div class="customer-info" style="
                        background: #f9fafb;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 20px;
                    ">
                        <h4 style="color: #374151; margin: 0 0 10px 0;">Customer Details</h4>
                        <p style="margin: 5px 0; color: #6b7280;"><strong>Name:</strong> ${user.name}</p>
                        <p style="margin: 5px 0; color: #6b7280;"><strong>Phone:</strong> ${user.contactNumber}</p>
                        <p style="margin: 5px 0; color: #6b7280;"><strong>Email:</strong> ${user.email}</p>
                    </div>
                    
                    <div class="items-section" style="margin-bottom: 20px;">
                        <h4 style="color: #374151; margin-bottom: 15px;">Items Purchased</h4>
                        <div class="items-list">
                            ${orderPayload.items.map(item => `
                                <div style="
                                    display: flex;
                                    justify-content: space-between;
                                    align-items: center;
                                    padding: 10px 0;
                                    border-bottom: 1px solid #e5e7eb;
                                ">
                                    <div>
                                        <div style="font-weight: 500; color: #374151;">${item.code}</div>
                                        <div style="color: #6b7280; font-size: 14px;">Qty: ${item.quantity} × Rs. ${item.price.toFixed(2)}</div>
                                    </div>
                                    <div style="font-weight: 500; color: #374151;">Rs. ${(item.quantity * item.price).toFixed(2)}</div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                    
                    <div class="bill-summary" style="
                        background: #f9fafb;
                        padding: 15px;
                        border-radius: 8px;
                        border: 2px solid #e5e7eb;
                    ">
                        <h4 style="color: #374151; margin: 0 0 15px 0;">Payment Summary</h4>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span style="color: #6b7280;">Subtotal:</span>
                            <span style="color: #374151;">Rs. ${(response.total || orderPayload.total).toFixed(2)}</span>
                        </div>
                        ${response.discount && response.discount > 0 ? `
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                                <span style="color: #6b7280;">Discount:</span>
                                <span style="color: #ef4444;">-Rs. ${response.discount.toFixed(2)}</span>
                            </div>
                        ` : ''}
                        <hr style="border: none; border-top: 1px solid #d1d5db; margin: 10px 0;">
                        <div style="display: flex; justify-content: space-between; font-size: 18px; font-weight: bold;">
                            <span style="color: #374151;">Total Paid:</span>
                            <span style="color: #059669;">Rs. ${(response.finalTotal || orderPayload.total).toFixed(2)}</span>
                        </div>
                    </div>
                    
                    <div class="bill-actions" style="margin-top: 20px; text-align: center;">
                        <button onclick="closeBillPopup()" style="
                            background: #6b7280;
                            color: white;
                            border: none;
                            padding: 10px 20px;
                            border-radius: 5px;
                            cursor: pointer;
                            font-size: 14px;
                        ">Close</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Add bill popup to the page
    document.body.insertAdjacentHTML('beforeend', billContent);
}

function closeBillPopup() {
    const billPopup = document.getElementById('billPopup');
    if (billPopup) {
        billPopup.remove();
    }
    
    // Ensure cart is cleared and display is refreshed
    CartManager.clearCart();
    CartManager.updateCartCount();
    loadCart();
}

// Initialize cart display on page load
loadCart();