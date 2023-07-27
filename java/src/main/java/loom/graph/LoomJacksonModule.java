package loom.graph;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module to support {@link LoomGraph}.
 */
public class LoomJacksonModule extends SimpleModule {
    public LoomJacksonModule() {
        super("LoomGraphModule");
        addKeyDeserializer(NSName.class, new NSName.JsonSupport.KeyDeserializer());
    }
}
