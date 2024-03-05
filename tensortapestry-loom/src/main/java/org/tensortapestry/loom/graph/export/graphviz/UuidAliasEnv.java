package org.tensortapestry.loom.graph.export.graphviz;

import java.util.*;
import org.tensortapestry.zspace.indexing.IndexingFns;

public final class UuidAliasEnv {

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

  public UuidAliasEnv() {
    this(new HashMap<>());
  }

  public UuidAliasEnv(Map<UUID, String> aliasMap) {
    this.aliasMap = new HashMap<>(aliasMap);
  }

  public boolean hasIdAlias(String alias) {
    return aliasMap.containsValue(alias);
  }

  public synchronized String getIdAlias(UUID uuid) {
    if (!aliasMap.containsKey(uuid)) {
      int h = IndexingFns.throwOnMinAbs(uuid.hashCode());
      var base =
        VOCAB1.get(h % VOCAB1.size()) + ":" + VOCAB2.get((h / VOCAB1.size()) % VOCAB2.size());

      for (int i = 1;; i++) {
        var alias = base + ":" + i;
        if (!hasIdAlias(alias)) {
          aliasMap.put(uuid, alias);
          return alias;
        }
      }
    }
    return aliasMap.get(uuid);
  }
}
