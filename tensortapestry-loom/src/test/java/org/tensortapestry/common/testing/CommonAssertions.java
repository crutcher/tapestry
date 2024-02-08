package org.tensortapestry.common.testing;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.tensortapestry.common.json.JsonUtil;
import org.tensortapestry.common.text.PrettyDiffUtils;
import org.tensortapestry.common.validation.ListValidationIssueCollector;
import org.tensortapestry.common.validation.ValidationIssue;
import org.tensortapestry.zspace.experimental.ZSpaceTestAssertions;

public interface CommonAssertions extends ZSpaceTestAssertions {
    default void assertValidationIssues(
            ListValidationIssueCollector collector,
            ValidationIssue... expected
    ) {
        assertValidationIssues(collector.getIssues(), expected);
    }

    default void assertValidationIssues(
            @Nullable List<ValidationIssue> issues,
            ValidationIssue... expected
    ) {
        assertThat(issues).isNotNull();
        assert issues != null;

        var cmp = Comparator.comparing(JsonUtil::toJson);
        var sortedIssues = issues.stream().sorted(cmp).toList();
        var sortedExpected = Stream.of(expected).sorted(cmp).toList();
        assertEquivalentJson(JsonUtil.toJson(sortedIssues), JsonUtil.toJson(sortedExpected));
    }

    @SuppressWarnings("InconsistentOverloads")
    default void assertEquivalentJson(
            String actualName,
            String actual,
            String expectedName,
            String expected
    ) {
        // System.out.println("assertEquivalentJson.actual: " + actual);
        // System.out.println("assertEquivalentJson.expected: " + expected);

        var actualNode = JsonUtil.parseToJsonNodeTree(actual);
        var expectedNode = JsonUtil.parseToJsonNodeTree(expected);
        if (actualNode.equals(expectedNode)) {
            return;
        }

        var prettyActual = JsonUtil.reformatToPrettyJson(actual);
        var prettyExpected = JsonUtil.reformatToPrettyJson(expected);

        var diff = String.format(
                "JSON Comparison Error: %s != %s\n%s\n",
                actualName,
                expectedName,
                PrettyDiffUtils.indentUdiff("> ", prettyExpected, prettyActual)
        );

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
