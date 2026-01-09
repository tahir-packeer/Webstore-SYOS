package org.example.persistence.database;

import org.junit.jupiter.api.Test;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {
    @Test
    void testConnect() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        try (Connection conn = db.connect()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        } catch (Exception e) {
            fail("Exception thrown during DB connection: " + e.getMessage());
        }
    }

    @Test
    void testCloseConnection() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        try (Connection conn = db.connect()) {
            assertNotNull(conn, "Connection should not be null");
            db.closeConnection(conn);
            assertTrue(conn.isClosed(), "Connection should be closed after closeConnection is called");
        } catch (Exception e) {
            fail("Exception thrown during DB connection or closing: " + e.getMessage());
        }
    }
}
