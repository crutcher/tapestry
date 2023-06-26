package loom.graph;

import com.fasterxml.jackson.annotation.*;
import java.lang.annotation.*;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import loom.common.HasToJsonString;
import loom.common.IdUtils;
import loom.common.JsonUtil;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = TObserver.class),
      @JsonSubTypes.Type(value = TTagBase.class),
      @JsonSubTypes.Type(value = TOperatorBase.class),
      @JsonSubTypes.Type(value = TSequencePoint.class),
      @JsonSubTypes.Type(value = TTensor.class),
    })
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "box"),
      @TNodeBase.DisplayOptions.Attribute(name = "style", value = "filled"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#ffffff")
    })
public abstract class TNodeBase implements HasToJsonString, TNodeInterface {
  public static class DisplayOptions {
    @Nonnull public final Map<String, String> nodeAttributes;

    public DisplayOptions(Class<? extends TNodeBase> cls) {
      List<Class<?>> superclasses = new ArrayList<>();
      Class<?> tmp = cls;
      while (tmp != null) {
        superclasses.add(tmp);
        tmp = tmp.getSuperclass();
      }
      Collections.reverse(superclasses);

      nodeAttributes = new HashMap<>();
      for (var c : superclasses) {
        var na = c.getDeclaredAnnotation(NodeAttributes.class);
        if (na != null) {
          for (var a : na.value()) {
            nodeAttributes.put(a.name(), a.value());
          }
        }
      }
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Attribute {
      String name();

      String value();
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface NodeAttributes {
      Attribute[] value();
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

  public Map<String, Object> displayData() {
    return JsonUtil.toMap(this);
  }

  @Nonnull
  public DisplayOptions displayOptions() {
    return new DisplayOptions(getClass());
  }

  public String jsonTypeName() {
    return getClass().getAnnotation(JsonTypeName.class).value();
  }

  public final boolean hasGraph() {
    return graph != null;
  }

  @Override
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
