package com.github.crutcher.workshop.common.concurrent;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonExecutorsTest implements WithAssertions {
  @Test
  public void test_commonComputePool() throws Exception {
    ExecutorService service = CommonExecutors.commonComputePool();

    assertThat(service).isSameAs(ForkJoinPool.commonPool());

    // Should be a no-op.
    service.shutdown();

    assertThat(service.awaitTermination(1, TimeUnit.DAYS)).isFalse();

    // Should still run.
    CompletableFuture.runAsync(() -> {}, service).join();
  }

  @Test
  public void test_commonAsyncPool() throws Exception {
    ExecutorService service = CommonExecutors.commonAsyncPool();

    assertThat(service).isNotSameAs(ForkJoinPool.commonPool());

    // Should be a no-op.
    service.shutdown();

    assertThat(service.awaitTermination(1, TimeUnit.DAYS)).isFalse();

    // Should still run.
    CompletableFuture.runAsync(() -> {}, service).join();
  }

  @Test
  public void test_commonBlockingIoPool() throws Exception {
    var futures = new ArrayList<CompletableFuture<Void>>();

    ExecutorService service = CommonExecutors.commonBlockingIoPool();

    // Should be a no-op.
    service.shutdown();

    int maxParallelism = CommonExecutors.getCommonBlockingIoPoolMaxParallelism();
    int numBatches = 5;
    Duration delay = Duration.ofMillis(50);

    Stopwatch duration = Stopwatch.createStarted();

    for (int i = 0; i < numBatches * maxParallelism; ++i) {
      var f = new CompletableFuture<Void>();
      futures.add(f);

      service.execute(
          () -> {
            try {
              Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }
            f.complete(null);
          });
    }

    // Should be a no-op.
    assertThat(service.shutdownNow()).isEmpty();

    assertThat(service.awaitTermination(1, TimeUnit.DAYS)).isFalse();

    FutureFuncs.allOf(futures).join();

    assertThat(duration.elapsed()).isGreaterThan(delay.multipliedBy(numBatches));
  }
}
