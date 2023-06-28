package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonTypeName("Tensor")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
        value = {
                @TNodeBase.NodeDisplayOptions.Attribute(name = "shape", value = "box3d"),
                @TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#d0d0ff")
        })
public class TTensor extends TNodeBase {
    @Nonnull
    @Getter
    public final ZPoint shape;

    @Nonnull
    @Getter
    public final String dtype;

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
        default TWithInputEdge bindInput(String label, TTensor tensor) {
            return assertGraph().addNode(new TWithInputEdge(getId(), tensor.id, label));
        }

        @CanIgnoreReturnValue
        default Map<String, TWithInputEdge> bindInputs(Map<String, TTensor> inputs) {
            var edges = new HashMap<String, TWithInputEdge>();
            for (var entry : inputs.entrySet()) {
                edges.put(entry.getKey(), bindInput(entry.getKey(), entry.getValue()));
            }
            return edges;
        }
    }

    @JsonTypeName("WithInput")
    @TTagBase.SourceType(THasInputsProperty.class)
    @TEdgeBase.TargetType(TTensor.class)
    @NodeDisplayOptions.NodeAttributes(
            value = {@NodeDisplayOptions.Attribute(name = "fillcolor", value = "#DDA6E0")})
    public static final class TWithInputEdge
            extends TSourceKeyedEdge<TWithInputEdge, THasInputsProperty, TTensor> {
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

    public interface THasResultsProperty extends TNodeInterface {
        default TTensor bindResult(String label, ZPoint shape, String dtype) {
            var g = assertGraph();
            var t = g.addNode(new TTensor(shape, dtype));
            g.addNode(new TResultEdge(t.id, getId(), label));
            return t;
        }
    }

    @JsonTypeName("ResultOf")
    @TTagBase.SourceType(TTensor.class)
    @TEdgeBase.TargetType(THasResultsProperty.class)
    @NodeDisplayOptions.NodeAttributes(
            value = {@NodeDisplayOptions.Attribute(name = "fillcolor", value = "#A7E1D5")})
    public static final class TResultEdge
            extends TTargetKeyedEdge<TResultEdge, TTensor, THasResultsProperty> {
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
