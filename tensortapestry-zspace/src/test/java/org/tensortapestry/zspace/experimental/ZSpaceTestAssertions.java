package org.tensortapestry.zspace.experimental;

import net.javacrumbs.jsonunit.assertj.JsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.WithAssertions;
import org.jetbrains.annotations.Nullable;
import org.tensortapestry.zspace.impl.ZSpaceJsonUtil;

public interface ZSpaceTestAssertions extends WithAssertions {
  default void assertObjectJsonEquivalence(Object obj, String json) {
    try {
      assertThatJson(obj).isEqualTo(json);
    } catch (AssertionError e) {
      String objJson = ZSpaceJsonUtil.toPrettyJson(obj);
      // We establish a 'clean' json by re-serializing the JSON derived object.
      var objFromJson = ZSpaceJsonUtil.fromJson(json, obj.getClass());
      var cleanJson = ZSpaceJsonUtil.toPrettyJson(objFromJson);
      assertThatJson(objJson).isEqualTo(cleanJson);
    }
  }

  default JsonAssert.ConfigurableJsonAssert assertThatJson(@Nullable Object actual) {
    return JsonAssertions.assertThatJson(actual);
  }
}