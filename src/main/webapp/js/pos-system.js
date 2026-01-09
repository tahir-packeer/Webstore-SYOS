// POS System JavaScript
let cart = [];
let currentBillNumber = null;
let billTotal = 0;
let discountAmount = 0;
let appliedDiscountCode = null;
let selectedCustomer = null;
let searchTimeout = null;

document.addEventListener('DOMContentLoaded', function () {
    if (!Router.requireAuth(['cashier'])) {
        return;
    }

    const user = SessionManager.getUser();
    document.getElementById('userInfo').textContent = `Cashier: ${user.name || user.username || 'Cashier'}`;

    // Setup form submission
    document.getElementById('itemEntryForm').addEventListener('submit', addItemToCart);

    // Setup customer registration form
    const customerForm = document.getElementById('customerRegistrationForm');
    if (customerForm) {
        customerForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            
            const name = document.getElementById('newCustomerName').value.trim();
            const phone = document.getElementById('newCustomerPhone').value.trim();
            
            if (!name || !phone) {
                showMessage('Please fill in all customer fields', 'error');
                return;
            }
            
            // Validate phone number format
            if (!/^\d{10}$/.test(phone.replace(/\D/g, ''))) {
                showMessage('Please enter a valid 10-digit phone number', 'error');
                return;
            }
            
            try {
                const customerData = { name, contactNumber: phone };
                const response = await APIClient.post('/syos/api/pos-customers/register', customerData);
                
                if (response.success) {
                    // Auto-select the newly registered customer
                    selectedCustomer = {
                        id: response.customerId,
                        name: name,
                        phone: phone
                    };
                    
                    // Update selected customer info
                    document.getElementById('selectedCustomerName').textContent = name;
                    document.getElementById('selectedCustomerPhone').textContent = phone;
                    document.getElementById('selectedCustomerInfo').style.display = 'block';
                    
                    // Clear form
                    document.getElementById('newCustomerName').value = '';
                    document.getElementById('newCustomerPhone').value = '';
                    
                    showMessage(`Customer ${name} registered successfully`, 'success');
                } else {
                    showMessage(response.message || 'Registration failed', 'error');
                }
            } catch (error) {
                console.error('Customer registration error:', error);
                if (error.message.includes('already exists')) {
                    showMessage('A customer with this phone number already exists', 'error');
                } else {
                    showMessage(`Registration error: ${error.message}`, 'error');
                }
            }
        });
    }

    // Setup cash tendered input
    document.getElementById('cashTendered').addEventListener('input', function () {
        const cashAmount = parseFloat(this.value) || 0;
        const total = billTotal;

        if (cashAmount >= total && total > 0) {
            // Auto-calculate change when cash amount is sufficient
            calculateChange();
        } else {
            document.getElementById('completeBtn').style.display = 'none';
            document.getElementById('changeDisplay').style.display = 'none';
        }
    });

    // Check if returning from bill page and need to reset
    const shouldReset = sessionStorage.getItem("resetPOS");
    if (shouldReset === "true") {
        sessionStorage.removeItem("resetPOS");
        resetPOSForNewTransaction();
    } else {
        // Set a placeholder bill number - actual invoice numbers are generated server-side
        currentBillNumber = "PENDING";
        // Note: currentBillNumber display removed as it's not in the HTML
        updateDateTime();
    }

    // Focus on item code input
    document.getElementById('itemCode').focus();

    // Auto-update time every minute
    setInterval(updateDateTime, 60000);
});

async function generateNextBillNumber() {
    // Invoice numbers are now generated server-side during bill creation
    // Display a placeholder until the actual invoice is generated
    currentBillNumber = "PENDING";
    // Note: No display element for bill number in current HTML
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
    // Note: currentDateTime element not found in HTML - removing display
    console.log("Current time:", dateTime);
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
            <div class="empty-cart">
                No items added yet. Enter item codes to add to cart.
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
                <div class="cart-item">
                    <div class="item-header">
                        <div class="item-info">
                            <div class="item-name">${item.name}</div>
                            <div class="item-code">${item.code}</div>
                        </div>
                        <div class="item-price-section">
                            <div class="item-unit-price">@ Rs ${item.price.toFixed(2)} each</div>
                            <div class="item-total-price">Rs ${item.totalPrice.toFixed(2)}</div>
                        </div>
                    </div>
                    <div class="item-controls">
                        <div class="qty-controls">
                            <button class="qty-btn" onclick="adjustQuantity(${index}, -1)">−</button>
                            <span class="qty-display">${item.quantity}</span>
                            <button class="qty-btn" onclick="adjustQuantity(${index}, 1)">+</button>
                        </div>
                        <button class="remove-item-btn" onclick="removeItem(${index})">
                            Remove
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

    // Also reset customer
    selectedCustomer = null;
    updateCustomerDisplay(null);

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
        document.getElementById('completeBtn').style.display = 'none';
        document.getElementById('changeDisplay').style.display = 'none';
        return;
    }

    const change = cashTendered - finalTotal;
    document.getElementById('changeAmount').textContent = `Rs ${change.toFixed(2)}`;
    document.getElementById('changeDisplay').style.display = 'block';
    document.getElementById('completeBtn').style.display = 'block';
}

async function completeSale() {
    console.log('completeSale function called - should redirect to bill page');

    const cashTendered = parseFloat(document.getElementById('cashTendered').value);
    const finalTotal = billTotal - discountAmount;
    const change = cashTendered - finalTotal;

    const billData = {
        // Remove invoiceNumber - let server generate it
        items: cart,
        fullPrice: billTotal,
        discount: discountAmount,
        discountCode: appliedDiscountCode,
        cashTendered: cashTendered,
        changeAmount: change,
        billDate: new Date().toISOString().split('T')[0],
        transactionType: 'COUNTER',
        storeType: 'STORE',
        customer: selectedCustomer ? {
            id: selectedCustomer.id,
            name: selectedCustomer.name,
            contactNumber: selectedCustomer.phone
        } : null
    };

    try {
        const response = await APIClient.post('/syos/api/bills/create', billData);

        // Prepare bill data for the cashier bill page
        const cashierBillData = {
            response: {
                billId: response.billId,
                invoiceNumber: response.invoiceNumber, // Use server-generated invoice number
                total: billTotal,
                finalTotal: response.finalTotal || (billTotal - discountAmount),
                discount: discountAmount
            },
            cartItems: cart.map(item => ({
                code: item.code,
                name: item.name,
                quantity: item.quantity,
                price: item.price,
                totalPrice: item.totalPrice
            })),
            cashTendered: cashTendered,
            changeAmount: change,
            customer: selectedCustomer ? {
                name: selectedCustomer.name,
                phone: selectedCustomer.phone
            } : {
                name: "Walk-in Customer",
                phone: "N/A"
            },
            discountCode: appliedDiscountCode,
            timestamp: new Date().toISOString()
        };

        // Store bill data for the bill page
        sessionStorage.setItem('cashierBillData', JSON.stringify(cashierBillData));
        
        console.log("Stored cashier bill data:", cashierBillData);
        console.log("SessionStorage set successfully");

        showMessage('Sale completed successfully! Redirecting to bill...', 'success');

        console.log(`Bill created successfully with ID: ${response.billId}, Invoice: ${response.invoiceNumber}`);
        console.log('About to redirect to bill.html');

        // Redirect to bill page after a short delay
        setTimeout(() => {
            console.log('Redirecting now...');
            window.location.href = 'bill.html?billId=' + response.billId + '&t=' + Date.now();
        }, 1000);

    } catch (error) {
        console.error('Sale completion error:', error);
        showMessage(`Error completing sale: ${error.message}`, 'error');
    }
}

function resetPOSForNewTransaction() {
    // Reset cart and totals
    cart = [];
    billTotal = 0;
    discountAmount = 0;
    appliedDiscountCode = null;
    selectedCustomer = null;

    // Reset form fields
    document.getElementById('itemCode').value = '';
    document.getElementById('itemQuantity').value = '1';
    document.getElementById('cashTendered').value = '';

    // Reset discount section
    const discountCodeInput = document.getElementById('discountCode');
    if (discountCodeInput) {
        discountCodeInput.value = '';
        discountCodeInput.disabled = false;
    }

    const removeDiscountBtn = document.getElementById('removeDiscountBtn');
    if (removeDiscountBtn) {
        removeDiscountBtn.style.display = 'none';
    }

    // Reset customer section
    updateCustomerDisplay(null);

    // Reset payment section visibility
    const paymentSection = document.getElementById('paymentSection');
    const actionButtons = document.getElementById('actionButtons');
    const changeDisplay = document.getElementById('changeDisplay');

    if (paymentSection) paymentSection.style.display = 'none';
    if (actionButtons) actionButtons.style.display = 'block';
    if (changeDisplay) changeDisplay.style.display = 'none';

    // Update display
    updateCartDisplay();
    // Set placeholder bill number - actual invoice generated server-side
    currentBillNumber = "PENDING";
    // Note: No display element for bill number in current HTML

    // Focus on item code input
    document.getElementById('itemCode').focus();
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

    // Reset customer
    selectedCustomer = null;
    updateCustomerDisplay(null);

    document.getElementById('cashTendered').value = '';

    document.getElementById('paymentSection').style.display = 'none';
    document.getElementById('actionButtons').style.display = 'block';
    document.getElementById('changeDisplay').style.display = 'none';

    updateCartDisplay();
    // Set placeholder bill number - actual invoice generated server-side  
    currentBillNumber = "PENDING";
    // Note: No display element for bill number in current HTML
    document.getElementById('itemCode').focus();

    showMessage('Ready for new transaction', 'success');
}

function showMessage(message, type) {
    // Show message using alert for now since itemEntryMessages element doesn't exist
    if (type === 'error') {
        console.error(message);
        alert(message);
    } else if (type === 'success') {
        console.log(message);
        // Don't show success messages as alerts to avoid spam
    } else {
        console.log(message);
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

// Customer Management Functions
function openCustomerPanel() {
    document.getElementById('customerPanelOverlay').classList.add('show');
    document.getElementById('customerPanel').classList.add('show');
    document.getElementById('customerSearch').focus();
    
    // Clear previous search results
    document.getElementById('searchResults').innerHTML = '';
    document.getElementById('selectedCustomerInfo').style.display = 'none';
}

function closeCustomerPanel() {
    document.getElementById('customerPanelOverlay').classList.remove('show');
    document.getElementById('customerPanel').classList.remove('show');
    
    // Clear form data
    document.getElementById('customerSearch').value = '';
    document.getElementById('newCustomerName').value = '';
    document.getElementById('newCustomerPhone').value = '';
    document.getElementById('searchResults').innerHTML = '';
    document.getElementById('selectedCustomerInfo').style.display = 'none';
}

function searchCustomers() {
    const searchTerm = document.getElementById('customerSearch').value.trim();
    
    // Clear previous timeout
    if (searchTimeout) {
        clearTimeout(searchTimeout);
    }
    
    // Debounce search
    searchTimeout = setTimeout(async () => {
        if (searchTerm.length < 2) {
            document.getElementById('searchResults').innerHTML = '';
            return;
        }
        
        try {
            const response = await APIClient.get(`/syos/api/pos-customers/search?term=${encodeURIComponent(searchTerm)}`);
            displaySearchResults(response.customers || []);
        } catch (error) {
            console.error('Customer search error:', error);
            document.getElementById('searchResults').innerHTML = '<div class="no-results">Error searching customers</div>';
        }
    }, 300);
}

function displaySearchResults(customers) {
    const resultsContainer = document.getElementById('searchResults');
    
    if (customers.length === 0) {
        resultsContainer.innerHTML = '<div class="no-results">No customers found</div>';
        return;
    }
    
    let html = '';
    customers.forEach(customer => {
        html += `
            <div class="search-result-item" onclick="selectCustomer(${customer.id}, '${customer.name}', '${customer.contactNumber}')">
                <div class="result-name">${customer.name}</div>
                <div class="result-phone">${customer.contactNumber}</div>
            </div>
        `;
    });
    
    resultsContainer.innerHTML = html;
}

function selectCustomer(id, name, phone) {
    selectedCustomer = { id, name, phone };
    
    // Update selected customer info
    document.getElementById('selectedCustomerName').textContent = name;
    document.getElementById('selectedCustomerPhone').textContent = phone;
    document.getElementById('selectedCustomerInfo').style.display = 'block';
    
    // Clear search
    document.getElementById('customerSearch').value = '';
    document.getElementById('searchResults').innerHTML = '';
}

function confirmCustomerSelection() {
    if (!selectedCustomer) {
        showMessage('No customer selected', 'error');
        return;
    }
    
    // Update customer display in main panel
    updateCustomerDisplay(selectedCustomer);
    
    // Close panel
    closeCustomerPanel();
    
    showMessage(`Customer ${selectedCustomer.name} selected`, 'success');
}

function updateCustomerDisplay(customer) {
    const customerDisplay = document.getElementById('customerDisplay');
    
    if (customer) {
        customerDisplay.innerHTML = `
            <div class="customer-info">
                <div class="customer-name">${customer.name}</div>
                <div class="customer-phone">${customer.phone}</div>
            </div>
        `;
        
        // Show remove button
        document.getElementById('removeCustomerBtn').style.display = 'inline-block';
    } else {
        customerDisplay.innerHTML = '<div class="customer-placeholder">No customer selected</div>';
        document.getElementById('removeCustomerBtn').style.display = 'none';
    }
}

function removeCustomer() {
    selectedCustomer = null;
    updateCustomerDisplay(null);
    showMessage('Customer removed', 'info');
}