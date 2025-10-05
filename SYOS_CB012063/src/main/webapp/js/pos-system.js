// POS System JavaScript
let cart = [];
let currentBillNumber = null;
let billTotal = 0;
let discountAmount = 0;
let appliedDiscountCode = null;

document.addEventListener('DOMContentLoaded', function() {
    if (!Router.requireAuth(['cashier'])) {
        return;
    }
    
    const user = SessionManager.getUser();
    document.getElementById('userInfo').textContent = `Cashier: ${user.name || user.username || 'Cashier'}`;
    
    // Setup form submission
    document.getElementById('itemEntryForm').addEventListener('submit', addItemToCart);
    
    // Setup cash tendered input
    document.getElementById('cashTendered').addEventListener('input', function() {
        const cashAmount = parseFloat(this.value) || 0;
        const total = billTotal;
        
        if (cashAmount >= total && total > 0) {
            document.getElementById('calculateBtn').style.display = 'block';
        } else {
            document.getElementById('calculateBtn').style.display = 'block';
            document.getElementById('completeBtn').style.display = 'none';
            document.getElementById('changeDisplay').style.display = 'none';
        }
    });
    
    generateNextBillNumber();
    updateDateTime();
    
    // Focus on item code input
    document.getElementById('itemCode').focus();
    
    // Auto-update time every minute
    setInterval(updateDateTime, 60000);
});

async function generateNextBillNumber() {
    try {
        const response = await APIClient.get('/syos/api/bills/next-number');
        currentBillNumber = response.nextNumber || 1;
        document.getElementById('currentBillNumber').textContent = currentBillNumber;
    } catch (error) {
        // Generate a fallback bill number
        currentBillNumber = Date.now().toString().slice(-6);
        document.getElementById('currentBillNumber').textContent = currentBillNumber;
    }
}

function updateDateTime() {
    const now = new Date();
    const dateTime = now.toLocaleString('en-LK', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
    document.getElementById('currentDateTime').textContent = dateTime;
}

async function addItemToCart(event) {
    event.preventDefault();
    
    const itemCode = document.getElementById('itemCode').value.toUpperCase().trim();
    const quantity = parseInt(document.getElementById('itemQuantity').value);
    
    if (!itemCode || quantity <= 0) {
        showMessage('Please enter valid item code and quantity', 'error');
        return;
    }

    try {
        // Fetch item details from database
        const itemResponse = await APIClient.get(`/syos/api/items/${itemCode}`);
        
        // Check if item already in cart
        const existingItemIndex = cart.findIndex(item => item.code === itemCode);
        
        if (existingItemIndex >= 0) {
            cart[existingItemIndex].quantity += quantity;
            cart[existingItemIndex].totalPrice = cart[existingItemIndex].quantity * cart[existingItemIndex].price;
        } else {
            cart.push({
                code: itemCode,
                name: itemResponse.name,
                price: itemResponse.price,
                quantity: quantity,
                totalPrice: itemResponse.price * quantity
            });
        }

        // Reset form
        document.getElementById('itemCode').value = '';
        document.getElementById('itemQuantity').value = '1';
        document.getElementById('itemCode').focus();
        
        updateCartDisplay();
        showMessage(`Added ${quantity}x ${itemResponse.name} to cart`, 'success');
        
    } catch (error) {
        console.error('Add item error:', error);
        showMessage(`Error: ${error.message}`, 'error');
    }
}

function updateCartDisplay() {
    const cartContainer = document.getElementById('cartItems');
    
    if (cart.length === 0) {
        cartContainer.innerHTML = `
            <div class="empty-cart-message">
                <i class="fas fa-shopping-cart fa-3x" style="color: var(--text-muted); margin-bottom: 15px;"></i>
                <p class="text-muted text-center">No items in cart</p>
                <small class="text-muted">Enter item codes above to add to cart</small>
            </div>
        `;
        billTotal = 0;
        document.getElementById('proceedBtn').disabled = true;
    } else {
        let html = '';
        billTotal = 0;
        
        cart.forEach((item, index) => {
            billTotal += item.totalPrice;
            html += `
                <div class="pos-item">
                    <div class="item-header">
                        <div class="item-code">${item.code}</div>
                        <div class="item-price">Rs ${item.totalPrice.toFixed(2)}</div>
                    </div>
                    <div class="item-name">${item.name}</div>
                    <div class="item-controls">
                        <div class="qty-control">
                            <button class="qty-btn" onclick="adjustQuantity(${index}, -1)">-</button>
                            <span class="qty-display">${item.quantity}</span>
                            <button class="qty-btn" onclick="adjustQuantity(${index}, 1)">+</button>
                        </div>
                        <small class="text-muted">@ Rs ${item.price.toFixed(2)} each</small>
                        <button class="remove-btn" onclick="removeItem(${index})">
                            <i class="fas fa-trash"></i> Remove
                        </button>
                    </div>
                </div>
            `;
        });
        
        cartContainer.innerHTML = html;
        document.getElementById('proceedBtn').disabled = false;
    }
    
    document.getElementById('billTotal').textContent = `Rs ${billTotal.toFixed(2)}`;
    
    // Calculate final total with discount
    const finalTotal = billTotal - discountAmount;
    document.getElementById('finalTotal').textContent = `Rs ${finalTotal.toFixed(2)}`;
    
    // Show/hide discount row
    if (discountAmount > 0) {
        document.getElementById('discountRow').style.display = 'flex';
        document.getElementById('discountAmount').textContent = `-Rs ${discountAmount.toFixed(2)}`;
    } else {
        document.getElementById('discountRow').style.display = 'none';
    }
    
    document.getElementById('totalItems').textContent = cart.reduce((sum, item) => sum + item.quantity, 0);
}

function adjustQuantity(index, change) {
    const newQuantity = cart[index].quantity + change;
    
    if (newQuantity <= 0) {
        removeItem(index);
        return;
    }
    
    cart[index].quantity = newQuantity;
    cart[index].totalPrice = cart[index].quantity * cart[index].price;
    updateCartDisplay();
    
    showMessage(`Updated quantity for ${cart[index].name}`, 'success');
}

function removeItem(index) {
    const itemName = cart[index].name;
    cart.splice(index, 1);
    updateCartDisplay();
    showMessage(`${itemName} removed from cart`, 'success');
}

function clearCart() {
    cart = [];
    
    // Also reset discount
    removeDiscount();
    
    updateCartDisplay();
    document.getElementById('paymentSection').style.display = 'none';
    document.getElementById('actionButtons').style.display = 'block';
    showMessage('Cart cleared', 'success');
}

function proceedToPayment() {
    if (cart.length === 0) {
        showMessage('Please add items to cart first', 'error');
        return;
    }
    
    document.getElementById('paymentSection').style.display = 'block';
    document.getElementById('actionButtons').style.display = 'none';
    document.getElementById('cashTendered').focus();
}

// Discount Code Functions
async function applyDiscountCode() {
    const discountCodeInput = document.getElementById('discountCode');
    const code = discountCodeInput.value.trim();
    
    if (!code) {
        showMessage('Please enter a discount code', 'error');
        return;
    }
    
    if (cart.length === 0) {
        showMessage('Please add items to cart before applying discount', 'error');
        return;
    }
    
    try {
        const response = await APIClient.get(`/syos/api/discount-codes/validate?code=${encodeURIComponent(code)}`);
        
        if (response.success && response.discount) {
            discountAmount = response.discount;
            appliedDiscountCode = code;
            
            // Update display
            updateCartDisplay();
            
            // Show remove button
            document.getElementById('removeDiscountBtn').style.display = 'inline-block';
            discountCodeInput.disabled = true;
            
            showMessage(`Discount applied: Rs ${discountAmount.toFixed(2)} off`, 'success');
        } else {
            showMessage(response.message || 'Invalid discount code', 'error');
        }
    } catch (error) {
        console.error('Discount validation error:', error);
        showMessage('Error validating discount code', 'error');
    }
}

function removeDiscount() {
    discountAmount = 0;
    appliedDiscountCode = null;
    
    // Reset input
    const discountCodeInput = document.getElementById('discountCode');
    discountCodeInput.value = '';
    discountCodeInput.disabled = false;
    
    // Hide remove button
    document.getElementById('removeDiscountBtn').style.display = 'none';
    
    // Update display
    updateCartDisplay();
    
    showMessage('Discount removed', 'info');
}

function calculateChange() {
    const cashTendered = parseFloat(document.getElementById('cashTendered').value) || 0;
    const finalTotal = billTotal - discountAmount;
    
    if (cashTendered < finalTotal) {
        showMessage(`Insufficient cash! Need Rs ${(finalTotal - cashTendered).toFixed(2)} more`, 'error');
        return;
    }
    
    const change = cashTendered - finalTotal;
    document.getElementById('changeAmount').textContent = `Rs ${change.toFixed(2)}`;
    document.getElementById('changeDisplay').style.display = 'block';
    document.getElementById('completeBtn').style.display = 'block';
    document.getElementById('calculateBtn').style.display = 'none';
}

async function completeSale() {
    const cashTendered = parseFloat(document.getElementById('cashTendered').value);
    const finalTotal = billTotal - discountAmount;
    const change = cashTendered - finalTotal;
    
    // Safe access to customer fields (they might not exist in the current UI)
    const customerNameElement = document.getElementById('customerName');
    const customerContactElement = document.getElementById('customerContact');
    const customerName = customerNameElement ? customerNameElement.value.trim() : '';
    const customerContact = customerContactElement ? customerContactElement.value.trim() : '';

    const billData = {
        invoiceNumber: `SYOS-${currentBillNumber}`,
        items: cart,
        fullPrice: billTotal,
        discount: discountAmount,
        discountCode: appliedDiscountCode,
        cashTendered: cashTendered,
        changeAmount: change,
        billDate: new Date().toISOString().split('T')[0],
        transactionType: 'COUNTER',
        storeType: 'STORE',
        customer: customerName ? {
            name: customerName,
            contactNumber: customerContact
        } : null
    };

    try {
        const response = await APIClient.post('/syos/api/bills/create', billData);
        
        // Show success popup with bill details
        showBillPopup(response, billData);
        
        showMessage('Sale completed successfully! Bill saved to database.', 'success');
        
    } catch (error) {
        console.error('Sale completion error:', error);
        showMessage(`Error completing sale: ${error.message}`, 'error');
    }
}

function showBillPopup(response, billData) {
    const now = new Date();
    
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
                    background: #2563eb;
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
                            <p style="margin: 5px 0;">Invoice: ${response.invoiceNumber || billData.invoiceNumber}</p>
                            <p style="margin: 5px 0;">Date: ${now.toLocaleDateString()}</p>
                            <p style="margin: 5px 0;">Time: ${now.toLocaleTimeString()}</p>
                            <p style="margin: 5px 0;">Cashier: POS System</p>
                        </div>
                    </div>
                    
                    <div class="items-section" style="margin-bottom: 20px;">
                        <h4 style="color: #374151; margin-bottom: 15px;">Items Purchased</h4>
                        <div class="items-list">
                            ${billData.items.map(item => `
                                <div style="
                                    display: flex;
                                    justify-content: space-between;
                                    align-items: center;
                                    padding: 10px 0;
                                    border-bottom: 1px solid #e5e7eb;
                                ">
                                    <div>
                                        <div style="font-weight: 500; color: #374151;">${item.code}</div>
                                        <div style="color: #6b7280; font-size: 14px;">${item.name}</div>
                                        <div style="color: #6b7280; font-size: 14px;">Qty: ${item.quantity} × Rs. ${item.price.toFixed(2)}</div>
                                    </div>
                                    <div style="font-weight: 500; color: #374151;">Rs. ${item.totalPrice.toFixed(2)}</div>
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
                            <span style="color: #374151;">Rs. ${billData.fullPrice.toFixed(2)}</span>
                        </div>
                        ${billData.discount && billData.discount > 0 ? `
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                                <span style="color: #6b7280;">Discount:</span>
                                <span style="color: #ef4444;">-Rs. ${billData.discount.toFixed(2)}</span>
                            </div>
                        ` : ''}
                        <hr style="border: none; border-top: 1px solid #d1d5db; margin: 10px 0;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span style="color: #6b7280;">Cash Tendered:</span>
                            <span style="color: #374151;">Rs. ${billData.cashTendered.toFixed(2)}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; font-size: 18px; font-weight: bold;">
                            <span style="color: #374151;">Change:</span>
                            <span style="color: #059669;">Rs. ${billData.changeAmount.toFixed(2)}</span>
                        </div>
                    </div>
                    
                    <div class="bill-actions" style="margin-top: 20px; text-align: center;">
                        <button onclick="closeBillPopup(); startNewTransaction();" style="
                            background: #16a34a;
                            color: white;
                            border: none;
                            padding: 10px 20px;
                            border-radius: 5px;
                            cursor: pointer;
                            font-size: 14px;
                        ">New Transaction</button>
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
}

function startNewTransaction() {
    cart = [];
    billTotal = 0;
    
    // Reset discount
    discountAmount = 0;
    appliedDiscountCode = null;
    const discountCodeInput = document.getElementById('discountCode');
    discountCodeInput.value = '';
    discountCodeInput.disabled = false;
    document.getElementById('removeDiscountBtn').style.display = 'none';
    
    // Safe access to customer fields (they might not exist in the current UI)
    const customerNameElement = document.getElementById('customerName');
    const customerContactElement = document.getElementById('customerContact');
    if (customerNameElement) customerNameElement.value = '';
    if (customerContactElement) customerContactElement.value = '';
    
    document.getElementById('cashTendered').value = '';
    
    document.getElementById('paymentSection').style.display = 'none';
    document.getElementById('actionButtons').style.display = 'block';
    document.getElementById('changeDisplay').style.display = 'none';
    
    updateCartDisplay();
    generateNextBillNumber();
    document.getElementById('itemCode').focus();
    
    showMessage('Ready for new transaction', 'success');
}

function showMessage(message, type) {
    const messagesDiv = document.getElementById('itemEntryMessages');
    const className = type === 'error' ? 'error-message' : 'success-message';
    
    messagesDiv.innerHTML = `<div class="${className}">${message}</div>`;
    
    setTimeout(() => {
        messagesDiv.innerHTML = '';
    }, 3000);
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