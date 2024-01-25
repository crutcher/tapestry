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
import org.tensortapestry.loom.zspace.ZRangeProjectionMap;

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
    public IPFSignatureBuilder inputs(@Nonnull Map<String, List<ZRangeProjectionMap>> ipfs) {
      this.inputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder input(
      @Nonnull String name,
      @Nonnull List<ZRangeProjectionMap> ipfs
    ) {
      this.inputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder input(@Nonnull String name, @Nonnull ZRangeProjectionMap ipf) {
      this.inputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder outputs(@Nonnull Map<String, List<ZRangeProjectionMap>> ipfs) {
      this.outputs = ipfs;
      return this;
    }

    @CanIgnoreReturnValue
    public IPFSignatureBuilder output(
      @Nonnull String name,
      @Nonnull List<ZRangeProjectionMap> ipfs
    ) {
      this.outputs.put(name, new ArrayList<>(ipfs));
      return this;
    }

    public IPFSignatureBuilder output(@Nonnull String name, @Nonnull ZRangeProjectionMap ipf) {
      this.outputs.computeIfAbsent(name, k -> new ArrayList<>()).add(ipf);
      return this;
    }
  }

  @Nonnull
  Map<String, List<ZRangeProjectionMap>> inputs;

  @Nonnull
  Map<String, List<ZRangeProjectionMap>> outputs;
}
