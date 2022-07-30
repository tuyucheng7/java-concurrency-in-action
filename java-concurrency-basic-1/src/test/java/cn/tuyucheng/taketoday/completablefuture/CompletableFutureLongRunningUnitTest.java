package cn.tuyucheng.taketoday.completablefuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;

class CompletableFutureLongRunningUnitTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompletableFutureLongRunningUnitTest.class);

    @Test
    @DisplayName("whenRunningCompletableFutureAsynchronously_thenGetMethodWatisForResult")
    void whenRunningCompletableFutureAsynchronously_thenGetMethodWatisForResult() throws InterruptedException, ExecutionException {
        Future<String> completableFuture = calculateAsync();
        String result = completableFuture.get();
        assertEquals("Hello", result);
    }

    private Future<String> calculateAsync() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            MILLISECONDS.sleep(500);
            completableFuture.complete("Hello");
            return null;
        });
        return completableFuture;
    }

    @Test
    @DisplayName("whenRunningCompletableFutureWithResult_thenGetMethodReturnsImmediately")
    void whenRunningCompletableFutureWithResult_thenGetMethodReturnsImmediately() throws ExecutionException, InterruptedException {
        Future<String> completableFuture = CompletableFuture.completedFuture("Hello");
        String result = completableFuture.get();
        assertEquals("Hello", result);
    }

    private Future<String> calculateAsyncWithCancellation() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(500);
            completableFuture.cancel(false);
            return null;
        });
        return completableFuture;
    }

    @Test
    @DisplayName("whenCancelingTheFuture_thenThrowsCancellationException")
    void whenCancelingTheFuture_thenThrowsCancellationException() {
        Future<String> future = calculateAsyncWithCancellation();
        assertThrows(CancellationException.class, future::get);
    }

    @Test
    @DisplayName("whenCreatingCompletableFutureWithSupplyAsync_thenFutureReturnValue")
    void whenCreatingCompletableFutureWithSupplyAsync_thenFutureReturnValue() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");
        assertEquals("Hello", future.get());
    }

    @Test
    @DisplayName("whenAddingThenAcceptToFuture_thenFunctionExecutesAfterComputationIsFinished")
    void whenAddingThenAcceptToFuture_thenFunctionExecutesAfterComputationIsFinished() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<Void> future = completableFuture.thenAccept(s -> LOGGER.debug("Computation returned: {}", s));
        future.get();
    }

    @Test
    @DisplayName("whenAddingThenRunToFuture_thenFunctionExecutesAfterComputationIsFinished")
    void whenAddingThenRunToFuture_thenFunctionExecutesAfterComputationIsFinished() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<Void> future = completableFuture.thenRun(() -> LOGGER.debug("Computation finished."));
        future.get();
    }

    @Test
    @DisplayName("whenAddingThenApplyToFuture_thenFunctionExecutesAfterComputationIsFinished")
    void whenAddingThenApplyToFuture_thenFunctionExecutesAfterComputationIsFinished() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> completableFuture = future.thenApply(s -> s + " World");
        assertEquals("Hello World", completableFuture.get());
    }

    @Test
    @DisplayName("whenUsingThenCompose_thenFuturesExecuteSequentially")
    void whenUsingThenCompose_thenFuturesExecuteSequentially() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Hello")
                .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));
        assertEquals("Hello World", completableFuture.get());
    }

    @Test
    @DisplayName("whenUsingThenCombine_thenWaitForExecutionOfBothFutures")
    void whenUsingThenCombine_thenWaitForExecutionOfBothFutures() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Hello")
                .thenCombine(CompletableFuture.supplyAsync(() -> " World"), (s1, s2) -> s1 + s2);
        assertEquals("Hello World", completableFuture.get());
    }

    @Test
    @DisplayName("whenUsingThenAcceptBoth_thenWaitForExecutionOfBothFutures")
    void whenUsingThenAcceptBoth_thenWaitForExecutionOfBothFutures() {
        CompletableFuture.supplyAsync(() -> "Hello")
                .thenAcceptBoth(CompletableFuture.supplyAsync(() -> " World"), (s1, s2) -> LOGGER.debug("Computation resulted: {}", s1 + s2));
    }

    @Test
    @DisplayName("whenFutureCombinedWithAllOfCompletes_thenAllFuturesAreDone")
    void whenFutureCombinedWithAllOfCompletes_thenAllFuturesAreDone() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello ");
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "Beautiful ");
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "World");

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2, future3);
        combinedFuture.get();

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());

        String combined = Stream.of(future1, future2, future3)
                .map(CompletableFuture::join)
                .collect(Collectors.joining());
        assertEquals("Hello Beautiful World", combined);
    }

    @Test
    @DisplayName("whenFutureThrows_thenHandleMethodReceivesException")
    void whenFutureThrows_thenHandleMethodReceivesException() throws ExecutionException, InterruptedException {
        String name = null;
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            if (name == null) {
                throw new RuntimeException("Computation error!");
            }
            return "Hello, " + name;
        }).handle((s, ex) -> s != null ? s : "Hello, Stranger!");
        assertEquals("Hello, Stranger!", completableFuture.get());
    }

    @Test
    @DisplayName("whenCompletingFutureExceptionlly_thenGetMethodThrows")
    void whenCompletingFutureExceptionlly_thenGetMethodThrows() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        completableFuture.completeExceptionally(new RuntimeException("Calculation failed!"));
        assertThrows(ExecutionException.class, completableFuture::get);
    }

    @Test
    @DisplayName("whenAddingThenApplyAsyncToFuture_thenFunctionExecutesAfterComputationIsFinished")
    void whenAddingThenApplyAsyncToFuture_thenFunctionExecutesAfterComputationIsFinished() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> future = completableFuture.thenApplyAsync(s -> s + " World");
        assertEquals("Hello World", future.get());
    }

    @Test
    @DisplayName("whenPassingTransformation_thenFunctionExecutionWithThenApply")
    void whenPassingTransformation_thenFunctionExecutionWithThenApply() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> finalResult = compute().thenApply(s -> s + 1);
        assertEquals(11, finalResult.get());
    }

    @Test
    @DisplayName("whenPassingPreviousStage_thenFunctionExecutionWithThenCompose")
    void whenPassingPreviousStage_thenFunctionExecutionWithThenCompose() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> finalResult = compute().thenCompose(this::computeAnother);
        assertEquals(20, finalResult.get());
    }

    public CompletableFuture<Integer> compute() {
        return CompletableFuture.supplyAsync(() -> 10);
    }

    public CompletableFuture<Integer> computeAnother(Integer i) {
        return CompletableFuture.supplyAsync(() -> 10 + i);
    }
}