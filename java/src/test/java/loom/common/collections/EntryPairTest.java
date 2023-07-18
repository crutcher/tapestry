package loom.common.collections;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class EntryPairTest implements CommonAssertions {
  @Test
  public void testOf() {
    EntryPair<String, Integer> pair = EntryPair.of("foo", 42);
    assertThat(pair.getKey()).isEqualTo("foo");
    assertThat(pair.getValue()).isEqualTo(42);
  }

  @Test
  public void testToString() {
    EntryPair<String, Integer> pair = EntryPair.of("foo", 42);
    assertThat(pair.toString()).isEqualTo("foo=42");
  }

  @Test
  public void testMutateEqualsHash() {
    EntryPair<String, Integer> pair = EntryPair.of("foo", 42);

    assertThat(pair).isEqualTo(EntryPair.of("foo", 42)).hasSameHashCodeAs(EntryPair.of("foo", 42));

    assertThat(pair.setValue(43)).isEqualTo(42);
    assertThat(pair.getValue()).isEqualTo(43);

    assertThat(pair).isEqualTo(EntryPair.of("foo", 43)).hasSameHashCodeAs(EntryPair.of("foo", 43));
  }
}
