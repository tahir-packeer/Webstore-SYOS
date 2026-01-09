let currentStockItems = [];

// Enhanced alert system for the command center
function showAlert(message, type = 'success', autoHide = true) {
    const alertSystem = document.getElementById('alert-system');
    const alertMessage = document.getElementById('alert-message');
    
    if (!alertSystem || !alertMessage) return;
    
    alertSystem.className = `alert-system alert-${type} show`;
    alertMessage.textContent = message;
    
    if (autoHide) {
        setTimeout(() => {
            alertSystem.classList.remove('show');
        }, 5000);
    }
}

function hideAlert() {
    const alertSystem = document.getElementById('alert-system');
    if (alertSystem) {
        alertSystem.classList.remove('show');
    }
}

// Panel functions for new slide panel system
function showStockPanel(itemId, itemName) {
    document.getElementById('stock-item-id').value = itemId;
    document.getElementById('stock-item-name-display').textContent = itemName;
    document.getElementById('stock-panel').classList.add('active');
}

function showOnlineMovePanel(itemId, itemName, quantity) {
    document.getElementById('online-item-id').value = itemId;
    document.getElementById('online-item-name').textContent = itemName;
    document.getElementById('online-quantity').textContent = quantity;
    document.getElementById('online-move-panel').classList.add('active');
}

function showEditItemPanel(item) {
    console.log('showEditItemPanel called with item:', item); // Debug log
    
    if (item) {
        document.getElementById('item-id').value = item.id || '';
        document.getElementById('item-name').value = item.name || '';
        document.getElementById('item-price').value = item.price || '';
        document.getElementById('store-quantity').value = item.store_quantity || 0;
        document.getElementById('website-quantity').value = item.website_quantity || 0;
        
        console.log('Form populated with item ID:', item.id); // Debug log
    } else {
        console.log('WARNING: showEditItemPanel called with no item data!'); // Debug log
        // Clear form for new item
        document.getElementById('item-form').reset();
        document.getElementById('item-id').value = '';
    }
    document.getElementById('item-panel').classList.add('active');
}

function showDiscountPanel(discount = null) {
    if (discount) {
        document.getElementById('discount-id').value = discount.id || '';
        document.getElementById('discount-code').value = discount.code || '';
        document.getElementById('discount-value').value = discount.discount_value || '';
    } else {
        document.getElementById('discount-form').reset();
        document.getElementById('discount-id').value = '';
    }
    document.getElementById('discount-panel').classList.add('active');
}

function closePanel(panelId) {
    document.getElementById(panelId).classList.remove('active');
}

// Form submission functions
async function submitStock(event) {
    event.preventDefault();
    
    const itemId = document.getElementById('stock-item-id').value;
    const quantity = parseInt(document.getElementById('stock-quantity').value);
    const shelfType = document.getElementById('stock-shelf-type').value;
    
    if (!itemId || !quantity || !shelfType) {
        showAlert('Please fill in all fields', 'error');
        return;
    }
    
    try {
        const response = await fetch('/syos/api/store-manager/reshelveItems', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ stock_id: parseInt(itemId), quantity: quantity, shelf_type: shelfType })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert('Stock added successfully', 'success');
            closePanel('stock-panel');
            document.getElementById('stock-form').reset();
            loadItems(); // Refresh the items list
        } else {
            showAlert(data.message || 'Failed to add stock', 'error');
        }
    } catch (error) {
        showAlert('Error adding stock', 'error');
    }
}

async function submitMove(event) {
    event.preventDefault();
    
    const itemId = document.getElementById('online-item-id').value;
    const quantity = parseInt(document.getElementById('online-move-quantity').value);
    const currentQuantity = parseInt(document.getElementById('online-quantity').textContent);
    
    if (!itemId || !quantity) {
        showAlert('Please enter a quantity to transfer', 'error');
        return;
    }
    
    if (quantity > currentQuantity) {
        showAlert('Cannot transfer more than available quantity', 'error');
        return;
    }
    
    try {
        const response = await fetch('/syos/api/store-manager/moveOnlineToStore', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ item_id: parseInt(itemId), quantity: quantity })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert('Item transferred successfully', 'success');
            closePanel('online-move-panel');
            document.getElementById('online-move-form').reset();
            loadShelfStatus(); // Refresh the shelf status
            loadShelfItems(); // Refresh the shelf items list  
        } else {
            showAlert(data.message || 'Failed to transfer item', 'error');
        }
    } catch (error) {
        showAlert('Error transferring item', 'error');
    }
}

async function submitEditItem(event) {
    event.preventDefault();
    
    const itemId = document.getElementById('item-id').value;
    console.log('Edit Item - Item ID from form:', itemId); // Debug log
    
    const formData = {
        name: document.getElementById('item-name').value,
        price: parseFloat(document.getElementById('item-price').value),
        store_quantity: parseInt(document.getElementById('store-quantity').value) || 0,
        website_quantity: parseInt(document.getElementById('website-quantity').value) || 0
    };
    
    // CRITICAL: Always include item_id for updates when editing existing items
    if (itemId && itemId !== '' && itemId !== '0') {
        formData.item_id = parseInt(itemId);
        console.log('Including item_id in request:', formData.item_id); // Debug log
    } else {
        console.log('WARNING: No valid item ID found - this will create a new item!'); // Debug log
        showAlert('Error: Cannot update item - missing item ID', 'error');
        return;
    }
    
    if (!formData.name || !formData.price) {
        showAlert('Please fill in all required fields', 'error');
        return;
    }
    
    try {
        const url = '/syos/api/store-manager/updateItem';
        const method = 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert(itemId ? 'Item updated successfully' : 'Item added successfully', 'success');
            closePanel('item-panel');
            document.getElementById('item-form').reset();
            loadItems(); // Refresh the items list
        } else {
            showAlert(data.message || 'Failed to save item', 'error');
        }
    } catch (error) {
        showAlert('Error saving item', 'error');
    }
}

async function submitDiscount(event) {
    event.preventDefault();
    
    const discountId = document.getElementById('discount-id').value;
    const formData = {
        code: document.getElementById('discount-code').value,
        discount_value: parseFloat(document.getElementById('discount-value').value)
    };
    
    if (!formData.code || !formData.discount_value) {
        showAlert('Please fill in all fields', 'error');
        return;
    }
    
    try {
        const url = discountId ? `/syos/api/discount-codes/${discountId}` : '/syos/api/discount-codes/';
        const method = discountId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert(discountId ? 'Discount updated successfully' : 'Discount created successfully', 'success');
            closePanel('discount-panel');
            document.getElementById('discount-form').reset();
            loadDiscountCodes(); // Refresh the discount codes list
        } else {
            showAlert(data.message || 'Failed to save discount', 'error');
        }
    } catch (error) {
        showAlert('Error saving discount', 'error');
    }
}

// Legacy support for old modal system - now redirects to panels
function showStockModal(stockId, itemName, quantity) {
    showStockPanel(stockId, itemName, quantity);
}

function showOnlineMoveModal(itemId, itemName, quantity) {
    showOnlineMovePanel(itemId, itemName, quantity);
}

function showEditItemModal(itemData = null) {
    showEditItemPanel(itemData);
}

function showDiscountModal(discountData = null) {
    showDiscountPanel(discountData);
}

function closeModal(modalId) {
    // Legacy function - redirect to panel close
    const panelMap = {
        'stock-modal': 'stock-panel',
        'online-move-modal': 'online-move-panel',
        'item-modal': 'edit-item-panel',
        'discount-modal': 'discount-panel'
    };
    const panelId = panelMap[modalId] || modalId;
    closePanel(panelId);
}

// Load stock data
async function loadStock() {
    try {
        const response = await fetch('/syos/api/store-manager/listStock');
        if (response.ok) {
            const result = await response.json();
            // Extract data from response
            const stockData = result.data || result;
            currentStockItems = stockData; // Store globally
            displayStock(result); // Pass full result to handle data extraction
        } else {
            // Failed to load stock data
        }
    } catch (error) {
        // Error loading stock data
    }
}

// Display stock in table
function displayStock(stockData) {
    const tbody = document.querySelector('#stock-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    // Check if data is nested in a data property
    const items = stockData.data || stockData;
    
    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.id}</td>
            <td>${item.name}</td>
            <td>${item.quantity}</td>
            <td>${item.location || 'Stock Room'}</td>
            <td>
                <button class="btn btn-secondary" onclick="reshelveItem(${item.id})">Move to Shelf</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Load shelf items for online to store movement
async function loadShelfItems() {
    try {
        const response = await fetch('/syos/api/store-manager/listShelf');
        const data = await response.json();
        
        if (data.success) {
            displayOnlineItems(data.data || []);
        } else {
            showAlert(data.message || 'Failed to load shelf items', 'danger');
        }
    } catch (error) {
        showAlert('Error loading shelf items', 'danger');
    }
}

// Display online items for store movement
function displayOnlineItems(shelfItems) {
    const container = document.getElementById('online-items-list');
    if (!container) return;
    
    if (!shelfItems || shelfItems.length === 0) {
        container.innerHTML = '<div class="loading">No shelf items available</div>';
        return;
    }
    
    // Filter for website shelf items only
    const websiteItems = shelfItems.filter(item => 
        item.shelf_type && item.shelf_type.toUpperCase() === 'WEBSITE' && item.quantity > 0
    );
    
    if (websiteItems.length === 0) {
        container.innerHTML = '<div class="loading">No website items available to move</div>';
        return;
    }
    
    const html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>ITEM CODE</th>
                    <th>ITEM NAME</th>
                    <th>QUANTITY</th>
                    <th>SHELF TYPE</th>
                    <th>ACTIONS</th>
                </tr>
            </thead>
            <tbody>
                ${websiteItems.map(item => `
                    <tr>
                        <td>${item.code || 'N/A'}</td>
                        <td>${item.name || 'Unknown Item'}</td>
                        <td>${item.quantity || 0}</td>
                        <td>${item.shelf_type || 'N/A'}</td>
                        <td>
                            <button class="action-btn transfer-btn" 
                                    data-item-id="${item.item_id}" 
                                    data-item-name="${item.name || 'Unknown Item'}" 
                                    data-quantity="${item.quantity || 0}">
                                TRANSFER
                            </button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
    
    // Add event listeners for transfer buttons
    container.querySelectorAll('.transfer-btn').forEach(button => {
        button.addEventListener('click', function() {
            const itemId = this.dataset.itemId;
            const itemName = this.dataset.itemName;
            const quantity = this.dataset.quantity;
            showOnlineMovePanel(itemId, itemName, quantity);
        });
    });
}

// Load all items for management
async function loadItems() {
    try {
        const response = await fetch('/syos/api/store-manager/listItems');
        const data = await response.json();
        
        if (data.success) {
            displayItems(data.data || []);
        } else {
            showAlert(data.message || 'Failed to load items', 'danger');
        }
    } catch (error) {
        showAlert('Error loading items', 'danger');
    }
}

// Display all items for management
function displayItems(items) {
    const container = document.getElementById('items-list');
    if (!container) return;
    
    if (!items || items.length === 0) {
        container.innerHTML = '<div class="loading">NO ITEMS AVAILABLE</div>';
        return;
    }
    
    const html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>ITEM CODE</th>
                    <th>ITEM NAME</th>
                    <th>PRICE</th>
                    <th>STORE QTY</th>
                    <th>WEBSITE QTY</th>
                    <th>ACTIONS</th>
                </tr>
            </thead>
            <tbody>
                ${items.map(item => `
                    <tr>
                        <td>${item.code || 'N/A'}</td>
                        <td>${item.name || 'Unknown Item'}</td>
                        <td>LKR ${item.price || '0.00'}</td>
                        <td>${item.store_quantity || 0}</td>
                        <td>${item.website_quantity || 0}</td>
                        <td>
                            <button class="action-btn edit-btn" data-item-id="${item.id}">
                                EDIT
                            </button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
    
    // Add event listeners for edit buttons
    container.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', function() {
            const itemId = this.dataset.itemId;
            editItem(itemId);
        });
    });
}

// Edit item function
async function editItem(itemId) {
    console.log('editItem called with itemId:', itemId); // Debug log
    
    try {
        const response = await fetch(`/syos/api/store-manager/getItem?id=${itemId}`);
        const data = await response.json();
        
        console.log('API response for getItem:', data); // Debug log
        
        if (data.success && data.data) {
            const item = data.data;
            console.log('Item data received:', item); // Debug log
            showEditItemPanel(item);
        } else {
            console.error('Failed to load item:', data); // Debug log
            showAlert('Failed to load item details', 'danger');
        }
    } catch (error) {
        console.error('Error loading item details:', error); // Debug log
        showAlert('Error loading item details', 'danger');
    }
}

// Save item function for form submission
async function saveItem(event) {
    event.preventDefault();
    
    const itemId = document.getElementById('item-id')?.value;
    const formData = {
        name: document.getElementById('item-name')?.value,
        price: parseFloat(document.getElementById('item-price')?.value),
        store_quantity: parseInt(document.getElementById('store-quantity')?.value) || 0,
        website_quantity: parseInt(document.getElementById('website-quantity')?.value) || 0
    };
    
    // Include item_id in the request body for updates
    if (itemId) {
        formData.item_id = parseInt(itemId);
    }
    
    if (!formData.name || !formData.price) {
        showAlert('Please fill in all required fields', 'error');
        return;
    }
    
    try {
        const url = '/syos/api/store-manager/updateItem';
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert(itemId ? 'Item updated successfully' : 'Item added successfully', 'success');
            closePanel('item-panel');
            document.getElementById('item-form').reset();
            loadItems(); // Refresh the items list
        } else {
            showAlert(data.message || 'Failed to save item', 'error');
        }
    } catch (error) {
        showAlert('Error saving item', 'error');
    }
}

// Load shelf status for overview
async function loadShelfStatus() {
    try {
        const response = await fetch('/syos/api/store-manager/listShelf');
        const data = await response.json();
        
        if (data.success) {
            displayShelfStatus(data.data || []);
        } else {
            showAlert(data.message || 'Failed to load shelf status', 'danger');
        }
    } catch (error) {
        showAlert('Error loading shelf status', 'danger');
    }
}

// Display shelf status overview
function displayShelfStatus(shelfItems) {
    const container = document.getElementById('shelf-status');
    if (!container) return;
    
    if (!shelfItems || shelfItems.length === 0) {
        container.innerHTML = '<div class="loading">NO SHELF ITEMS AVAILABLE</div>';
        return;
    }
    
    const html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>ITEM CODE</th>
                    <th>ITEM NAME</th>
                    <th>SHELF TYPE</th>
                    <th>QUANTITY</th>
                </tr>
            </thead>
            <tbody>
                ${shelfItems.map(item => `
                    <tr>
                        <td>${item.code || 'N/A'}</td>
                        <td>${item.name || 'Unknown Item'}</td>
                        <td>${item.shelf_type || 'N/A'}</td>
                        <td>${item.quantity || 0}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
}

// Move item to store function
async function moveToStore(itemId) {
    try {
        const response = await fetch(`/syos/api/store-manager/getItem?id=${itemId}`);
        const data = await response.json();
        
        if (data.success && data.data) {
            const item = data.data;
            showOnlineMoveModal(item);
        } else {
            showAlert('Failed to load item details', 'danger');
        }
    } catch (error) {
        showAlert('Error loading item details', 'danger');
    }
}









// Utility function for reshelving
function reshelveItem(productId) {
    // Find the item in global storage
    const item = currentStockItems.find(stock => stock.id === productId);
    if (item) {
        showStockPanel(item.id, item.name);
    } else {
        showAlert('Item not found', 'error');
    }
}

// Move stock to shelf function
async function moveStockToShelf(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const requestData = {
        stock_id: parseInt(formData.get('stock_id')),
        quantity: parseInt(formData.get('quantity')),
        shelf_type: formData.get('shelf_type')
    };
    
    try {
        showAlert('Moving stock to shelf...', 'info');
        
        const response = await fetch('/syos/api/store-manager/reshelveItems', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert('Stock moved to shelf successfully!', 'success');
            closeModal('stock-modal');
            loadStock(); // Refresh stock list
            loadShelfStatus(); // Refresh shelf status
        } else {
            showAlert(data.message || 'Failed to move stock', 'danger');
        }
    } catch (error) {
        showAlert('Error moving stock to shelf', 'danger');
    }
}

// Move online to store function - for form submission compatibility
async function moveOnlineToStore(event) {
    // This redirects to our new submitMove function
    return await submitMove(event);
}



// Discount Code Management Functions
async function loadDiscountCodes() {
    try {
        const response = await fetch('/syos/api/discount-codes/');
        const data = await response.json();
        
        if (data.success) {
            displayDiscountCodes(data.data);
        } else {
            showAlert('Failed to load discount codes', 'error');
            document.getElementById('discount-codes-list').innerHTML = '<p class="text-danger">Failed to load discount codes</p>';
        }
    } catch (error) {
        showAlert('Error loading discount codes', 'error');
        document.getElementById('discount-codes-list').innerHTML = '<p class="text-danger">Error loading discount codes</p>';
    }
}

function displayDiscountCodes(codes) {
    const container = document.getElementById('discount-codes-list');
    
    if (!codes || codes.length === 0) {
        container.innerHTML = '<div class="loading">NO DISCOUNT CODES FOUND</div>';
        return;
    }
    
    const html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>CODE</th>
                    <th>DISCOUNT VALUE</th>
                    <th>CREATED DATE</th>
                    <th>ACTIONS</th>
                </tr>
            </thead>
            <tbody>
                ${codes.map(code => `
                    <tr>
                        <td><strong>${code.code}</strong></td>
                        <td>Rs. ${code.discount_value.toFixed(2)}</td>
                        <td>${new Date(code.created_date).toLocaleDateString()}</td>
                        <td>
                            <button class="action-btn" onclick="editDiscountCode(${code.id}, '${code.code}', ${code.discount_value})">
                                EDIT
                            </button>
                            <button class="action-btn secondary" onclick="deleteDiscountCode(${code.id})">
                                DELETE
                            </button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    container.innerHTML = html;
}

// Enhanced discount modal function that redirects to panel
function showDiscountModal(id = null, code = '', discount_value = '') {
    if (id) {
        showDiscountPanel({id: id, code: code, discount_value: discount_value});
    } else {
        showDiscountPanel();
    }
}

function editDiscountCode(id, code, discount_value) {
    showDiscountPanel({id: id, code: code, discount_value: discount_value});
}

function handleSaveDiscountCode() {
    // This function redirects to the panel submit function
    const form = document.getElementById('discount-form');
    if (form) {
        const event = new Event('submit');
        submitDiscount(event);
    }
    return false;
}

async function deleteDiscountCode(id) {
    // Find the code name for confirmation
    const container = document.getElementById('discount-codes-list');
    const row = container.querySelector(`button[onclick*="${id}"]`)?.closest('tr');
    const code = row ? row.querySelector('td strong')?.textContent || 'this discount code' : 'this discount code';
    
    if (!confirm(`Are you sure you want to delete the discount code "${code}"?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/syos/api/discount-codes/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (result.success) {
            showAlert('Discount code deleted successfully!', 'success');
            loadDiscountCodes(); // Refresh the list
        } else {
            showAlert(result.error || 'Failed to delete discount code', 'error');
        }
    } catch (error) {
        showAlert('Error deleting discount code. Please try again.', 'error');
    }
}

// Tab switching function for the dashboard
function switchTab(tabName) {
    // Hide all tab contents
    const contents = document.querySelectorAll('.tab-content');
    contents.forEach(content => {
        content.style.display = 'none';
    });
    
    // Remove active class from all tabs
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Show selected tab content
    const selectedContent = document.getElementById(tabName);
    if (selectedContent) {
        selectedContent.style.display = 'block';
    }
    
    // Add active class to selected tab
    const selectedTab = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
    if (selectedTab) {
        selectedTab.classList.add('active');
    }
    
    // Load data for the selected tab
    switch(tabName) {
        case 'inventory':
            loadShelfItems();
            loadShelfStatus();
            break;
        case 'items':
            loadItems();
            break;
        case 'discounts':
            loadDiscountCodes();
            break;
    }
}

// Initialize dashboard when page loads
// Logout function
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        SessionManager.clearSession(); // Use clearSession instead of logout to avoid redirect conflict
        showAlert('Logged out successfully', 'info');
        setTimeout(() => {
            window.location.href = '/syos/index.html';
        }, 1000);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    switchTab('inventory');
    loadStock();
    loadShelfItems();
    loadShelfStatus();
    loadItems();
    
    setTimeout(() => {
        loadDiscountCodes();
    }, 100);
    
    hideAlert();
});

// Universal table search function for dynamically populated content
function searchTable(containerId, searchValue) {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    const filter = searchValue.toLowerCase();
    
    // Look for table rows in the container
    const table = container.querySelector('table');
    if (table) {
        // Search table rows (skip header row)
        const rows = table.querySelectorAll('tbody tr');
        
        rows.forEach(row => {
            const text = row.textContent || row.innerText || '';
            
            if (text.toLowerCase().indexOf(filter) > -1 || filter === '') {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    } else {
        // Fallback for non-table content
        const items = container.querySelectorAll('.item-row, .item-card, div[style*="border"], div[class*="item"]');
        const searchElements = items.length > 0 ? items : container.children;
        
        for (let i = 0; i < searchElements.length; i++) {
            const element = searchElements[i];
            const text = element.textContent || element.innerText || '';
            
            // Skip loading messages or empty content
            if (text.toLowerCase().includes('loading') || text.trim() === '') continue;
            
            if (text.toLowerCase().indexOf(filter) > -1 || filter === '') {
                element.style.display = '';
            } else {
                element.style.display = 'none';
            }
        }
    }
}