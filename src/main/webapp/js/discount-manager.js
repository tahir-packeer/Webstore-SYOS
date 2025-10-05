window.handleSaveDiscountCode = function() {
    try {
        const codeInput = document.getElementById('discount-code');
        const valueInput = document.getElementById('discount-value');
        const idInput = document.getElementById('discount-id');
        
        if (!codeInput || !valueInput) {
            if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) UIUtils.showAlert('Error: Form inputs not found', 'danger');
            else alert('Error: Form inputs not found');
            return;
        }
        
        const code = codeInput.value.trim().toUpperCase();
        const value = parseFloat(valueInput.value);
        const id = idInput ? idInput.value.trim() : '';
                
        if (!code) {
            if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) UIUtils.showAlert('Please enter a discount code', 'warning');
            else alert('Please enter a discount code');
            codeInput.focus();
            return;
        }
        
        if (isNaN(value) || value <= 0) {
            if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) UIUtils.showAlert('Please enter a valid discount value greater than 0', 'warning');
            else alert('Please enter a valid discount value greater than 0');
            valueInput.focus();
            return;
        }
        
        if (typeof showAlert === 'function') {
            showAlert('Saving discount code...', 'info');
        }
        
        saveDiscountToAPI(code, value, id);
        
        } catch (error) {
        if (typeof UIUtils !== 'undefined' && UIUtils.showAlert) UIUtils.showAlert('Error: ' + error.message, 'danger');
        else alert('Error: ' + error.message);
    }
};

async function saveDiscountToAPI(code, value, id) {
    try {
        const discountData = {
            code: code,
            discount_value: value
        };
        
        let url, method;
        
        if (id && id !== '') {
            url = `/syos/api/discount-codes/${id}`;
            method = 'PUT';
        } else {
            url = '/syos/api/discount-codes/';
            method = 'POST';
        }
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(discountData)
        });
        
        const result = await response.json();
        
        if (response.ok && result.success) {
            const action = id ? 'updated' : 'created';
            if (typeof showAlert === 'function') {
                showAlert(`Discount code ${action} successfully!`, 'success');
            } else {
                alert(`Discount code ${action} successfully!`);
            }
            
            if (typeof closeModal === 'function') {
                closeModal('discount-modal');
            } else {
                const modal = document.getElementById('discount-modal');
                if (modal) {
                    modal.classList.remove('active');
                    modal.style.display = 'none';
                }
            }
            
            if (typeof loadDiscountCodes === 'function') {
                loadDiscountCodes();
            }
        } else {
            const errorMsg = result.error || result.message || `Failed to ${id ? 'update' : 'create'} discount code`;
            if (typeof showAlert === 'function') {
                showAlert(errorMsg, 'error');
            } else {
                alert('Error: ' + errorMsg);
            }
        }
        
    } catch (error) {
        if (typeof showAlert === 'function') {
            showAlert('Network error. Please check your connection and try again.', 'error');
        } else {
            alert('Network error. Please check your connection and try again.');
        }
    }
}
