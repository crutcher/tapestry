package org.tensortapestry.jacquard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
public class MapSelectionSpec {

  @SuppressWarnings("unused")
  public static class MapSelectionSpecBuilder {
    {
      items = new ArrayList<>();
      exhaustive = true;
    }

    public MapSelectionSpecBuilder nonempty_list(String name) {
      return item(Item.builder().name(name).nonempty_list().build());
    }

    public MapSelectionSpecBuilder empty_list(String name) {
      return item(Item.builder().name(name).empty_list().build());
    }

    public MapSelectionSpecBuilder item(Item item) {
      items.add(item);
      return this;
    }

    public MapSelectionSpecBuilder item(String name, int shape) {
      return item(Item.builder().name(name).shape(shape).build());
    }
  }

  @Value
  @Builder
  @RequiredArgsConstructor
  public static class Item {

    @SuppressWarnings("unused")
    public static class ItemBuilder {

      public Item.ItemBuilder nonempty_list() {
        return shape(Item.NONEMPTY_LIST);
      }

      public Item.ItemBuilder empty_list() {
        return shape(Item.EMPTY_LIST);
      }
    }

    /**
     * Indicates that {@link #shape} is required list; which must not be empty.
     */
    public static final int NONEMPTY_LIST = -1;

    /**
     * Indicates that {@link #shape} is required list; which may be empty.
     */
    public static final int EMPTY_LIST = 0;

    String name;
    int shape;

    <T> void check(Map<String, Integer> sizeMap, Map<String, List<T>> map) {
      var size = sizeMap.get(name);
      if (shape != Item.EMPTY_LIST && size == 0) {
        throw new IllegalStateException(
          "Expected non-empty \"%s\": %s".formatted(name, SelectionMapUtils.sortedMapString(map))
        );
      }
      if (shape > 0 && size != shape) {
        throw new IllegalStateException(
          "Expected size %d for \"%s\", but found %d: %s".formatted(
              shape,
              name,
              size,
              SelectionMapUtils.sortedMapString(map)
            )
        );
      }
    }
  }

  public MapSelectionSpec(List<Item> items, boolean exhaustive) {
    this.items = List.copyOf(items);
    this.exhaustive = exhaustive;
    if (items.stream().map(Item::getName).distinct().count() != items.size()) {
      throw new IllegalArgumentException("Duplicate item names: " + items);
    }
  }

  @Nonnull
  List<Item> items;

  boolean exhaustive;

  /**
   * Check that the map contains all the required items, and that the items have the correct
   * shape.
   *
   * @param map the map to check.
   * @param <T> the type of the values.
   * @throws IllegalStateException if the map does not contain all the required items, or
   *         if the items have the wrong shape.
   */
  public <T> void check(@Nonnull Map<String, List<T>> map) {
    Map<String, Integer> sizeMap = new HashMap<>();
    for (var item : items) {
      var name = item.name;
      var values = map.get(name);
      if (values == null) {
        throw new IllegalStateException(
          "Missing required item \"%s\": %s".formatted(name, SelectionMapUtils.sortedMapString(map))
        );
      }
      sizeMap.put(name, values.size());
    }

    if (isExhaustive() && items.size() != map.size()) {
      var itemNames = items.stream().map(Item::getName).toList();
      var extra = map.keySet().stream().filter(name -> !itemNames.contains(name)).sorted().toList();
      throw new IllegalStateException(
        "Expecting %s exhaustive keys, but found extra: %s".formatted(itemNames, extra)
      );
    }

    for (var item : items) {
      item.check(sizeMap, map);
    }
  }

  /**
   * Select the values from the map, in the order of the items in the spec.
   *
   * @param map the map to select from.
   * @param <T> the type of the values.
   * @return the selected values.
   * @throws IllegalStateException if the map does not contain all the required items, or
   *         if the items have the wrong shape.
   */
  @Nonnull
  public <T> List<List<T>> selectAsList(@Nonnull Map<String, List<T>> map) {
    check(map);
    return items.stream().map(item -> map.get(item.getName())).toList();
  }

  /**
   * Select the values from the map, in the order of the items in the spec.
   *
   * @param map the map to select from.
   * @param <T> the type of the values.
   * @return the selected values.
   * @throws IllegalStateException if the map does not contain all the required items, or
   *         if the items have the wrong shape.
   */
  @Nonnull
  public <T> Map<String, List<T>> selectAsMap(@Nonnull Map<String, List<T>> map) {
    check(map);
    return items.stream().collect(Collectors.toMap(Item::getName, item -> map.get(item.getName())));
  }
}
