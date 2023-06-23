package loom.graph;

import com.fasterxml.jackson.annotation.*;
import loom.common.HasToJsonString;
import loom.common.IdUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TTagBase.class),
                @JsonSubTypes.Type(value = TOperatorBase.class),
                @JsonSubTypes.Type(value = TSequencePoint.class),
        })
@TNodeBase.DisplayOptions.BackgroundColor("#ffffff")
public abstract class TNodeBase implements HasToJsonString {
    public static class DisplayOptions {
        private DisplayOptions() {
        }

        @Inherited
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        public @interface BackgroundColor {
            String value() default "";
        }
    }

    @Nullable
    @JsonIgnore
    TGraph graph = null;

    @Nonnull
    @JsonProperty(required = true)
    public final UUID id;

    TNodeBase(@Nullable UUID id) {
        this.id = IdUtils.coerceUUID(id);
    }

    TNodeBase() {
        this(null);
    }

    public String displayBackgroundColor() {
        return getClass().getAnnotation(DisplayOptions.BackgroundColor.class).value();
    }

    public String jsonTypeName() {
        return getClass().getAnnotation(JsonTypeName.class).value();
    }

    public final boolean hasGraph() {
        return graph != null;
    }

    public final TGraph assertGraph() {
        if (graph == null) {
            throw new IllegalStateException("Node is not part of a graph");
        }
        return graph;
    }

    public void validate() {
        assertGraph();
    }

    public abstract TNodeBase copy();
}
