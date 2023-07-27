package loom.alt.attrgraph;

import loom.testing.CommonAssertions;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoomSchemaTest implements CommonAssertions {
    @Test
    public void testBasic() {
        var schema = LoomSchema.builder()
                .urn(LoomBuiltinNS.BUILTINS_URN)
                .type("tensor", LoomSchema.Type.builder().name("tensor").build())
                .attribute("shape", LoomSchema.Attribute.builder().name("shape").build())
                .build();

        var env = new LoomEnvironment();
        env.addSchema(schema);

        assertThat(env.getType(LoomBuiltinNS.TENSOR)).isEqualTo(schema.getType("tensor"));
        assertThat(env.getAttribute(LoomBuiltinNS.SHAPE)).isEqualTo(schema.getAttribute("shape"));

        assertJsonEquals(
                schema,
                """
                {
                  "urn": "%s",
                  "types": {
                    "tensor": {
                      "name": "tensor"
                    }
                  },
                  "attributes": {
                    "shape": {
                      "name": "shape"
                    }
                  }
                }
                """
                        .formatted(LoomBuiltinNS.BUILTINS_URN)
        );
    }
}