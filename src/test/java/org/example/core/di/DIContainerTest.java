package org.example.core.di;

import org.example.shared.interfaces.ItemService;
import org.example.presentation.controllers.ItemController;
import org.example.persistence.gateways.ItemGateway;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DIContainerTest {

    private DIContainer container;

    @BeforeEach
    void setUp() {
        container = DIContainer.getInstance();
        container.clearSingletons(); // Clean state for each test
    }

    @Test
    @Order(1)
    @DisplayName("Should create singleton instance of ItemService")
    void resolve_ItemService_CreatesSingleton() {
        // Act
        ItemService service1 = container.resolve(ItemService.class);
        ItemService service2 = container.resolve(ItemService.class);

        // Assert
        assertNotNull(service1);
        assertNotNull(service2);
        assertSame(service1, service2, "ItemService should be singleton");
    }

    @Test
    @Order(2)
    @DisplayName("Should create ItemController with injected dependencies")
    void createItemController_WithDependencies_Success() {
        // Act
        ItemController controller = container.createItemController();

        // Assert
        assertNotNull(controller);

        // Test that the controller is functional (has proper dependencies)
        assertDoesNotThrow(() -> {
            boolean exists = controller.itemExists("NONEXISTENT");
            assertFalse(exists); // Should not throw exception
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should resolve ItemGateway as singleton")
    void resolve_ItemGateway_CreatesSingleton() {
        // Act
        ItemGateway gateway1 = container.resolve(ItemGateway.class);
        ItemGateway gateway2 = container.resolve(ItemGateway.class);

        // Assert
        assertNotNull(gateway1);
        assertNotNull(gateway2);
        assertSame(gateway1, gateway2, "ItemGateway should be singleton");
    }

    @Test
    @Order(4)
    @DisplayName("Should check service registration correctly")
    void isRegistered_RegisteredService_ReturnsTrue() {
        // Act & Assert
        assertTrue(container.isRegistered(ItemService.class));
        assertTrue(container.isRegistered(ItemController.class));
        assertTrue(container.isRegistered(ItemGateway.class));
    }

    @Test
    @Order(5)
    @DisplayName("Should throw exception for unregistered service")
    void resolve_UnregisteredService_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> container.resolve(String.class) // String is not registered
        );

        assertTrue(exception.getMessage().contains("Service not registered"));
    }

    @Test
    @Order(6)
    @DisplayName("Should register and resolve custom service")
    void registerCustom_CustomService_ResolvesCorrectly() {
        // Arrange
        String customService = "Custom Test Service";

        // Act
        container.registerCustom(String.class, customService);
        String resolved = container.resolve(String.class);

        // Assert
        assertEquals(customService, resolved);
        assertTrue(container.isRegistered(String.class));
    }

    @Test
    @Order(7)
    @DisplayName("Should create functional controllers through factory methods")
    void createControllers_ThroughFactory_Functional() {
        // Act
        ItemController itemController = container.createItemController();

        // Assert
        assertNotNull(itemController);

        // Test basic functionality
        assertDoesNotThrow(() -> {
            itemController.itemExists("TEST");
            // Should work without throwing exceptions
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should maintain singleton behavior after clearSingletons")
    void clearSingletons_ThenResolve_CreatesNewSingleton() {
        // Arrange
        ItemService service1 = container.resolve(ItemService.class);

        // Act
        container.clearSingletons();
        ItemService service2 = container.resolve(ItemService.class);
        ItemService service3 = container.resolve(ItemService.class);

        // Assert
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotNull(service3);
        assertNotSame(service1, service2, "Should be different after clearing");
        assertSame(service2, service3, "Should be same singleton after clearing");
    }
}
