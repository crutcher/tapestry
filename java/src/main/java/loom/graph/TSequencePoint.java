package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonTypeName("SequencePoint")
public class TSequencePoint extends TSequencedBase {
  @JsonCreator
  TSequencePoint(@Nullable @JsonProperty(value = "id", required = true) UUID id) {
    super(id);
  }

  TSequencePoint() {
    this((UUID) null);
  }

  TSequencePoint(TSequencePoint source) {
    this(source.id);
  }

  @Override
  public TSequencePoint copy() {
    return new TSequencePoint(this);
  }
}
