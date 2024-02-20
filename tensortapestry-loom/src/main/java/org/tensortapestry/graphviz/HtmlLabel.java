package org.tensortapestry.graphviz;

import lombok.Value;

@Value
public class HtmlLabel {

  public static HtmlLabel from(Object label) {
    return new HtmlLabel(label.toString());
  }

  String label;

  @Override
  public String toString() {
    return "<" + label + ">";
  }
}
