package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.zspace.ZPoint;

@JsonTypeName("Tensor")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "box3d"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#d0d0ff")
    })
public class TTensor extends TNodeBase {
  @Nonnull @Getter public final ZPoint shape;

  @Nonnull @Getter public final String dtype;

  @JsonCreator
  public TTensor(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "shape", required = true) ZPoint shape,
      @Nonnull @JsonProperty(value = "dtype", required = true) String dtype) {
    super(id);
    this.shape = shape;
    this.dtype = dtype;
  }

  public TTensor(@Nonnull ZPoint shape, @Nonnull String dtype) {
    this(null, shape, dtype);
  }

  public TTensor(@Nonnull TTensor source) {
    this(source.id, source.shape, source.dtype);
  }

  @Override
  public TTensor copy() {
    return new TTensor(this);
  }

  public interface THasInputsProperty extends TNodeInterface {
    @CanIgnoreReturnValue
    default TInputEdge bindInput(String label, TTensor tensor) {
      return assertGraph().addNode(new TInputEdge(getId(), tensor.id, label));
    }

    @CanIgnoreReturnValue
    default Map<String, TInputEdge> bindInputs(Map<String, TTensor> inputs) {
      var edges = new HashMap<String, TInputEdge>();
      for (var entry : inputs.entrySet()) {
        edges.put(entry.getKey(), bindInput(entry.getKey(), entry.getValue()));
      }
      return edges;
    }
  }

  @JsonTypeName("Input")
  @TTagBase.SourceType(THasInputsProperty.class)
  @TEdgeBase.TargetType(TTensor.class)
  @DisplayOptions.NodeAttributes(
      value = {@DisplayOptions.Attribute(name = "fillcolor", value = "#DDA6E0")})
  public static final class TInputEdge extends TEdgeBase<THasInputsProperty, TTensor> {
    @Nonnull @Getter private final String name;

    @JsonCreator
    public TInputEdge(
        @Nullable @JsonProperty(value = "id", required = true) UUID id,
        @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
        @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
        @Nonnull @JsonProperty(value = "name", required = true) String name) {
      super(id, sourceId, targetId);
      this.name = name;
    }

    public TInputEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
      this(null, sourceId, targetId, name);
    }

    public TInputEdge(@Nonnull TInputEdge source) {
      this(source.id, source.sourceId, source.targetId, source.name);
    }

    @Override
    public TInputEdge copy() {
      return new TInputEdge(this);
    }

    @Override
    public Map<String, Object> displayData() {
      var data = super.displayData();
      if (name.equals("input")) {
        data.remove("name");
      }
      return data;
    }

    @Override
    public void validate() {
      super.validate();
      var conflictingEdges =
          assertGraph()
              .queryEdges(TInputEdge.class)
              .withSourceId(getSourceId())
              .withFilter(e -> e.getName().equals(getName()))
              .toList();
      if (conflictingEdges.size() > 1) {
        throw new IllegalStateException("Conflicting input names: " + conflictingEdges);
      }
    }
  }

  public interface TYieldsResultsProperty extends TNodeInterface {
    default TTensor bindResult(String label, ZPoint shape, String dtype) {
      var g = assertGraph();
      var t = g.addNode(new TTensor(shape, dtype));
      g.addNode(new TResultEdge(t.id, getId(), label));
      return t;
    }

    @JsonIgnore
    @CanIgnoreReturnValue
    default Map<String, TTensor> getResults() {
      return assertGraph()
          .queryEdges(TResultEdge.class)
          .withTargetId(getId())
          .toStream()
          .collect(Collectors.toMap(TResultEdge::getName, TResultEdge::getSource));
    }
  }

  @JsonTypeName("ResultOf")
  @TTagBase.SourceType(TTensor.class)
  @TEdgeBase.TargetType(TYieldsResultsProperty.class)
  @DisplayOptions.NodeAttributes(
      value = {@DisplayOptions.Attribute(name = "fillcolor", value = "#A7E1D5")})
  public static final class TResultEdge extends TEdgeBase<TTensor, TYieldsResultsProperty> {
    @Nonnull @Getter private final String name;

    @JsonCreator
    public TResultEdge(
        @Nullable @JsonProperty(value = "id", required = true) UUID id,
        @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
        @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
        @Nonnull @JsonProperty(value = "name", required = true) String name) {
      super(id, sourceId, targetId);
      this.name = name;
    }

    public TResultEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
      this(null, sourceId, targetId, name);
    }

    public TResultEdge(@Nonnull TResultEdge source) {
      this(source.id, source.sourceId, source.targetId, source.name);
    }

    @Override
    public TResultEdge copy() {
      return new TResultEdge(this);
    }

    @Override
    public Map<String, Object> displayData() {
      var data = super.displayData();
      if (name.equals("result")) {
        data.remove("name");
      }
      return data;
    }

    @Override
    public void validate() {
      super.validate();
      var conflictingEdges =
          assertGraph()
              .queryEdges(TResultEdge.class)
              .withTargetId(getTargetId())
              .withFilter(e -> e.getName().equals(getName()))
              .toList();
      if (conflictingEdges.size() > 1) {
        throw new IllegalStateException("Conflicting result names: " + conflictingEdges);
      }
    }
  }
}
