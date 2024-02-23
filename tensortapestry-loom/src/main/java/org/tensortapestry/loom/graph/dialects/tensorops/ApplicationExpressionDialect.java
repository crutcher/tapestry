package org.tensortapestry.loom.graph.dialects.tensorops;

import guru.nidi.graphviz.engine.Format;
import java.awt.*;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
import org.tensortapestry.loom.graph.dialects.common.TypeRestrictionConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.*;
import org.tensortapestry.loom.graph.export.graphviz.GraphVisualizer;

@UtilityClass
public class ApplicationExpressionDialect {

  public final LoomEnvironment ENVIRONMENT = LoomEnvironment
    .builder()
    .typeSupportProvider(
      TypeRestrictionConstraint
        .builder()
        .nodeType(NoteNode.TYPE)
        .nodeType(TensorNode.TYPE)
        .nodeType(OperationNode.TYPE)
        .nodeType(ApplicationNode.TYPE)
        .tagType(TensorOpNodes.IPF_INDEX_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IPF_SIGNATURE_ANNOTATION_TYPE)
        .tagType(TensorOpNodes.IO_SEQUENCE_POINT_TYPE)
        .build()
    )
    .jsonSchemaFactoryManager(CommonEnvironments.COMMON_SCHEMA_MANAGER)
    .constraint(new SchemaTypeConstraint())
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd", "loom")
    .urlAlias("http://tensortapestry.org/schemas/loom/2024-01/tag_types.jsd", "loom")
    .constraint(CommonEnvironments.commonDTypeConstraint())
    .constraint(new TensorOperationAgreementConstraint())
    .constraint(new NoTensorOperationCyclesConstraint())
    .constraint(new OperationIPFSignatureAgreementConstraint())
    .constraint(new OperationApplicationAgreementConstraint())
    .constraint(new ApplicationIPFSignatureAgreementConstraint())
    .constraint(new ApplicationOutputRangeCoverageIsExactConstraint())
    .build();

  public LoomGraph newGraph() {
    return ENVIRONMENT.newGraph();
  }

  public Image toImage(LoomGraph graph) {
    var exporter = GraphVisualizer.buildDefault();
    var export = exporter.export(graph);
    var gv = export.getGraphviz();
    // System.err.println(export.getDotGraph());
    return gv.render(Format.PNG).toImage();
  }
}
