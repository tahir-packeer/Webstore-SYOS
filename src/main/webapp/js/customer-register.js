// Customer Registration JavaScript

// Check if already logged in
document.addEventListener('DOMContentLoaded', function() {
    if (SessionManager.isLoggedIn() && SessionManager.checkSession()) {
        const userRole = SessionManager.getUserRole();
        window.location.href = 'dashboard.html';
    }
});

// Handle form submission
document.getElementById('registrationForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    // Get form data
    const formData = new FormData(e.target);
    const firstName = formData.get('firstName')?.trim();
    const lastName = formData.get('lastName')?.trim();
    const email = formData.get('email')?.trim();
    const phone = formData.get('phone')?.trim();
    const address = formData.get('address')?.trim();
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    // Basic validation
    if (!firstName || firstName.length < 2) {
        UIUtils.showAlert('Please enter a valid first name (at least 2 characters)', 'danger');
        return;
    }
    
    if (!lastName || lastName.length < 2) {
        UIUtils.showAlert('Please enter a valid last name (at least 2 characters)', 'danger');
        return;
    }
    
    if (!email || !email.includes('@')) {
        UIUtils.showAlert('Please enter a valid email address', 'danger');
        return;
    }
    
    if (!phone) {
        UIUtils.showAlert('Please enter a phone number', 'danger');
        return;
    }
    
    if (!address || address.length < 10) {
        UIUtils.showAlert('Please enter a complete address (at least 10 characters)', 'danger');
        return;
    }
    
    if (!password || password.length < 6) {
        UIUtils.showAlert('Password must be at least 6 characters long', 'danger');
        return;
    }
    
    if (password !== confirmPassword) {
        UIUtils.showAlert('Passwords do not match', 'danger');
        return;
    }
    
    const customerData = {
        name: `${firstName} ${lastName}`,
        contactNumber: phone,
        email: email,
        address: address,
        password: password
    };

    const submitButton = e.target.querySelector('button[type="submit"]');
    const hideLoading = UIUtils.showLoading(submitButton);

    try {
        const response = await APIClient.post(API_ENDPOINTS.CUSTOMER_REGISTER, customerData);
        
        UIUtils.showAlert('Account created successfully! You can now sign in.', 'success');
        
        // Auto-login the user after successful registration
        const userData = {
            id: response.id || Date.now(),
            name: customerData.name,
            email: customerData.email,
            contactNumber: customerData.contactNumber,
            address: customerData.address,
            role: 'CUSTOMER',
            loginTime: new Date().toISOString()
        };
        
        SessionManager.setUser(userData);
        
        // Redirect to customer dashboard after successful registration
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 2000);
        
    } catch (error) {
        UIUtils.showAlert(
            error.message || 'Registration failed. Please try again.',
            'danger'
        );
    } finally {
        hideLoading();
    }
});

// Real-time validation
document.addEventListener('input', function(e) {
    if (e.target.matches('#email')) {
        const email = e.target.value;
        if (email && !FormValidator.validateEmail(email)) {
            FormValidator.showFieldError(e.target, 'Please enter a valid email address');
        } else {
            FormValidator.clearFieldError(e.target);
        }
    }
    
    
    if (e.target.matches('#confirmPassword')) {
        const password = document.getElementById('password').value;
        const confirmPassword = e.target.value;
        
        if (confirmPassword && password !== confirmPassword) {
            FormValidator.showFieldError(e.target, 'Passwords do not match');
        } else {
            FormValidator.clearFieldError(e.target);
        }
    }
});

// Modal functions
function showModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

function showTerms() {
    showModal('termsModal');
}

function showPrivacy() {
    showModal('privacyModal');
}

// Close modals when clicking outside
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('show');
    }
});