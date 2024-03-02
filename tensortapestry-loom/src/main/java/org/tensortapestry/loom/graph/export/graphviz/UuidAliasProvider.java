package org.tensortapestry.loom.graph.export.graphviz;

import java.util.*;
import java.util.function.Function;

public class UuidAliasProvider implements Function<UUID, String> {

  private static final List<String> VOCAB1 = List.of(
    "red",
    "green",
    "blue",
    "yellow",
    "orange",
    "purple",
    "cyan",
    "magenta",
    "lime",
    "pink"
  );
  private static final List<String> VOCAB2 = List.of(
    "apple",
    "banana",
    "cherry",
    "date",
    "elderberry",
    "fig",
    "grape",
    "honeydew",
    "kiwi",
    "lemon"
  );

  private final Map<UUID, String> aliasMap;

  public UuidAliasProvider() {
    this(new HashMap<>());
  }

  public UuidAliasProvider(Map<UUID, String> aliasMap) {
    this.aliasMap = new HashMap<>(aliasMap);
  }

  private static int abs(int x) {
    if (x < 0) {
      return -x;
    }
    return x;
  }

  @Override
  public synchronized String apply(UUID uuid) {
    if (!aliasMap.containsKey(uuid)) {
      int h = abs(uuid.hashCode());
      var base =
        VOCAB1.get(h % VOCAB1.size()) + ":" + VOCAB2.get((h / VOCAB1.size()) % VOCAB2.size());

      for (int i = 1;; i++) {
        var alias = base + ":" + i;
        if (!aliasMap.containsValue(alias)) {
          aliasMap.put(uuid, alias);
          return alias;
        }
      }
    }
    return aliasMap.get(uuid);
  }
}
