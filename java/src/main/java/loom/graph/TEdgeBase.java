package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.lang.annotation.*;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@TEdgeBase.TargetType(TNodeBase.class)
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TCanBeSequencedProperty.TWaitsOnEdge.class),
      @JsonSubTypes.Type(value = TTensor.TResultEdge.class),
      @JsonSubTypes.Type(value = TTensor.TConsumesEdge.class),
      @JsonSubTypes.Type(value = TParameters.TWithParametersEdge.class),
    })
public abstract class TEdgeBase<S extends TNodeInterface, T extends TNodeInterface>
    extends TTagBase<S> {
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

  /** The id of the target node. */
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
