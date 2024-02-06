package org.tensortapestry.weft.metakernels.expressions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class DimShapePatternTest implements CommonAssertions {

    @Test
    public void test_parse() {
        var source = "[*batch, shape=[height, width], channels]";
        var matcher = DimShapeMatcher.parse(source);
        assertThat(matcher).hasToString(source);

        assertThat(matcher.getPatterns())
                .containsExactly(
                        DimShapeMatcher.ShapePattern.DimRange.star("batch"),
                        new DimShapeMatcher.ShapePattern.DimGroup(
                                "shape",
                                List.of(
                                        new DimShapeMatcher.ShapePattern.NamedDim("height"),
                                        new DimShapeMatcher.ShapePattern.NamedDim("width")
                                )
                        ),
                        new DimShapeMatcher.ShapePattern.NamedDim("channels")
                );

        var shape = new int[]{100, 20, 5, 10, 255, 255, 3};

        DimShapeMatcher.DimMatchIndex matchIndex = matcher.layout(shape.length);
        assertThat(matchIndex)
                .isEqualTo(
                        DimShapeMatcher.DimMatchIndex
                                .builder()
                                .size(7)
                                .dim("height", 4)
                                .dim("width", 5)
                                .dim("channels", 6)
                                .group("batch", List.of(0, 1, 2, 3))
                                .group("shape", List.of(4, 5))
                                .build()
                );

        assertThat(matchIndex.toShapeIndex(shape))
                .isEqualTo(
                        DimShapeMatcher.DimShapeIndex
                                .builder()
                                .size(7)
                                .dim("height", 255)
                                .dim("width", 255)
                                .dim("channels", 3)
                                .group("batch", List.of(100, 20, 5, 10))
                                .group("shape", List.of(255, 255))
                                .build()
                );
    }

    @Test
    public void test_parse_deep() {
        var source = "[a, b=[x, y=[j, +k]], c]";
        var matcher = DimShapeMatcher.parse(source);

        assertThat(matcher.getPatterns())
                .containsExactly(
                        new DimShapeMatcher.ShapePattern.NamedDim("a"),
                        new DimShapeMatcher.ShapePattern.DimGroup(
                                "b",
                                List.of(
                                        new DimShapeMatcher.ShapePattern.NamedDim("x"),
                                        new DimShapeMatcher.ShapePattern.DimGroup(
                                                "y",
                                                List.of(
                                                        new DimShapeMatcher.ShapePattern.NamedDim("j"),
                                                        DimShapeMatcher.ShapePattern.DimRange.plus("k")
                                                )
                                        )
                                )
                        ),
                        new DimShapeMatcher.ShapePattern.NamedDim("c")
                );

        assertThat(matcher.layout(7))
                .isEqualTo(
                        DimShapeMatcher.DimMatchIndex
                                .builder()
                                .size(7)
                                .dim("a", 0)
                                .dim("x", 1)
                                .dim("j", 2)
                                .group("k", List.of(3, 4, 5))
                                .dim("c", 6)
                                .group("b", List.of(1, 2, 3, 4, 5))
                                .group("y", List.of(2, 3, 4, 5))
                                .build()
                );
    }

    @Test
    public void test_too_many_ranges() {
        var source = "[*batch, shape=[height, +width], channels, *extra]";
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DimShapeMatcher.parse(source))
                .withMessage(
                        "More than one range: [*batch, +width, *extra] :: [*batch, shape=[height, +width], channels, *extra]"
                );
    }

    @Test
    public void test_duplicate_names() {
        var source = "[*batch, shape=[height, width], channels, batch, width]";
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DimShapeMatcher.parse(source))
                .withMessage(
                        "Duplicate names: [batch, width] :: [*batch, shape=[height, width], channels, batch, width]"
                );
    }
}
