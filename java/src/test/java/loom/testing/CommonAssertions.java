package loom.testing;

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
}
