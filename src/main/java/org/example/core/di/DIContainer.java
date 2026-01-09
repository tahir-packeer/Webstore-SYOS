package org.example.core.di;

import org.example.shared.interfaces.*;
import org.example.business.services.*;
import org.example.persistence.gateways.*;
import org.example.presentation.controllers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// Simple Dependency Injection Container
// Provides centralized service resolution and dependency management
// Implements Service Locator pattern with singleton and prototype scopes
public class DIContainer {

    private static DIContainer instance;
    private final Map<Class<?>, Supplier<?>> services = new HashMap<>();
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    private DIContainer() {
        registerServices();
    }

    public static DIContainer getInstance() {
        if (instance == null) {
            synchronized (DIContainer.class) {
                if (instance == null) {
                    instance = new DIContainer();
                }
            }
        }
        return instance;
    }

    // Register all services and their dependencies
    private void registerServices() {
        // Register Gateways
        registerSingleton(ItemGateway.class, ItemGateway::getInstance);

        // Register Services
        registerSingleton(ItemService.class, () -> new ItemServiceImpl(resolve(ItemGateway.class)));

        // Register Controllers with dependency injection
        registerPrototype(ItemController.class, () -> new ItemController(resolve(ItemService.class)));

        // Register legacy controllers for backward compatibility
        registerSingleton(StockController.class, StockController::new);
        registerSingleton(ShelfController.class, ShelfController::new);
    }

    // Register a service as singleton (one instance per application)
    public <T> void registerSingleton(Class<T> serviceClass, Supplier<T> factory) {
        services.put(serviceClass, factory);
    }

    // Register a service as prototype (new instance every time)
    public <T> void registerPrototype(Class<T> serviceClass, Supplier<T> factory) {
        services.put(serviceClass, factory);
    }

    // Resolve a service by its class type
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> serviceClass) {
        // Check if singleton instance exists
        if (singletons.containsKey(serviceClass)) {
            return (T) singletons.get(serviceClass);
        }

        // Get factory for the service
        Supplier<?> factory = services.get(serviceClass);
        if (factory == null) {
            throw new IllegalArgumentException("Service not registered: " + serviceClass.getName());
        }

        // Create instance
        T instance = (T) factory.get();

        // Store singleton if applicable (check if it's in singleton registry)
        if (isSingleton(serviceClass)) {
            singletons.put(serviceClass, instance);
        }

        return instance;
    }

    // Check if a service is registered as singleton
    private boolean isSingleton(Class<?> serviceClass) {
        // Simple heuristic: if it's a Gateway or Manager, it's likely a singleton
        return serviceClass.getSimpleName().contains("Gateway") ||
                serviceClass.getSimpleName().contains("Manager") ||
                serviceClass == ItemService.class; // ItemService configured as singleton
    }

    // Create a new ItemController with proper dependency injection
    public ItemController createItemController() {
        return resolve(ItemController.class);
    }

    // Create a new StockControllerLegacy (legacy)
    public StockController createStockController() {
        return resolve(StockController.class);
    }

    // Create a new ShelfController (legacy)
    public ShelfController createShelfController() {
        return resolve(ShelfController.class);
    }

    // Clear all singletons (useful for testing)
    public void clearSingletons() {
        singletons.clear();
    }

    // Get service registration information
    public boolean isRegistered(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    // Register a custom service (for testing or extension)
    public <T> void registerCustom(Class<T> serviceClass, T instance) {
        singletons.put(serviceClass, instance);
        services.put(serviceClass, () -> instance);
    }
}
