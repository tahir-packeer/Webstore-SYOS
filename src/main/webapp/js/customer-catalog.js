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
        const stockData = await APIClient.get(API_ENDPOINTS.STOCK, { type: 'website' });
        
        if (!stockData || stockData.length === 0) {
            showNoProductsState();
            return;
        }

        allProducts = stockData.map(item => ({
            id: item.id,
            code: item.code,
            name: item.name,
            price: parseFloat(item.price) || 0.00,
            availableQty: item.quantity
        }));
        
        if (allProducts.length === 0) {
            showNoProductsState();
            return;
        }

        filteredProducts = [...allProducts];
        loadingState.style.display = 'none';
        displayProducts();
        
    } catch (error) {
        console.error('Error loading products:', error);
        loadingState.style.display = 'none';
        productsContainer.style.display = 'block';
        productsContainer.innerHTML = `
            <div style="text-align: center; padding: 2rem; border: 3px solid black; background: white; margin: 2rem;">
                <h3>Unable to load products</h3>
                <p>${error.message}</p>
                <button onclick="loadProducts()" style="background: black; color: white; border: 3px solid black; padding: 1rem 2rem; font-weight: 700; text-transform: uppercase;">Try Again</button>
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
    container.style.display = 'block';
    
    // Calculate pagination
    const startIndex = (currentPage - 1) * productsPerPage;
    const endIndex = startIndex + productsPerPage;
    const pageProducts = filteredProducts.slice(startIndex, endIndex);
    
    // Clear container
    container.innerHTML = '';
    
    // Create product items
    pageProducts.forEach((product, index) => {
        const productItem = createMagazineProductItem(product, index);
        container.appendChild(productItem);
    });
    
    // Update pagination
    updatePagination();
}

function createMagazineProductItem(product, index) {
    const container = document.createElement('div');
    
    // Alternate layout direction for visual interest
    const isReverse = index % 2 === 1;
    container.className = `product-magazine-item ${isReverse ? 'reverse' : ''}`;
    
    // Determine stock status
    const stockStatus = product.availableQty > 10 ? 'in-stock' : 
                       product.availableQty > 0 ? 'low-stock' : 'out-stock';
    
    const stockText = product.availableQty > 10 ? 'In Stock' :
                     product.availableQty > 0 ? 'Low Stock' : 'Out of Stock';
    
    const stockClass = product.availableQty > 10 ? 'status-in-stock' :
                      product.availableQty > 0 ? 'status-low-stock' : 'status-out-stock';
    
    // Generate product icon (first 2 letters of product name)
    const productIcon = product.name.substring(0, 2).toUpperCase();
    
    container.innerHTML = `
        <div class="product-visual-section">
            <div class="status-badge ${stockClass}">${stockText}</div>
            <div class="product-visual-content">
                <div class="product-icon">${productIcon}</div>
                <div class="product-stock-display">
                    <div class="stock-label">Available</div>
                    <div class="stock-number">${product.availableQty}</div>
                </div>
            </div>
        </div>
        
        <div class="product-details-section">
            <div class="product-header">
                <h3 class="product-name">${product.name}</h3>
                <div class="product-code-display">Code: ${product.code}</div>
            </div>
            
            <div class="product-middle-section">
                <div class="product-description">
                    ${product.availableQty > 0 ? 
                        `<p style="color: #666; font-style: italic;">Ready for immediate purchase and delivery.</p>` :
                        `<p style="color: #dc3545; font-weight: 600;">Currently unavailable. Check back soon!</p>`
                    }
                </div>
            </div>
            
            <div class="product-footer">
                <div class="product-price-display">
                    <span class="price-currency">LKR</span> ${product.price.toFixed(2)}
                </div>
                <button class="magazine-add-btn ${product.availableQty === 0 ? 'disabled' : ''}" 
                        onclick="addToCart(${product.id})" 
                        ${product.availableQty === 0 ? 'disabled' : ''}>
                    ${product.availableQty === 0 ? 'Sold Out' : 'Add to Cart'}
                </button>
            </div>
        </div>
    `;
    
    return container;
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
    UIUtils.showAlert(`${product.name} added to cart!`, 'success', 2000);
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

// Add CSS for magazine-style layout enhancements
function addCatalogStyles() {
    const style = document.createElement('style');
    style.textContent = `
        /* Enhanced Magazine Layout Animations */
        .product-magazine-item {
            animation: fadeInUp 0.6s ease-out;
        }
        
        .product-magazine-item:nth-child(even) {
            animation-delay: 0.1s;
        }
        
        .product-magazine-item:nth-child(odd) {
            animation-delay: 0.2s;
        }
        
        @keyframes fadeInUp {
            from {
                opacity: 0;
                transform: translateY(30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        /* Loading State Enhancement */
        .loading {
            display: inline-block;
            width: 40px;
            height: 40px;
            border: 3px solid black;
            border-radius: 50%;
            border-top-color: transparent;
            animation: spin 1s ease-in-out infinite;
        }
        
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        
        /* Pagination Styling to Match Magazine Theme */
        .btn-group .btn {
            border: 2px solid black !important;
            background: white !important;
            color: black !important;
            font-weight: 600 !important;
            text-transform: uppercase !important;
            letter-spacing: 1px !important;
        }
        
        .btn-group .btn:hover:not(:disabled) {
            background: black !important;
            color: white !important;
        }
        
        .btn-group .btn:disabled {
            opacity: 0.5 !important;
            cursor: not-allowed !important;
        }
        
        /* No Products State Styling */
        #noProductsState {
            text-align: center !important;
            padding: 4rem 2rem !important;
            border: 3px solid black !important;
            background: white !important;
            margin: 2rem auto !important;
            max-width: 600px !important;
        }
        
        #noProductsState h4 {
            font-size: 2rem !important;
            font-weight: 900 !important;
            text-transform: uppercase !important;
            letter-spacing: 2px !important;
            margin-bottom: 1rem !important;
        }
        
        #noProductsState p {
            font-size: 1.1rem !important;
            color: #666 !important;
            margin-bottom: 2rem !important;
        }
        
        #noProductsState .btn {
            background: black !important;
            color: white !important;
            border: 3px solid black !important;
            padding: 1rem 2rem !important;
            font-weight: 700 !important;
            text-transform: uppercase !important;
            letter-spacing: 1px !important;
        }
        
        /* Alert Enhancements */
        .alert {
            border: 3px solid black !important;
            border-radius: 0 !important;
            font-weight: 600 !important;
        }
        
        .alert-danger {
            background: white !important;
            color: #dc3545 !important;
            border-color: #dc3545 !important;
        }
    `;
    document.head.appendChild(style);
}