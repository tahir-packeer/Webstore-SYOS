package org.example.Service;

import org.example.Manager.RequestQueueManager;
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