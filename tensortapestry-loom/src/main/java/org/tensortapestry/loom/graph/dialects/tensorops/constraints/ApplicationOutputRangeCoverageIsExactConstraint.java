package org.tensortapestry.loom.graph.dialects.tensorops.constraints;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.tensortapestry.common.json.JsonPathUtils;
import org.tensortapestry.common.lazy.LazyString;
import org.tensortapestry.common.lazy.Thunk;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.common.validation.ValidationIssueCollector;
import org.tensortapestry.loom.graph.LoomConstants;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.ValidationUtils;
import org.tensortapestry.loom.graph.dialects.tensorops.ApplicationNode;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationNode;
import org.tensortapestry.zspace.ZRange;

/**
 * This constraint checks that the output range coverage of the sum of the `Application` node shards
 * of an `Operation` node exactly cover the corresponding `Operation` node's output range for the
 * same selection.
 */
public class ApplicationOutputRangeCoverageIsExactConstraint implements LoomEnvironment.Constraint {

  @Override
  public void checkRequirements(LoomEnvironment env) {
    env.assertConstraint(OperationApplicationAgreementConstraint.class);
  }

  @Override
  public void validateConstraint(
    @Nonnull @SuppressWarnings("unused") LoomEnvironment env,
    @Nonnull LoomGraph graph,
    @Nonnull ValidationIssueCollector issueCollector
  ) {
    for (var application : graph.byType(ApplicationNode.class)) {
      ValidationUtils.validateNodeReference(
        graph,
        application.getOperationId(),
        OperationNode.TYPE,
        new LazyString(() ->
          JsonPathUtils.concatJsonPath(application.getJsonPath(), "body", "operationId")
        ),
        issueCollector,
        Thunk.of(() -> List.of(application.asValidationContext("Application Node")))
      );
    }

    for (var operation : graph.byType(OperationNode.class)) {
      validateOperationNode(operation, issueCollector);
    }
  }

  private static boolean validateOperationNode(
    OperationNode operation,
    ValidationIssueCollector issueCollector
  ) {
    boolean valid = true;
    var lazyContexts = Thunk.of(() -> List.of(operation.asValidationContext("Operation Node")));

    var shards = operation.getApplicationNodes().toList();
    var shardIds = shards.stream().map(ApplicationNode::getId).toList();

    // check the output range coverage:
    // 3. The sum of the shard range sizes is equal to the output range size.
    for (var entry : operation.getOutputs().entrySet()) {
      final var ioName = entry.getKey();
      final var selections = entry.getValue();

      final var k = selections.size();

      var shardSelections = shards
        .stream()
        .map(s -> s.viewBodyAs(ApplicationNode.Body.class).getOutputs().get(ioName))
        .toList();

      for (int idx = 0; idx < k; ++idx) {
        var sigRange = selections.get(idx).getRange();
        var finalIdx = idx;

        var shardRanges = shardSelections.stream().map(s -> s.get(finalIdx).getRange()).toList();

        var totalSize = shardRanges.stream().mapToInt(ZRange::getSize).sum();
        if (totalSize != sigRange.getSize()) {
          // There are overlapping shard ranges.
          issueCollector.addIssue(
            ValidationIssue
              .builder()
              .type(LoomConstants.Errors.NODE_VALIDATION_ERROR)
              .summary("Overlapping Application output key \"%s[%d]\" ranges", ioName, idx)
              .context(
                ValidationIssue.Context
                  .builder()
                  .name("Application Shard Ranges")
                  .data(rangeMap(shardIds, shardRanges))
              )
              .withContexts(lazyContexts)
          );
          valid = false;
        }
      }
    }

    return valid;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Map<UUID, ZRange> rangeMap(List<UUID> shardIds, List<ZRange> shardRanges) {
    return Streams
      .zip(shardIds.stream(), shardRanges.stream(), Map::entry)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
