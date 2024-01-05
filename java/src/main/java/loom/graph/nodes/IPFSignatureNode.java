package loom.graph.nodes;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import loom.common.json.HasToJsonString;
import loom.graph.LoomEnvironment;
import loom.graph.LoomNode;
import loom.graph.constraints.IPFSignatureAgreementConstraint;

@Jacksonized
@SuperBuilder
@Getter
@Setter
@LoomEnvironment.WithConstraints({IPFSignatureAgreementConstraint.class})
public class IPFSignatureNode extends LoomNode<IPFSignatureNode, IPFSignature> {
  public static final String TYPE = "IPFSignatureNode";

  public abstract static class IPFSignatureNodeBuilder<
          C extends IPFSignatureNode, B extends IPFSignatureNodeBuilder<C, B>>
      extends LoomNodeBuilder<IPFSignatureNode, IPFSignature, C, B> {
    {
      // Set the node type.
      type(TYPE);
    }
  }

  public static IPFSignatureNodeBuilder<?, ?> withBody(
      Consumer<IPFSignature.IPFSignatureBuilder> cb) {
    var bodyBuilder = IPFSignature.builder();
    cb.accept(bodyBuilder);
    return builder().body(bodyBuilder.build());
  }

  @Delegate(excludes = {HasToJsonString.class})
  @Nonnull
  private IPFSignature body;
}
