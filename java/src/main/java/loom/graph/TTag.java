package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.lang.annotation.*;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * A tag is a node that is attached to another node.
 *
 * <p>The sourceId field is the id of the node that this tag is attached to; the type of that node
 * is specified by the SourceType annotation.
 */
@TTag.SourceType(TNode.class)
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TEdge.class),
    })
public abstract class TTag extends TNode {
  /**
   * Runtime annotation to specify the source type of a TTag.
   *
   * <p>Inherited so that subclasses of TTag can inherit the annotation.
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface SourceType {
    Class<? extends TNode> value();
  }

  /**
   * For a given TTag class, return the source type.
   *
   * @param cls the TTag class.
   * @return the source type class.
   */
  public static Class<? extends TNode> getSourceType(Class<? extends TNode> cls) {
    return cls.getAnnotation(TTag.SourceType.class).value();
  }

  @Nonnull
  @JsonProperty(required = true)
  public final UUID sourceId;

  @JsonCreator
  TTag(
      @Nonnull @JsonProperty(value = "id", required = true) UUID id,
      @Nonnull @JsonProperty(value = "sourceId", required = true) UUID sourceId) {
    super(id);
    this.sourceId = sourceId;
  }

  TTag(@Nonnull UUID sourceId) {
    this(null, sourceId);
  }

  @Override
  public void validate() {
    super.validate();
    assertGraph().lookupNode(sourceId, getSourceType(getClass()));
  }
}
