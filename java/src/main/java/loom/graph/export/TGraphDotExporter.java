package loom.graph.export;

import com.google.common.html.HtmlEscapers;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import loom.graph.TEdgeBase;
import loom.graph.TGraph;
import loom.graph.TTagBase;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@SuperBuilder
public final class TGraphDotExporter {
    @Builder.Default
    private boolean enableNodeIds = false;

    @Builder.Default
    private Rank.RankDir rankDir = Rank.RankDir.RIGHT_TO_LEFT;

    @Builder.Default
    private int minPrefixLength = 2;

    public BufferedImage toImage(TGraph tgraph) {
        return Graphviz.fromGraph(toGraph(tgraph)).render(Format.PNG).toImage();
    }

    /**
     * Assign display symbols to nodes.
     *
     * <p>We want to give every node a short display name. 1. We'd like to derive the symbol from the
     * node's UUID. 2. We'd like the symbol to be as short as possible.
     *
     * <p>Process: 1. Take the md5 hash of the UUID, and 2. Find the shortest prefixes of these ids
     * that are unique.
     *
     * @param tGraph          The graph to assign symbols to.
     * @param minPrefixLength The minimum length of the prefix to try.
     * @return A map from node UUIDs to symbols.
     */
    private static Map<UUID, String> nodeSymbols(TGraph tGraph, int minPrefixLength) {
        Map<UUID, String> longSyms = new HashMap<>();
        for (var tnode : tGraph) {
            try {
                var digest =
                        MessageDigest.getInstance("MD5")
                                .digest(tnode.id.toString().getBytes(StandardCharsets.UTF_8));
                longSyms.put(tnode.id, HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = minPrefixLength; i <= 32; i++) {
            Map<UUID, String> shortSyms = new HashMap<>();
            for (var entry : longSyms.entrySet()) {
                shortSyms.put(entry.getKey(), entry.getValue().substring(0, i));
            }
            if (shortSyms.size() == longSyms.size()) {
                return shortSyms;
            }
        }

        return longSyms;
    }

    public Graph toGraph(TGraph tgraph) {
        Graph g =
                Factory.graph("G")
                        .directed()
                        .graphAttr()
                        .with(Rank.dir(rankDir))
                        .graphAttr()
                        .with("nodesep", "0.7")
                        .nodeAttr()
                        .with("margin", "0.1");

        var nodeSyms = nodeSymbols(tgraph, minPrefixLength);

        for (var tnode : tgraph) {
            var sym = nodeSyms.get(tnode.id);

            String title = tnode.jsonTypeName();

            Map<String, Object> data = tnode.displayData();
            data.remove("@type");
            if (!enableNodeIds) {
                data.remove("id");

                if (tnode instanceof TTagBase<?>) {
                    data.remove("sourceId");
                }
                if (tnode instanceof TEdgeBase<?, ?>) {
                    data.remove("targetId");
                }
            }

            Map<String, String> tableAttrs =
                    new HashMap<>(
                            Map.of(
                                    "border", "0",
                                    "cellborder", "0",
                                    "cellspacing", "0"));

            var attrs =
                    tableAttrs.entrySet().stream()
                            .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                            .collect(Collectors.joining(" "));

            String label =
                    "<table "
                            + attrs
                            + ">"
                            + "<tr><td colspan=\"2\">"
                            + title
                            + "</td></tr>"
                            + formatRecursiveDataRow(data)
                            + "</table>";

            var gnode =
                    Factory.node(tnode.id.toString())
                            .with(Label.raw("<" + label + ">"))
                            .with("xlabel", "#" + sym);
            for (var entry : tnode.displayOptions().nodeAttributes.entrySet()) {
                gnode = gnode.with(entry.getKey(), entry.getValue());
            }

            if (tnode instanceof TEdgeBase<?, ?> tedge
                    && data.isEmpty()
                    && tgraph
                    .queryEdges(TEdgeBase.class)
                    .withTargetId(tedge.id)
                    .toStream()
                    .toList()
                    .isEmpty()) {

                g =
                        g.with(
                                Factory.node(tedge.sourceId.toString())
                                        .link(
                                                Factory.to(Factory.node(tedge.targetId.toString())).with(Label.of(title))));

                continue;
            }

            g = g.with(gnode);

            if (tnode instanceof TTagBase<?> ttag) {
                g =
                        g.with(
                                Factory.node(ttag.sourceId.toString())
                                        .link(Factory.to(Factory.node(ttag.id.toString()))));
            }

            if (tnode instanceof TEdgeBase<?, ?> tedge) {
                g =
                        g.with(
                                Factory.node(tedge.id.toString())
                                        .link(Factory.to(Factory.node(tedge.targetId.toString()))));
            }
        }

        return g;
    }

    private String formatRecursiveData(Object data) {
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) data;

            return "<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">"
                    + formatRecursiveDataRow(m)
                    + "</table>";
        } else {
            return HtmlEscapers.htmlEscaper().escape(data.toString());
        }
    }

    private String formatRecursiveDataRow(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        for (var entry : data.entrySet()) {
            sb.append("<tr>")
                    .append("<td align=\"right\"><b>")
                    .append(entry.getKey())
                    .append(":</b></td>")
                    .append("<td align=\"left\">")
                    .append(formatRecursiveData(entry.getValue()))
                    .append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }
}
