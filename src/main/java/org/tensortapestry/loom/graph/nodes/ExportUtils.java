package org.tensortapestry.loom.graph.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.tensortapestry.loom.common.json.JsonPathUtils;

@UtilityClass
public class ExportUtils {

  @Value
  public class JsonPathChain {

    @Nullable JsonPathChain parent;

    @Nonnull
    String selector;

    int depth;

    public static JsonPathChain root() {
      return new JsonPathChain(null, "");
    }

    public JsonPathChain(@Nullable JsonPathChain parent, @Nonnull String selector) {
      this.parent = parent;
      this.selector = selector;
      this.depth = (this.parent == null) ? 0 : this.parent.depth + 1;
    }

    public String[] pathArray() {
      String[] path = new String[depth + 1];
      for (var link = this; link != null; link = link.parent) {
        path[link.depth] = link.selector;
      }
      return path;
    }

    public String jsonpath() {
      return JsonPathUtils.concatJsonPath((Object[]) pathArray());
    }
  }

  public void findLinks(JsonNode data, Predicate<UUID> isNode, BiConsumer<String, UUID> onLink) {
    findLinks(JsonPathChain.root(), data, isNode, onLink);
  }

  public void findLinks(
    JsonPathChain jsonPathChain,
    JsonNode data,
    Predicate<UUID> isNode,
    BiConsumer<String, UUID> onLink
  ) {
    switch (data.getNodeType()) {
      case OBJECT:
        for (var it = data.fields(); it.hasNext();) {
          var entry = it.next();
          var key = entry.getKey();
          var value = entry.getValue();
          findLinks(new JsonPathChain(jsonPathChain, key), value, isNode, onLink);
        }
        break;
      case ARRAY:
        var array = (ArrayNode) data;
        for (var idx = 0; idx < array.size(); ++idx) {
          var value = array.get(idx);
          findLinks(new JsonPathChain(jsonPathChain, "[%d]".formatted(idx)), value, isNode, onLink);
        }
        break;
      default:
        try {
          var id = UUID.fromString(data.asText());
          if (isNode.test(id)) {
            onLink.accept(jsonPathChain.jsonpath(), id);
          }
        } catch (IllegalArgumentException e) {
          // pass
        }
    }
  }
}
