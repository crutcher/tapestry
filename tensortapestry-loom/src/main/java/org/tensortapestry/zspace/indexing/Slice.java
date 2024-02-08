package org.tensortapestry.zspace.indexing;

import java.util.ArrayList;
import javax.annotation.Nullable;
import lombok.*;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Slice extends Selector {

  @Builder.Default
  @Nullable Integer start = null;

  @Builder.Default
  @Nullable Integer end = null;

  @Builder.Default
  @Nullable Integer step = null;

  public Slice(@Nullable Integer start, @Nullable Integer end) {
    this(start, end, null);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public String toString() {
    var parts = new ArrayList<String>();
    if (start != null) {
      parts.add(start.toString());
    }
    parts.add(":");
    if (end != null) {
      parts.add(end.toString());
    }
    if (step != null) {
      parts.add(":");
      parts.add(step.toString());
    }
    return String.join("", parts);
  }
}
