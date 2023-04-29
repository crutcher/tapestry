package com.github.crutcher.workshop.common.concurrent;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public final class FutureFuncs {
  private FutureFuncs() {}

  /**
   * Block this thread waiting for the CompletableFuture to complete; but swallow all exceptions.
   *
   * @param future the future to wait on.
   * @return the same future, completed either normally or exceptionally.
   */
  @CanIgnoreReturnValue
  public static <T> CompletableFuture<T> wait(CompletableFuture<T> future) {
    try {
      future.join();
    } catch (Throwable t) {
      // Swallow any exceptions.
    }
    return future;
  }

  /**
   * Returns a new CompletableFuture that is completed when all of the given CompletableFutures
   * complete.
   *
   * <p>CompletableFuture.allOf for Collections.
   *
   * @param futures collection of futures.
   * @return a new CompletableFuture.
   */
  public static <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> futures) {
    return CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new));
  }
}
