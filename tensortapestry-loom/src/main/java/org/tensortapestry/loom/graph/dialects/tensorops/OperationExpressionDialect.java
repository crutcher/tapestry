package org.tensortapestry.loom.graph.dialects.tensorops;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import java.awt.*;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
import org.tensortapestry.loom.graph.dialects.common.TypeRestrictionConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.NoTensorOperationCyclesConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.OperationIPFSignatureAgreementConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.TensorOperationAgreementConstraint;
import org.tensortapestry.loom.graph.export.graphviz.LoomGraphvizExporter;
import org.tensortapestry.loom.graph.export.graphviz.OperationExpressionLoomGraphvizExporter;

@UtilityClass
public class OperationExpressionDialect {

  public static final LoomEnvironment ENVIRONMENT = LoomEnvironment
    .builder()
    .typeSupportProvider(
      TypeRestrictionConstraint
        .builder()
        .nodeType(TensorNode.TYPE)
        .nodeType(OperationNode.TYPE)
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
    .build();

  public LoomGraph newGraph() {
    return ENVIRONMENT.newGraph();
  }

  public LoomGraphvizExporter getGraphvizExporter() {
    return OperationExpressionLoomGraphvizExporter.builder().build();
  }

  public LoomGraphvizExporter.ExportContext export(LoomGraph graph) {
    return getGraphvizExporter().export(graph);
  }

  public Graphviz toGraphviz(LoomGraph graph) {
    return export(graph).toGraphvizWrapper();
  }

  public Image toImage(LoomGraph graph) {
    return toGraphviz(graph).render(Format.PNG).toImage();
  }
}
