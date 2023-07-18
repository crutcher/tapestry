package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import loom.common.HasToJsonString;

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

  @JsonDeserialize(keyUsing = ScopedName.JsonSupport.KeyDeserializer.class)
  @Singular
  private final Map<ScopedName, String> attributes;

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
