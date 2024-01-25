package org.tensortapestry.loom.graph.dialects.tensorops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.tensortapestry.loom.common.json.HasToJsonString;
import org.tensortapestry.loom.zspace.IndexProjectionFunction;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IPFSignature implements HasToJsonString {

  @SuppressWarnings("unused")
  public static class IPFSignatureBuilder {
    {
      this.inputs = new HashMap<>();
      this.outputs = new HashMap<>();
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder inputs(@Nonnull Map<String, List<IndexProjectionFunction>> ipfs) {
      this.inputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder input(
      @Nonnull String name,
      @Nonnull List<IndexProjectionFunction> ipfs
    ) {
      this.inputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder input(@Nonnull String name, @Nonnull IndexProjectionFunction ipf) {
      this.inputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder outputs(@Nonnull Map<String, List<IndexProjectionFunction>> ipfs) {
      this.outputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder output(
      @Nonnull String name,
      @Nonnull List<IndexProjectionFunction> ipfs
    ) {
      this.outputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder output(@Nonnull String name, @Nonnull IndexProjectionFunction ipf) {
      this.outputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }
  }

  @Nonnull
  Map<String, List<IndexProjectionFunction>> inputs;

  @Nonnull
  Map<String, List<IndexProjectionFunction>> outputs;
}
