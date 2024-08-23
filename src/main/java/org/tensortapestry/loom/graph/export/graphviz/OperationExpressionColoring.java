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
public class OperationExpressionColoring implements EntityColorSchemeEnv {

  public static EntityColorSchemeEnv colorTensorOperationNodes(
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

    return SchemeMapColorSchemeEnv.of(schemeMap);
  }

  public static class Builder {

    public static List<Color> decodeRgbColorList(List<String> colorList) {
      return colorList.stream().map(Color::decode).toList();
    }

    @Nonnull
    private List<Color> colorList = decodeRgbColorList(
      List.of("#f43143", "#01be5e", "#088ce4", "#7a50f0", "#a938bc", "#e6518f", "#455d75")
    );

    @CanIgnoreReturnValue
    public Builder colorList(@Nonnull List<Color> colorList) {
      this.colorList = List.copyOf(colorList);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder rgbColorList(@Nonnull List<String> rgbList) {
      return colorList(decodeRgbColorList(rgbList));
    }

    public OperationExpressionColoring build(@Nonnull LoomGraph graph) {
      return new OperationExpressionColoring(graph, colorList);
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
  private final EntityColorSchemeEnv colorSchemes;

  public OperationExpressionColoring(@Nonnull LoomGraph graph, @Nonnull List<Color> colorList) {
    this.graph = graph;
    this.colorList = List.copyOf(colorList);
    this.colorSchemes = colorTensorOperationNodes(graph, colorList);
  }
}
