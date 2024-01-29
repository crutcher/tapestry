package org.tensortapestry.loom.graph.dialects.common;

import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNode;

@UtilityClass
public class CommonNodes {

  public final String NOTE_NODE_TYPE =
    "http://tensortapestry.org/schemas/loom/2024-01/node_types.jsd#/$defs/Note";

  public LoomNode.LoomNodeBuilder noteBuilder(
    LoomGraph graph,
    Consumer<NoteBody.NoteBodyBuilder> config
  ) {
    var bodyBuilder = NoteBody.builder();
    config.accept(bodyBuilder);
    return graph.nodeBuilder(NOTE_NODE_TYPE).body(bodyBuilder.build());
  }
}
