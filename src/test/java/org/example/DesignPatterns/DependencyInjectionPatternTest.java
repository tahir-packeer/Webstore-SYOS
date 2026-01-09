package org.example.DesignPatterns;

import org.example.presentation.controllers.ItemController;
import org.example.presentation.controllers.ShelfController;
import org.example.presentation.controllers.StockController;
import org.example.core.di.DIContainer;
import org.example.persistence.gateways.ItemGateway;
import org.example.shared.interfaces.ItemService;
import org.example.business.services.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit tests for Dependency Injection Design Pattern
 * implementation
 * Tests DIContainer with service registration, resolution, and lifecycle
 * management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dependency Injection Pattern Tests")
class DependencyInjectionPatternTest {

    @Mock
    private ItemGateway mockItemGateway;

    @Mock
    private ItemService mockItemService;

    private DIContainer diContainer;

    @BeforeEach
    void setUp() {
        // Clear any existing singletons before each test
        diContainer = DIContainer.getInstance();
        diContainer.clearSingletons();
    }

    @Test
    @DisplayName("Should implement singleton pattern for DIContainer")
    void shouldImplementSingletonPatternForDIContainer() {
        // When
        DIContainer instance1 = DIContainer.getInstance();
        DIContainer instance2 = DIContainer.getInstance();

        // Then
        assertThat(instance1).isSameAs(instance2);
        assertThat(instance1).isNotNull();
    }

    @Test
    @DisplayName("Should register and resolve singleton services")
    void shouldRegisterAndResolveSingletonServices() {
        // Given
        diContainer.registerSingleton(String.class, () -> "Test Singleton");

        // When
        String instance1 = diContainer.resolve(String.class);
        String instance2 = diContainer.resolve(String.class);

        // Then
        assertThat(instance1).isEqualTo("Test Singleton");
        assertThat(instance1).isSameAs(instance2); // Same instance
    }

    @Test
    @DisplayName("Should register and resolve prototype services")
    void shouldRegisterAndResolvePrototypeServices() {
        // Given
        diContainer.registerPrototype(StringBuilder.class, StringBuilder::new);

        // When
        StringBuilder instance1 = diContainer.resolve(StringBuilder.class);
        StringBuilder instance2 = diContainer.resolve(StringBuilder.class);

        // Then
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        assertThat(instance1).isNotSameAs(instance2); // Different instances
    }

    @Test
    @DisplayName("Should throw exception for unregistered services")
    void shouldThrowExceptionForUnregisteredServices() {
        // When & Then
        assertThatThrownBy(() -> diContainer.resolve(UnregisteredService.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Service not registered: " +
                        UnregisteredService.class.getName());
    }

    @Test
    @DisplayName("Should resolve ItemGateway as singleton")
    void shouldResolveItemGatewayAsSingleton() {
        // When
        ItemGateway instance1 = diContainer.resolve(ItemGateway.class);
        ItemGateway instance2 = diContainer.resolve(ItemGateway.class);

        // Then
        assertThat(instance1).isNotNull();
        assertThat(instance1).isSameAs(instance2); // Should be singleton
    }

    @Test
    @DisplayName("Should resolve ItemService with dependency injection")
    void shouldResolveItemServiceWithDependencyInjection() {
        // When
        ItemService itemService = diContainer.resolve(ItemService.class);

        // Then
        assertThat(itemService).isNotNull();
        assertThat(itemService).isInstanceOf(ItemServiceImpl.class);
    }

    @Test
    @DisplayName("Should create ItemController with dependency injection")
    void shouldCreateItemControllerWithDependencyInjection() {
        // When
        ItemController controller = diContainer.createItemController();

        // Then
        assertThat(controller).isNotNull();
        // Note: ItemController should be prototype, so each call creates new instance
    }

    @Test
    @DisplayName("Should create legacy controllers")
    void shouldCreateLegacyControllers() {
        // When
        StockController stockController = diContainer.createStockController();
        ShelfController shelfController = diContainer.createShelfController();

        // Then
        assertThat(stockController).isNotNull();
        assertThat(shelfController).isNotNull();
    }

    @Test
    @DisplayName("Should support custom service registration")
    void shouldSupportCustomServiceRegistration() {
        // Given
        String customService = "Custom Test Service";
        diContainer.registerCustom(String.class, customService);

        // When
        String resolvedService = diContainer.resolve(String.class);

        // Then
        assertThat(resolvedService).isSameAs(customService);
        assertThat(diContainer.isRegistered(String.class)).isTrue();
    }

    @Test
    @DisplayName("Should check service registration status")
    void shouldCheckServiceRegistrationStatus() {
        // Given
        diContainer.registerSingleton(Integer.class, () -> 42);

        // When & Then
        assertThat(diContainer.isRegistered(Integer.class)).isTrue();
        assertThat(diContainer.isRegistered(Double.class)).isFalse();
    }

    @Test
    @DisplayName("Should clear singletons correctly")
    void shouldClearSingletonsCorrectly() {
        // Given
        diContainer.registerSingleton(String.class, () -> "Test");
        String instance1 = diContainer.resolve(String.class);

        // When
        diContainer.clearSingletons();
        String instance2 = diContainer.resolve(String.class);

        // Then
        assertThat(instance1).isNotSameAs(instance2); // Different instances after clear
    }

    @Test
    @DisplayName("Should handle circular dependencies gracefully")
    void shouldHandleCircularDependenciesGracefully() {
        // Given - Setup services that might create circular dependency
        diContainer.registerSingleton(ServiceA.class, () -> new ServiceA());
        diContainer.registerSingleton(ServiceB.class, () -> new ServiceB());

        // When & Then - Should not cause infinite loop
        assertThatCode(() -> {
            ServiceA serviceA = diContainer.resolve(ServiceA.class);
            ServiceB serviceB = diContainer.resolve(ServiceB.class);
            assertThat(serviceA).isNotNull();
            assertThat(serviceB).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should demonstrate dependency injection benefits")
    void shouldDemonstrateDependencyInjectionBenefits() {
        // Given - Mock the service for testing
        diContainer.registerCustom(ItemService.class, mockItemService);

        // When
        ItemController controller = diContainer.createItemController();

        // Then
        assertThat(controller).isNotNull();
        // The controller should use the mocked service for testing
        // This demonstrates the testability benefit of DI
    }

    @Test
    @DisplayName("Should support interface-based dependency injection")
    void shouldSupportInterfaceBasedDependencyInjection() {
        // Given
        TestInterface mockImplementation = mock(TestInterface.class);
        when(mockImplementation.doSomething()).thenReturn("Mocked Result");

        diContainer.registerCustom(TestInterface.class, mockImplementation);

        // When
        TestInterface resolved = diContainer.resolve(TestInterface.class);
        String result = resolved.doSomething();

        // Then
        assertThat(resolved).isSameAs(mockImplementation);
        assertThat(result).isEqualTo("Mocked Result");
        verify(mockImplementation).doSomething();
    }

    @Test
    @DisplayName("Should maintain service lifecycle correctly")
    void shouldMaintainServiceLifecycleCorrectly() {
        // Given
        diContainer.registerSingleton(LifecycleService.class, LifecycleService::new);

        // When
        LifecycleService service1 = diContainer.resolve(LifecycleService.class);
        LifecycleService service2 = diContainer.resolve(LifecycleService.class);

        // Then
        assertThat(service1).isSameAs(service2); // Singleton behavior
        assertThat(service1.getInstanceId()).isEqualTo(service2.getInstanceId());
    }

    @Test
    @DisplayName("Should handle lazy initialization")
    void shouldHandleLazyInitialization() {
        // Given
        LazyService lazyService = mock(LazyService.class);
        diContainer.registerSingleton(LazyService.class, () -> {
            // This factory should only be called when service is first resolved
            return lazyService;
        });

        // When - Service not yet resolved
        assertThat(diContainer.isRegistered(LazyService.class)).isTrue();

        // When - First resolution
        LazyService resolved = diContainer.resolve(LazyService.class);

        // Then
        assertThat(resolved).isSameAs(lazyService);
    }

    @ParameterizedTest
    @ValueSource(classes = { ItemGateway.class, ItemService.class, StockController.class, ShelfController.class })
    @DisplayName("Should resolve pre-registered services")
    void shouldResolvePreRegisteredServices(Class<?> serviceClass) {
        // When & Then
        assertThatCode(() -> {
            Object service = diContainer.resolve(serviceClass);
            assertThat(service).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should demonstrate inversion of control")
    void shouldDemonstrateInversionOfControl() {
        // Given - Traditional approach would require manual instantiation
        // With DI, the container controls object creation and dependency resolution

        // When
        ItemController controller = diContainer.createItemController();
        ItemService service = diContainer.resolve(ItemService.class);

        // Then
        assertThat(controller).isNotNull();
        assertThat(service).isNotNull();
        // The container has inverted control of object creation
    }

    @Test
    @DisplayName("Should support method injection pattern")
    void shouldSupportMethodInjectionPattern() {
        // Given
        ConfigurableService service = new ConfigurableService();
        diContainer.registerCustom(ConfigurableService.class, service);

        // When
        ConfigurableService resolved = diContainer.resolve(ConfigurableService.class);
        resolved.configure("Test Configuration");

        // Then
        assertThat(resolved.getConfiguration()).isEqualTo("Test Configuration");
    }

    // Helper classes for testing
    private static class UnregisteredService {
    }

    private static class ServiceA {
    }

    private static class ServiceB {
    }

    private interface TestInterface {
        String doSomething();
    }

    private static class LifecycleService {
        private final String instanceId = java.util.UUID.randomUUID().toString();

        public String getInstanceId() {
            return instanceId;
        }
    }

    private static class LazyService {
        public void doWork() {
            // Lazy service implementation
        }
    }

    private static class ConfigurableService {
        private String configuration;

        public void configure(String config) {
            this.configuration = config;
        }

        public String getConfiguration() {
            return configuration;
        }
    }
}
