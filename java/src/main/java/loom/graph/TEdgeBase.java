package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@TEdgeBase.TargetType(TNodeBase.class)
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TCanBeSequencedProperty.TWaitsOnEdge.class),
                @JsonSubTypes.Type(value = TTensor.TResultEdge.class),
                @JsonSubTypes.Type(value = TTensor.TWithInputEdge.class),
                @JsonSubTypes.Type(value = TParameters.TWithParametersEdge.class),
                @JsonSubTypes.Type(value = TBlockIndex.TWithIndexEdge.class),
        })
@TEdgeBase.EdgeDisplayOptions.ConstrainEdge(true)
public abstract class TEdgeBase<S extends TNodeInterface, T extends TNodeInterface>
        extends TTagBase<S> {
    public static class EdgeDisplayOptions {
        public final boolean constrainEdge;

        public EdgeDisplayOptions(Class<? extends TEdgeBase<?, ?>> cls) {
            List<Class<?>> superclasses = new ArrayList<>();
            Class<?> tmp = cls;
            while (tmp != null) {
                superclasses.add(tmp);
                tmp = tmp.getSuperclass();
            }
            Collections.reverse(superclasses);

            boolean constrain = true;
            for (var c : superclasses) {
                var na = c.getDeclaredAnnotation(TEdgeBase.EdgeDisplayOptions.ConstrainEdge.class);
                if (na != null) {
                    constrain = na.value();
                }
            }
            constrainEdge = constrain;
        }

        @Inherited
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        public @interface ConstrainEdge {
            boolean value();
        }
    }

    /**
     * Runtime annotation to specify the target type of an TEdge.
     *
     * <p>Inherited so that subclasses of TEdge can inherit the annotation.
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TargetType {
        Class<? extends TNodeInterface> value();
    }

    /**
     * For a given TEdge class, return the target type.
     *
     * @param cls the TEdge class.
     * @return the target type class.
     */
    public static Class<? extends TNodeInterface> getTargetType(
            Class<? extends TEdgeBase<?, ?>> cls) {
        return cls.getAnnotation(TargetType.class).value();
    }

    /**
     * The id of the target node.
     */
    @Getter
    @Nonnull
    @JsonProperty(required = true)
    public final UUID targetId;

    public TEdgeBase(@Nullable UUID id, @Nonnull UUID sourceId, @Nonnull UUID targetId) {
        super(id, sourceId);
        this.targetId = targetId;
    }

    public TEdgeBase(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
        this(null, sourceId, targetId);
    }

    @Nonnull
    public EdgeDisplayOptions edgeDisplayOptions() {
        @SuppressWarnings("unchecked")
        var clz = (Class<? extends TEdgeBase<?, ?>>) getClass();
        return new EdgeDisplayOptions(clz);
    }

    @Override
    public void validate() {
        super.validate();
        getTarget();
    }

    @JsonIgnore
    public final T getTarget() {
        @SuppressWarnings("unchecked")
        var typ = (Class<T>) getTargetType((Class<TEdgeBase<S, T>>) getClass());
        return assertGraph().lookupNode(targetId, typ);
    }
}
