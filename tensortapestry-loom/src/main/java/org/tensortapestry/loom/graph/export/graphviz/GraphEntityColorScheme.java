package org.tensortapestry.loom.graph.export.graphviz;

import java.awt.*;
import lombok.Value;

@Value(staticConstructor = "of")
public class GraphEntityColorScheme {

  Color primary;
  Color secondary;
}
