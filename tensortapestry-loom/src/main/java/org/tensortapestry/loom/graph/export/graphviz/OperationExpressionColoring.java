package org.tensortapestry.loom.graph.export.graphviz;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.experimental.Delegate;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.TraversalUtils;

@Data
public class OperationExpressionColoring implements GraphEntityColorSchemeProvider {

  public static GraphEntityColorSchemeProvider colorTensorOperationNodes(
    LoomGraph graph,
    List<Color> colorList
  ) {
    var coloring = TraversalUtils.tensorOperationColoring(graph);
    var k = colorList.size();

    Map<UUID, GraphEntityColorScheme> schemeMap = new HashMap<>();

    for (Map.Entry<Integer, Set<UUID>> colorPair : EnumerationUtils.enumerate(
      coloring.getColorClasses()
    )) {
      int color = colorPair.getKey();
      var colorSet = colorPair.getValue();

      for (var idxId : EnumerationUtils.enumerate(colorSet.stream().sorted().toList())) {
        int idx = idxId.getKey();
        var id = idxId.getValue();

        var h = color % k;
        var baseColor = colorList.get(h);

        var s = (h + idx + 1 + (k / 2)) % k;
        if (s == h) {
          s = (s + 1) % k;
        }
        var stripeColor = colorList.get(s);

        schemeMap.put(id, GraphEntityColorScheme.of(baseColor, stripeColor));
      }
    }

    return SchemeMapColorSchemeProvider.of(schemeMap);
  }

  public static class Builder {

    @Nonnull
    private LoomGraph graph;

    @Nonnull
    private List<Color> colorList = List
      .of("#f43143", "#01be5e", "#088ce4", "#7a50f0", "#a938bc", "#e6518f", "#455d75")
      .stream()
      .map(Color::decode)
      .toList();

    @CanIgnoreReturnValue
    public Builder graph(@Nonnull LoomGraph graph) {
      this.graph = graph;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder colorList(@Nonnull List<Color> colorList) {
      this.colorList = List.copyOf(colorList);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder rgbColorList(@Nonnull List<String> colorList) {
      this.colorList = colorList.stream().map(Color::decode).toList();
      return this;
    }

    public OperationExpressionColoring build() {
      return new OperationExpressionColoring(Objects.requireNonNull(graph, "graph"), colorList);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @Nonnull
  private final LoomGraph graph;

  @Nonnull
  private final List<Color> colorList;

  @Delegate
  @Nonnull
  private final GraphEntityColorSchemeProvider colorSchemes;

  public OperationExpressionColoring(@Nonnull LoomGraph graph, @Nonnull List<Color> colorList) {
    this.graph = graph;
    this.colorList = List.copyOf(colorList);
    this.colorSchemes = colorTensorOperationNodes(graph, colorList);
  }
}
