package loom.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.Data;

@Data
public class LoomEnvironment {
  Map<String, LoomSchema> schemaMap = new HashMap<>();

  Map<String, String> aliasMap = new HashMap<>();

  public void addSchema(LoomSchema schema) {
    schemaMap.put(schema.getUrn(), schema);
  }

  public LoomSchema getSchema(String urn) {
    var schema = schemaMap.get(urn);
    if (schema == null) {
      throw new NoSuchElementException(urn);
    }
    return schema;
  }

  public LoomSchema.Type getType(NSName name) {
    return getSchema(name.urn()).getType(name.name());
  }

  public LoomSchema.Attribute getAttribute(NSName name) {
    return getSchema(name.urn()).getAttribute(name.name());
  }
}
