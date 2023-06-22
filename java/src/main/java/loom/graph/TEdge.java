package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.lang.annotation.*;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@TEdge.TargetType(TNode.class)
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = THappensAfter.class),
    })
public abstract class TEdge extends TTag {
  /**
   * Runtime annotation to specify the target type of an TEdge.
   *
   * <p>Inherited so that subclasses of TEdge can inherit the annotation.
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface TargetType {
    Class<? extends TNode> value();
  }

  /**
   * For a given TEdge class, return the target type.
   *
   * @param cls the TEdge class.
   * @return the target type class.
   */
  public static Class<? extends TNode> getTargetType(Class<? extends TEdge> cls) {
    return cls.getAnnotation(TargetType.class).value();
  }

  /** The id of the target node. */
  @Getter
  @Nonnull
  @JsonProperty(required = true)
  public final UUID targetId;

  TEdge(@Nullable UUID id, @Nonnull UUID sourceId, @Nonnull UUID targetId) {
    super(id, sourceId);
    this.targetId = targetId;
  }

  TEdge(@Nonnull UUID sourceId, @Nonnull UUID targetId) {
    this(null, sourceId, targetId);
  }

  @JsonIgnore
  public TNode getTarget() {
    return assertGraph().lookupNode(targetId, getTargetType(getClass()));
  }

  @Override
  public void validate() {
    super.validate();
    assertGraph().lookupNode(targetId, getTargetType(getClass()));
  }
}
