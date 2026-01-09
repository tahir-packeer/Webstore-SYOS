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
    itemElement.className = 'cart-item';
    
    itemElement.innerHTML = `
        <div class="row align-items-center">
            <div class="col-6">
                <h5 style="margin: 0 0 0.5rem 0; font-weight: bold; color: black;">${item.name}</h5>
                <p style="margin: 0.25rem 0; color: #666; font-size: 0.9rem;">Code: ${item.code}</p>
                <p style="margin: 0.25rem 0; color: black; font-weight: 600; font-size: 1.1rem;">${UIUtils.formatCurrency(item.price)}</p>
            </div>
            <div class="col-3">
                <div style="display: flex; align-items: center; gap: 0.5rem; justify-content: center;">
                    <button style="
                        background: white; 
                        color: black; 
                        border: 2px solid black; 
                        width: 30px; 
                        height: 30px; 
                        cursor: pointer; 
                        font-weight: bold;
                        border-radius: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    " onclick="updateQuantity(${index}, ${item.quantity - 1})" 
                       onmouseover="this.style.background='black'; this.style.color='white';"
                       onmouseout="this.style.background='white'; this.style.color='black';">-</button>
                    <input type="number" style="
                        width: 60px; 
                        text-align: center; 
                        border: 2px solid black; 
                        padding: 0.25rem;
                        border-radius: 0;
                        background: white;
                        color: black;
                        font-weight: bold;
                    " value="${item.quantity}" min="1" 
                           onchange="updateQuantity(${index}, parseInt(this.value))">
                    <button style="
                        background: white; 
                        color: black; 
                        border: 2px solid black; 
                        width: 30px; 
                        height: 30px; 
                        cursor: pointer; 
                        font-weight: bold;
                        border-radius: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    " onclick="updateQuantity(${index}, ${item.quantity + 1})"
                       onmouseover="this.style.background='black'; this.style.color='white';"
                       onmouseout="this.style.background='white'; this.style.color='black';">+</button>
                </div>
            </div>
            <div class="col-2 text-center">
                <strong style="font-size: 1.1rem; color: black;">${UIUtils.formatCurrency(item.price * item.quantity)}</strong>
            </div>
            <div class="col-1 text-center">
                <button style="
                    background: #dc3545; 
                    color: white; 
                    border: 2px solid #dc3545; 
                    width: 30px; 
                    height: 30px; 
                    cursor: pointer; 
                    font-weight: bold;
                    border-radius: 0;
                    font-size: 1.2rem;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                " onclick="removeItem(${index})" title="Remove item"
                   onmouseover="this.style.background='white'; this.style.color='#dc3545';"
                   onmouseout="this.style.background='#dc3545'; this.style.color='white';">×</button>
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
        
        // Store bill data in sessionStorage for the bill page
        const billData = {
            response: response,
            orderPayload: orderPayload,
            user: user,
            cartItems: cartItems
        };
        sessionStorage.setItem('billData', JSON.stringify(billData));
        
        // Redirect to bill page instead of showing popup
        window.location.href = 'bill.html';
        
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

// Initialize cart display on page load
document.addEventListener('DOMContentLoaded', loadCart);
loadCart();