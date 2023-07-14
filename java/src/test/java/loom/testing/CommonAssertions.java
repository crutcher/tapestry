package loom.testing;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import loom.common.JsonUtil;
import org.assertj.core.api.WithAssertions;

public interface CommonAssertions extends WithAssertions {
  default void assertJsonEquals(Object obj, String json) {
    String objJson = JsonUtil.toJson(obj);

    // We establish a 'clean' json by re-serializing the JSON derived object.
    var objFromJson = JsonUtil.fromJson(json, obj.getClass());
    var cleanJson = JsonUtil.toJson(objFromJson);

    // Does the serialization of the source object to JSON match the cleaned JSON?
    assertThat(objJson).isEqualTo(cleanJson);
  }

  default void assertJsonEquals(Object obj, JsonValue json) {
    assertJsonEquals(obj, json.toString());
  }

  default void assertJsonEquals(Object obj, JsonObjectBuilder json) {
    assertJsonEquals(obj, json.build());
  }

  default void assertJsonEquals(Object obj, JsonArrayBuilder json) {
    assertJsonEquals(obj, json.build());
  }
}
