package com.github.crutcher.workshop.common.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common Runtime Executors for various purposes. */
public final class CommonExecutors {
  private static final Logger logger = LoggerFactory.getLogger(CommonExecutors.class);

  private CommonExecutors() {}

  private static final ExecutorService COMMON_CPUBOUND_EXECUTOR = ForkJoinPool.commonPool();

  private static final ExecutorService COMMON_ASYNC_EXECUTOR =
      new ShutdownIgnoringExecutorService(
          Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()));

  @SuppressWarnings("ConstantField")
  @Nullable
  private static ExecutorService COMMON_BLOCKINGIO_EXECUTOR;

  /**
   * Common ExecutorService for CPU-bound tasks (format encoders, parsers, etc).
   *
   * <p>This is an alias for the {@code ForkJoinPool.commonPool()}, which is the default executor
   * for compute-heavy ForkJoin tasks.
   *
   * <p>These tasks burn a single thread for a while, and running more of them than the total number
   * of CPUs will definitely run all tasks slower, as performance is lost in context switching.
   *
   * <p>shutdown() will be ignored by this service.
   *
   * @return the common service.
   */
  public static ExecutorService commonComputePool() {
    return COMMON_CPUBOUND_EXECUTOR;
  }

  /**
   * Common ExecutorService for short lived, non-blocking async tasks.
   *
   * <p>This is a work-stealing pool, sized to match the number of available cpus to avoid context
   * switching costs; but separate from {code commonComputePool()}/{@code
   * ForkJoinPool.commonPool()}.
   *
   * <p>The justification for having a separate pool from the compute bound pool is that these tasks
   * are usually the glue connecting and triggering other async tasks, frequently IO; and we want
   * them to make progress, even if that slows down compute bound tasks.
   *
   * @return the common service.
   */
  public static ExecutorService commonAsyncPool() {
    return COMMON_ASYNC_EXECUTOR;
  }

  /**
   * Get the maximum parallelism of the {@code commonBlockingIoPool()}.
   *
   * <p>The default parallelism is 10 * numProcessors(), but that can be overridden by the System
   * property: "com.github.crutcher.workshop.common.concurrent.CommonExecutors.blockingIoPoolSize"
   *
   * @return the max parallelism.
   */
  public static int getCommonBlockingIoPoolMaxParallelism() {
    Integer maxBlockingThreads = 10 * Runtime.getRuntime().availableProcessors();

    String prop =
        System.getProperty(
            "com.github.crutcher.workshop.common.concurrent.CommonExecutors.blockingIoPoolSize");

    if (prop != null) {
      maxBlockingThreads = Integer.valueOf(maxBlockingThreads);
    }

    return maxBlockingThreads;
  }

  /**
   * Get a common Executor for scheduling synchronous blocking APIs; generally Network blocking or
   * IO blocking calls.
   *
   * <p>The goal of this pool is to permit blocking IO calls (which mostly sleep and don't use the
   * CPU) to grow to a much larger size than the number of CPUs; while still bounding the upper
   * limit of thread stacks produced.
   *
   * <p>See: {@code getCommonBlockingIoPoolMaxParallelism()}
   *
   * @return the common service.
   */
  public static synchronized ExecutorService commonBlockingIoPool() {
    if (COMMON_BLOCKINGIO_EXECUTOR == null) {
      int maxBlockingThreads = getCommonBlockingIoPoolMaxParallelism();

      logger.info("Starting commonBlockingIoPool with maxParallelism: {}", maxBlockingThreads);

      ThreadPoolExecutor threadPoolExecutor =
          new ThreadPoolExecutor(
              maxBlockingThreads,
              maxBlockingThreads,
              60L,
              TimeUnit.SECONDS,
              new LinkedBlockingDeque<>());

      threadPoolExecutor.allowCoreThreadTimeOut(true);

      COMMON_BLOCKINGIO_EXECUTOR = new ShutdownIgnoringExecutorService(threadPoolExecutor);
    }

    return COMMON_BLOCKINGIO_EXECUTOR;
  }
}
