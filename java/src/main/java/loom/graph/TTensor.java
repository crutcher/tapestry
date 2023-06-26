package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@JsonTypeName("Tensor")
@TNodeBase.DisplayOptions.NodeAttributes(
        value = {
                @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "box3d"),
                @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#d0d0ff")
        })
public class TTensor extends TNodeBase {
    @Nonnull
    @Getter
    public final ZPoint shape;

    @Nonnull
    @Getter
    public final String dtype;

    public TTensor(@Nullable UUID id, @Nonnull ZPoint shape, @Nonnull String dtype) {
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

    public interface TConsumesInputsProperty extends TNodeInterface {
        @CanIgnoreReturnValue
        default TConsumesEdge bindInput(TTensor tensor, String label) {
            return assertGraph().addNode(new TConsumesEdge(getId(), tensor.id, label));
        }
    }

    @JsonTypeName("Consumes")
    @TTagBase.SourceType(TConsumesInputsProperty.class)
    @TEdgeBase.TargetType(TTensor.class)
    @DisplayOptions.NodeAttributes(
            value = {@DisplayOptions.Attribute(name = "fillcolor", value = "#DDA6E0")})
    public static final class TConsumesEdge extends TEdgeBase<TConsumesInputsProperty, TTensor> {
        @Nonnull
        @Getter
        private final String name;

        @JsonCreator
        public TConsumesEdge(
                @Nullable @JsonProperty(value = "id", required = true) UUID id,
                @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId,
                @Nonnull @JsonProperty(value = "targetId", required = true) UUID targetId,
                @Nonnull @JsonProperty(value = "name", required = true) String name) {
            super(id, sourceId, targetId);
            this.name = name;
        }

        public TConsumesEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId, @Nonnull String name) {
            this(null, sourceId, targetId, name);
        }

        public TConsumesEdge(@Nonnull TConsumesEdge source) {
            this(source.id, source.sourceId, source.targetId, source.name);
        }

        @Override
        public TConsumesEdge copy() {
            return new TConsumesEdge(this);
        }

        @Override
        public Map<String, Object> displayData() {
            var data = super.displayData();
            if (name.equals("input")) {
                data.remove("name");
            }
            return data;
        }
    }

    public interface TYieldsResultsProperty extends TNodeInterface {
        default TTensor bindResult(ZPoint shape, String dtype, String label) {
            var g = assertGraph();
            var t = g.addNode(new TTensor(shape, dtype));
            g.addNode(new TResultEdge(t.id, getId(), label));
            return t;
        }
    }

    @JsonTypeName("ResultOf")
    @TTagBase.SourceType(TTensor.class)
    @TEdgeBase.TargetType(TYieldsResultsProperty.class)
    @DisplayOptions.NodeAttributes(
            value = {@DisplayOptions.Attribute(name = "fillcolor", value = "#A7E1D5")})
    public static final class TResultEdge extends TEdgeBase<TTensor, TYieldsResultsProperty> {
        @Nonnull
        @Getter
        private final String name;

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
    }
}
