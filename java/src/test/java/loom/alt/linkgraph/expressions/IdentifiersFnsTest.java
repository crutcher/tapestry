package loom.alt.linkgraph.expressions;

import loom.testing.CommonAssertions;
import org.junit.Test;

public class IdentifiersFnsTest implements CommonAssertions {
  @Test
  public void test_validAtomicIdentifier() {
    for (var identifier : new String[] {"a", "a1", "a1_", "a1_b2_c3"}) {
      assertThat(IdentifiersFns.validAtomicIdentifier(identifier)).isEqualTo(identifier);
    }

    for (var bad : new String[] {"", "1", "1a", "a.1b", "a-b"}) {
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> IdentifiersFns.validAtomicIdentifier(bad));
    }
  }

  @Test
  public void test_validDottedIdentifier() {
    for (var identifier : new String[] {"a", "a1", "a1_", "a1_b2_c3", "a.b", "a1.b3_c5.d7"}) {
      assertThat(IdentifiersFns.validDottedIdentifier(identifier)).isEqualTo(identifier);
    }

    for (var bad : new String[] {"", "1", "1a", "a.1b", "a-b", "a.", ".a"}) {
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> IdentifiersFns.validDottedIdentifier(bad));
    }
  }

  @Test
  public void test_splitDottedIdentifier() {
    assertThat(IdentifiersFns.splitDottedIdentifier("a")).isEqualTo(new String[] {"a"});
    assertThat(IdentifiersFns.splitDottedIdentifier("a.b")).isEqualTo(new String[] {"a", "b"});
    assertThat(IdentifiersFns.splitDottedIdentifier("a.b.c"))
        .isEqualTo(new String[] {"a", "b", "c"});
  }
}
