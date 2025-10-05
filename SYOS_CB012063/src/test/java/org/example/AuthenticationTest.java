package org.example;

import org.example.Controller.Authentication;
import org.example.Model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthenticationTest {

    @Test
    void get_CashierDetails_when_loggedInAsCashier() {
       Authentication authentication = new Authentication();

       String name = "cashier1";
       String password = "cash123";

       User user = authentication.authenticateUser(name, password);

       assertNotNull(user, "User should not be null after successful authentication");
       assertEquals("cashier1", user.getName(), "Username should match");
       assertEquals("cashier", user.getType(), "User should have role cashier");
    }

    @Test
    void get_StoreManagerDetails_when_loggedInAsAdmin() {
        Authentication authentication = new Authentication();

        String name = "manager1";
        String password = "store123";

        User user = authentication.authenticateUser(name, password);

        assertNotNull(user, "User should not be null after successful authentication");
        assertEquals("manager1", user.getName(), "Username should match");
        assertEquals("storemanager", user.getType(), "User should have role storemanager");
    }

    @Test
    void get_ManagerDetails_when_loggedInAsAdmin() {
        Authentication authentication = new Authentication();

        String name = "admin";
        String password = "admin123";

        User user = authentication.authenticateUser(name, password);

        assertNotNull(user, "User should not be null after successful authentication");
        assertEquals("admin", user.getName(), "Username should match");
        assertEquals("manager", user.getType(), "User should have role manager");
    }

    @Test
    void get_invalidUserDetails_when_loggedInWithInvalidCredentials() {
        Authentication authentication = new Authentication();

        String name = "invalidUser";
        String password = "wrongPassword";

        User user = authentication.authenticateUser(name, password);

        assertEquals(null, user, "User should be null for invalid credentials");
    }
}
