package loom.alt.densegraph;

import java.util.List;
import java.util.Map;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.alt.linkgraph.expressions.DimensionMap;
import loom.alt.linkgraph.expressions.IndexProjectionFunction;

@Value
@Jacksonized
@SuperBuilder
public class EGPolyhedralSignature {
  public DimensionMap indexMap;

  @Singular public Map<String, List<IndexProjectionFunction>> inputProjections;

  @Singular public Map<String, List<IndexProjectionFunction>> outputProjections;
}
