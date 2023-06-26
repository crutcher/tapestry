package loom.graph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@JsonTypeName("Parameters")
@TNodeBase.DisplayOptions.NodeAttributes(
    value = {
      @TNodeBase.DisplayOptions.Attribute(name = "shape", value = "tab"),
      @TNodeBase.DisplayOptions.Attribute(name = "fillcolor", value = "#E7DCB8"),
      @TNodeBase.DisplayOptions.Attribute(name = "margin", value = "0.15")
    })
public class TParameters extends TOperatorBase {
  @Nonnull @Getter public final Map<String, String> params;

  @JsonCreator
  public TParameters(
      @Nullable @JsonProperty(value = "id", required = true) UUID id,
      @Nullable @JsonProperty(value = "params") Map<String, String> params) {
    super(id);

    if (params == null) {
      this.params = Map.of();
    } else {
      this.params = Map.copyOf(params);
    }
  }

  public TParameters(@Nullable Map<String, String> params) {
    this(null, params);
  }

  public TParameters(@Nonnull TParameters source) {
    this(source.id, source.params);
  }

  @Override
  public TParameters copy() {
    return new TParameters(this);
  }

  @Override
  public Map<String, Object> displayData() {
    var data = super.displayData();
    data.remove("params");
    for (var entry : params.entrySet()) {
      data.put("@" + entry.getKey(), entry.getValue());
    }
    return data;
  }
}
