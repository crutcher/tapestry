package loom.testing;

import java.util.List;
import loom.common.json.JsonUtil;
import loom.common.text.PrettyDiffUtils;
import loom.validation.ValidationIssue;
import org.assertj.core.api.WithAssertions;

public interface CommonAssertions extends WithAssertions {
  default void assertValidationIssues(List<ValidationIssue> issues, ValidationIssue... expected) {
    assertEquivalentJson(JsonUtil.toJson(issues), JsonUtil.toJson(expected));
  }

  default void assertEquivalentJson(
      String actualName, String actual, String expectedName, String expected) {
    // System.out.println("assertEquivalentJson.actual: " + actual);
    // System.out.println("assertEquivalentJson.expected: " + expected);

    var actualNode = JsonUtil.parseToJsonNodeTree(actual);
    var expectedNode = JsonUtil.parseToJsonNodeTree(expected);
    if (actualNode.equals(expectedNode)) {
      return;
    }

    var prettyActual = JsonUtil.reformatToPrettyJson(actual);
    var prettyExpected = JsonUtil.reformatToPrettyJson(expected);

    var diff =
        String.format(
            "JSON Comparison Error: %s != %s\n%s\n",
            actualName,
            expectedName,
            PrettyDiffUtils.indentUdiff("> ", prettyExpected, prettyActual));

    assertThat(prettyActual).as(diff).isEqualTo(prettyExpected);
  }

  default void assertEquivalentJson(String actual, String expected) {
    assertEquivalentJson("actual", actual, "expected", expected);
  }

  default void assertJsonEquals(Object obj, String json) {
    String objJson = JsonUtil.toPrettyJson(obj);

    // We establish a 'clean' json by re-serializing the JSON derived object.
    var objFromJson = JsonUtil.fromJson(json, obj.getClass());
    var cleanJson = JsonUtil.toPrettyJson(objFromJson);

    // System.out.printf("assertJsonEquals.expectedJson: %s%n", json);
    // System.out.printf("assertJsonEquals.cleanedJson: %s%n", cleanJson);
    // System.out.printf("assertJsonEquals.objJson: %s%n", objJson);

    // Does the serialization of the source object to JSON match the cleaned JSON?
    assertEquivalentJson("Object Json", objJson, "Source Json", cleanJson);
  }

  /*
   default void assertJsonEquals(Object obj, JsonValue json) {
     assertJsonEquals(obj, json.toString());
   }
   default void assertJsonEquals(Object obj, JsonObjectBuilder json) {
    assertJsonEquals(obj, json.build());
  }

  default void assertJsonEquals(Object obj, JsonArrayBuilder json) {
    assertJsonEquals(obj, json.build());
  }
    */
}
