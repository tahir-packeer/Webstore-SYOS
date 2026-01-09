// General Dashboard JavaScript - Common functionality for all dashboard pages

// Initialize dashboard based on user role
document.addEventListener('DOMContentLoaded', function() {
    const user = SessionManager.getUser();
    const userRole = SessionManager.getUserRole();
    
    // Check authentication and redirect if needed
    if (!user || !SessionManager.checkSession()) {
        Router.navigate('/syos/index.html');
        return;
    }
    
    // Initialize dashboard based on role
    initializeDashboard(userRole, user);
    
    // Update cart count if applicable
    if (typeof CartManager !== 'undefined') {
        CartManager.updateCartCount();
    }
});

function initializeDashboard(role, user) {
    // Update user info displays
    updateUserInfo(user);
    
    // Initialize role-specific functionality
    switch(role) {
        case 'CUSTOMER':
            initializeCustomerDashboard(user);
            break;
        case 'CASHIER':
            initializeCashierDashboard(user);
            break;
        case 'MANAGER':
            initializeManagerDashboard(user);
            break;
        case 'STORE_MANAGER':
            initializeStoreManagerDashboard(user);
            break;
        default:
            console.warn('Unknown user role:', role);
    }
}

function updateUserInfo(user) {
    // Update welcome message
    const userNameElements = document.querySelectorAll('[data-user-name]');
    userNameElements.forEach(element => {
        element.textContent = user.name || user.email || 'User';
    });
    
    // Update user info displays
    const userInfoElements = document.querySelectorAll('[data-user-info]');
    userInfoElements.forEach(element => {
        element.textContent = `Welcome, ${user.name || user.email || 'User'}`;
    });
}

function initializeCustomerDashboard(user) {
    // Customer-specific dashboard initialization
    if (typeof loadDashboardData === 'function') {
        loadDashboardData();
    }
}

function initializeCashierDashboard(user) {
    // Cashier-specific dashboard initialization
    console.log('Initializing cashier dashboard for:', user.name);
}

function initializeManagerDashboard(user) {
    // Manager-specific dashboard initialization
    console.log('Initializing manager dashboard for:', user.name);
}

function initializeStoreManagerDashboard(user) {
    // Store manager-specific dashboard initialization
    console.log('Initializing store manager dashboard for:', user.name);
}

// Common logout functionality
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        SessionManager.clearSession();
        UIUtils.showAlert('Logged out successfully', 'info');
        setTimeout(() => {
            Router.navigate('/syos/index.html');
        }, 1000);
    }
}

// Navigation helpers
function navigateToHome() {
    Router.navigate('/syos/index.html');
}

function navigateToCustomerCatalog() {
    Router.navigate('/syos/pages/customer/catalog.html');
}

function navigateToCustomerCart() {
    Router.navigate('/syos/pages/customer/cart.html');
}