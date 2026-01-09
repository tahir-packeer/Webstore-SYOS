package org.example.business.managers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestQueueManager {
    private static RequestQueueManager instance;
    private static final Object lock = new Object();
    
    private final BlockingQueue<Runnable> requestQueue;
    private final ExecutorService executor;
    private final int MAX_THREADS = 10;
    private final int MAX_QUEUE_SIZE = 100;
    
    private RequestQueueManager() {
        this.requestQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
    }
    
    public static RequestQueueManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RequestQueueManager();
                }
            }
        }
        return instance;
    }
    
    public void submitRequest(Runnable task) {
        executor.submit(task);
    }
    
    public boolean isQueueFull() {
        return requestQueue.remainingCapacity() == 0;
    }
    
    public int getQueueSize() {
        return requestQueue.size();
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
