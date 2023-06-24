package loom.graph;

import com.fasterxml.jackson.annotation.*;
import java.lang.annotation.*;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.common.HasToJsonString;
import loom.common.IdUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TTagBase.class),
      @JsonSubTypes.Type(value = TOperatorBase.class),
      @JsonSubTypes.Type(value = TSequencePoint.class),
      @JsonSubTypes.Type(value = TTensor.class),
    })
@TNodeBase.DisplayOptions.BackgroundColor("#ffffff")
@TNodeBase.DisplayOptions.NodeShape("box")
public abstract class TNodeBase implements HasToJsonString {
  public static class DisplayOptions {
    @Nullable public final String backgroundColor;

    @Nullable public final String nodeShape;

    public DisplayOptions(Class<? extends TNodeBase> cls) {
      backgroundColor = cls.getAnnotation(BackgroundColor.class).value();
      nodeShape = cls.getAnnotation(NodeShape.class).value();
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface BackgroundColor {
      String value() default "";
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface NodeShape {
      String value() default "";
    }
  }

  @Nullable @JsonIgnore TGraph graph = null;

  @Getter
  @Nonnull
  @JsonProperty(required = true)
  public final UUID id;

  TNodeBase(@Nullable UUID id) {
    this.id = IdUtils.coerceUUID(id);
  }

  TNodeBase() {
    this(null);
  }

  public DisplayOptions displayOptions() {
    return new DisplayOptions(getClass());
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
