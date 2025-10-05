// Customer Catalog Page JavaScript
let allProducts = [];
let filteredProducts = [];
let currentPage = 1;
const productsPerPage = 12;

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    checkUserStatus();
    loadProducts();
    setupEventListeners();
    addCatalogStyles();
});

function checkUserStatus() {
    const user = SessionManager.getUser();
    if (user && SessionManager.checkSession()) {
        document.getElementById('userInfo').textContent = `Welcome, ${user.name || user.email || 'Customer'}`;
        document.getElementById('authButton').textContent = 'Logout';
        document.getElementById('authButton').onclick = logout;
    } else {
        document.getElementById('userInfo').textContent = 'Guest';
        document.getElementById('authButton').textContent = 'Login';
        document.getElementById('authButton').onclick = () => Router.navigate('/syos/pages/customer/login.html');
    }
    
    // Update cart count
    CartManager.updateCartCount();
}

function setupEventListeners() {
    // Search functionality
    document.getElementById('searchInput').addEventListener('input', UIUtils.debounce(filterProducts, 300));
    
    // Sort functionality
    document.getElementById('sortSelect').addEventListener('change', sortProducts);
}

async function loadProducts() {
    const productsContainer = document.getElementById('productsContainer');
    const loadingState = document.getElementById('loadingState');
    
    loadingState.style.display = 'block';
    productsContainer.style.display = 'none';

    try {
        // Get website shelf items (not warehouse stock) from StockServlet
        const stockData = await APIClient.get(API_ENDPOINTS.STOCK, { type: 'website' });
        
        if (!stockData || stockData.length === 0) {
            showNoProductsState();
            return;
        }

        // Transform website shelf data to product format for display
        // These are already filtered for WEBSITE type and quantity > 0
        allProducts = stockData.map(item => ({
            id: item.id,
            code: item.code,
            name: item.name,
            price: parseFloat(item.price) || 0.00,
            availableQty: item.quantity,
            // Website shelf items don't have purchase/expiry dates
            dateOfPurchase: item.date_of_purchase || null,
            dateOfExpiry: item.date_of_expiry || null
        }));
        
        if (allProducts.length === 0) {
            showNoProductsState();
            return;
        }

        filteredProducts = [...allProducts];
        loadingState.style.display = 'none';
        displayProducts(filteredProducts);
        
    } catch (error) {
        console.error('Error loading products:', error);
        loadingState.style.display = 'none';
        productsContainer.style.display = 'block';
        productsContainer.innerHTML = `
            <div class="col-12">
                <div class="alert alert-danger">
                    <h5>Unable to load products</h5>
                    <p>${error.message}</p>
                    <button class="btn btn-primary" onclick="loadProducts()">Try Again</button>
                </div>
            </div>
        `;
    }
}

function filterProducts() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    
    if (!searchTerm) {
        filteredProducts = [...allProducts];
    } else {
        filteredProducts = allProducts.filter(product => 
            product.name.toLowerCase().includes(searchTerm) ||
            product.code.toLowerCase().includes(searchTerm)
        );
    }
    
    currentPage = 1;
    displayProducts();
}

function sortProducts() {
    const sortValue = document.getElementById('sortSelect').value;
    
    switch (sortValue) {
        case 'price':
            filteredProducts.sort((a, b) => a.price - b.price);
            break;
        case 'price-desc':
            filteredProducts.sort((a, b) => b.price - a.price);
            break;
    }
    
    displayProducts();
}

function displayProducts() {
    const container = document.getElementById('productsContainer');
    const loadingState = document.getElementById('loadingState');
    const noProductsState = document.getElementById('noProductsState');
    
    loadingState.style.display = 'none';
    
    if (filteredProducts.length === 0) {
        showNoProductsState();
        return;
    }
    
    noProductsState.style.display = 'none';
    container.style.display = 'flex';
    
    // Calculate pagination
    const startIndex = (currentPage - 1) * productsPerPage;
    const endIndex = startIndex + productsPerPage;
    const pageProducts = filteredProducts.slice(startIndex, endIndex);
    
    // Clear container
    container.innerHTML = '';
    
    // Create product cards
    pageProducts.forEach(product => {
        const productCard = createProductCard(product);
        container.appendChild(productCard);
    });
    
    // Update pagination
    updatePagination();
}

function createProductCard(product) {
    const col = document.createElement('div');
    col.className = 'col-4 mb-4';
    
    const stockBadge = product.availableQty > 10 ? 
        '<span class="badge badge-success">In Stock</span>' :
        product.availableQty > 0 ? 
        '<span class="badge badge-warning">Low Stock</span>' :
        '<span class="badge badge-danger">Out of Stock</span>';
    
    const formattedPrice = `LKR ${product.price.toFixed(2)}`;
    
    col.innerHTML = `
        <div class="product-card h-100">
            <div class="product-card-header">
                <div class="product-badge-container">
                    ${stockBadge}
                </div>
                <div class="product-info">
                    <h5 class="product-title">${product.name}</h5>
                    <div class="product-code">Code: ${product.code}</div>
                </div>
            </div>
            <div class="product-card-body">
                <div class="product-stock-info">
                    <span class="stock-text">${product.availableQty} units available</span>
                </div>
                <div class="product-pricing">
                    <div class="price-container">
                        <span class="currency">LKR</span>
                        <span class="price">${product.price.toFixed(2)}</span>
                    </div>
                    <button class="add-to-cart-btn ${product.availableQty === 0 ? 'disabled' : ''}" 
                            onclick="addToCart(${product.id})" 
                            ${product.availableQty === 0 ? 'disabled' : ''}>
                        <span class="btn-text">Add to Cart</span>
                    </button>
                </div>
            </div>
        </div>
    `;
    
    return col;
}

function showNoProductsState() {
    document.getElementById('loadingState').style.display = 'none';
    document.getElementById('productsContainer').style.display = 'none';
    document.getElementById('noProductsState').style.display = 'block';
    document.getElementById('paginationContainer').style.display = 'none';
    
    // Update the no products message based on whether it's a search or no products at all
    const searchTerm = document.getElementById('searchInput').value;
    const noProductsDiv = document.getElementById('noProductsState');
    
    if (searchTerm) {
        noProductsDiv.querySelector('h4').textContent = 'No Products Found';
        noProductsDiv.querySelector('p').textContent = 'Try adjusting your search terms or clear the search to see all products';
    } else {
        noProductsDiv.querySelector('h4').textContent = 'No Products Available';
        noProductsDiv.querySelector('p').textContent = 'Check back later for new products or contact store management';
    }
}

function updatePagination() {
    const totalPages = Math.ceil(filteredProducts.length / productsPerPage);
    const paginationContainer = document.getElementById('paginationContainer');
    const pageInfo = document.getElementById('pageInfo');
    const prevButton = document.getElementById('prevButton');
    const nextButton = document.getElementById('nextButton');
    
    if (totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }
    
    paginationContainer.style.display = 'block';
    pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
    prevButton.disabled = currentPage === 1;
    nextButton.disabled = currentPage === totalPages;
}

function changePage(direction) {
    const totalPages = Math.ceil(filteredProducts.length / productsPerPage);
    const newPage = currentPage + direction;
    
    if (newPage >= 1 && newPage <= totalPages) {
        currentPage = newPage;
        displayProducts();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('sortSelect').value = 'price';
    filteredProducts = [...allProducts];
    currentPage = 1;
    displayProducts();
}

function handleAuth() {
    return CustomerAuth.getCustomerId() !== null;
}

function checkAuth() {
    const customerId = CustomerAuth.getCustomerId();
    if (!customerId) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

function addToCart(productId, quantity = 1) {
    const product = allProducts.find(p => p.id === productId);
    if (!product || product.availableQty === 0) {
        UIUtils.showAlert('Product is out of stock', 'warning');
        return;
    }
    
    if (quantity > product.availableQty) {
        UIUtils.showAlert(`Only ${product.availableQty} units available`, 'warning');
        return;
    }
    
    const cartItem = {
        id: product.id,
        code: product.code,
        name: product.name,
        price: product.price,
        maxQuantity: product.availableQty
    };
    
    CartManager.addToCart(cartItem, quantity);
    UIUtils.showAlert(`${product.name} (${quantity} units) added to cart!`, 'success', 2000);
    CartManager.updateCartCount();
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

// Add CSS for badges and card styling
function addCatalogStyles() {
    const style = document.createElement('style');
    style.textContent = `
        /* Product Card Styling */
        .product-card {
            background: var(--white);
            border-radius: 16px;
            box-shadow: var(--shadow);
            transition: var(--transition);
            overflow: hidden;
            border: 1px solid var(--border-color);
        }
        
        .product-card:hover {
            transform: translateY(-8px);
            box-shadow: var(--shadow-lg);
        }
        
        .product-card-header {
            background: var(--primary-color);
            padding: 1.5rem;
            position: relative;
            color: var(--white);
        }
        
        .product-badge-container {
            position: absolute;
            top: 1rem;
            right: 1rem;
        }
        
        .product-info {
            padding-right: 6rem;
        }
        
        .product-title {
            margin: 0 0 0.5rem 0;
            font-size: 1.25rem;
            font-weight: var(--font-weight-semibold);
            color: var(--white);
            line-height: 1.3;
        }
        
        .product-code {
            font-size: 0.875rem;
            color: rgba(255, 255, 255, 0.8);
            font-weight: 400;
        }
        
        .product-card-body {
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            gap: 1rem;
            flex-grow: 1;
        }
        
        .product-stock-info {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            padding: 0.75rem;
            background: var(--neutral-color);
            border-radius: var(--border-radius);
            border-left: 4px solid var(--accent-color);
        }
        
        .stock-text {
            font-size: 0.875rem;
            color: var(--text-light);
            font-weight: var(--font-weight-medium);
        }
        
        .product-pricing {
            margin-top: auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 1rem;
        }
        
        .price-container {
            display: flex;
            align-items: baseline;
            gap: 0.25rem;
        }
        
        .currency {
            font-size: 0.875rem;
            color: var(--text-light);
            font-weight: var(--font-weight-medium);
        }
        
        .price {
            font-size: 1.5rem;
            font-weight: var(--font-weight-bold);
            color: var(--text-color);
        }
        
        .add-to-cart-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            padding: 0.75rem 1.25rem;
            background: var(--accent-color);
            color: var(--white);
            border: none;
            border-radius: var(--border-radius);
            font-weight: var(--font-weight-semibold);
            font-size: 0.875rem;
            cursor: pointer;
            transition: var(--transition);
            box-shadow: var(--shadow);
        }
        
        .add-to-cart-btn:hover:not(.disabled) {
            background: var(--secondary-color);
            transform: translateY(-1px);
            box-shadow: var(--shadow-hover);
        }
        
        .add-to-cart-btn.disabled {
            background: var(--border-color);
            color: var(--text-light);
            cursor: not-allowed;
            box-shadow: none;
        }
        
        .btn-text {
            font-size: 0.875rem;
        }
        
        /* Badge Styling */
        .badge {
            padding: 0.375rem 0.75rem;
            font-size: 0.75rem;
            border-radius: 20px;
            color: var(--white);
            font-weight: var(--font-weight-semibold);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            box-shadow: var(--shadow);
        }
        
        .badge-success { 
            background: var(--accent-color);
        }
        
        .badge-warning { 
            background: var(--secondary-color);
        }
        
        .badge-danger { 
            background: var(--primary-color);
        }
        
        /* Responsive Design */
        @media (max-width: 768px) {
            .product-card-header {
                padding: 1rem;
            }
            
            .product-info {
                padding-right: 5rem;
            }
            
            .product-title {
                font-size: 1.1rem;
            }
            
            .product-card-body {
                padding: 1rem;
            }
            
            .product-pricing {
                flex-direction: column;
                align-items: stretch;
                gap: 0.75rem;
            }
            
            .add-to-cart-btn {
                justify-content: center;
                width: 100%;
            }
        }
        
        /* Utility Classes */
        .h-100 { height: 100%; }
        .flex-grow-1 { flex-grow: 1; }
        .ml-2 { margin-left: 0.5rem; }
    `;
    document.head.appendChild(style);
}