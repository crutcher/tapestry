package com.github.crutcher.workshop.common.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ShutdownIgnoringExecutorServiceTest implements WithAssertions {
  @Test
  public void test_delegate() throws Exception {
    ExecutorService mock = Mockito.mock(ExecutorService.class);

    ShutdownIgnoringExecutorService service = new ShutdownIgnoringExecutorService(mock);

    // Ignored/not-delegated.
    service.shutdown();
    Mockito.verify(mock, Mockito.never()).shutdown();

    assertThat(service.shutdownNow()).isEmpty();
    Mockito.verify(mock, Mockito.never()).shutdownNow();

    assertThat(service.awaitTermination(1, TimeUnit.DAYS)).isFalse();
    Mockito.verify(mock, Mockito.never()).awaitTermination(1, TimeUnit.DAYS);

    {
      Runnable runnable = () -> {};

      service.execute(runnable);
      Mockito.verify(mock).execute(runnable);
    }

    {
      Callable<Integer> callable = () -> 12;
      CompletableFuture<Integer> f = new CompletableFuture<>();

      Mockito.when(mock.submit(callable)).thenReturn(f);

      assertThat(service.submit(callable)).isSameAs(f);
      Mockito.verify(mock).submit(callable);
    }
  }
}
