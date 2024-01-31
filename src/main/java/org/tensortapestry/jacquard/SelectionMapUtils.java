package org.tensortapestry.jacquard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SelectionMapUtils {

  String sortedMapString(Map<String, ?> map) {
    return map
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
      .collect(Collectors.joining(", ", "{", "}"));
  }

  /**
   * Returns the single value in the map, or throws an exception if there is not exactly one value.
   * @param map the selection map.
   * @return the single value in the map.
   * @param <T> the type of the values.
   */
  public <T> T getSingularItem(Map<String, List<T>> map) {
    if (map.size() != 1) {
      throw new IllegalStateException(
        "Expected map with single entry, but found " +
        map.size() +
        " entries: " +
        sortedMapString(map)
      );
    }
    var entry = map.entrySet().stream().findAny().orElseThrow();
    var items = entry.getValue();

    if (items.size() != 1) {
      throw new IllegalStateException(
        "Expected map with single entry, but found \"%s\" with %d values: %s".formatted(
            entry.getKey(),
            items.size(),
            items
          )
      );
    }

    return items.getFirst();
  }
}
