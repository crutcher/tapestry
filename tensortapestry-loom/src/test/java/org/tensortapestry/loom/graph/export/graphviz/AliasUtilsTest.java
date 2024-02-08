package org.tensortapestry.loom.graph.export.graphviz;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.BaseTestClass;

public class AliasUtilsTest extends BaseTestClass {

  @Test
  public void test_uuidAliasMap() {
    var id1 = UUID.fromString("3e12c250-0104-44fe-9327-9687b21cf903");
    var id2 = UUID.fromString("dfdf267f-df43-49a1-9d96-6135342b8ab1");
    var id3 = UUID.fromString("ef5ed0d8-a2ff-4474-abd6-29584ecbaad5");
    var id4 = UUID.fromString("618a3d04-e018-46fd-9df4-1ed72f7288b4");

    var ids = List.of(id1, id2, id3, id4);

    assertThat(AliasUtils.uuidAliasMap(ids, 2))
      .containsEntry(id1, "3ad")
      .containsEntry(id2, "3a4")
      .containsEntry(id3, "3a2")
      .containsEntry(id4, "3a8");

    assertThat(AliasUtils.uuidAliasMap(ids, 4))
      .containsEntry(id1, "3add")
      .containsEntry(id2, "3a45")
      .containsEntry(id3, "3a27")
      .containsEntry(id4, "3a8e");
  }
}
