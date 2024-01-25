package org.tensortapestry.loom.zspace.experimental;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.assertj.core.api.WithAssertions;
import org.tensortapestry.loom.zspace.serialization.ZSpaceJsonUtil;

public interface ZSpaceTestAssertions extends WithAssertions {
  default void assertObjectJsonEquivalence(Object obj, String json) {
    String objJson = ZSpaceJsonUtil.toPrettyJson(obj);

    // We establish a 'clean' json by re-serializing the JSON derived object.
    var objFromJson = ZSpaceJsonUtil.fromJson(json, obj.getClass());
    var cleanJson = ZSpaceJsonUtil.toPrettyJson(objFromJson);

    assertThatJson(objJson).isEqualTo(cleanJson);
  }
}
