package org.tensortapestry.graphviz;

import javax.annotation.Nonnull;

public final class GraphvizAttributeMap
  extends AbstractGraphvizAttributeMap<GraphvizAttributeMap, GraphvizAttribute> {

  @Override
  public void validateAttribute(@Nonnull String key, @Nonnull Object value) {
    checkKey("graphviz", GraphvizAttribute.class, key, value);
  }
}
