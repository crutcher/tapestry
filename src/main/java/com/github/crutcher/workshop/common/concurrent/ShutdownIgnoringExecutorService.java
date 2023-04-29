package com.github.crutcher.workshop.common.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** ExecutorService wrapper which ignores shutdown(). */
public class ShutdownIgnoringExecutorService implements ExecutorService {

  private final ExecutorService delegate;

  public ShutdownIgnoringExecutorService(ExecutorService delegate) {
    this.delegate = delegate;
  }

  /**
   * Get the wrapped ExecutorService.
   */
  public ExecutorService getDelegate() {
    return delegate;
  }

  /** The wrapped version is a no-op. */
  @Override
  public void shutdown() {
    // Ignore.
  }

  /** The wrapped version is a no-op, and always returns a new empty list. */
  @Override
  public List<Runnable> shutdownNow() {
    // Ignore.
    return new ArrayList<>();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  /** The wrapped version is a no-op, and always returns false immediately. */
  @Override
  public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
    return false;
  }

  @Override
  public <T> Future<T> submit(Callable<T> callable) {
    return delegate.submit(callable);
  }

  @Override
  public <T> Future<T> submit(Runnable runnable, T t) {
    return delegate.submit(runnable, t);
  }

  @Override
  public Future<?> submit(Runnable runnable) {
    return delegate.submit(runnable);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection)
      throws InterruptedException {
    return delegate.invokeAll(collection);
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit)
      throws InterruptedException {
    return delegate.invokeAll(collection, l, timeUnit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> collection)
      throws InterruptedException, ExecutionException {
    return delegate.invokeAny(collection);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(collection, l, timeUnit);
  }

  @Override
  public void execute(Runnable runnable) {
    delegate.execute(runnable);
  }
}
