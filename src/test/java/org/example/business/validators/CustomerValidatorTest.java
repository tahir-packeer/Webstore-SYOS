package org.example.business.validators;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CustomerValidator Test Suite")
public class CustomerValidatorTest {

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid email formats should return true")
        void isValidEmail_ValidFormats_ReturnsTrue() {
            assertTrue(CustomerValidator.isValidEmail("user@example.com"));
            assertTrue(CustomerValidator.isValidEmail("test.email@domain.org"));
            assertTrue(CustomerValidator.isValidEmail("user+tag@example.co.uk"));
            assertTrue(CustomerValidator.isValidEmail("firstname.lastname@company.com"));
            assertTrue(CustomerValidator.isValidEmail("user123@test123.com"));
            assertTrue(CustomerValidator.isValidEmail("user_name@domain-name.com"));
        }

        @Test
        @Order(2)
        @DisplayName("Invalid email formats should return false")
        void isValidEmail_InvalidFormats_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidEmail("plainaddress"));
            assertFalse(CustomerValidator.isValidEmail("@missingdomain.com"));
            assertFalse(CustomerValidator.isValidEmail("missing@.com"));
            assertFalse(CustomerValidator.isValidEmail("missing.domain@.com"));
            assertFalse(CustomerValidator.isValidEmail("user@"));
            assertFalse(CustomerValidator.isValidEmail("user@domain"));
            assertFalse(CustomerValidator.isValidEmail("user space@domain.com"));
            assertFalse(CustomerValidator.isValidEmail("user..double.dot@domain.com"));
        }

        @Test
        @Order(3)
        @DisplayName("Null and empty email should return false")
        void isValidEmail_NullAndEmpty_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidEmail(null));
            assertFalse(CustomerValidator.isValidEmail(""));
            assertFalse(CustomerValidator.isValidEmail("   "));
        }

        @Test
        @Order(4)
        @DisplayName("Email exceeding length limit should return false")
        void isValidEmail_ExceedsLength_ReturnsFalse() {
            String longEmail = "a".repeat(95) + "@test.com"; // 104 characters total
            assertFalse(CustomerValidator.isValidEmail(longEmail));
        }

        @Test
        @Order(5)
        @DisplayName("Email with whitespace should be trimmed and validated")
        void isValidEmail_WithWhitespace_TrimmedAndValidated() {
            assertTrue(CustomerValidator.isValidEmail("  user@example.com  "));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Phone Number Validation Tests")
    class PhoneValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid phone number formats should return true")
        void isValidPhoneNumber_ValidFormats_ReturnsTrue() {
            assertTrue(CustomerValidator.isValidPhoneNumber("1234567890"));
            assertTrue(CustomerValidator.isValidPhoneNumber("+1-234-567-8900"));
            assertTrue(CustomerValidator.isValidPhoneNumber("(123) 456-7890"));
            assertTrue(CustomerValidator.isValidPhoneNumber("123.456.7890"));
            assertTrue(CustomerValidator.isValidPhoneNumber("+44 20 7946 0958"));
            assertTrue(CustomerValidator.isValidPhoneNumber("0771234567"));
        }

        @Test
        @Order(2)
        @DisplayName("Invalid phone number formats should return false")
        void isValidPhoneNumber_InvalidFormats_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidPhoneNumber("123456")); // Too short
            assertFalse(CustomerValidator.isValidPhoneNumber("12345678901234567890123")); // Too long
            assertFalse(CustomerValidator.isValidPhoneNumber("abcdefghij"));
            assertFalse(CustomerValidator.isValidPhoneNumber("123-abc-7890"));
        }

        @Test
        @Order(3)
        @DisplayName("Null and empty phone number should return false")
        void isValidPhoneNumber_NullAndEmpty_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidPhoneNumber(null));
            assertFalse(CustomerValidator.isValidPhoneNumber(""));
            assertFalse(CustomerValidator.isValidPhoneNumber("   "));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Name Validation Tests")
    class NameValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid names should return true")
        void isValidName_ValidNames_ReturnsTrue() {
            assertTrue(CustomerValidator.isValidName("John Doe"));
            assertTrue(CustomerValidator.isValidName("María José"));
            assertTrue(CustomerValidator.isValidName("O'Connor"));
            assertTrue(CustomerValidator.isValidName("Jean-Pierre"));
            assertTrue(CustomerValidator.isValidName("李明"));
            assertTrue(CustomerValidator.isValidName("John123"));
        }

        @Test
        @Order(2)
        @DisplayName("Invalid names should return false")
        void isValidName_InvalidNames_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidName(null));
            assertFalse(CustomerValidator.isValidName(""));
            assertFalse(CustomerValidator.isValidName("   "));
            assertFalse(CustomerValidator.isValidName("123456")); // No letters
            assertFalse(CustomerValidator.isValidName("!@#$%")); // No letters
        }

        @Test
        @Order(3)
        @DisplayName("Name exceeding length limit should return false")
        void isValidName_ExceedsLength_ReturnsFalse() {
            String longName = "John " + "A".repeat(252); // 257 characters total
            assertFalse(CustomerValidator.isValidName(longName));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Address Validation Tests")
    class AddressValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid addresses should return true")
        void isValidAddress_ValidAddresses_ReturnsTrue() {
            assertTrue(CustomerValidator.isValidAddress("123 Main St"));
            assertTrue(CustomerValidator.isValidAddress("Apt 5, 456 Oak Ave, City, State 12345"));
            assertTrue(CustomerValidator.isValidAddress("P.O. Box 789"));
        }

        @Test
        @Order(2)
        @DisplayName("Invalid addresses should return false")
        void isValidAddress_InvalidAddresses_ReturnsFalse() {
            assertFalse(CustomerValidator.isValidAddress(null));
            assertFalse(CustomerValidator.isValidAddress(""));
            assertFalse(CustomerValidator.isValidAddress("   "));
        }

        @Test
        @Order(3)
        @DisplayName("Address exceeding length limit should return false")
        void isValidAddress_ExceedsLength_ReturnsFalse() {
            String longAddress = "A".repeat(501);
            assertFalse(CustomerValidator.isValidAddress(longAddress));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Customer Data Validation Tests")
    class CustomerDataValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid customer data should pass validation")
        void validateCustomerData_ValidData_PassesValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateCustomerData(
                "John Doe", "0771234567", "john@example.com", "123 Main St"
            );
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
        }

        @Test
        @Order(2)
        @DisplayName("Invalid customer data should fail validation with appropriate errors")
        void validateCustomerData_InvalidData_FailsValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateCustomerData(
                "", "invalid-phone", "invalid-email", null
            );
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertTrue(result.getErrors().contains("Name is required"));
            assertTrue(result.getErrors().contains("Phone number is required"));
            assertTrue(result.getErrors().contains("Email must be in valid format"));
        }

        @Test
        @Order(3)
        @DisplayName("Customer data with optional fields null should pass validation")
        void validateCustomerData_OptionalFieldsNull_PassesValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateCustomerData(
                "John Doe", "0771234567", null, null
            );
            assertTrue(result.isValid());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Online Customer Data Validation Tests")
    class OnlineCustomerDataValidationTests {

        @Test
        @Order(1)
        @DisplayName("Valid online customer data should pass validation")
        void validateOnlineCustomerData_ValidData_PassesValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateOnlineCustomerData(
                "John Doe", "0771234567", "john@example.com", "123 Main St"
            );
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
        }

        @Test
        @Order(2)
        @DisplayName("Online customer data missing email should fail validation")
        void validateOnlineCustomerData_MissingEmail_FailsValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateOnlineCustomerData(
                "John Doe", "0771234567", null, "123 Main St"
            );
            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains("Email is required for online customers"));
        }

        @Test
        @Order(3)
        @DisplayName("Online customer data missing address should fail validation")
        void validateOnlineCustomerData_MissingAddress_FailsValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateOnlineCustomerData(
                "John Doe", "0771234567", "john@example.com", null
            );
            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains("Address is required for online customers"));
        }

        @Test
        @Order(4)
        @DisplayName("Online customer data with empty email should fail validation")
        void validateOnlineCustomerData_EmptyEmail_FailsValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateOnlineCustomerData(
                "John Doe", "0771234567", "   ", "123 Main St"
            );
            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains("Email is required for online customers"));
        }

        @Test
        @Order(5)
        @DisplayName("Online customer data with empty address should fail validation")
        void validateOnlineCustomerData_EmptyAddress_FailsValidation() {
            CustomerValidator.ValidationResult result = CustomerValidator.validateOnlineCustomerData(
                "John Doe", "0771234567", "john@example.com", "   "
            );
            assertFalse(result.isValid());
            assertTrue(result.getErrors().contains("Address is required for online customers"));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("ValidationResult Tests")
    class ValidationResultTests {

        @Test
        @Order(1)
        @DisplayName("New ValidationResult should be valid")
        void newValidationResult_ShouldBeValid() {
            CustomerValidator.ValidationResult result = new CustomerValidator.ValidationResult();
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
            assertEquals("", result.getErrors());
        }

        @Test
        @Order(2)
        @DisplayName("ValidationResult with errors should be invalid")
        void validationResultWithErrors_ShouldBeInvalid() {
            CustomerValidator.ValidationResult result = new CustomerValidator.ValidationResult();
            result.addError("First error");
            result.addError("Second error");
            
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertEquals("First error; Second error", result.getErrors());
        }
    }
}
