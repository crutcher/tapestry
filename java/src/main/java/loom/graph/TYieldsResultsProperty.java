package loom.graph;

import loom.zspace.ZPoint;

public interface TYieldsResultsProperty extends TNodeInterface {
  default TTensor bindResult(ZPoint shape, String dtype, String label) {
    var g = assertGraph();
    var t = g.addNode(new TTensor(shape, dtype));
    g.addNode(new TResultEdge(t.id, getId(), label));
    return t;
  }
}
