package com.github.crutcher.workshop.common.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class FutureFuncsTest implements WithAssertions {
  @Test
  public void test_wait_success() {
    var count = new AtomicInteger();

    CompletableFuture<Integer> f = CompletableFuture.supplyAsync(count::incrementAndGet);

    assertThat(FutureFuncs.wait(f)).isSameAs(f).isCompletedWithValue(1);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_wait_failure() {
    CompletableFuture<Integer> f =
        CompletableFuture.supplyAsync(
            () -> {
              throw new AssertionError("boom");
            });

    assertThat(FutureFuncs.wait(f))
        .isSameAs(f)
        .hasFailedWithThrowableThat()
        .isInstanceOf(AssertionError.class)
        .hasMessage("boom");
  }

  @Test
  public void test_allOf_success() {
    var f1 = new CompletableFuture<Integer>();
    var f2 = new CompletableFuture<Integer>();

    var futures = List.of(f1, f2);

    CompletableFuture<Void> all = FutureFuncs.allOf(futures);
    assertThat(all).isNotDone();

    f1.complete(12);
    assertThat(all).isNotDone();

    f2.complete(3);

    assertThat(all).isCompleted();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_allOf_failure() {
    var f1 = new CompletableFuture<Integer>();
    var f2 = new CompletableFuture<Integer>();

    var futures = List.of(f1, f2);

    CompletableFuture<Void> all = FutureFuncs.allOf(futures);
    assertThat(all).isNotDone();

    f1.completeExceptionally(new AssertionError("msg"));
    assertThat(all).isNotDone();

    f2.complete(3);

    assertThat(all)
        .hasFailedWithThrowableThat()
        .isInstanceOf(AssertionError.class)
        .hasMessage("msg");
  }
}
