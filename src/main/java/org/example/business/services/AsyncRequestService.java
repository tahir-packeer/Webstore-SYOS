package org.example.business.services;

import org.example.business.managers.RequestQueueManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncRequestService {
    private final RequestQueueManager queueManager;
    
    public AsyncRequestService() {
        this.queueManager = RequestQueueManager.getInstance();
    }
    
    public <T> CompletableFuture<T> processAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task::get);
    }
    
    public void processRequest(Runnable task) {
        queueManager.submitRequest(task);
    }
    
    public boolean canAcceptRequest() {
        return !queueManager.isQueueFull();
    }
}
