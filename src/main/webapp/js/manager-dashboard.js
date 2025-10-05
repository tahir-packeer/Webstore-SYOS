// Manager Dashboard JavaScript

document.addEventListener('DOMContentLoaded', function() {
    if (!Router.requireAuth(['admin'])) {
        return;
    }
    
    const user = SessionManager.getUser();
    document.getElementById('userInfo').textContent = `Welcome, ${user.username || 'Admin'}`;
    
    loadQuickStats();
});

async function loadQuickStats() {
    try {
        // Load today's sales data
        await loadTodaysSales();
        
        // Load stock alert data
        await loadStockAlerts();
        
    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

async function loadTodaysSales() {
    try {
        const today = new Date().toISOString().split('T')[0];
        const response = await fetch(`/syos/api/reports?type=daily-sales&date=${today}`);
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        const totalSales = result.summary.totalSales || 0;
        const totalOrders = result.summary.totalTransactions || 0;
        
        document.getElementById('totalSales').textContent = `LKR ${totalSales.toFixed(2)}`;
        document.getElementById('totalOrders').textContent = totalOrders;
        
    } catch (error) {
        console.error('Error loading sales data:', error);
        document.getElementById('totalSales').textContent = 'Error';
        document.getElementById('totalOrders').textContent = 'Error';
    }
}

async function loadStockAlerts() {
    try {
        // Get reorder level data
        const reorderResponse = await fetch('/syos/api/reports?type=reorder-level');
        const reorderResult = await reorderResponse.json();
        
        if (reorderResult.error) {
            throw new Error(reorderResult.error);
        }
        
        const lowStockItems = reorderResult.summary.itemsBelowReorderLevel || 0;
        const expiringItems = 0; // Placeholder since we don't have expiry data
        
        document.getElementById('lowStockItems').textContent = lowStockItems;
        document.getElementById('expiringItems').textContent = expiringItems;
        
    } catch (error) {
        console.error('Error loading stock data:', error);
        document.getElementById('lowStockItems').textContent = 'Error';
        document.getElementById('expiringItems').textContent = 'Error';
    }
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        SessionManager.clearSession();
        setTimeout(() => {
            Router.navigate('/syos/index.html');
        }, 1000);
    }
}