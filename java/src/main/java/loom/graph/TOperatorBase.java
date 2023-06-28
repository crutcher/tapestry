package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TBlockOperator.class),
      @JsonSubTypes.Type(value = TCellOperator.class),
      @JsonSubTypes.Type(value = TFusionOperator.class),
      @JsonSubTypes.Type(value = TMacroOperator.class),
      @JsonSubTypes.Type(value = TViewOperator.class),
    })
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "cds"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#75DDDD"),
      @TNodeBase.NodeDisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public abstract class TOperatorBase extends TNodeBase
    implements TParameters.THasParametersProperty,
        TNodeInterface {

  @Nonnull @Getter public final String op;

  protected TOperatorBase(@Nullable UUID id, @Nonnull String op) {
    super(id);
    this.op = op;
  }

    @CanIgnoreReturnValue
    public TWithInputEdge bindInput(String label, TTensor tensor) {
      return assertGraph().addNode(new TWithInputEdge(getId(), tensor.id, label));
    }

    @CanIgnoreReturnValue
    public Map<String, TWithInputEdge> bindInputs(Map<String, TTensor> inputs) {
      var edges = new HashMap<String, TWithInputEdge>();
      for (var entry : inputs.entrySet()) {
        edges.put(entry.getKey(), bindInput(entry.getKey(), entry.getValue()));
      }
      return edges;
    }

    public TTensor bindResult(String label, ZPoint shape, String dtype) {
      var g = assertGraph();
      var t = g.addNode(new TTensor(shape, dtype));
      g.addNode(new TResultEdge(t.id, getId(), label));
      return t;
    }

    @JsonTypeName("WithInput")
    @TTagBase.SourceType(TOperatorBase.class)
    @TEdgeBase.TargetType(TTensor.class)
    @NodeDisplayOptions.NodeAttributes(
        value = {@NodeDisplayOptions.Attribute(name = "fillcolor", value = "#DDA6E0")})
    public static final class TWithInputEdge
        extends TSourceKeyedEdge<TWithInputEdge, TOperatorBase, TTensor> {
      @JsonCreator
      public TWithInputEdge(
          @Nullable @JsonProperty(value = "id", required = true) UUID id,
          @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
          @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
          @Nonnull @JsonProperty(value = "key", required = true) String key) {
        super(id, sourceId, targetId, key);
      }

      public TWithInputEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
        this(null, sourceId, targetId, name);
      }

      public TWithInputEdge(@Nonnull TWithInputEdge source) {
        this(source.id, source.sourceId, source.targetId, source.key);
      }

      @Override
      public TWithInputEdge copy() {
        return new TWithInputEdge(this);
      }
    }

    @JsonTypeName("ResultOf")
    @TTagBase.SourceType(TTensor.class)
    @TEdgeBase.TargetType(TOperatorBase.class)
    @NodeDisplayOptions.NodeAttributes(
        value = {@NodeDisplayOptions.Attribute(name = "fillcolor", value = "#A7E1D5")})
    public static final class TResultEdge
        extends TTargetKeyedEdge<TResultEdge, TTensor, TOperatorBase> {
      @JsonCreator
      public TResultEdge(
          @Nullable @JsonProperty(value = "id", required = true) UUID id,
          @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
          @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
          @Nonnull @JsonProperty(value = "key", required = true) String key) {
        super(id, sourceId, targetId, key);
      }

      public TResultEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
        this(null, sourceId, targetId, name);
      }

      public TResultEdge(@Nonnull TResultEdge source) {
        this(source.id, source.sourceId, source.targetId, source.key);
      }

      @Override
      public TResultEdge copy() {
        return new TResultEdge(this);
      }
    }
}
