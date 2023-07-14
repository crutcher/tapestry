package loom.alt.densegraph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.zspace.ZRange;

@JsonTypeName("Operation")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class EGOperation extends EGNodeBase {
  public abstract static class EGOperationBuilder<
          C extends EGOperation, B extends EGOperationBuilder<C, B>>
      extends EGNodeBaseBuilder<C, B> {

    public B withSignature(EGSignature sig) {
      return signature(sig.getId());
    }
  }

  @Nonnull public UUID signature;
  @Builder.Default @Nullable public ZRange index = null;

  @Singular @Nonnull public Map<String, String> options;
  @Singular @Nonnull private Map<String, List<UUID>> inputs;
  @Singular @Nonnull private Map<String, List<UUID>> results;

  @JsonIgnore
  public EGSignature getSignature(ExprGraph graph) {
    return graph.getNode(signature, EGSignature.class);
  }

  @JsonIgnore
  public Map<String, List<EGTensor>> getInputs(ExprGraph graph) {
    return inputs.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(id -> graph.getNode(id, EGTensor.class))
                        .collect(java.util.stream.Collectors.toList())));
  }

  @JsonIgnore
  public Map<String, List<EGTensor>> getResults(ExprGraph graph) {
    return results.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(id -> graph.getNode(id, EGTensor.class))
                        .collect(java.util.stream.Collectors.toList())));
  }

  @Override
  public void validationErrors(
      @Nonnull ExprGraph graph, @Nonnull Consumer<ValidationError> onError) {
    super.validationErrors(graph, onError);

    try {
      graph.validateContains(signature);
    } catch (ValidationError e) {
      onError.accept(e);
    }

    for (var input : inputs.entrySet()) {
      if (!NamePatterns.IDENTIFIER_ATOM.matcher(input.getKey()).matches()) {
        onError.accept(
            new ValidationError(id, String.format("Invalid input name: %s", input.getKey())));
      }
      for (var inputId : input.getValue()) {
        try {
          graph.getNode(inputId, EGTensor.class);
        } catch (ClassCastException e) {
          onError.accept(
              new ValidationError(
                  id,
                  String.format(
                      "Invalid input node type: %s (expected Tensor)", inputId.toString())));
        } catch (IllegalArgumentException e) {
          onError.accept(
              new ValidationError(
                  id, String.format("Input node %s not in tree", inputId.toString())));
        }
      }
    }

    for (var result : results.entrySet()) {
      if (!NamePatterns.IDENTIFIER_ATOM.matcher(result.getKey()).matches()) {
        onError.accept(
            new ValidationError(id, String.format("Invalid result name: %s", result.getKey())));
      }
      for (var resultId : result.getValue()) {
        try {
          graph.getNode(resultId, EGTensor.class);
        } catch (ClassCastException e) {
          onError.accept(
              new ValidationError(
                  id,
                  String.format(
                      "Invalid result node type: %s (expected Tensor)", resultId.toString())));
        } catch (IllegalArgumentException e) {
          onError.accept(
              new ValidationError(
                  id, String.format("result node %s not in tree", resultId.toString())));
        }
      }
    }
  }
}
