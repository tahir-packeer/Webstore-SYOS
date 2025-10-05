// Customer Dashboard JavaScript
// Import utils.js functionality

// Check authentication
document.addEventListener('DOMContentLoaded', function() {
    if (!Router.requireAuth(['CUSTOMER'])) {
        return;
    }
    
    const user = SessionManager.getUser();
    document.getElementById('userName').textContent = user.name || user.email || 'Customer';
    
    loadDashboardData();
});

async function loadDashboardData() {
    try {
        // Update cart statistics
        updateCartStats();
        
    } catch (error) {
        UIUtils.showAlert('Error loading dashboard data', 'warning');
    }
}

function updateCartStats() {
    const cart = CartManager.getCart();
    const cartTotal = CartManager.getCartTotal();
    const cartCount = CartManager.getCartCount();
    
    document.getElementById('cartItemsCount').textContent = cartCount;
    document.getElementById('cartTotal').textContent = UIUtils.formatCurrency(cartTotal);
    
    // Update cart count in navigation
    CartManager.updateCartCount();
}

function addToCart(code, name, price) {
    const item = {
        code: code,
        name: name,
        price: price
    };
    
    CartManager.addToCart(item, 1);
    UIUtils.showAlert(`${name} added to cart!`, 'success', 2000);
    updateCartStats();
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