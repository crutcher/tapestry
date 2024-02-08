package org.tensortapestry.loom.graph.export.graphviz;

import lombok.experimental.UtilityClass;
import org.tensortapestry.zspace.ZRangeProjectionMap;

@UtilityClass
public class IPFFormatter {

  /**
   * Convert an ZRangeProjectionMap to a Graphviz HTML Label element.
   *
   * @param ipf the ZRangeProjectionMap.
   * @return the Graphviz HTML Label element.
   */
  public GH.TableWrapper renderRangeProjectionMap(ZRangeProjectionMap ipf) {
    var affineMap = ipf.getAffineMap();

    var tableWrapper = GH.table().border(0).cellborder(0).cellspacing(0);

    if (affineMap.getInputNDim() == 0 || affineMap.getOutputNDim() == 0) {
      tableWrapper.add(
        GH.td(
          "[−]",
          GH.subscript(
            "%d×%d".formatted(ipf.getAffineMap().getOutputNDim(), ipf.getAffineMap().getInputNDim())
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
      for (int i = 0; i < affineMap.getOutputNDim(); ++i) {
        var tr = GH.tr().withParent(projTable);
        for (int j = 0; j < affineMap.getInputNDim(); ++j) {
          var td = GH.td().withParent(tr);
          String sides = "";
          if (j == 0) {
            td.border(1);
            sides += "L";
          }
          if (j == affineMap.getInputNDim() - 1) {
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
