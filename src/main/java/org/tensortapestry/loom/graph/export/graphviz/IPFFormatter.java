package org.tensortapestry.loom.graph.export.graphviz;

import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.zspace.IndexProjectionFunction;

@UtilityClass
public class IPFFormatter {

  /**
   * Convert an IndexProjectionFunction to a Graphviz HTML Label element.
   *
   * @param ipf the IndexProjectionFunction.
   * @return the Graphviz HTML Label element.
   */
  public GH.TableWrapper renderIPF(IndexProjectionFunction ipf) {
    var affineMap = ipf.getAffineMap();

    var tableWrapper = GH.table().border(0).cellborder(0).cellspacing(0);

    if (affineMap.inputNDim() == 0 || affineMap.outputNDim() == 0) {
      tableWrapper.add(
        GH.td(
          "[−]",
          GH.subscript(
            "%d×%d".formatted(ipf.getAffineMap().outputNDim(), ipf.getAffineMap().inputNDim())
          ),
          " ⊕ ",
          ipf.getShape().toString()
        )
      );
    } else {
      var row = GH.tr().withParent(tableWrapper);

      var projTable = GH.table().border(0).cellborder(0).cellspacing(0);
      row.add(GH.td(projTable));

      var projection = affineMap.projection;
      var offset = affineMap.getOffset();
      for (int i = 0; i < affineMap.outputNDim(); ++i) {
        var tr = GH.tr().withParent(projTable);
        for (int j = 0; j < affineMap.inputNDim(); ++j) {
          var td = GH.td().withParent(tr);
          String sides = "";
          if (j == 0) {
            td.border(1);
            sides += "L";
          }
          if (j == affineMap.inputNDim() - 1) {
            td.border(1);
            sides += "R";
          }
          if (!sides.isEmpty()) {
            td.sides(sides);
          }

          td.add(Integer.toString(projection.get(i, j)));
        }
        GH.td().withParent(tr).border(1).sides("R").add(Integer.toString(offset.get(i)));
      }

      row.add(GH.td(" ⊕ " + ipf.getShape()));
    }

    return tableWrapper;
  }
}
