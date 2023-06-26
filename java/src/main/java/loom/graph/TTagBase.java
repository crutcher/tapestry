package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.util.UUID;

/**
 * A tag is a node that is attached to another node.
 *
 * <p>The sourceId field is the id of the node that this tag is attached to; the type of that node
 * is specified by the SourceType annotation.
 */
@TTagBase.SourceType(TNodeBase.class)
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = TLabelTag.class),
                @JsonSubTypes.Type(value = TEdgeBase.class),
        })
public abstract class TTagBase<S extends TNodeInterface> extends TNodeBase {
    /**
     * Runtime annotation to specify the source type of a TTag.
     *
     * <p>Inherited so that subclasses of TTag can inherit the annotation.
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SourceType {
        Class<? extends TNodeInterface> value();
    }

    /**
     * For a given TTag class, return the source type.
     *
     * @param cls the TTag class.
     * @return the source type class.
     */
    public static Class<? extends TNodeInterface> getSourceType(Class<? extends TNodeBase> cls) {
        return cls.getAnnotation(TTagBase.SourceType.class).value();
    }

    @Nonnull
    @JsonProperty(required = true)
    public final UUID sourceId;

    @JsonCreator
    public TTagBase(
            @Nullable @JsonProperty(value = "id", required = true) UUID id,
            @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId) {
        super(id);
        this.sourceId = sourceId;
    }

    public TTagBase(@Nonnull UUID sourceId) {
        this(null, sourceId);
    }

    @Override
    public void validate() {
        super.validate();
        getSource();
    }

    @JsonIgnore
    public final S getSource() {
        @SuppressWarnings("unchecked")
        var typ = (Class<S>) getSourceType(getClass());
        return assertGraph().lookupNode(sourceId, typ);
    }
}
