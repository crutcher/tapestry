package loom.testing;

import loom.common.JsonUtil;
import org.assertj.core.api.WithAssertions;

public interface CommonAssertions extends WithAssertions {
  default void assertJsonEquals(Object obj, String json) {
    // Does the JSON parse as the object class?
    var objFromJson = JsonUtil.fromJson(json, obj.getClass());

    // Does the object match the objFromJson?
    assertThat(objFromJson).isEqualTo(obj);

    // We establish a 'clean' json by re-serializing the JSON derived object.
    var cleanJson = JsonUtil.toJson(objFromJson);

    String objJson = JsonUtil.toJson(obj);

    // Does the serialization of the source object to JSON match
    // the cleaned JSON?
    assertThat(objJson).isEqualTo(cleanJson);
  }
}
