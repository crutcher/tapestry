package org.tensortapestry.common.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;
import org.tensortapestry.zspace.ZTensor;

public class JsonViewWrapperTest implements CommonAssertions, JsonUtil.WithNodeBuilders {

  @Jacksonized
  @Builder
  @Data
  public static class ExpNode {

    @SuppressWarnings("unused")
    public static class ExpNodeBuilder {

      public ExpNodeBuilder body(Object value) {
        if (value instanceof JsonViewWrapper wrapper) {
          this.body = wrapper;
        } else {
          this.body = JsonViewWrapper.of(value);
        }
        return this;
      }

      public ExpNodeBuilder annotation(String key, Object value) {
        if (this.annotations == null) {
          this.annotations = new HashMap<>();
        }
        if (value instanceof JsonViewWrapper wrapper) {
          this.annotations.put(key, wrapper);
        } else {
          this.annotations.put(key, JsonViewWrapper.of(value));
        }
        return this;
      }
    }

    private final JsonViewWrapper body;
    private final Map<String, JsonViewWrapper> annotations;

    public ExpNode(JsonViewWrapper body, Map<String, JsonViewWrapper> annotations) {
      this.body = body;
      this.annotations = new HashMap<>();
      if (annotations != null) {
        this.annotations.putAll(annotations);
      }
    }
  }

  @Test
  public void test_view_map() {
    var vm = ExpNode
      .builder()
      .body(ZTensor.newVector(1, 2, 3))
      .annotation("abc", ZTensor.newVector(4, 5, 6))
      .build();

    assertThat(vm.getBody().viewAs(ZTensor.class)).isEqualTo(ZTensor.newVector(1, 2, 3));

    assertJsonEquals(vm, "{\"body\":[1,2,3], \"annotations\":{\"abc\":[4,5,6]}}");
  }

  @Test
  public void test_null() {
    var wrapper = new JsonViewWrapper(nullNode());
    assertThat(wrapper.viewAs(List.class)).isNull();
  }

  @Test
  public void test_raw_container() {
    var wrapper = new JsonViewWrapper(arrayNode().add(1).add(2).add(3));

    assertEquivalentJson(JsonUtil.toJson(wrapper), "[1,2,3]");
    JsonUtil.fromJson("[1,2,3]", JsonViewWrapper.class);

    assertJsonEquals(wrapper, "[1,2,3]");

    assertThat(wrapper.viewAs(ZTensor.class))
      .isSameAs(wrapper.viewAs(ZTensor.class))
      .isEqualTo(ZTensor.newVector(1, 2, 3));

    assertThat(wrapper.viewAs(List.class)).isEqualTo(List.of(1, 2, 3));

    assertThatExceptionOfType(ViewConversionError.class)
      .isThrownBy(() -> wrapper.viewAs(Float.class))
      .withMessageContaining("Failed to convert [1,2,3] to class java.lang.Float");
  }
}
