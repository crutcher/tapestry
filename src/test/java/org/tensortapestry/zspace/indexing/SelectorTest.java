package org.tensortapestry.zspace.indexing;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

class SelectorTest implements WithAssertions {

  @Test
  public void testParseSelectorAtom() {
    assertThat(Selector.parseSelectorAtom("...")).isEqualTo(new Selector.Ellipsis());
    assertThat(Selector.parseSelectorAtom("+")).isEqualTo(new Selector.NewAxis());
    assertThat(Selector.parseSelectorAtom("1")).isEqualTo(new Selector.Index(1));

    assertThat(Selector.parseSelectorAtom(":")).isEqualTo(new Selector.Slice(null, null, null));
    assertThat(Selector.parseSelectorAtom("::-2")).isEqualTo(new Selector.Slice(null, null, -2));
    assertThat(Selector.parseSelectorAtom(":2")).isEqualTo(new Selector.Slice(null, 2, null));
    assertThat(Selector.parseSelectorAtom("1:2")).isEqualTo(new Selector.Slice(1, 2, null));
  }

  @Test
  public void test_toString() {
    var selectors = Selector.parseSelectors("1, +100, +1, +, ..., :, 3:, :5, ::-2");
    assertThat(selectors).hasToString("[1, +100, +, +, ..., :, 3:, :5, ::-2]");
  }

  @Test
  public void test_newAxis() {
    assertThat(Selector.newAxis()).isEqualTo(new Selector.NewAxis());
    assertThat(Selector.newAxis(2)).isEqualTo(new Selector.NewAxis(2));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> Selector.newAxis(0))
      .withMessage("Size must be greater than 0: 0");
  }

  @Test
  public void test_slice_builders() {
    assertThat(Selector.sliceBuilder().start(1).build())
      .isEqualTo(new Selector.Slice(1, null, null));
    assertThat(Selector.sliceBuilder().end(2).build()).isEqualTo(new Selector.Slice(null, 2, null));
    assertThat(Selector.sliceBuilder().step(-2).build())
      .isEqualTo(new Selector.Slice(null, null, -2));

    assertThat(Selector.slice()).isEqualTo(new Selector.Slice(null, null, null));
    assertThat(Selector.slice(1, 2)).isEqualTo(new Selector.Slice(1, 2, null));
    assertThat(Selector.slice(1, 2, -2)).isEqualTo(new Selector.Slice(1, 2, -2));
  }

  @Test
  public void testParseSelectors() {
    assertThat(Selector.parseSelectors("1,2,3"))
      .containsExactly(new Selector.Index(1), new Selector.Index(2), new Selector.Index(3));
    assertThat(Selector.parseSelectors("1,2:3"))
      .containsExactly(new Selector.Index(1), new Selector.Slice(2, 3, null));

    assertThat(Selector.parseSelectors("1, +, ..., +, :, ::-2"))
      .containsExactly(
        new Selector.Index(1),
        new Selector.NewAxis(),
        new Selector.Ellipsis(),
        new Selector.NewAxis(),
        Selector.slice(null, null, null),
        Selector.sliceBuilder().step(-2).build()
      );
  }
}
