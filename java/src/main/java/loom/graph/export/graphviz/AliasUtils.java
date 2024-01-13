package loom.graph.export.graphviz;

import com.google.common.collect.Streams;
import java.util.*;
import loom.common.DigestUtils;
import loom.common.text.TextUtils;

public class AliasUtils {
  /**
   * Given a collection of UUIDs, return a map of UUID to a short alias.
   *
   * <p>The short alias is taken as first {@code k} characters of the hex md5 sum of the ids, where
   * {@code k} is the length of the longest common prefix of the md5 sums; or {@code
   * minAliasLength}, whichever is greater.
   *
   * @param ids collection of UUIDs
   * @param minAliasLength minimum length of the alias
   * @return map of UUID to alias
   */
  @SuppressWarnings("UnstableApiUsage")
  public static Map<UUID, String> uuidAliasMap(Collection<UUID> ids, int minAliasLength) {
    var idHashes = ids.stream().map(id -> DigestUtils.toMD5HexString(id.toString())).toList();

    var labelLen = Math.max(TextUtils.longestCommonPrefix(idHashes).length() + 1, minAliasLength);

    var labels = new HashMap<UUID, String>();
    Streams.forEachPair(
        ids.stream(), idHashes.stream(), (id, hash) -> labels.put(id, hash.substring(0, labelLen)));

    return Collections.unmodifiableMap(labels);
  }
}