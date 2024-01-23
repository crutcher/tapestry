package org.tensortapestry.loom.graph.nodes;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.zspace.ZRange;

@Value
@Jacksonized
@Builder
public class IPFIndex {

  public static final String ANNOTATION_TYPE = "IPFIndex";

  ZRange range;
}
