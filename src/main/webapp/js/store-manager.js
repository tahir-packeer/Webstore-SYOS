let currentStockItems = [];

function switchTab(tabName) {
    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(tab => tab.style.display = 'none');
    
    // Remove active class from all tab buttons
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => button.classList.remove('active'));
    
    // Show the selected tab content
    const selectedTab = document.getElementById(tabName + '-tab');
    if (selectedTab) {
        selectedTab.style.display = 'block';
    }
    
    // Add active class to the clicked tab button
    const clickedButton = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
    if (clickedButton) {
        clickedButton.classList.add('active');
    }
}

// Alert system
function showAlert(message, type = 'info', autoHide = true) {
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) return;
    
    alertContainer.innerHTML = `
        <div class="alert alert-${type}">
            <span>${message}</span>
        </div>
    `;
    
    alertContainer.style.display = 'block';
    
    // Auto-hide after 5 seconds
    if (autoHide) {
        setTimeout(() => {
            hideAlert();
        }, 5000);
    }
}

function hideAlert() {
    const alertContainer = document.getElementById('alert-container');
    if (alertContainer) {
        alertContainer.style.display = 'none';
        alertContainer.innerHTML = '';
    }
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
        container.innerHTML = '<p class="text-muted">No shelf items available</p>';
        return;
    }
    
    // Filter for website shelf items only
    const websiteItems = shelfItems.filter(item => 
        item.shelf_type && item.shelf_type.toUpperCase() === 'WEBSITE' && item.quantity > 0
    );
    
    if (websiteItems.length === 0) {
        container.innerHTML = '<p class="text-muted">No website items available to move</p>';
        return;
    }
    
    const html = websiteItems.map(item => `
        <div class="list-item">
            <div class="item-info">
                <strong>${item.name || 'Unknown Item'}</strong>
                <span class="item-code">${item.code || 'N/A'}</span>
                <span class="shelf-type">Shelf: ${item.shelf_type || 'N/A'}</span>
                <span class="item-quantity">Qty: ${item.quantity || 0}</span>
            </div>
            <div class="item-actions">
                <button class="btn btn-sm btn-success" onclick="moveToStore(${item.item_id})">
                    Move to Store
                </button>
            </div>
        </div>
    `).join('');
    
    container.innerHTML = html;
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
        container.innerHTML = '<p class="text-muted">No items available</p>';
        return;
    }
    
    const html = items.map(item => `
        <div class="list-item">
            <div class="item-info">
                <strong>${item.name || 'Unknown Item'}</strong>
                <span class="item-code">${item.code || 'N/A'}</span>
                <span class="item-price">LKR ${item.price || '0.00'}</span>
                <span class="item-quantity">Store: ${item.store_quantity || 0} | Website: ${item.website_quantity || 0}</span>
            </div>
            <div class="item-actions">
                <button class="btn btn-sm btn-primary" onclick="editItem(${item.id})">
                    Edit
                </button>
            </div>
        </div>
    `).join('');
    
    container.innerHTML = html;
}

// Edit item function
async function editItem(itemId) {
    try {
        const response = await fetch(`/syos/api/store-manager/getItem?id=${itemId}`);
        const data = await response.json();
        
        if (data.success && data.data) {
            const item = data.data;
            showEditItemModal(item);
        } else {
            showAlert('Failed to load item details', 'danger');
        }
    } catch (error) {
        showAlert('Error loading item details', 'danger');
    }
}

// Save item function for form submission
function saveItem(event) {
    event.preventDefault();
    
    const itemId = document.getElementById('item-id')?.value;
    const formData = {
        name: document.getElementById('item-name')?.value,
        price: parseFloat(document.getElementById('item-price')?.value),
        store_quantity: parseInt(document.getElementById('store-quantity')?.value) || 0,
        website_quantity: parseInt(document.getElementById('website-quantity')?.value) || 0
    };
    
    // Add item_id for updates (not for new items)
    if (itemId && itemId.trim() !== '') {
        formData.item_id = parseInt(itemId);
    }
    
    if (!formData.name || !formData.price) {
        showAlert('Please fill in required fields (Name and Price)', 'danger');
        return;
    }
    
    submitEditItem(formData);
}

// Submit item form data
async function submitEditItem(formData) {
    try {
        const isEdit = formData.item_id && formData.item_id > 0;
        showAlert(isEdit ? 'Updating item...' : 'Creating item...', 'info');
        
        const response = await fetch('/syos/api/store-manager/updateItem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert(`Item ${isEdit ? 'updated' : 'created'} successfully!`, 'success');
            closeModal('item-modal');
            loadItems(); // Refresh items list
        } else {
            showAlert(data.message || `Failed to ${isEdit ? 'update' : 'create'} item`, 'danger');
        }
    } catch (error) {
        showAlert('Error with item operation', 'danger');
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
        container.innerHTML = '<p class="text-muted">No shelf items available</p>';
        return;
    }
    
    const html = shelfItems.map(item => `
        <div class="list-item">
            <div class="item-info">
                <strong>${item.name || 'Unknown Item'}</strong>
                <span class="item-code">${item.code || 'N/A'}</span>
                <span class="shelf-type">Type: ${item.shelf_type || 'N/A'}</span>
                <span class="item-quantity">Qty: ${item.quantity || 0}</span>
            </div>
        </div>
    `).join('');
    
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

// Modal function for online move
function showOnlineMoveModal(item) {
    document.querySelector('#online-move-modal h3').textContent = `Move Item to Store: ${item.name || 'Unknown Item'}`;
    
    if (document.getElementById('online-item-id')) {
        document.getElementById('online-item-id').value = item.id || '';
    }
    if (document.getElementById('online-item-name')) {
        document.getElementById('online-item-name').textContent = item.name || '';
    }
    if (document.getElementById('online-quantity')) {
        document.getElementById('online-quantity').textContent = item.website_quantity || 0;
    }
    
    const modal = document.getElementById('online-move-modal');
    if (modal) {
        modal.style.display = 'block';
        modal.classList.add('active');
    }
}

// Modal close functions
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('active');
        
        // Clean up event listeners to prevent memory leaks
        if (modalId === 'discount-modal') {
            const form = document.getElementById('discount-form');
            if (form) {
                const inputs = form.querySelectorAll('input[type="text"], input[type="number"]');
                inputs.forEach(input => {
                    input.removeEventListener('keydown', handleDiscountKeyDown);
                });
            }
        }
    }
}

// Additional modal functions for item management and discount codes
function showEditItemModal(item = null) {
    const title = item ? `Edit Item: ${item.name}` : 'Add New Item';
    const modal = document.getElementById('item-modal');
    
    if (modal) {
        const titleElement = modal.querySelector('h3');
        if (titleElement) {
            titleElement.textContent = title;
        }
        
        // Populate form fields if item exists
        if (item) {
            const fields = {
                'item-id': item.id || '',
                'item-name': item.name || '',
                'item-price': item.price || '',
                'store-quantity': item.store_quantity || '',
                'website-quantity': item.website_quantity || ''
            };
            
            Object.keys(fields).forEach(fieldId => {
                const element = document.getElementById(fieldId);
                if (element) {
                    element.value = fields[fieldId];
                }
            });
        } else {
            // Clear form for new item
            const fieldIds = ['item-id', 'item-name', 'item-price', 'reorder-level', 'store-quantity', 'website-quantity'];
            fieldIds.forEach(fieldId => {
                const element = document.getElementById(fieldId);
                if (element) {
                    element.value = '';
                }
            });
        }
        
        modal.style.display = 'block';
        modal.classList.add('active');
    }
}







// Utility function for reshelving
function reshelveItem(productId) {
    // Find the item in global storage
    const item = currentStockItems.find(stock => stock.id === productId);
    if (item) {
        showStockModal(item);
    } else {
    }
}

// Modal function for stock
function showStockModal(item) {
    document.querySelector('#stock-modal h3').textContent = `Move Stock: ${item.name || 'Unknown Item'}`;
    
    if (document.getElementById('stock-id')) {
        document.getElementById('stock-id').value = item.id || '';
    }
    if (document.getElementById('stock-item-name')) {
        document.getElementById('stock-item-name').textContent = item.name || '';
    }
    if (document.getElementById('available-quantity')) {
        document.getElementById('available-quantity').textContent = item.quantity || 0;
    }
    
    const modal = document.getElementById('stock-modal');
    if (modal) {
        modal.style.display = 'block';
        modal.classList.add('active');
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

// Move online to store function
async function moveOnlineToStore(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const requestData = {
        item_id: parseInt(formData.get('item_id')),
        quantity: parseInt(formData.get('quantity'))
    };
    
    try {
        showAlert('Moving items from online to store...', 'info');
        
        const response = await fetch('/syos/api/store-manager/moveOnlineToStore', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showAlert('Items moved from online to store successfully!', 'success');
            closeModal('online-move-modal');
            loadShelfItems(); // Refresh shelf items list
        } else {
            showAlert(data.message || 'Failed to move items from online to store', 'danger');
        }
    } catch (error) {
        showAlert('Error moving items from online to store', 'danger');
    }
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
        container.innerHTML = '<p class="text-muted">No discount codes found. <a href="#" onclick="showDiscountModal()">Create your first discount code</a></p>';
        return;
    }
    
    container.innerHTML = '';
    
    codes.forEach(code => {
        const codeElement = document.createElement('div');
        codeElement.className = 'item-card p-3 mb-3';
        codeElement.style.cssText = `
            border: 1px solid var(--border-color);
            border-radius: var(--border-radius);
            background: white;
            box-shadow: var(--shadow);
        `;
        
        codeElement.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
                <div class="item-info">
                    <div><strong>${code.code}</strong></div>
                    <div class="discount-value">Discount: Rs. ${code.discount_value.toFixed(2)}</div>
                    <div class="created-date">Created: ${new Date(code.created_date).toLocaleDateString()}</div>
                </div>
                <div class="item-actions">
                    <button class="btn btn-sm btn-primary" onclick="editDiscountCode(${code.id}, '${code.code}', ${code.discount_value})">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteDiscountCode(${code.id})">Delete</button>
                </div>
            </div>
        `;
        
        container.appendChild(codeElement);
    });
}

function showDiscountModal(id = null, code = '', discount_value = '') {
    const modal = document.getElementById('discount-modal');
    const form = document.getElementById('discount-form');
    const title = document.getElementById('discount-modal-title');
    
    if (!modal || !form || !title) {
        return;
    }
    
    // Reset form
    form.reset();
    document.getElementById('discount-id').value = '';
    
    if (id) {
        title.textContent = 'Edit Discount Code';
        // Load existing data for editing
        document.getElementById('discount-id').value = id;
        document.getElementById('discount-code').value = code;
        document.getElementById('discount-value').value = discount_value;
    } else {
        title.textContent = 'Add Discount Code';
    }
    
    // Show modal with proper CSS classes
    modal.classList.add('active');
    
    // Add keyboard event handlers to prevent Enter key issues
    const inputs = form.querySelectorAll('input[type="text"], input[type="number"]');
    inputs.forEach(input => {
        input.removeEventListener('keydown', handleDiscountKeyDown); // Remove existing listeners
        input.addEventListener('keydown', handleDiscountKeyDown);
    });
}

// Separate function to handle keyboard events in discount modal
function handleDiscountKeyDown(e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        e.stopPropagation();
        handleSaveDiscountCode();
        return false;
    }
}

function editDiscountCode(id, code, discount_value) {
    showDiscountModal(id, code, discount_value);
}

function handleSaveDiscountCode() {
    // This function is overridden by discount-manager.js
    return false;
}

// Simplified save function
async function saveDiscountCodeDirect(discountData, discountId) {
    try {
        let url, method;
        
        if (discountId) {
            url = `/syos/api/discount-codes/${discountId}`;
            method = 'PUT';
        } else {
            url = '/syos/api/discount-codes/';
            method = 'POST';
        }
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(discountData)
        });
        
        const result = await response.json();
        
        if (result.success) {
            const action = discountId ? 'updated' : 'created';
            showAlert(`Discount code ${action} successfully!`, 'success');
            closeModal('discount-modal');
            loadDiscountCodes();
        } else {
            showAlert(result.error || `Failed to ${discountId ? 'update' : 'create'} discount code`, 'error');
        }
    } catch (error) {
        showAlert('Error saving discount code. Please try again.', 'error');
    }
}

async function saveDiscountCode(event) {
    event.preventDefault();
    event.stopPropagation();
    
    const form = event.target;
    const formData = new FormData(form);
    const discountId = formData.get('id');
    
    const discountData = {
        code: formData.get('code').trim().toUpperCase(),
        discount_value: parseFloat(formData.get('discount_value'))
    };
    
    if (!discountData.code) {
        showAlert('Please enter a discount code', 'warning');
        return false;
    }
    
    if (discountData.discount_value <= 0) {
        showAlert('Discount value must be greater than 0', 'warning');
        return false;
    }
    
    try {
        let response;
        if (discountId) {
            response = await fetch(`/syos/api/discount-codes/${discountId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(discountData)
            });
        } else {
            response = await fetch('/syos/api/discount-codes/', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(discountData)
            });
        }
        
        const result = await response.json();
        
        if (result.success) {
            const action = discountId ? 'updated' : 'created';
            showAlert(`Discount code ${action} successfully!`, 'success');
            closeModal('discount-modal');
            loadDiscountCodes();
        } else {
            showAlert(result.error || `Failed to ${discountId ? 'update' : 'create'} discount code`, 'error');
        }
    } catch (error) {
        showAlert('Error saving discount code. Please try again.', 'error');
    }
    
    return false;
}

async function deleteDiscountCode(id, code) {
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
    
    // For dynamically populated containers, search within all content
    const items = container.querySelectorAll('.item-row, .item-card, div[style*="border"], div[class*="item"]');
    const filter = searchValue.toLowerCase();
    
    // If no specific items found, search all div children
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