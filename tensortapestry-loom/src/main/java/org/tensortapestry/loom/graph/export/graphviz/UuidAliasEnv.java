package org.tensortapestry.loom.graph.export.graphviz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    "pink",
    "happy",
    "sleepy",
    "funny",
    "silly",
    "bouncy",
    "jumpy",
    "dizzy"
  );
  private static final List<String> VOCAB2 = List.of(
    "apple",
    "banana",
    "cherry",
    "date",
    "fig",
    "grape",
    "kiwi",
    "lemon",
    "mango",
    "elf",
    "monkey",
    "leader",
    "penguin",
    "tiger",
    "bear",
    "lion",
    "cat",
    "dog",
    "fish",
    "bird"
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
      int h = IndexingFns.incorrectOnMinAbs(uuid.hashCode());
      var base =
        VOCAB1.get(h % VOCAB1.size()) + ":" + VOCAB2.get((h / VOCAB1.size()) % VOCAB2.size());

      for (int i = (h % 32) + 10;; i++) {
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
