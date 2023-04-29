package com.github.crutcher.workshop.apitest.java.util.concurrent;

import com.github.crutcher.workshop.common.concurrent.FutureFuncs;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

/** Test probing the incompletely documented behavior of the CompletableFuture api. */
public class VerifyCompletableFutureTest implements WithAssertions {

  /**
   * Checked Exceptions (or any Throwable) can be thrown by CompletableFuture handlers by wrapping
   * them in {@code CompletionException}s.
   *
   * <p>The wrapped Throwables appear on the resulting CompletableFuture as though they'd been
   * thrown directly; without a double layer of exception wrapping.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void verify_Throwable_encapsulation() {
    {
      // This is a normal error, just throwing an un-checked exception.
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                throw new Error("msg");
              });

      assertThat(FutureFuncs.wait(future))
          .hasFailedWithThrowableThat()
          .isInstanceOf(Error.class)
          .hasMessage("msg");

      // When join()ed, the error is wrapped in CompletionException.
      assertThatExceptionOfType(CompletionException.class)
          .isThrownBy(future::join)
          .withCause(new Error("msg"));

      // When get()ed, the error is wrapped in CompletionException.
      assertThatExceptionOfType(ExecutionException.class)
          .isThrownBy(future::get)
          .withCause(new Error("msg"));
    }

    {
      // This is a wrapped exception, note that the result is not double-wrapped.
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                throw new CompletionException(new Exception("msg"));
              });

      assertThat(FutureFuncs.wait(future))
          .hasFailedWithThrowableThat()
          .isInstanceOf(Exception.class)
          .hasMessage("msg");

      // When join()ed, the error is wrapped in CompletionException.
      assertThatExceptionOfType(CompletionException.class)
          .isThrownBy(future::join)
          .withCause(new Exception("msg"));

      // When get()ed, the error is wrapped in CompletionException.
      assertThatExceptionOfType(ExecutionException.class)
          .isThrownBy(future::get)
          .withCause(new Exception("msg"));
    }
  }

  /**
   * The {code CompletableFuture.{submit,run}Async} methods capture Throwables and prevent them from
   * escaping their Executors.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void verify_supplyAsync_exceptionPropagation() {
    var pending = new ArrayList<Runnable>();

    // Executor.submit(Runnable) will accumulate tasks.
    Executor executor = pending::add;

    // These task succeed.
    CompletableFuture<Integer> supplySuccess = CompletableFuture.supplyAsync(() -> 12, executor);
    CompletableFuture<Void> runSuccess = CompletableFuture.runAsync(() -> {}, executor);

    // These tasks fail with a messy error.
    CompletableFuture<String> supplyFailure =
        CompletableFuture.supplyAsync(
            () -> {
              throw new Error("msg");
            },
            executor);
    CompletableFuture<Void> runFailure =
        CompletableFuture.runAsync(
            () -> {
              throw new Error("msg");
            },
            executor);

    // All tasks have been scheduled, but not run.
    assertThat(pending).hasSize(4);

    // Neither future is done.
    assertThat(supplySuccess).isNotDone();
    assertThat(supplyFailure).isNotDone();
    assertThat(runSuccess).isNotDone();
    assertThat(runFailure).isNotDone();

    // Run all tasks; none will throw.
    pending.forEach(Runnable::run);

    assertThat(supplySuccess).isCompletedWithValue(12);
    assertThat(runSuccess).isCompleted();

    assertThat(supplyFailure)
        .hasFailedWithThrowableThat()
        .isInstanceOf(Error.class)
        .hasMessage("msg");

    assertThat(runFailure).hasFailedWithThrowableThat().isInstanceOf(Error.class).hasMessage("msg");
  }
}
