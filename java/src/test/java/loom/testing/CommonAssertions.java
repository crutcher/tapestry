package loom.testing;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import loom.common.serialization.JsonUtil;
import org.assertj.core.api.WithAssertions;

public interface CommonAssertions extends WithAssertions {
  default void assertEquivalentJson(String actual, String expected) {
    actual = JsonUtil.reformatToPrettyJson(actual);
    expected = JsonUtil.reformatToPrettyJson(expected);

    // System.out.printf("assertJsonEquals.json1: %s%n", json1);
    // System.out.printf("assertJsonEquals.json2: %s%n", json2);

    assertThat(actual).describedAs("actual != expected").isEqualTo(expected);
  }

  default void assertJsonEquals(Object obj, String json) {

    json = JsonUtil.reformatToPrettyJson(json);

    String objJson = JsonUtil.toPrettyJson(obj);

    // We establish a 'clean' json by re-serializing the JSON derived object.
    var objFromJson = JsonUtil.fromJson(json, obj.getClass());
    var cleanJson = JsonUtil.toPrettyJson(objFromJson);

    // System.out.printf("assertJsonEquals.expectedJson: %s%n", json);
    // System.out.printf("assertJsonEquals.cleanedJson: %s%n", cleanJson);
    // System.out.printf("assertJsonEquals.objJson: %s%n", objJson);

    // Does the serialization of the source object to JSON match the cleaned JSON?
    assertThat(objJson).describedAs("Object Json != Source Json").isEqualTo(cleanJson);
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
