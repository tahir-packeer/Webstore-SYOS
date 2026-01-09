// API Configuration and Utilities
const API_BASE_URL = 'http://localhost:8080'; 

// API Endpoints - Absolute paths including context path
const API_ENDPOINTS = {
    // Customer Authentication and Management
    CUSTOMER_LOGIN: '/syos/api/customers',      // POST with email & password
    CUSTOMER_REGISTER: '/syos/api/customers',   // POST with registration data
    CUSTOMER_PROFILE: '/syos/api/customers',    // GET with ID parameter
    
    // Staff Authentication
    STAFF_LOGIN: '/syos/api/staff/login',
    
    // Sales & Billing
    SALES_OTC: '/syos/api/sales', // Over-the-counter sales (SalesServlet)
    SALES_ONLINE: '/syos/api/online-sales', // Online sales (OnlineSalesServlet)
    SALES_HISTORY: '/syos/api/sales',
    
    // Stock Management (StockServlet)
    STOCK: '/syos/api/stock',
    STOCK_MOVE: '/syos/api/stock',
    
    // Item Management (via StockServlet - items come from stock data)
    ITEMS: '/syos/api/stock',
    ITEMS_SEARCH: '/syos/api/stock',
    ITEMS_API: '/syos/api/items',           // ItemsController endpoints
    ITEMS_SEARCH_API: '/syos/api/items/search',  // Search endpoint
    
    // Reports (ReportServlet)
    REPORTS: '/syos/api/reports',
    REPORTS_SALES: '/syos/api/reports?type=sales',
    REPORTS_STOCK: '/syos/api/reports?type=stock',
    REPORTS_REORDER: '/syos/api/reports?type=reorder',
    REPORTS_RESHELVING: '/syos/api/reports?type=reshelving'
};

// Session Management
class SessionManager {
    static setUser(userData) {
        localStorage.setItem('currentUser', JSON.stringify(userData));
        localStorage.setItem('loginTime', new Date().toISOString());
    }
    
    static getUser() {
        const userData = localStorage.getItem('currentUser');
        return userData ? JSON.parse(userData) : null;
    }
    
    static clearSession() {
        localStorage.removeItem('currentUser');
        localStorage.removeItem('loginTime');
        localStorage.removeItem('cart');
    }
    
    static logout() {
        this.clearSession();
        // Redirect to login page
        window.location.href = '/syos/index.html';
    }
    
    static isLoggedIn() {
        return this.getUser() !== null;
    }
    
    static getUserRole() {
        const user = this.getUser();
        return user ? user.role : null;
    }
    
    static checkSession() {
        const loginTime = localStorage.getItem('loginTime');
        if (!loginTime) return false;
        
        const now = new Date();
        const login = new Date(loginTime);
        const hoursDiff = (now - login) / (1000 * 60 * 60);
        
        // Session expires after 8 hours
        if (hoursDiff > 8) {
            this.clearSession();
            return false;
        }
        
        return true;
    }
}

// API Client
class APIClient {
    static async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };
        
        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorData}`);
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return await response.text();
        } catch (error) {
            throw error;
        }
    }
    
    static async get(endpoint, params = {}) {
        const url = new URL(`${API_BASE_URL}${endpoint}`);
        Object.keys(params).forEach(key => {
            if (params[key] !== undefined && params[key] !== null) {
                url.searchParams.append(key, params[key]);
            }
        });
        
        return this.request(url.pathname + url.search);
    }
    
    static async post(endpoint, data = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    static async put(endpoint, data = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    static async delete(endpoint) {
        return this.request(endpoint, {
            method: 'DELETE'
        });
    }
}

// UI Utilities
class UIUtils {
    static showAlert(message, type = 'info', duration = 5000) {
        const alertContainer = document.getElementById('alert-container') || this.createAlertContainer();
        
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.innerHTML = `
            <span>${message}</span>
            <button type="button" class="alert-close" onclick="this.parentElement.remove()">×</button>
        `;
        
        alertContainer.appendChild(alert);
        
        if (duration > 0) {
            setTimeout(() => {
                if (alert.parentElement) {
                    alert.remove();
                }
            }, duration);
        }
        
        return alert;
    }
    
    static createAlertContainer() {
        const container = document.createElement('div');
        container.id = 'alert-container';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 1050;
            max-width: 400px;
        `;
        document.body.appendChild(container);
        return container;
    }
    
    static showLoading(element) {
        const originalContent = element.innerHTML;
        element.innerHTML = '<span class="loading"></span> Loading...';
        element.disabled = true;
        
        return () => {
            element.innerHTML = originalContent;
            element.disabled = false;
        };
    }
    
    static formatCurrency(amount) {
        return `LKR ${parseFloat(amount || 0).toFixed(2)}`;
    }
    
    static formatDate(date) {
        return new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(new Date(date));
    }
    
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
    
    static throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
}

// Form Validation
class FormValidator {
    static validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }
    
    static validatePhone(phone) {
        const re = /^[\+]?[1-9][\d]{0,15}$/;
        return re.test(phone.replace(/\s/g, ''));
    }
    
    static validateRequired(value) {
        return value && value.toString().trim().length > 0;
    }
    
    static validateMinLength(value, minLength) {
        return value && value.toString().length >= minLength;
    }
    
    static validateMaxLength(value, maxLength) {
        return !value || value.toString().length <= maxLength;
    }
    
    static validateNumber(value) {
        return !isNaN(value) && isFinite(value);
    }
    
    static validatePositiveNumber(value) {
        return this.validateNumber(value) && parseFloat(value) > 0;
    }
    
    static showFieldError(field, message) {
        field.classList.add('error');
        
        // Remove existing error message
        const existingError = field.parentElement.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }
        
        // Add new error message
        if (message) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'error-message';
            errorDiv.textContent = message;
            field.parentElement.appendChild(errorDiv);
        }
    }
    
    static clearFieldError(field) {
        field.classList.remove('error');
        const errorMessage = field.parentElement.querySelector('.error-message');
        if (errorMessage) {
            errorMessage.remove();
        }
    }
    
    static validateForm(formElement, rules) {
        let isValid = true;
        
        Object.keys(rules).forEach(fieldName => {
            const field = formElement.querySelector(`[name="${fieldName}"]`);
            if (!field) return;
            
            const fieldRules = rules[fieldName];
            const value = field.value;
            
            this.clearFieldError(field);
            
            for (const rule of fieldRules) {
                let valid = true;
                let message = '';
                
                switch (rule.type) {
                    case 'required':
                        valid = this.validateRequired(value);
                        message = rule.message || 'This field is required';
                        break;
                    case 'email':
                        valid = !value || this.validateEmail(value);
                        message = rule.message || 'Please enter a valid email';
                        break;
                    case 'phone':
                        valid = !value || this.validatePhone(value);
                        message = rule.message || 'Please enter a valid phone number';
                        break;
                    case 'minLength':
                        valid = this.validateMinLength(value, rule.value);
                        message = rule.message || `Minimum ${rule.value} characters required`;
                        break;
                    case 'maxLength':
                        valid = this.validateMaxLength(value, rule.value);
                        message = rule.message || `Maximum ${rule.value} characters allowed`;
                        break;
                    case 'number':
                        valid = !value || this.validateNumber(value);
                        message = rule.message || 'Please enter a valid number';
                        break;
                    case 'positiveNumber':
                        valid = !value || this.validatePositiveNumber(value);
                        message = rule.message || 'Please enter a positive number';
                        break;
                }
                
                if (!valid) {
                    this.showFieldError(field, message);
                    isValid = false;
                    break;
                }
            }
        });
        
        return isValid;
    }
}

// Cart Management (for customers)
class CartManager {
    static getCart() {
        const cart = localStorage.getItem('cart');
        return cart ? JSON.parse(cart) : [];
    }
    
    static addToCart(item, quantity = 1) {
        const cart = this.getCart();
        const existingItem = cart.find(cartItem => cartItem.code === item.code);
        
        if (existingItem) {
            const newQuantity = existingItem.quantity + quantity;
            // Check if adding this quantity would exceed available stock
            if (newQuantity > item.maxQuantity) {
                const remainingSpace = item.maxQuantity - existingItem.quantity;
                if (remainingSpace > 0) {
                    existingItem.quantity = item.maxQuantity;
                    UIUtils.showAlert(`Only ${remainingSpace} more units could be added. Cart updated to maximum available (${item.maxQuantity}).`, 'warning');
                } else {
                    UIUtils.showAlert(`Cannot add more. You already have the maximum available quantity (${item.maxQuantity}) in cart.`, 'warning');
                    return cart;
                }
            } else {
                existingItem.quantity = newQuantity;
            }
        } else {
            // Validate quantity for new items
            if (quantity > item.maxQuantity) {
                UIUtils.showAlert(`Only ${item.maxQuantity} units available`, 'warning');
                return cart;
            }
            cart.push({
                ...item,
                quantity: quantity
            });
        }
        
        localStorage.setItem('cart', JSON.stringify(cart));
        this.updateCartCount();
        return cart;
    }
    
    static removeFromCart(itemCode) {
        const cart = this.getCart();
        const updatedCart = cart.filter(item => item.code !== itemCode);
        localStorage.setItem('cart', JSON.stringify(updatedCart));
        this.updateCartCount();
        return updatedCart;
    }
    
    static updateCartItem(itemCode, quantity) {
        const cart = this.getCart();
        const item = cart.find(cartItem => cartItem.code === itemCode);
        
        if (item) {
            if (quantity <= 0) {
                return this.removeFromCart(itemCode);
            } else {
                // Validate against maximum available quantity
                if (quantity > item.maxQuantity) {
                    UIUtils.showAlert(`Only ${item.maxQuantity} units available for ${item.name}`, 'warning');
                    item.quantity = item.maxQuantity;
                } else {
                    item.quantity = quantity;
                }
                localStorage.setItem('cart', JSON.stringify(cart));
                this.updateCartCount();
            }
        }
        
        return cart;
    }
    
    static clearCart() {
        localStorage.removeItem('cart');
        this.updateCartCount();
    }
    
    static getCartTotal() {
        const cart = this.getCart();
        return cart.reduce((total, item) => total + (item.price * item.quantity), 0);
    }
    
    static getCartCount() {
        const cart = this.getCart();
        return cart.reduce((count, item) => count + item.quantity, 0);
    }
    
    static updateCartCount() {
        const cartCountElements = document.querySelectorAll('.cart-count');
        const count = this.getCartCount();
        
        cartCountElements.forEach(element => {
            element.textContent = count;
            element.style.display = count > 0 ? 'inline' : 'none';
        });
    }
}

// Navigation and Routing
class Router {
    static navigate(path) {
        window.location.href = path;
    }
    
    static requireAuth(allowedRoles = []) {
        if (!SessionManager.checkSession()) {
            this.navigate('/syos/index.html');
            return false;
        }
        
        if (allowedRoles.length > 0) {
            const userRole = SessionManager.getUserRole();
            if (!allowedRoles.includes(userRole)) {
                UIUtils.showAlert('Access denied. Insufficient permissions.', 'danger');
                return false;
            }
        }
        
        return true;
    }
    
    static redirectToDashboard(role) {
        const dashboardRoutes = {
            'CUSTOMER': '/syos/pages/customer/dashboard.html',
            'cashier': '/syos/pages/cashier/pos-system.html',
            'store manager': '/syos/pages/store-manager/dashboard.html',
            'admin': '/syos/pages/manager/dashboard.html'
        };
        
        const route = dashboardRoutes[role];
        if (route) {
            this.navigate(route);
        } else {
            UIUtils.showAlert('Unknown user role: ' + role, 'danger');
        }
    }
}

// Table utilities
class TableUtils {
    static createTable(data, columns, options = {}) {
        const table = document.createElement('table');
        table.className = 'table';
        
        if (options.striped) {
            table.classList.add('table-striped');
        }
        
        // Create header
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        
        columns.forEach(column => {
            const th = document.createElement('th');
            th.textContent = column.title;
            if (column.width) {
                th.style.width = column.width;
            }
            headerRow.appendChild(th);
        });
        
        if (options.actions) {
            const actionTh = document.createElement('th');
            actionTh.textContent = 'Actions';
            actionTh.style.width = '150px';
            headerRow.appendChild(actionTh);
        }
        
        thead.appendChild(headerRow);
        table.appendChild(thead);
        
        // Create body
        const tbody = document.createElement('tbody');
        
        data.forEach((row, index) => {
            const tr = document.createElement('tr');
            
            columns.forEach(column => {
                const td = document.createElement('td');
                let value = row[column.key];
                
                if (column.formatter) {
                    value = column.formatter(value, row);
                }
                
                td.innerHTML = value;
                tr.appendChild(td);
            });
            
            if (options.actions) {
                const actionTd = document.createElement('td');
                const actionGroup = document.createElement('div');
                actionGroup.className = 'btn-group';
                
                options.actions.forEach(action => {
                    const button = document.createElement('button');
                    button.className = `btn btn-sm btn-${action.type || 'secondary'}`;
                    button.textContent = action.text;
                    button.onclick = () => action.handler(row, index);
                    actionGroup.appendChild(button);
                });
                
                actionTd.appendChild(actionGroup);
                tr.appendChild(actionTd);
            }
            
            tbody.appendChild(tr);
        });
        
        table.appendChild(tbody);
        
        if (options.container) {
            const container = document.createElement('div');
            container.className = 'table-container';
            container.appendChild(table);
            return container;
        }
        
        return table;
    }
    
    static addSearchFilter(tableContainer, searchColumns = []) {
        const searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.className = 'form-control';
        searchInput.placeholder = 'Search...';
        searchInput.style.marginBottom = '1rem';
        
        const table = tableContainer.querySelector('table');
        const rows = Array.from(table.querySelectorAll('tbody tr'));
        
        searchInput.addEventListener('input', UIUtils.debounce((e) => {
            const searchTerm = e.target.value.toLowerCase();
            
            rows.forEach(row => {
                const cells = Array.from(row.querySelectorAll('td'));
                const searchText = searchColumns.length > 0 
                    ? searchColumns.map(index => cells[index]?.textContent || '').join(' ')
                    : cells.map(cell => cell.textContent).join(' ');
                
                const matches = searchText.toLowerCase().includes(searchTerm);
                row.style.display = matches ? '' : 'none';
            });
        }, 300));
        
        tableContainer.insertBefore(searchInput, table);
        return searchInput;
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Update cart count if on customer pages
    if (window.location.pathname.includes('/customer/')) {
        CartManager.updateCartCount();
    }
    
    // Add alert container styles
    const style = document.createElement('style');
    style.textContent = `
        .alert-close {
            background: none;
            border: none;
            font-size: 1.2rem;
            cursor: pointer;
            margin-left: 1rem;
            opacity: 0.7;
        }
        .alert-close:hover {
            opacity: 1;
        }
        .alert {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 0.5rem;
        }
    `;
    document.head.appendChild(style);
});

// Export for use in other files
window.SessionManager = SessionManager;
window.APIClient = APIClient;
window.UIUtils = UIUtils;
window.FormValidator = FormValidator;
window.CartManager = CartManager;
window.Router = Router;
window.TableUtils = TableUtils;
window.API_ENDPOINTS = API_ENDPOINTS;