package loom.graph.nodes;

import java.util.UUID;
import loom.testing.BaseTestClass;
import loom.validation.ListValidationIssueCollector;
import loom.validation.LoomValidationError;
import loom.validation.ValidationIssue;
import loom.zspace.ZPoint;
import loom.zspace.ZRange;
import org.junit.Test;

public class TensorNodeTest extends BaseTestClass {
  @Test
  public void test_invalidBody() {
    assertThatExceptionOfType(LoomValidationError.class)
        .isThrownBy(() -> TensorNode.Body.builder().dtype("int32").shape(ZPoint.of(-2)).build())
        .withMessageContaining("shape must be positive and non-empty: [-2]");

    assertThatExceptionOfType(LoomValidationError.class)
        .isThrownBy(
            () ->
                TensorNode.Body.builder()
                    .dtype("int32")
                    .shape(ZPoint.of(1, 2))
                    .origin(ZPoint.of(-2))
                    .build())
        .withMessageContaining("origin [-2] dimensions != shape [1, 2] dimensions");
  }

  @Test
  public void test_scalar_body() {
    {
      // Without origin
      var body = TensorNode.Body.builder().dtype("int32").shape(ZPoint.of()).build();
      assertJsonEquals(
          body,
          """
            {
                "dtype": "int32",
                "shape": []
            }
            """);

      assertThat(body.getNDim()).isEqualTo(0);
      assertThat(body.getSize()).isEqualTo(1);
      assertThat(body.getShape()).isEqualTo(ZPoint.of());
      assertThat(body.getOrigin()).isNull();

      assertThat(body.getEffectiveOrigin()).isEqualTo(ZPoint.of());
      assertThat(body.getEffectiveRange()).isEqualTo(ZRange.fromShape());
    }

    {
      // With origin
      var body =
          TensorNode.Body.builder().dtype("int32").shape(ZPoint.of()).origin(ZPoint.of()).build();
      assertJsonEquals(
          body,
          """
                {
                    "dtype": "int32",
                    "shape": [],
                    "origin": []
                }
                """);

      assertThat(body.getNDim()).isEqualTo(0);
      assertThat(body.getSize()).isEqualTo(1);
      assertThat(body.getShape()).isEqualTo(ZPoint.of());

      assertThat(body.getOrigin()).isEqualTo(ZPoint.of());
      assertThat(body.getEffectiveOrigin()).isEqualTo(ZPoint.of());
      assertThat(body.getEffectiveRange()).isEqualTo(ZRange.fromShape());
    }
  }

  @Test
  public void test_body() {
    {
      // Without origin
      var body = TensorNode.Body.builder().dtype("int32").shape(ZPoint.of(2, 3)).build();

      assertJsonEquals(
          body,
          """
          {
            "dtype": "int32",
            "shape": [2, 3]
          }
          """);

      assertThat(body.getNDim()).isEqualTo(2);
      assertThat(body.getSize()).isEqualTo(6);

      assertThat(body.getOrigin()).isNull();

      assertThat(body).hasToString("TensorNode.Body(dtype=int32, shape=[2, 3], origin=null)");

      assertThat(body.getEffectiveOrigin()).isEqualTo(ZPoint.newZeros(2));
      assertThat(body.getEffectiveRange()).isEqualTo(ZRange.fromShape(2, 3));
    }

    {
      // With origin
      var body =
          TensorNode.Body.builder()
              .dtype("int32")
              .shape(ZPoint.of(2, 3))
              .origin(ZPoint.of(-1, 2))
              .build();

      assertJsonEquals(
          body,
          """
            {
              "dtype": "int32",
              "shape": [2, 3],
              "origin": [-1, 2]
            }
            """);

      assertThat(body.getNDim()).isEqualTo(2);
      assertThat(body.getSize()).isEqualTo(6);

      assertThat(body.getOrigin()).isEqualTo(ZPoint.of(-1, 2));

      assertThat(body).hasToString("TensorNode.Body(dtype=int32, shape=[2, 3], origin=[-1, 2])");

      assertThat(body.getEffectiveOrigin()).isEqualTo(ZPoint.of(-1, 2));
      assertThat(body.getEffectiveRange())
          .isEqualTo(ZRange.fromStartWithShape(ZPoint.of(-1, 2), ZPoint.of(2, 3)));
    }
  }

  @Test
  public void test_protoType() {
    var proto = TensorNode.Prototype.builder().validDType("int32").build();

    proto.addValidDType("float32");

    assertThat(proto.getValidDTypes()).contains("int32", "float32");

    {
      var issueCollector = new ListValidationIssueCollector();
      var tensor =
          TensorNode.withBody(b -> b.dtype("xyz").shape(ZPoint.of(2, 3)))
              .id(UUID.randomUUID())
              .build();
      proto.validateNode(tensor, issueCollector);
      assertThat(issueCollector.getIssues())
          .containsOnly(
              ValidationIssue.builder()
                  .type("NodeValidationError")
                  .summary("dtype (xyz) must be one of [int32, float32]")
                  .build());
    }
  }
}
