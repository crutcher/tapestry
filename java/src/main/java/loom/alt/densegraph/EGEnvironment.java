package loom.alt.densegraph;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class EGEnvironment {
  @Singular @Nonnull
  private final Map<ScopedName, EGOperatorDefinition> signatures = new HashMap<>();

  /**
   * Lookup the signature for the given name.
   *
   * @param name The name to lookup.
   * @return The signature for the given name.
   */
  public EGOperatorDefinition getDef(ScopedName name) {
    return signatures.get(name);
  }

  /**
   * Add a signature to the graph.
   *
   * @param signature The signature to add.
   * @return The defs for chaining.
   */
  public EGEnvironment addDef(EGOperatorDefinition signature) {
    signatures.put(signature.name, signature);
    return this;
  }

  /**
   * Lookup the signatures for the given scope.
   *
   * @param scope The scope to lookup.
   * @return The signatures for the given scope.
   */
  public Map<String, EGOperatorDefinition> defsForScope(String scope) {
    var result = new HashMap<String, EGOperatorDefinition>();
    for (var entry : signatures.entrySet()) {
      if (entry.getKey().scope().equals(scope)) {
        result.put(entry.getKey().name(), entry.getValue());
      }
    }
    return result;
  }
}
