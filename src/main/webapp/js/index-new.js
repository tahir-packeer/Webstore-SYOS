// Main Landing Page JavaScript - Phase 1: Minimalistic UI with Full Functionality
console.log('🚀 SYOS Index.js loaded - New Minimalistic Design');

// Check if user is already logged in
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔍 Checking user session...');
    
    if (typeof SessionManager !== 'undefined' && SessionManager.isLoggedIn() && SessionManager.checkSession()) {
        const userRole = SessionManager.getUserRole();
        console.log('✅ User already logged in with role:', userRole);
        Router.redirectToDashboard(userRole);
    } else {
        console.log('ℹ️ No active session found');
    }
});

// Enhanced Modal Management
function showModal(modalId) {
    console.log('🎯 Opening modal:', modalId);
    closeAllModals();
    
    const overlay = document.getElementById('modalOverlay');
    const modal = document.getElementById(modalId);
    
    if (overlay && modal) {
        overlay.classList.add('show');
        modal.classList.add('show');
        console.log('✅ Modal opened successfully');
    } else {
        console.error('❌ Modal elements not found:', { overlay, modal });
    }
}

function closeAllModals() {
    console.log('🔒 Closing all modals');
    
    const overlay = document.getElementById('modalOverlay');
    if (overlay) overlay.classList.remove('show');
    
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('show');
    });
}

// Main Action Functions
function showCustomerOptions() {
    console.log('🛍️ Showing customer options');
    showModal('customerModal');
}

function showStaffLogin() {
    console.log('👥 Showing staff login');
    showModal('staffLoginModal');
}

function showCustomerLogin() {
    console.log('👤 Showing customer login');
    showModal('customerLoginModal');
}

// Navigation Functions with Enhanced Error Handling
function redirectToCustomerLogin() {
    console.log('🔗 Redirecting to customer login page');
    closeAllModals();
    
    try {
        if (typeof Router !== 'undefined') {
            Router.navigate('/syos/pages/customer/login.html');
        } else {
            window.location.href = '/syos/pages/customer/login.html';
        }
    } catch (error) {
        console.error('❌ Navigation error:', error);
        window.location.href = '/syos/pages/customer/login.html';
    }
}

function redirectToCustomerRegister() {
    console.log('🔗 Redirecting to customer registration page');
    closeAllModals();
    
    try {
        if (typeof Router !== 'undefined') {
            Router.navigate('/syos/pages/customer/register.html');
        } else {
            window.location.href = '/syos/pages/customer/register.html';
        }
    } catch (error) {
        console.error('❌ Navigation error:', error);
        window.location.href = '/syos/pages/customer/register.html';
    }
}

function browseCatalog() {
    console.log('🔗 Redirecting to catalog page');
    closeAllModals();
    
    try {
        if (typeof Router !== 'undefined') {
            Router.navigate('/syos/pages/customer/catalog.html');
        } else {
            window.location.href = '/syos/pages/customer/catalog.html';
        }
    } catch (error) {
        console.error('❌ Navigation error:', error);
        window.location.href = '/syos/pages/customer/catalog.html';
    }
}

// Enhanced Staff Login Handler with Better Error Handling
document.addEventListener('DOMContentLoaded', function() {
    const staffForm = document.getElementById('staffLoginForm');
    if (staffForm) {
        console.log('📝 Attaching staff login handler');
        
        staffForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log('🔐 Staff login attempt started');
            
            const formData = new FormData(e.target);
            const credentials = {
                username: formData.get('username'),
                password: formData.get('password'),
                role: formData.get('role')
            };

            console.log('📊 Staff login credentials:', { 
                username: credentials.username, 
                role: credentials.role 
            });

            const submitButton = e.target.querySelector('button[type="submit"]');
            const originalText = submitButton.textContent;
            
            // Show loading state
            submitButton.textContent = 'Signing in...';
            submitButton.disabled = true;

            try {
                // Ensure API utilities are available
                if (typeof APIClient === 'undefined' || typeof API_ENDPOINTS === 'undefined') {
                    throw new Error('API utilities not loaded');
                }

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
                
                console.log('✅ Staff login response:', response);
                
                const userData = {
                    id: response.id,
                    username: credentials.username,
                    role: response.type || credentials.role, 
                    name: response.name,
                    loginTime: new Date().toISOString()
                };
                
                SessionManager.setUser(userData);
                
                if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) {
                    UIUtils.showAlert('Login successful!', 'success');
                } else {
                    console.log('✅ Login successful!');
                }
                
                closeAllModals();
                
                setTimeout(() => {
                    Router.redirectToDashboard(response.type || credentials.role);
                }, 1000);
                
            } catch (error) {
                console.error('❌ Staff login error:', error);
                
                const errorMessage = error.message || 'Login failed. Please try again.';
                
                if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) {
                    UIUtils.showAlert(errorMessage, 'danger');
                } else {
                    alert(errorMessage);
                }
            } finally {
                // Reset button state
                submitButton.textContent = originalText;
                submitButton.disabled = false;
            }
        });
    }
});

// Enhanced Customer Login Handler
document.addEventListener('DOMContentLoaded', function() {
    const customerForm = document.getElementById('customerLoginForm');
    if (customerForm) {
        console.log('📝 Attaching customer login handler');
        
        customerForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log('🔐 Customer login attempt started');
            
            const formData = new FormData(e.target);
            const credentials = {
                email: formData.get('email'),
                password: formData.get('password')
            };

            console.log('📊 Customer login credentials:', { email: credentials.email });

            const submitButton = e.target.querySelector('button[type="submit"]');
            const originalText = submitButton.textContent;
            
            // Show loading state
            submitButton.textContent = 'Signing in...';
            submitButton.disabled = true;

            try {
                // Ensure API utilities are available
                if (typeof APIClient === 'undefined' || typeof API_ENDPOINTS === 'undefined') {
                    throw new Error('API utilities not loaded');
                }

                // Authenticate customer by posting login credentials
                const response = await APIClient.post(API_ENDPOINTS.CUSTOMER_LOGIN, {
                    email: credentials.email,
                    password: credentials.password
                });

                console.log('✅ Customer login response:', response);

                if (response && response.success) {
                    const userData = {
                        id: response.id,
                        name: response.name,
                        email: response.email,
                        contactNumber: response.contactNumber,
                        address: response.address,
                        role: 'CUSTOMER',
                        loginTime: new Date().toISOString()
                    };

                    SessionManager.setUser(userData);
                    
                    if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) {
                        UIUtils.showAlert('Welcome back!', 'success');
                    } else {
                        console.log('✅ Welcome back!');
                    }
                    
                    closeAllModals();

                    setTimeout(() => {
                        Router.redirectToDashboard('CUSTOMER');
                    }, 1000);
                } else {
                    throw new Error('Invalid email or password');
                }

            } catch (error) {
                console.error('❌ Customer login error:', error);
                
                const errorMessage = error.message || 'Login failed. Please check your credentials.';
                
                if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) {
                    UIUtils.showAlert(errorMessage, 'danger');
                } else {
                    alert(errorMessage);
                }
            } finally {
                // Reset button state
                submitButton.textContent = originalText;
                submitButton.disabled = false;
            }
        });
    }
});

// Keyboard Support
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeAllModals();
    }
});

// Expose functions globally for inline onclick handlers
window.showCustomerOptions = showCustomerOptions;
window.showStaffLogin = showStaffLogin;
window.showCustomerLogin = showCustomerLogin;
window.closeAllModals = closeAllModals;
window.redirectToCustomerLogin = redirectToCustomerLogin;
window.redirectToCustomerRegister = redirectToCustomerRegister;
window.browseCatalog = browseCatalog;

console.log('✅ All functions exposed globally:', {
    showCustomerOptions: typeof window.showCustomerOptions,
    showStaffLogin: typeof window.showStaffLogin,
    showCustomerLogin: typeof window.showCustomerLogin,
    closeAllModals: typeof window.closeAllModals,
    redirectToCustomerLogin: typeof window.redirectToCustomerLogin,
    redirectToCustomerRegister: typeof window.redirectToCustomerRegister,
    browseCatalog: typeof window.browseCatalog
});