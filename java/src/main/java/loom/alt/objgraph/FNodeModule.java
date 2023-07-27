package loom.alt.objgraph;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class FNodeModule extends SimpleModule {
  public FNodeModule() {
    super("FNodeModule");
    addKeyDeserializer(JNSName.class, new JNSName.JsonSupport.KeyDeserializer());
  }
}
