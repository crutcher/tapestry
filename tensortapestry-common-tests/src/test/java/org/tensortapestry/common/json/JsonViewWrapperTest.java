package org.tensortapestry.common.json;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class JsonViewWrapperTest implements CommonAssertions, JsonUtil.WithNodeBuilders {

  @Jacksonized
  @Builder
  @Data
  public static class ExpNode {

    @SuppressWarnings("unused")
    public static class ExpNodeBuilder {

      @CanIgnoreReturnValue
      public ExpNodeBuilder body(Object value) {
        if (value instanceof JsonViewWrapper wrapper) {
          this.body = wrapper;
        } else {
          this.body = JsonViewWrapper.of(value);
        }
        return this;
      }

      @CanIgnoreReturnValue
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

  @Value
  @Jacksonized
  @Builder
  public static class Example {

    List<Integer> data;
  }

  @Test
  public void test_view_map() {
    var example1 = Example.builder().data(List.of(1, 2, 3)).build();
    var example2 = Example.builder().data(List.of(4, 5, 6)).build();
    var vm = ExpNode.builder().body(example1).annotation("abc", example2).build();

    assertThat(vm.getBody().viewAs(Example.class)).isEqualTo(example1);

    assertJsonEquals(
      vm,
      "{\"body\":{\"data\":[1,2,3]}, \"annotations\":{\"abc\":{\"data\":[4,5,6]}}}"
    );
  }

  @Test
  public void test_null() {
    var wrapper = new JsonViewWrapper(nullNode());
    assertThat(wrapper.viewAs(List.class)).isNull();
  }

  @Test
  public void test_raw_container() {
    String source = "{\"data\": [1,2,3]}";
    var wrapper = new JsonViewWrapper(JsonUtil.parseToJsonNodeTree(source));

    assertEquivalentJson(JsonUtil.toJson(wrapper), source);
    JsonUtil.fromJson(source, JsonViewWrapper.class);

    assertJsonEquals(wrapper, source);

    assertThat(wrapper.viewAs(Example.class))
      .isSameAs(wrapper.viewAs(Example.class))
      .isEqualTo(Example.builder().data(List.of(1, 2, 3)).build());

    assertThat(wrapper.viewAs(Object.class)).isEqualTo(Map.of("data", List.of(1, 2, 3)));

    assertThatExceptionOfType(ViewConversionError.class)
      .isThrownBy(() -> wrapper.viewAs(Float.class))
      .withMessageContaining("Failed to convert <{\"data\":[1,2,3]}> to class java.lang.Float");
  }
}
