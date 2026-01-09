package org.example.business.validators;

import java.util.regex.Pattern;

public class CustomerValidator {
    
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9\\p{L}-]+\\.)+[a-zA-Z\\p{L}]{2,7}$";
    
    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);
    
    // Phone number validation pattern (supports various formats)
    private static final String PHONE_PATTERN = 
        "^(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(\\d{1,4}\\)\\s?)?[\\d\\s.-]{7,15}$";
    
    private static final Pattern PHONE_REGEX = Pattern.compile(PHONE_PATTERN);
    
    /**
     * Validates if the provided email address is in correct format
     * @param email the email address to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        email = email.trim();
        
        if (email.length() > 100) {
            return false;
        }
        
        return EMAIL_REGEX.matcher(email).matches();
    }
    
    /**
     * Validates if the provided phone number is in correct format
     * @param phoneNumber the phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        phoneNumber = phoneNumber.trim();
        
        if (phoneNumber.length() < 7 || phoneNumber.length() > 20) {
            return false;
        }
        
        return PHONE_REGEX.matcher(phoneNumber).matches();
    }
    
    /**
     * Validates if the provided name is acceptable
     * @param name the name to validate
     * @return true if name is valid, false otherwise
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        name = name.trim();
        
        if (name.length() > 255) {
            return false;
        }
        
        // Name should contain at least one letter (Unicode-aware)
        if (!name.matches(".*\\p{L}.*")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates if the provided address is acceptable
     * @param address the address to validate
     * @return true if address is valid, false otherwise
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        
        address = address.trim();
        
        if (address.length() > 500) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Comprehensive validation for customer registration data
     * @param name customer name
     * @param phoneNumber customer phone number
     * @param email customer email (can be null for in-store customers)
     * @param address customer address (can be null for in-store customers)
     * @return ValidationResult containing validation status and error messages
     */
    public static ValidationResult validateCustomerData(String name, String phoneNumber, String email, String address) {
        ValidationResult result = new ValidationResult();
        
        // Validate name
        if (!isValidName(name)) {
            result.addError("Name is required and must contain at least one letter (max 255 characters)");
        }
        
        // Validate phone number
        if (!isValidPhoneNumber(phoneNumber)) {
            result.addError("Phone number is required and must be in valid format (7-20 characters)");
        }
        
        // Validate email (if provided)
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            result.addError("Email must be in valid format (e.g., user@example.com, max 100 characters)");
        }
        
        // Validate address (if provided)
        if (address != null && !address.trim().isEmpty() && !isValidAddress(address)) {
            result.addError("Address is too long (max 500 characters)");
        }
        
        return result;
    }
    
    /**
     * Validation for online customer registration (email and address required)
     * @param name customer name
     * @param phoneNumber customer phone number
     * @param email customer email (required for online customers)
     * @param address customer address (required for online customers)
     * @return ValidationResult containing validation status and error messages
     */
    public static ValidationResult validateOnlineCustomerData(String name, String phoneNumber, String email, String address) {
        ValidationResult result = new ValidationResult();
        
        // Validate name
        if (!isValidName(name)) {
            result.addError("Name is required and must contain at least one letter (max 255 characters)");
        }
        
        // Validate phone number
        if (!isValidPhoneNumber(phoneNumber)) {
            result.addError("Phone number is required and must be in valid format (7-20 characters)");
        }
        
        // Email is required for online customers
        if (email == null || email.trim().isEmpty()) {
            result.addError("Email is required for online customers");
        } else if (!isValidEmail(email)) {
            result.addError("Email must be in valid format (e.g., user@example.com, max 100 characters)");
        }
        
        // Address is required for online customers
        if (address == null || address.trim().isEmpty()) {
            result.addError("Address is required for online customers");
        } else if (!isValidAddress(address)) {
            result.addError("Address is too long (max 500 characters)");
        }
        
        return result;
    }
    
    /**
     * Inner class to hold validation results
     */
    public static class ValidationResult {
        private boolean isValid = true;
        private StringBuilder errors = new StringBuilder();
        
        public void addError(String error) {
            if (isValid) {
                isValid = false;
            }
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(error);
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrors() {
            return errors.toString();
        }
        
        public boolean hasErrors() {
            return !isValid;
        }
    }
}
