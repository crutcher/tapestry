package org.tensortapestry.jacquard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Value
public class MapExprSelection {

  public static class MapExprSelectionBuilder {
    {
      items = new ArrayList<>();
    }

    public MapExprSelectionBuilder item(Map.Entry<String, String> item) {
      items.add(item);
      return this;
    }

    public MapExprSelectionBuilder item(String key, String expr) {
      return item(java.util.Map.entry(key, expr));
    }
  }

  public static final String ANY = "*";
  public static final String NON_EMPTY = "+";

  @Singular
  List<Map.Entry<String, String>> items;

  Map<String, String> rangeMap;

  @JsonIgnore
  Set<String> keys;

  @JsonIgnore
  Map<String, Expression> expressionMap;

  static String reformat(String expr) {
    return expr.replaceAll("\\s+", "");
  }

  @Builder
  public MapExprSelection(List<Map.Entry<String, String>> items) {
    this.items =
      items
        .stream()
        .map(e -> Map.entry(e.getKey(), reformat(e.getValue())))
        .collect(Collectors.toList());

    keys = items.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    if (keys.size() != items.size()) {
      throw new IllegalArgumentException("Duplicate keys in selection: " + items);
    }

    var rangeMap = new HashMap<String, String>();
    var expressionMap = new HashMap<String, Expression>();
    for (var entry : items) {
      var name = entry.getKey();
      var expr = entry.getValue();

      if (expr == null) {
        throw new IllegalArgumentException("Null expression for key: " + name);
      }
      if (expr.equals(ANY) || expr.equals(NON_EMPTY)) {
        rangeMap.put(name, expr);
        continue;
      }

      var e = new ExpressionBuilder(expr).implicitMultiplication(true).variables(keys).build();

      if (e.getVariableNames().contains(name)) {
        throw new IllegalArgumentException(
          "Expression %s contains variable %s: %s".formatted(expr, name, this)
        );
      }

      expressionMap.put(name, e);
    }
    this.rangeMap = Map.copyOf(rangeMap);
    this.expressionMap = Map.copyOf(expressionMap);
  }

  @Override
  public String toString() {
    return items
      .stream()
      .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
      .collect(Collectors.joining(", ", "{", "}"));
  }

  public <T> void check(Map<String, List<T>> map) {
    Map<String, Double> variables = new HashMap<>();
    for (var name : keys) {
      var values = map.get(name);
      if (values == null) {
        throw new IllegalArgumentException("Missing key: " + name);
      }
      int size = values.size();
      variables.put(name, (double) size);
    }
    for (var name : keys) {
      var size = map.get(name).size();

      var range = rangeMap.get(name);
      if (range != null) {
        if (range.equals(ANY)) {
          continue;
        }
        if (range.equals(NON_EMPTY)) {
          if (size == 0) {
            throw new IllegalArgumentException("Empty list");
          }
          continue;
        }
        throw new RuntimeException("unreachable");
      }

      var expr = new Expression(expressionMap.get(name));
      var expected = (int) expr.setVariables(variables).evaluate();

      if (expected < 0) {
        throw new IllegalArgumentException("Negative value for %s: %s".formatted(name, expected));
      }
      if (size != expected) {
        throw new IllegalArgumentException(
          "Unexpected size for \"%s\", expected %d, found %d: %s".formatted(
              name,
              expected,
              size,
              this
            )
        );
      }
    }
  }
}
