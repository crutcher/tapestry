package org.tensortapestry.loom.graph.export.graphviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import guru.nidi.graphviz.engine.Graphviz;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.json.JsonViewWrapper;
import org.tensortapestry.graphviz.DotGraph;
import org.tensortapestry.graphviz.GraphvizAttribute;
import org.tensortapestry.graphviz.HtmlLabel;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.LoomNodeWrapper;

@Getter
@SuperBuilder
public abstract class LoomGraphvizExporter {

  @Builder.Default
  private final UuidAliasEnv uuidAliasEnv = new UuidAliasEnv();

  @Builder.Default
  private final boolean showUuids = false;

  @Builder.Default
  private final String bgColor = "#E2E2E2";

  protected final ExportContext newContext(LoomGraph graph) {
    var context = new ExportContext(graph, OperationExpressionColoring.builder().build(graph));

    context
      .getDotGraph()
      .getAttributes()
      // .set(GraphvizAttribute.SMOOTHING, "avg_dist")
      .set(GraphvizAttribute.SCALE, 2.5)
      .set(GraphvizAttribute.NEWRANK, true)
      .set(GraphvizAttribute.SPLINES, "ortho")
      .set(GraphvizAttribute.CONCENTRATE, true)
      // .set(GraphAttribute.CLUSTERRANK, "local")
      // .set(GraphAttribute.NODESEP, 0.4)
      .set(GraphvizAttribute.RANKSEP, 0.6)
      .set(GraphvizAttribute.BGCOLOR, getBgColor());

    return context;
  }

  public abstract ExportContext export(LoomGraph graph);

  @Value
  public static class EntityNodeContext {

    @Nonnull
    LoomNodeWrapper<?> loomNode;

    @Nonnull
    DotGraph.SubGraph subGraph;

    @Nonnull
    DotGraph.Node dotNode;

    @Nonnull
    GH.TableWrapper labelTable;
  }

  @Data
  public final class ExportContext {

    @Nonnull
    private final LoomGraph loomGraph;

    @Nonnull
    @Delegate
    private final EntityColorSchemeEnv entityColorSchemeEnv;

    @Nonnull
    private final DotGraph dotGraph = new DotGraph();

    public String toDotString() {
      return dotGraph.toString();
    }

    public Graphviz toGraphvizWrapper() {
      return Graphviz.fromString(toDotString());
    }

    public String nodeAlias(UUID nodeId) {
      return "«%s»".formatted(uuidAliasEnv.getIdAlias(nodeId));
    }

    public EntityNodeContext createEntityNode(
      String title,
      LoomNodeWrapper<?> loomNode,
      DotGraph.SubGraph subGraph
    ) {
      var dotNode = subGraph.createNode(loomNode.getId().toString());
      dotNode.set(GraphvizAttribute.SHAPE, "box").set(GraphvizAttribute.STYLE, "filled");

      GH.TableWrapper labelTable = GH
        .table()
        .bgcolor("white")
        .border(0)
        .cellborder(1)
        .cellspacing(0);

      labelTable.add(
        GH
          .tr()
          .add(
            GH.td().colspan(2).border(1).add(GH.font().add(GH.bold(nodeAlias(loomNode.getId()))))
          )
      );

      if (showUuids) {
        labelTable.add(
          GH
            .tr()
            .add(
              GH
                .td()
                .colspan(2)
                .border(1)
                .add(GH.font().add(GH.italic(loomNode.getId().toString())))
            )
        );
      }

      addTitleRow(labelTable, title);

      dotNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      return new EntityNodeContext(loomNode, getDotGraph().getRoot(), dotNode, labelTable);
    }

    public void renderNodeTags(LoomNodeWrapper<?> node) {
      renderNodeTags(node, getDotGraph().getRoot());
    }

    public void renderNodeTags(LoomNodeWrapper<?> node, DotGraph.SubGraph subGraph) {
      renderNodeTags(node, node.unwrap().getTags(), subGraph);
    }

    public void renderNodeTags(
      LoomNodeWrapper<?> loomNode,
      Map<String, JsonViewWrapper> tags,
      DotGraph.SubGraph subGraph
    ) {
      var nodeId = loomNode.getId();

      if (tags.isEmpty()) {
        return;
      }
      var env = getLoomGraph().assertEnv();

      String dotId = nodeId + "_tags";

      var dotTagNode = subGraph.createNode(dotId);
      dotTagNode
        .set(GraphvizAttribute.SHAPE, "component")
        .set(GraphvizAttribute.STYLE, "filled")
        .set(GraphvizAttribute.PENWIDTH, 2)
        .set(GraphvizAttribute.FILLCOLOR, "#45eebf");

      var labelTable = GH.table().border(0).cellborder(0).cellspacing(2).cellpadding(2);
      for (var entry : tags.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
        renderDataTable(
          env.getTypeAlias(entry.getKey()),
          JsonUtil.valueToJsonNodeTree(entry.getValue())
        )
          .bgcolor("white")
          .withParent(labelTable);
      }

      dotTagNode.set(GraphvizAttribute.LABEL, HtmlLabel.from(labelTable));

      var dotNode = dotGraph.assertLookup(nodeId.toString(), DotGraph.Node.class);
      dotGraph
        .createEdge(dotNode, dotTagNode)
        .set(GraphvizAttribute.PENWIDTH, 3)
        .set(GraphvizAttribute.COLOR, "black:white:white:white:white:white:white:black")
        .set(GraphvizAttribute.ARROWHEAD, "none")
        .set(GraphvizAttribute.WEIGHT, 5);

      dotGraph.sameRank(dotNode, dotTagNode);
    }

    public GH.TableWrapper renderDataTable(String title, JsonNode data) {
      var table = GH.table().border(0).cellborder(1).cellspacing(0).cellpadding(0);

      addDataTableRows(table, title, data);

      return table;
    }

    public void addTitleRow(GH.TableWrapper table, String title) {
      table.add(
        GH
          .td()
          .colspan(2)
          .align(GH.TableDataAlign.LEFT)
          .add(GH.font().color("teal").add(GH.bold(" %s ".formatted(title))))
      );
    }

    public void addDataTableRows(GH.TableWrapper table, String title, JsonNode data) {
      addTitleRow(table, title);
      table.add(GH.td().colspan(2).add(jsonToElement(data)));
    }

    public GH.ElementWrapper<?> asDataKeyValueTr(Object key, Object... values) {
      return GH.tr(asDataKeyTd(key.toString()), asDataValueTd(values));
    }

    public GH.TableDataWrapper asDataKeyTd(String key) {
      return GH
        .td()
        .align(GH.TableDataAlign.RIGHT)
        .valign(GH.VerticalAlign.TOP)
        .add(GH.bold(" %s ".formatted(key)));
    }

    public GH.TableDataWrapper asDataValueTd(Object... value) {
      var td = GH.td().align(GH.TableDataAlign.LEFT).valign(GH.VerticalAlign.TOP);

      if (value.length == 1 && value[0] instanceof GH.TableWrapper tw) {
        tw.border(0);

        td.cellpadding(0);
        td.add(value[0]);
      } else {
        td.add(GH.bold().add(" ").add(value).add(" "));
      }

      return td;
    }

    public void addObjectDataRows(GH.TableWrapper table, ObjectNode object) {
      table.addAll(
        JsonUtil.Tree
          .entryStream(object)
          .map(entry ->
            GH.tr(asDataKeyTd(entry.getKey()), asDataValueTd(jsonToElement(entry.getValue())))
          )
      );
    }

    public GH.ElementWrapper<?> jsonToElement(JsonNode node) {
      switch (node) {
        case ArrayNode array -> {
          if (JsonUtil.Tree.isAllNumeric(array)) {
            return GH.font().color("blue").add(GH.bold(JsonUtil.toJson(array)));
          } else {
            return GH
              .table()
              .border(0)
              .cellborder(1)
              .cellspacing(0)
              .cellpadding(2)
              .addAll(
                JsonUtil.Tree.stream(array).map(item -> GH.tr(asDataValueTd(jsonToElement(item))))
              );
          }
        }
        case ObjectNode object -> {
          if (object.isEmpty()) {
            return GH.bold("(empty)");
          }
          var table = GH.table().border(0).cellborder(1).cellspacing(0);

          addObjectDataRows(table, object);
          return table;
        }
        default -> {
          var text = node.asText();

          try {
            // Auto-detect UUIDs and link them to the reference node,
            // iff there is such a node.
            var uuid = UUID.fromString(text);
            var refNode = loomGraph.getNode(uuid);
            if (refNode != null) {
              return GH.bold(nodeAlias(refNode.getId()));
            }
          } catch (IllegalArgumentException e) {
            // pass
          }

          return GH.bold(text);
        }
      }
    }
  }
}
