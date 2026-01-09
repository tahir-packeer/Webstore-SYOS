// Admin Reports JavaScript

let currentReportType = 'daily-sales';
let reportData = {};

document.addEventListener('DOMContentLoaded', function() {
    if (!Router.requireAuth(['admin', 'store_manager'])) {
        return;
    }
    
    const user = SessionManager.getUser();
    document.getElementById('userInfo').textContent = `Welcome, ${user.username || 'Admin'}`;
    
    // Set default dates
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('dailySalesDate').value = today;
    
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    document.getElementById('billTransactionStartDate').value = weekAgo.toISOString().split('T')[0];
    document.getElementById('billTransactionEndDate').value = today;
    
    // Set initial active report (daily sales)
    const firstLink = document.querySelector('.report-link[data-report="daily-sales"]');
    if (firstLink) {
        firstLink.classList.add('active');
    }
    
    // Load initial reports
    loadItemsNeedShelvingReport();
    loadReorderLevelReport();
    loadStockReport();
});

function switchReport(reportType) {
    // Update sidebar link active state
    document.querySelectorAll('.report-link').forEach(link => {
        link.classList.remove('active');
    });
    
    // Find and activate the clicked link
    const activeLink = document.querySelector(`[data-report="${reportType}"]`);
    if (activeLink) {
        activeLink.classList.add('active');
    }
    
    // Hide all report sections
    document.querySelectorAll('.report-section').forEach(section => {
        section.classList.remove('active');
    });
    
    // Show selected section
    const targetSection = document.getElementById(reportType);
    if (targetSection) {
        targetSection.classList.add('active');
    }
    
    currentReportType = reportType;
    
    // Load data for the selected report type
    switch(reportType) {
        case 'daily-sales':
            loadDailySalesReport();
            break;
        case 'items-shelving':
            loadItemsNeedShelvingReport();
            break;
        case 'reorder-level':
            loadReorderLevelReport();
            break;
        case 'stock-report':
            loadStockReport();
            break;
        case 'bill-transaction':
            loadBillTransactionReport();
            break;
    }
}

async function loadDailySalesReport() {
    const date = document.getElementById('dailySalesDate').value;
    const transactionType = document.getElementById('dailySalesTransactionType').value;
    const storeType = document.getElementById('dailySalesStoreType').value;
    
    if (!date) return;
    
    try {
        const params = new URLSearchParams({
            type: 'daily-sales',
            date: date
        });
        
        if (transactionType) params.append('transactionType', transactionType);
        if (storeType) params.append('storeType', storeType);
        
        const response = await fetch(`/syos/api/reports?${params}`);
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        reportData['daily-sales'] = result;
        
        // Update summary
        const summaryHtml = `
            <div class="summary-card">
                <div class="summary-value">LKR ${result.summary.totalSales.toFixed(2)}</div>
                <div class="summary-label">Total Sales</div>
            </div>
            <div class="summary-card">
                <div class="summary-value">${result.summary.totalTransactions}</div>
                <div class="summary-label">Transactions</div>
            </div>
            <div class="summary-card">
                <div class="summary-value">LKR ${(result.summary.totalSales / Math.max(result.summary.totalTransactions, 1)).toFixed(2)}</div>
                <div class="summary-label">Avg Transaction</div>
            </div>
        `;
        document.getElementById('dailySalesSummary').innerHTML = summaryHtml;
        
        // Update table
        const tableBody = document.getElementById('dailySalesData');
        if (result.data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="9" class="text-center">No sales data found for selected criteria</td></tr>';
        } else {
            tableBody.innerHTML = result.data.map(bill => `
                <tr>
                    <td>${bill.invoiceNumber}</td>
                    <td>${new Date(bill.billDate).toLocaleTimeString()}</td>
                    <td>LKR ${bill.fullPrice.toFixed(2)}</td>
                    <td>LKR ${bill.discount.toFixed(2)}</td>
                    <td>LKR ${bill.cashTendered.toFixed(2)}</td>
                    <td>LKR ${bill.changeAmount.toFixed(2)}</td>
                    <td>${bill.itemCount}</td>
                    <td><span class="badge badge-primary">${bill.customerType}</span></td>
                    <td><span class="badge badge-secondary">${bill.storeType}</span></td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading daily sales report:', error);
        showError('Failed to load daily sales report: ' + error.message);
    }
}

async function loadItemsNeedShelvingReport() {
    try {
        const response = await fetch('/syos/api/reports?type=items-need-shelving');
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        reportData['items-shelving'] = result;
        
        // Update summary
        const summaryHtml = `
            <div class="summary-card">
                <div class="summary-value">${result.summary.itemsNeedingShelving}</div>
                <div class="summary-label">Items Need Shelving</div>
            </div>
        `;
        document.getElementById('itemsShelvingSummary').innerHTML = summaryHtml;
        
        // Update table
        const tableBody = document.getElementById('itemsShelvingData');
        if (result.data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center">All items are properly stocked on shelves</td></tr>';
        } else {
            tableBody.innerHTML = result.data.map(item => {
                const priority = item.shelfQuantity < 10 ? 'HIGH' : item.shelfQuantity < 25 ? 'MEDIUM' : 'LOW';
                const badgeClass = priority === 'HIGH' ? 'alert-badge' : priority === 'MEDIUM' ? 'warning-badge' : 'success-badge';
                
                return `
                    <tr>
                        <td>${item.itemCode}</td>
                        <td>${item.itemName}</td>
                        <td>${item.shelfQuantity}</td>
                        <td>${item.totalStockQuantity}</td>
                        <td>${item.type}</td>
                        <td><span class="${badgeClass}">${priority}</span></td>
                    </tr>
                `;
            }).join('');
        }
    } catch (error) {
        console.error('Error loading items need shelving report:', error);
        showError('Failed to load items need shelving report: ' + error.message);
    }
}

async function loadReorderLevelReport() {
    try {
        const response = await fetch('/syos/api/reports?type=reorder-level');
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        reportData['reorder-level'] = result;
        
        // Update summary
        const summaryHtml = `
            <div class="summary-card">
                <div class="summary-value">${result.summary.itemsBelowReorderLevel}</div>
                <div class="summary-label">Items Below Reorder Level</div>
            </div>
        `;
        document.getElementById('reorderLevelSummary').innerHTML = summaryHtml;
        
        // Update table
        const tableBody = document.getElementById('reorderLevelData');
        if (result.data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center">All items are above reorder level</td></tr>';
        } else {
            tableBody.innerHTML = result.data.map(item => `
                <tr>
                    <td>${item.itemCode}</td>
                    <td>${item.itemName}</td>
                    <td>${item.currentStock}</td>
                    <td>${item.reorderLevel}</td>
                    <td><span class="alert-badge">${item.shortfall}</span></td>
                    <td><span class="alert-badge">REORDER NOW</span></td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading reorder level report:', error);
        showError('Failed to load reorder level report: ' + error.message);
    }
}

async function loadStockReport() {
    try {
        const response = await fetch('/syos/api/reports?type=stock-report');
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        reportData['stock-report'] = result;
        
        // Update summary
        const summaryHtml = `
            <div class="summary-card">
                <div class="summary-value">${result.summary.totalItems}</div>
                <div class="summary-label">Total Items</div>
            </div>
        `;
        document.getElementById('stockReportSummary').innerHTML = summaryHtml;
        
        // Update table
        const tableBody = document.getElementById('stockReportData');
        if (result.data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No stock data available</td></tr>';
        } else {
            tableBody.innerHTML = result.data.map(item => `
                <tr>
                    <td>${item.itemCode}</td>
                    <td>${item.itemName}</td>
                    <td>${item.totalQuantity}</td>
                    <td>${item.batchCount}</td>
                    <td>${item.earliestExpiry || 'N/A'}</td>
                    <td>${item.latestPurchase || 'N/A'}</td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading stock report:', error);
        showError('Failed to load stock report: ' + error.message);
    }
}

async function loadBillTransactionReport() {
    const startDate = document.getElementById('billTransactionStartDate').value;
    const endDate = document.getElementById('billTransactionEndDate').value;
    const transactionType = document.getElementById('billTransactionType').value;
    const storeType = document.getElementById('billStoreType').value;
    
    if (!startDate || !endDate) return;
    
    try {
        const params = new URLSearchParams({
            type: 'bill-transaction',
            startDate: startDate,
            endDate: endDate
        });
        
        if (transactionType) params.append('transactionType', transactionType);
        if (storeType) params.append('storeType', storeType);
        
        const response = await fetch(`/syos/api/reports?${params}`);
        const result = await response.json();
        
        if (result.error) {
            throw new Error(result.error);
        }
        
        reportData['bill-transaction'] = result;
        
        // Update summary
        const summaryHtml = `
            <div class="summary-card">
                <div class="summary-value">LKR ${result.summary.totalSales.toFixed(2)}</div>
                <div class="summary-label">Total Sales</div>
            </div>
            <div class="summary-card">
                <div class="summary-value">${result.summary.totalTransactions}</div>
                <div class="summary-label">Transactions</div>
            </div>
            <div class="summary-card">
                <div class="summary-value">LKR ${(result.summary.totalSales / Math.max(result.summary.totalTransactions, 1)).toFixed(2)}</div>
                <div class="summary-label">Avg Transaction</div>
            </div>
        `;
        document.getElementById('billTransactionSummary').innerHTML = summaryHtml;
        
        // Update table
        const tableBody = document.getElementById('billTransactionData');
        if (result.data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="10" class="text-center">No transactions found for selected criteria</td></tr>';
        } else {
            tableBody.innerHTML = result.data.map(bill => `
                <tr>
                    <td>${bill.invoiceNumber}</td>
                    <td>${new Date(bill.billDate).toLocaleString()}</td>
                    <td>LKR ${bill.fullPrice.toFixed(2)}</td>
                    <td>LKR ${bill.discount.toFixed(2)}</td>
                    <td>LKR ${bill.cashTendered.toFixed(2)}</td>
                    <td>LKR ${bill.changeAmount.toFixed(2)}</td>
                    <td>${bill.itemCount}</td>
                    <td>${bill.totalItems || 0}</td>
                    <td><span class="badge badge-primary">${bill.customerType}</span></td>
                    <td><span class="badge badge-secondary">${bill.storeType}</span></td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading bill transaction report:', error);
        showError('Failed to load bill transaction report: ' + error.message);
    }
}

function showError(message) {
    alert('Error: ' + message);
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        SessionManager.clearSession();
        setTimeout(() => {
            Router.navigate('/syos/index.html');
        }, 1000);
    }
}

// Universal table search function
function searchTable(tableId, searchValue) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const rows = table.getElementsByTagName('tr');
    const filter = searchValue.toLowerCase();
    
    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        const cells = row.getElementsByTagName('td');
        let found = false;
        
        // Skip if no cells (header row or empty row)
        if (cells.length === 0) continue;
        
        // Check each cell in the row
        for (let j = 0; j < cells.length; j++) {
            const cellText = cells[j].textContent || cells[j].innerText;
            if (cellText.toLowerCase().indexOf(filter) > -1) {
                found = true;
                break;
            }
        }
        
        // Show/hide the row based on search result
        if (found || filter === '') {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    }
}