// Cashier Dashboard JavaScript

document.addEventListener('DOMContentLoaded', function() {
    if (!Router.requireAuth(['cashier'])) {
        return;
    }

    const user = SessionManager.getUser();
    document.getElementById('userInfo').textContent = 'Welcome, ' + (user.username || 'Cashier');

    loadTodayStats();
    loadRecentTransactions();
});

async function loadTodayStats() {
    try {
        // Load today's sales statistics
        const stats = await APIClient.get('/api/reports/daily-sales?date=' + new Date().toISOString().split('T')[0]);

        // Update stat display
        document.getElementById('todaySales').textContent = 'Rs ' + (stats.totalSales ? stats.totalSales.toFixed(2) : '0.00');
        document.getElementById('transactionCount').textContent = stats.transactionCount || 0;
        document.getElementById('itemsSold').textContent = stats.totalItems || 0;
        document.getElementById('avgTransaction').textContent = 'Rs ' + (stats.averageTransaction ? stats.averageTransaction.toFixed(2) : '0.00');

    } catch (error) {
        UIUtils.showAlert('Error loading dashboard statistics', 'danger');
    }
}

async function loadRecentTransactions() {
    try {
        const transactions = await APIClient.get('/api/bills/recent?limit=10');
        const container = document.getElementById('recentTransactions');
        
        if (transactions && transactions.length > 0) {
            let html = '<div class="table-responsive"><table class="table"><thead><tr>';
            html += '<th>Bill #</th><th>Date</th><th>Amount</th><th>Items</th><th>Customer</th></tr></thead><tbody>';
            
            transactions.forEach(bill => {
                html += `<tr>
                    <td>${bill.invoiceNumber || 'N/A'}</td>
                    <td>${new Date(bill.billDate).toLocaleDateString()}</td>
                    <td>LKR ${bill.fullPrice ? bill.fullPrice.toFixed(2) : '0.00'}</td>
                    <td>${bill.totalItems || 0}</td>
                    <td>${bill.customerName || 'Walk-in'}</td>
                </tr>`;
            });
            
            html += '</tbody></table></div>';
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p class="text-muted text-center">No recent transactions found</p>';
        }
    } catch (error) {
        document.getElementById('recentTransactions').innerHTML = '<p class="text-muted text-center">Error loading transactions</p>';
    }
}

function showDailyStats() {
    UIUtils.showAlert('Today statistics loaded in the performance section below', 'info');
    document.querySelector('.card:last-child').scrollIntoView({ behavior: 'smooth' });
}

async function refreshStats() {
    UIUtils.showAlert('Refreshing today statistics...', 'info');
    await loadTodayStats();
    UIUtils.showAlert('Statistics refreshed!', 'success');
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