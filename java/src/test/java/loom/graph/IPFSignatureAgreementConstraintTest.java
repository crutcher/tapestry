package loom.graph;

import java.util.List;
import loom.graph.nodes.*;
import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

@SuppressWarnings("unused")
public class IPFSignatureAgreementConstraintTest extends BaseTestClass {
  @Test
  public void test_valid() {
    var env = CommonEnvironments.expressionEnvironment();
    env.assertConstraint(IPFSignatureAgreementConstraint.class);

    var graph = env.newGraph();

    var tensorA = TensorNode.withBody(b -> b.dtype("int32").shape(3, 4)).addTo(graph);

    var tensorB = TensorNode.withBody(b -> b.dtype("int32").shape(4, 5)).addTo(graph);

    var tensorC = TensorNode.withBody(b -> b.dtype("int32").shape(3, 5)).addTo(graph);

    var sigIndex =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 0), ZPoint.of(3, 5))))
            .addTo(graph);

    var ipfSignature =
        IPFSignatureNode.withBody(
                b ->
                    b.input(
                            "x",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 0}}))
                                    .shape(ZPoint.of(1, tensorA.getShape().get(1)))
                                    .build()))
                        .input(
                            "y",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{0, 0}, {0, 1}}))
                                    .shape(ZPoint.of(tensorB.getShape().get(0), 1))
                                    .build()))
                        .output(
                            "z",
                            List.of(
                                IndexProjectionFunction.builder()
                                    .affineMap(ZAffineMap.fromMatrix(new int[][] {{1, 0}, {0, 1}}))
                                    .build())))
            .addTo(graph);

    var sig =
        OperationSignatureNode.withBody(
                b ->
                    b.name("matmul")
                        .signatureId(ipfSignature.getId())
                        .indexId(sigIndex.getId())
                        .input("x", List.of(TensorSelection.from(tensorA)))
                        .input("y", List.of(TensorSelection.from(tensorB)))
                        .output("z", List.of(TensorSelection.from(tensorC))))
            .addTo(graph);

    var app1Index =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 0), ZPoint.of(3, 2))))
            .addTo(graph);

    var app1 =
        ApplicationNode.withBody(
                b ->
                    b.operationId(sig.getId())
                        .indexId(app1Index.getId())
                        .input("x", List.of(TensorSelection.from(tensorA)))
                        .input(
                            "y",
                            List.of(
                                new TensorSelection(
                                    tensorB.getId(), new ZRange(ZPoint.of(0, 0), ZPoint.of(4, 2)))))
                        .output(
                            "z",
                            List.of(
                                new TensorSelection(
                                    tensorC.getId(),
                                    new ZRange(ZPoint.of(0, 0), ZPoint.of(3, 2))))))
            .addTo(graph);

    var appIndex2 =
        IPFIndexNode.withBody(b -> b.range(new ZRange(ZPoint.of(0, 2), ZPoint.of(3, 5))))
            .addTo(graph);

    var app2 =
        ApplicationNode.withBody(
                b ->
                    b.operationId(sig.getId())
                        .indexId(appIndex2.getId())
                        .input(
                            "x",
                            List.of(
                                new TensorSelection(tensorA.getId(), tensorA.getEffectiveRange())))
                        .input(
                            "y",
                            List.of(
                                new TensorSelection(
                                    tensorB.getId(), new ZRange(ZPoint.of(0, 2), ZPoint.of(4, 5)))))
                        .output(
                            "z",
                            List.of(
                                new TensorSelection(
                                    tensorC.getId(),
                                    new ZRange(ZPoint.of(0, 2), ZPoint.of(3, 5))))))
            .addTo(graph);

    graph.validate();
  }
}
