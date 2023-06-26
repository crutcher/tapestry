package loom.graph;

public interface THasParametersProperty extends TNodeInterface {
  default TParameters bindParameters(TParameters parameters) {
    // TODO: validate/update singleton
    assertGraph().addNode(new TWithEdge(getId(), parameters.id));
    return parameters;
  }

  default TParameters getParameters() {
    return assertGraph()
        .queryEdges(TWithEdge.class)
        .withSourceId(getId())
        .toSingleton()
        .getTarget();
  }
}
