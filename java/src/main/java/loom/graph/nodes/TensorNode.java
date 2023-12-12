package loom.graph.nodes;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.graph.LoomEnvironment;
import loom.graph.LoomGraph;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;
import loom.zspace.ZPoint;

import javax.annotation.Nonnull;
import java.util.*;

@Jacksonized
@SuperBuilder
public final class TensorNode extends LoomGraph.Node<TensorNode, TensorNode.Body> {
  /**
   * LoomEnvironment validation rule: All tensors must have exactly one source operation.
   *
   * @param env the LoomEnvironment.
   * @param graph the LoomGraph.
   * @param issueCollector the ValidationIssueCollector.
   */
  public static void AllTensorsHaveExactlyOneSourceOperationConstraint(
      @SuppressWarnings("unused") LoomEnvironment env,
      LoomGraph graph,
      ValidationIssueCollector issueCollector) {
    graph.stream(TensorNode.Meta.TYPE, TensorNode.class)
        .forEach(
            tensor -> {
              List<OperationNode> sources = tensor._getSourceOperationNodes();
              if (sources.size() == 1) {
                return;
              }

              String desc = "Tensor";
              if (tensor.getLabel() != null) {
                desc = "%s (%s)".formatted(desc, tensor.getLabel());
              }

              var issueBuilder =
                  ValidationIssue.builder()
                      .type(TensorNode.NODE_VALIDATION_ERROR)
                      .param("nodeType", TensorNode.Meta.TYPE)
                      .context(
                          ValidationIssue.Context.builder()
                              .name("Tensor")
                              .jsonpath(tensor.getJsonPath())
                              .jsonData(tensor.toJsonString())
                              .build());

              if (sources.isEmpty()) {
                issueBuilder.summary("%s has no Operation source".formatted(desc));

              } else {
                issueBuilder.summary(
                    "%s has too many Operation sources: %d".formatted(desc, sources.size()));

                issueBuilder.message("Tensor id: %s".formatted(tensor.getId()));

                // Sort the sources by ID so that the order is deterministic.
                sources = new ArrayList<>(sources);
                sources.sort(
                    Comparator.comparing(
                        n -> n.getLabel() == null ? n.getId().toString() : n.getLabel()));

                for (int idx = 0; idx < sources.size(); idx++) {
                  var source = sources.get(idx);

                  var name = "Source Operation #" + idx;
                  if (source.getLabel() != null) {
                    name = "%s (%s)".formatted(name, source.getLabel());
                  }

                  issueBuilder.context(
                      ValidationIssue.Context.builder()
                          .name(name)
                          .jsonpath(source.getJsonPath())
                          .jsonData(source.toJsonString())
                          .build());
                }
              }

              issueCollector.add(issueBuilder);
            });
  }

  @Data
  @Jacksonized
  @Builder
  public static final class Body {
    @Nonnull private String dtype;

    @Nonnull private ZPoint shape;
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder() {
    return new TensorNodeBuilderImpl().type(Meta.TYPE);
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body body) {
    return builder().body(body);
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param body the body to use.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(Body.BodyBuilder body) {
    return builder().body(body.build());
  }

  /**
   * Create a new TensorNodeBuilder, with the type set to {@link Meta#TYPE}.
   *
   * @param dtype the dtype.
   * @param shape the shape.
   * @return the new TensorNodeBuilder.
   */
  public static TensorNodeBuilder<TensorNode, ?> builder(String dtype, ZPoint shape) {
    return builder().body(Body.builder().dtype(dtype).shape(shape).build());
  }

  @Override
  public Class<Body> getBodyClass() {
    return Body.class;
  }

  @Builder
  @Getter
  public static final class Meta extends LoomGraph.NodeMeta<TensorNode, Body> {
    public static final String TYPE = "TensorNode";

    public static final String BODY_SCHEMA =
        """
        {
            "type": "object",
            "properties": {
              "dtype": {
                  "type": "string"
              },
              "shape": {
                  "type": "array",
                  "items": {
                    "type": "integer",
                    "minimum": 1
                  },
                  "minItems": 1
                }
              },
            "required": ["dtype", "shape"]
        }
        """;

    @Singular private final Set<String> validDTypes;

    @Builder
    public Meta(Set<String> validDTypes) {
      super(TensorNode.class, Body.class, BODY_SCHEMA);
      this.validDTypes = new HashSet<>(validDTypes);
    }

    /**
     * Add a valid dtype.
     *
     * @param validDType the valid dtype.
     * @return this Meta, for chaining.
     */
    @CanIgnoreReturnValue
    public Meta addValidDType(String validDType) {
      validDTypes.add(validDType);
      return this;
    }

    @Override
    public void validateNode(TensorNode node, ValidationIssueCollector issueCollector) {
      if (!node.getShape().coords.isStrictlyPositive()) {
        issueCollector.add(
            ValidationIssue.builder()
                .type(NODE_VALIDATION_ERROR)
                .message("shape must be positive and non-empty")
                .build());
      }
    }
  }

  /** Exists to support {@code @Delegate} for {@code getBody()}. */
  @SuppressWarnings("unused")
  @Delegate
  private Body delegateProvider() {
    return getBody();
  }

  List<OperationNode> _getSourceOperationNodes() {
    var id = getId();
    return assertGraph().stream(OperationNode.Meta.TYPE, OperationNode.class)
        .filter(op -> op.getOutputs().values().stream().anyMatch(ids -> ids.contains(id)))
        .toList();
  }

  /**
   * Get the operation node that produces this tensor.
   *
   * @return the operation node.
   */
  public OperationNode getSourceOperationNode() {
    var nodes = _getSourceOperationNodes();
    if (nodes.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one source operation node, but found " + nodes.size());
    }
    return nodes.getFirst();
  }

  /**
   * Get the operation node ID that produces this tensor.
   *
   * @return the operation node ID.
   */
  public UUID getSourceOperationId() {
    return getSourceOperationNode().getId();
  }
}
