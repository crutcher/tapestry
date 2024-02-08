package org.tensortapestry.zspace.indexing;

import java.util.ArrayList;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class Slice extends Selector {

  @Nullable Integer start;

  @Nullable Integer stop;

  @Nullable Integer step;

  @Override
  public String toString() {
    var parts = new ArrayList<String>();
    if (start != null) {
      parts.add(start.toString());
    }
    parts.add(":");
    if (stop != null) {
      parts.add(stop.toString());
    }
    if (step != null) {
      parts.add(":");
      parts.add(step.toString());
    }
    return String.join("", parts);
  }
}
