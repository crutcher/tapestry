package loom.graph.nodes;

import java.util.List;
import loom.graph.CommonEnvironments;
import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.zspace.ZAffineMap;
import loom.zspace.ZPoint;
import loom.zspace.ZTensor;
import org.junit.Test;

public class IPFSignatureNodeTest extends BaseTestClass {
  @Test
  public void test_valid() {
    var env = CommonEnvironments.expressionEnvironment();
    env.autowireNodeTypeClass(IPFSignatureNode.TYPE, IPFSignatureNode.class);
    var graph = env.newGraph();

    IPFSignatureNode.withBody(
            b ->
                b.input(
                        "x",
                        List.of(
                            new IndexProjectionFunction(
                                ZAffineMap.builder()
                                    .A(ZTensor.newMatrix(new int[][] {{1, 0}, {0, 1}, {1, 1}}))
                                    .b(ZTensor.newVector(10, 20, 30))
                                    .build(),
                                ZPoint.of(3, 3, 3))))
                    .output(
                        "y",
                        List.of(
                            new IndexProjectionFunction(
                                ZAffineMap.builder()
                                    .A(ZTensor.newMatrix(new int[][] {{2, 0}, {0, 2}, {1, 2}}))
                                    .b(ZTensor.newVector(10, 20, 30))
                                    .build(),
                                ZPoint.of(2, 2, 3)))))
        .buildOn(graph);

    graph.validate();
  }
}
