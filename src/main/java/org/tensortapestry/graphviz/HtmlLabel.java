package org.tensortapestry.graphviz;

import lombok.Value;

@Value
public class HtmlLabel {

  public static HtmlLabel from(Object label) {
    return new HtmlLabel(label);
  }

  Object label;

  @Override
  public String toString() {
    return "<" + label.toString() + ">";
  }
}
