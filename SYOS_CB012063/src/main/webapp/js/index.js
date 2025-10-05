// Main Landing Page JavaScript
// Check if user is already logged in
document.addEventListener('DOMContentLoaded', function() {
    if (SessionManager.isLoggedIn() && SessionManager.checkSession()) {
        const userRole = SessionManager.getUserRole();
        Router.redirectToDashboard(userRole);
    }
});

// Modal Management
function showModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

function showCustomerOptions() {
    showModal('customerModal');
}

function showStaffLogin() {
    showModal('staffLoginModal');
}

function showCustomerLogin() {
    showModal('customerLoginModal');
}

// Navigation Functions
function redirectToCustomerLogin() {
    Router.navigate('/syos/pages/customer/login.html');
}

function redirectToCustomerRegister() {
    Router.navigate('/syos/pages/customer/register.html');
}

function browseCatalog() {
    Router.navigate('/syos/pages/customer/catalog.html');
}

// Staff Login Handler
document.getElementById('staffLoginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const credentials = {
        username: formData.get('username'),
        password: formData.get('password'),
        role: formData.get('role')
    };

    const submitButton = e.target.querySelector('button[type="submit"]');
    const hideLoading = UIUtils.showLoading(submitButton);

    try {
        // Call authentication API
        let response;
        
        if (credentials.role === 'CUSTOMER') {
            response = await APIClient.post(API_ENDPOINTS.CUSTOMER_LOGIN, {
                email: credentials.username,
                password: credentials.password
            });
        } else {
            response = await APIClient.post(API_ENDPOINTS.STAFF_LOGIN, {
                username: credentials.username,
                password: credentials.password,
                role: credentials.role
            });
        }
        
        const userData = {
            id: response.id,
            username: credentials.username,
            role: response.type || credentials.role, 
            name: response.name,
            loginTime: new Date().toISOString()
        };
        
        SessionManager.setUser(userData);
        UIUtils.showAlert('Login successful!', 'success');
        
        setTimeout(() => {
            Router.redirectToDashboard(response.type || credentials.role);
        }, 1000);
        
    } catch (error) {
        UIUtils.showAlert(error.message || 'Login failed. Please try again.', 'danger');
    } finally {
        hideLoading();
    }
});

// Customer Login Handler
document.getElementById('customerLoginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const credentials = {
        email: formData.get('email'),
        password: formData.get('password')
    };

    const submitButton = e.target.querySelector('button[type="submit"]');
    const hideLoading = UIUtils.showLoading(submitButton);

    try {
        const customers = await APIClient.get(API_ENDPOINTS.CUSTOMER_LOGIN);
        
        // Find customer by email 
        const customer = Array.isArray(customers) ? 
            customers.find(c => c.email === credentials.email) : null;
        
        if (customer) {
            const userData = {
                id: customer.id,
                name: customer.name,
                email: customer.email,
                contactNumber: customer.contactNumber,
                address: customer.address,
                role: 'CUSTOMER',
                loginTime: new Date().toISOString()
            };
            
            SessionManager.setUser(userData);
            UIUtils.showAlert('Welcome back!', 'success');
            closeModal('customerLoginModal');
            
            setTimeout(() => {
                Router.redirectToDashboard('CUSTOMER');
            }, 1000);
        } else {
            throw new Error('Customer not found. Please register first.');
        }
        
    } catch (error) {
        UIUtils.showAlert(error.message || 'Login failed. Please check your credentials.', 'danger');
    } finally {
        hideLoading();
    }
});