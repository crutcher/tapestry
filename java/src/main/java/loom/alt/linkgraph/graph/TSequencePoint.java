package loom.alt.linkgraph.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonTypeName("SequencePoint")
@TNodeBase.NodeDisplayOptions.NodeAttributes(
    value = {@TNodeBase.NodeDisplayOptions.Attribute(name = "fillcolor", value = "#deebf7")})
public class TSequencePoint extends TNodeBase implements TCanBeSequencedProperty {
  @JsonCreator
  public TSequencePoint(@Nullable @JsonProperty(value = "id", required = true) UUID id) {
    super(id);
  }

  public TSequencePoint() {
    this((UUID) null);
  }

  public TSequencePoint(TSequencePoint source) {
    this(source.id);
  }

  @Override
  public TSequencePoint copy() {
    return new TSequencePoint(this);
  }
}
