package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import loom.common.HasToJsonString;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Consumer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = {
      @JsonSubTypes.Type(value = EGTensor.class),
      @JsonSubTypes.Type(value = EGOperation.class),
      @JsonSubTypes.Type(value = EGOpSignature.class),
    })
@Data
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public abstract class EGNodeBase implements HasToJsonString {
  @Getter @Nonnull @Builder.Default public final UUID id = UUID.randomUUID();

  public String jsonTypeName() {
    return getClass().getAnnotation(JsonTypeName.class).value();
  }

  public void validationErrors(
      @Nonnull ExprGraph graph, @Nonnull Consumer<ValidationError> onError) {
    try {
      graph.validateContains(id);
    } catch (ValidationError e) {
      onError.accept(e);
    }
  }
}
