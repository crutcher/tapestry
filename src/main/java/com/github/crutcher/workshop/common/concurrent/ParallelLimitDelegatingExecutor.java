package com.github.crutcher.workshop.common.concurrent;

import com.google.common.base.Preconditions;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executor which limits parallel Runnable execution delegated to another Executor.
 *
 * <p>In high-throughput applications, it is frequently useful to rate-parallelLimit the execution
 * of particular asynchronous tasks and task families; traditionally this is achieved by giving each
 * of these tasks and task families their own thread pool of some form. In high-throughput /
 * high-parallelism applications, the overhead of these seperated threadpools (and the stack memory
 * and cpu context switch cost associated with them) can easily dominate the resources.
 *
 * <p>This executor permits us to establish rate-limits for task families, without dedicating pools
 * for those families.
 */
public class ParallelLimitDelegatingExecutor implements Executor {
  private final AtomicInteger inFlightCount = new AtomicInteger(0);
  private final Queue<Runnable> pendingRunnables = new ConcurrentLinkedQueue<>();
  private final Executor executor;
  private int parallelLimit;

  /**
   * Constructs a ParallelLimitDelegatingExecutor.
   *
   * @param executor the Executor to delegate to.
   * @param parallelLimit the parallelLimit, >= 0.
   */
  public ParallelLimitDelegatingExecutor(Executor executor, int parallelLimit) {
    this.executor = executor;
    setParallelLimit(parallelLimit);
  }

  /**
   * Get the current parallelLimit.
   *
   * @return the current parallelLimit.
   */
  public synchronized int getParallelLimit() {
    return parallelLimit;
  }

  /**
   * Set the parallelLimit.
   *
   * @param parallelLimit the new parallelLimit, >= 0.
   */
  public synchronized void setParallelLimit(int parallelLimit) {
    Preconditions.checkArgument(parallelLimit >= 0, "negative parallelLimit: %s", parallelLimit);
    this.parallelLimit = parallelLimit;
    scheduleBacklog();
  }

  @Override
  public synchronized void execute(Runnable runnable) {
    if (inFlightCount.get() < parallelLimit) {
      delegate(runnable);
    } else {
      pendingRunnables.add(runnable);
    }
  }

  private void delegate(Runnable runnable) {
    inFlightCount.incrementAndGet();

    CompletableFuture<Void> ignored =
        CompletableFuture.runAsync(runnable, executor)
            .whenComplete(
                (r, t) -> {
                  inFlightCount.decrementAndGet();
                  scheduleBacklog();
                });
  }

  private void scheduleBacklog() {
    while (!pendingRunnables.isEmpty() && inFlightCount.get() < parallelLimit) {
      delegate(pendingRunnables.remove());
    }
  }
}
