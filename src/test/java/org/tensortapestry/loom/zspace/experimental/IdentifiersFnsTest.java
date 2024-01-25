package org.tensortapestry.loom.zspace.experimental;

import java.util.List;
import org.junit.Test;
import org.tensortapestry.loom.testing.BaseTestClass;

public class IdentifiersFnsTest extends BaseTestClass {

  public static final List<String> BAD_DOTTED_NAMES = List.of(
    "",
    "1",
    "_",
    "_1",
    "1_",
    "1_1",
    "1_1_",
    "1_1_1",
    ".a",
    "a.",
    "a..b",
    "a.b.",
    "a..b.",
    ".a.b.",
    "a..b.c",
    "a.b..c",
    "a.b.c.",
    "a..b..c",
    ".a.b.c.",
    "a..b..c.",
    "a.b..c.",
    "a.b.c..",
    "a..b..c..",
    ".a.b.c..",
    "a..b..c..",
    "a.b..c..",
    "a.b.c..."
  );
  public static final List<String> BAD_ATOMIC_NAMES = List.of(
    "",
    "1",
    "_",
    "_1",
    "1_",
    "1_1",
    "1_1_",
    "1_1_1"
  );

  @Test
  public void test_validAtomicIdentifier() {
    for (var name : List.of(
      "a",
      "A",
      "a1",
      "A1",
      "a_",
      "A_",
      "a1_",
      "A1_",
      "a_1",
      "A_1",
      "a_1_",
      "A_1_"
    )) {
      assertThat(IdentifiersFns.validAtomicIdentifier(name)).isEqualTo(name);
    }

    for (var name : BAD_ATOMIC_NAMES) {
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IdentifiersFns.validAtomicIdentifier(name))
        .withMessageContaining("invalid atomic identifier: \"%s\"".formatted(name));
    }
  }

  @Test
  public void test_validDottedIdentifier() {
    for (var name : List.of(
      "a",
      "A",
      "a1",
      "A1",
      "a_",
      "A_",
      "a1_",
      "A1_",
      "a_1",
      "A_1",
      "a_1_",
      "A_1_",
      "a.b",
      "A.B",
      "a1.b2",
      "A1.B2",
      "a_.b_",
      "A_.B_",
      "a1_.b2_",
      "A1_.B2_",
      "a_1.b_2",
      "A_1.B_2",
      "a_1_.b_2_",
      "A_1_.B_2_"
    )) {
      assertThat(IdentifiersFns.validDottedIdentifier(name)).isEqualTo(name);
    }

    for (var name : BAD_DOTTED_NAMES) {
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IdentifiersFns.validDottedIdentifier(name))
        .withMessageContaining("invalid dotted identifier: \"%s\"".formatted(name));
    }
  }

  @Test
  public void test_splitDottedIdentifier() {
    assertThat(IdentifiersFns.splitDottedIdentifier("a")).isEqualTo(new String[] { "a" });
    assertThat(IdentifiersFns.splitDottedIdentifier("a.b")).isEqualTo(new String[] { "a", "b" });
    assertThat(IdentifiersFns.splitDottedIdentifier("a.b.c"))
      .isEqualTo(new String[] { "a", "b", "c" });

    for (var name : BAD_DOTTED_NAMES) {
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IdentifiersFns.splitDottedIdentifier(name))
        .withMessageContaining("invalid dotted identifier: \"%s\"".formatted(name));
    }
  }
}
