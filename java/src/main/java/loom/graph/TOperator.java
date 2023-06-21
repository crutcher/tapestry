package loom.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("operator")
@Jacksonized
@SuperBuilder
public class TOperator extends TNode {}
