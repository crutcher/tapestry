package org.tensortapestry.weft.metakernels.expressions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.tensortapestry.common.collections.EnumerationUtils;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsLexer;
import org.tensortapestry.weft.metakernels.antlr.generated.IndexedDimShapesExpressionsParser;
import org.tensortapestry.zspace.ZPoint;

@Value
public class TensorShapePatternMatcher {

  @Data(staticConstructor = "of")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class Symbol {

    @Nonnull
    private final String name;

    public boolean hasIndexVar() {
      return false;
    }

    public String getIndexVar() {
      return null;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  public static class IndexedSymbol extends Symbol {

    public static IndexedSymbol of(String name, String index) {
      return new IndexedSymbol(name, index);
    }

    String indexVar;

    public IndexedSymbol(String name, String indexVar) {
      super(name);
      this.indexVar = indexVar;
    }

    @Override
    public String toString() {
      return getName() + "[" + indexVar + "]";
    }

    @Override
    public boolean hasIndexVar() {
      return true;
    }
  }

  @Data
  @RequiredArgsConstructor
  public abstract static class PatternItem {

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class SimpleDim extends PatternItem {

      public SimpleDim(Symbol name) {
        super(name);
      }

      @Override
      public String toString() {
        return symbol.toString();
      }

      @Override
      public List<PatternItem> leaves() {
        return List.of(this);
      }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class EllipsisGroup extends PatternItem {

      public EllipsisGroup(Symbol name) {
        super(name);
      }

      @Override
      public String toString() {
        return symbol.toString() + "...";
      }

      @Override
      public List<PatternItem> leaves() {
        return List.of(this);
      }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class PatternGroup extends PatternItem {

      List<PatternItem> items;

      public PatternGroup(Symbol name, List<PatternItem> items) {
        super(name);
        this.items = items;
      }

      @Override
      public String toString() {
        return (
          symbol.toString() +
          "=" +
          items.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"))
        );
      }

      @Override
      public List<PatternItem> leaves() {
        return items.stream().flatMap(i -> i.leaves().stream()).toList();
      }
    }

    final Symbol symbol;

    public abstract List<PatternItem> leaves();
  }

  public static TensorShapePatternMatcher parse(String source) {
    var lexer = new IndexedDimShapesExpressionsLexer(CharStreams.fromString(source));
    var tokens = new CommonTokenStream(lexer);
    var parser = new IndexedDimShapesExpressionsParser(tokens);
    var tree = parser.prog();

    var visitor = new TensorShapePatternExpressionVisitor();
    @SuppressWarnings("unchecked")
    var items = (List<PatternItem>) visitor.visit(tree);

    return new TensorShapePatternMatcher(items);
  }

  List<PatternItem> items;
  List<PatternItem> leaves;
  List<PatternItem.PatternGroup> depthFirstGroupOrder;
  int ellipsisStart;
  Set<String> indexNames;

  private TensorShapePatternMatcher(List<PatternItem> items) {
    this.items = List.copyOf(items);

    Set<String> names = new HashSet<>();
    Set<String> indexNames = new HashSet<>();
    Set<String> duplicates = new LinkedHashSet<>();

    List<PatternItem> leaves = items.stream().flatMap(i -> i.leaves().stream()).toList();

    {
      var ess = EnumerationUtils
        .enumerate(leaves)
        .stream()
        .filter(e -> e.getValue() instanceof PatternItem.EllipsisGroup)
        .toList();

      if (ess.size() > 1) {
        throw new IllegalArgumentException(
          "Multiple ellipsis: " + ess.stream().map(Map.Entry::getValue).toList()
        );
      }
      if (ess.size() == 1) {
        this.ellipsisStart = ess.getFirst().getKey();
      } else {
        this.ellipsisStart = -1;
      }
    }

    List<PatternItem> visitQueue = new ArrayList<>(items);
    List<PatternItem.PatternGroup> depthFirstGroupOrder = new ArrayList<>();
    while (!visitQueue.isEmpty()) {
      var expr = visitQueue.removeFirst();

      var patternName = expr.getSymbol();

      if (!names.add(patternName.getName())) {
        duplicates.add(patternName.getName());
      }

      var sym = expr.getSymbol();
      if (sym.hasIndexVar()) {
        indexNames.add(sym.getIndexVar());
      }

      if (expr instanceof PatternItem.PatternGroup patternGroup) {
        depthFirstGroupOrder.addFirst(patternGroup);
        visitQueue.addAll(patternGroup.getItems());
      }
    }
    this.indexNames = Set.copyOf(indexNames);
    this.leaves = List.copyOf(leaves);
    this.depthFirstGroupOrder = List.copyOf(depthFirstGroupOrder);

    var overlap = new HashSet<>(names);
    overlap.retainAll(indexNames);
    if (!overlap.isEmpty()) {
      throw new IllegalArgumentException("Overlap between names and index names: " + overlap);
    }

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Duplicate names: " + duplicates);
    }
  }

  public String toExpression() {
    return items.toString();
  }

  public TensorShapePatternMatch match(ZPoint shape) {
    int numDims = shape.getNDim();
    var smBuilder = TensorShapePatternMatch.builder().shape(shape);
    smBuilder.pattern(this);

    var leaves = getLeaves();

    int minSize = leaves.size();
    int ellipsisStart = getEllipsisStart();

    if (ellipsisStart != -1) {
      minSize--;
    }

    if (
      (ellipsisStart == -1 && numDims != leaves.size()) ||
      (ellipsisStart != -1 && numDims < minSize)
    ) {
      throw new IllegalArgumentException(
        "Mismatched pattern and dims (%d): %s".formatted(numDims, this)
      );
    }

    var dims = new HashMap<String, TensorShapePatternMatch.DimMatch>();
    var groups = new HashMap<String, TensorShapePatternMatch.GroupMatch>();

    int dimOffset = 0;
    for (int leafIdx = 0; leafIdx < leaves.size(); leafIdx++) {
      var pattern = leaves.get(leafIdx);

      var patternName = pattern.getSymbol();
      var rawName = patternName.getName();

      if (leafIdx == ellipsisStart) {
        int expansionSize = numDims - (leaves.size() - 1);
        int start = ellipsisStart + dimOffset;
        int end = start + expansionSize;
        var groupMatch = new TensorShapePatternMatch.GroupMatch(
          rawName,
          start,
          end,
          ZPoint.of(shape.unwrap().sliceDim(0, start, end))
        );
        groups.put(rawName, groupMatch);

        dimOffset = expansionSize - 1;
        continue;
      }

      var val = shape.get(leafIdx + dimOffset);
      var dimMatch = new TensorShapePatternMatch.DimMatch(rawName, leafIdx + dimOffset, val);
      dims.put(rawName, dimMatch);
    }

    for (var patternGroup : getDepthFirstGroupOrder()) {
      int start;
      {
        var first = patternGroup.getItems().getFirst();
        if (first instanceof PatternItem.SimpleDim) {
          start = dims.get(first.getSymbol().getName()).getIndex();
        } else {
          start = groups.get(first.getSymbol().getName()).getStart();
        }
      }

      int end;
      {
        var last = patternGroup.getItems().getLast();
        if (last instanceof PatternItem.SimpleDim) {
          end = dims.get(last.getSymbol().getName()).getIndex() + 1;
        } else {
          end = groups.get(last.getSymbol().getName()).getEnd();
        }
      }

      List<Integer> value = new ArrayList<>();
      for (var p : patternGroup.getItems()) {
        var name = p.getSymbol().getName();
        if (p instanceof PatternItem.SimpleDim) {
          value.add(dims.get(name).getValue());
        } else {
          groups.get(name).getValue().unwrap().forEachValue(value::add);
        }
      }

      var groupMatch = new TensorShapePatternMatch.GroupMatch(
        patternGroup.getSymbol().getName(),
        start,
        end,
        ZPoint.of(value)
      );

      groups.put(groupMatch.getName(), groupMatch);
    }

    smBuilder.dims(dims);
    smBuilder.groups(groups);

    return smBuilder.build();
  }
}
