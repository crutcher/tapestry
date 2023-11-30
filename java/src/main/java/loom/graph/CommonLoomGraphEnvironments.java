package loom.graph;

import loom.graph.nodes.NoCyclesConstraint;
import loom.graph.nodes.OperationNodeTypeBindings;
import loom.graph.nodes.TensorNodeTypeBindings;

public class CommonLoomGraphEnvironments {
  private CommonLoomGraphEnvironments() {}

  /**
   * Create a default environment.
   *
   * @return the environment.
   */
  public static LoomGraphEnv createDefault() {
    var env = new LoomGraphEnv();

    var tensorOps = env.addNodeTypeBindings(new TensorNodeTypeBindings());
    tensorOps.addDatatype("int32");

    env.addNodeTypeBindings(new OperationNodeTypeBindings());

    env.addConstraint(new NoCyclesConstraint());

    return env;
  }
}
