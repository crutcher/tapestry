package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("Parameters")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "tab"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#E7DCB8"),
      @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public class TParameters extends TNodeBase {
  @Nonnull @Getter public final Map<String, String> params;

  @JsonCreator
  public TParameters(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nullable @JsonProperty(value = "params") Map<String, String> params) {
    super(id);

    if (params == null) {
      this.params = Map.of();
    } else {
      this.params = Map.copyOf(params);
    }
  }

  public TParameters(@Nullable Map<String, String> params) {
    this(null, params);
  }

  public TParameters(@Nonnull TParameters source) {
    this(source.id, source.params);
  }

  @Override
  public TParameters copy() {
    return new TParameters(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    data.remove("params");
    for (var entry : params.entrySet()) {
      data.put("@" + entry.getKey(), entry.getValue());
    }
    return data;
  }

  public interface THasParametersProperty extends TNodeInterface {
    default TParameters bindParameters(TParameters parameters) {
      // TODO: validate/update singleton
      assertGraph().addNode(new TWithParametersEdge(getId(), parameters.id));
      return parameters;
    }

    @CanIgnoreReturnValue
    default TParameters bindParameters(Map<String, String> params) {
      return bindParameters(assertGraph().addNode(new TParameters(params)));
    }
  }

  @JsonTypeName("With")
  @TTagBase.SourceType(THasParametersProperty.class)
  @TEdgeBase.TargetType(TParameters.class)
  public static final class TWithParametersEdge extends TEdgeBase<TOperatorBase, TParameters> {
    @JsonCreator
    public TWithParametersEdge(
        @Nullable @JsonProperty(value = "id", required = true) UUID id,
        @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
        @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId) {
      super(id, sourceId, targetId);
    }

    public TWithParametersEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
      this(null, sourceId, targetId);
    }

    public TWithParametersEdge(@Nonnull TWithParametersEdge source) {
      this(source.id, source.sourceId, source.targetId);
    }

    @Override
    public TWithParametersEdge copy() {
      return new TWithParametersEdge(this);
    }

    @Override
    public void validate() {
      super.validate();
      var conflictingEdges =
          assertGraph().queryEdges(TWithParametersEdge.class).withSourceId(getSourceId()).toList();
      if (conflictingEdges.size() > 1) {
        throw new IllegalStateException("Multiple parameters: " + conflictingEdges);
      }
    }
  }
}
