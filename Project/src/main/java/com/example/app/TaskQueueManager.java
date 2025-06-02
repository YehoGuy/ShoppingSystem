package com.example.app;

import java.util.concurrent.*;
import java.util.function.Function;

class TaskRequest<T, R> {
    final T input;
    final CompletableFuture<R> future;

    TaskRequest(T input) {
        this.input = input;
        this.future = new CompletableFuture<>();
    }
}

public class TaskQueueManager<T, R> {
    private final BlockingQueue<TaskRequest<T, R>> taskQueue = new LinkedBlockingQueue<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Function<T, R> taskProcessor;

    public TaskQueueManager(Function<T, R> taskProcessor) {
        this.taskProcessor = taskProcessor;
        startWorker();
    }

    private void startWorker() {
        worker.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TaskRequest<T, R> req = taskQueue.take(); 
                    try {
                        R result = taskProcessor.apply(req.input);
                        req.future.complete(result);
                    } catch (Exception e) {
                        req.future.completeExceptionally(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                }
            }
        });
    }

    public R submitTask(T input) throws ExecutionException, InterruptedException {
        TaskRequest<T, R> request = new TaskRequest<>(input);
        taskQueue.add(request);
        return request.future.get(); // blocks
    }

    public void shutdown() {
        worker.shutdownNow();
    }
}
