package org.tensortapestry.loom.graph.dialects.tensorops;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import java.awt.*;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.CommonEnvironments;
import org.tensortapestry.loom.graph.LoomEnvironment;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.common.NoteNode;
import org.tensortapestry.loom.graph.dialects.common.SchemaTypeConstraint;
import org.tensortapestry.loom.graph.dialects.common.TypeRestrictionConstraint;
import org.tensortapestry.loom.graph.dialects.tensorops.constraints.*;
import org.tensortapestry.loom.graph.export.graphviz.ApplicationExpressionLoomGraphvizExporter;
import org.tensortapestry.loom.graph.export.graphviz.LoomGraphvizExporter;

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

  public LoomGraphvizExporter getGraphvizExporter() {
    return ApplicationExpressionLoomGraphvizExporter.builder().build();
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
