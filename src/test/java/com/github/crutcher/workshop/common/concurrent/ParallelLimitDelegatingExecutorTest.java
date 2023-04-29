package com.github.crutcher.workshop.common.concurrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executor;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class ParallelLimitDelegatingExecutorTest implements WithAssertions {
  @Test
  public void test_schedule() {
    var pending = new ArrayList<Runnable>();
    Executor mockExecutor = pending::add;

    var rateLimitExecutor = new ParallelLimitDelegatingExecutor(mockExecutor, 2);

    var set = new HashSet<String>();

    rateLimitExecutor.execute(() -> set.add("abc"));
    rateLimitExecutor.execute(() -> set.add("jkl"));
    rateLimitExecutor.execute(
        () -> {
          throw new Error("msg");
        });
    rateLimitExecutor.execute(() -> set.add("mno"));

    assertThat(pending).hasSize(2);
    assertThat(set).isEmpty();

    // Run the 'abc' task.
    pending.remove(0).run();

    assertThat(pending).hasSize(2);
    assertThat(set).containsOnly("abc");

    // Run the 'jkl' task.
    pending.remove(0).run();

    assertThat(pending).hasSize(2);
    assertThat(set).containsOnly("abc", "jkl");

    // Run all remaining tasks.
    while (!pending.isEmpty()) {
      pending.remove(0).run();
    }

    assertThat(pending).hasSize(0);
    assertThat(set).containsOnly("abc", "jkl", "mno");
  }

  @Test
  public void test_setLimit() {
    var pending = new ArrayList<Runnable>();
    Executor mockExecutor = pending::add;

    var rateLimitExecutor = new ParallelLimitDelegatingExecutor(mockExecutor, 0);

    var set = new HashSet<String>();

    rateLimitExecutor.execute(() -> set.add("abc"));
    rateLimitExecutor.execute(() -> set.add("jkl"));
    rateLimitExecutor.execute(
        () -> {
          throw new Error("msg");
        });
    rateLimitExecutor.execute(() -> set.add("mno"));

    assertThat(pending).hasSize(0);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> rateLimitExecutor.setParallelLimit(-1))
        .withMessage("negative parallelLimit: -1");

    rateLimitExecutor.setParallelLimit(10);

    assertThat(pending).hasSize(4);

    while (!pending.isEmpty()) {
      pending.remove(0).run();
    }

    assertThat(pending).hasSize(0);
    assertThat(set).containsOnly("abc", "jkl", "mno");
  }
}
