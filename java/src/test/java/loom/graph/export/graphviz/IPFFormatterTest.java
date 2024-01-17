package loom.graph.export.graphviz;

import loom.polyhedral.IndexProjectionFunction;
import loom.testing.BaseTestClass;
import loom.testing.XmlAssertions;
import loom.zspace.ZAffineMap;
import loom.zspace.ZTensor;
import org.junit.Test;

public class IPFFormatterTest extends BaseTestClass implements XmlAssertions {
  @Test
  public void test_3x2() {
    var ipf =
        IndexProjectionFunction.builder()
            .affineMap(new int[][] {{1, 1, 0}, {0, 1, 1}})
            .translate(2, 3)
            .shape(4, 4)
            .build();

    assertXmlEquals(
        IPFFormatter.renderIPF(ipf),
        """
        <table border="0" cellborder="0" cellspacing="0">
          <tr>
            <td>
              <table border="0" cellborder="0" cellspacing="0">
                <tr>
                  <td border="1" sides="L">1</td>
                  <td>1</td>
                  <td border="1" sides="R">0</td>
                  <td border="1" sides="R">2</td>
                </tr>
                <tr>
                  <td border="1" sides="L">0</td>
                  <td>1</td>
                  <td border="1" sides="R">1</td>
                  <td border="1" sides="R">3</td>
                </tr>
              </table>
            </td>
            <td> ⊕ [4, 4]</td>
          </tr>
        </table>
        """);
  }

  @Test
  public void test_2x1() {
    var ipf =
        IndexProjectionFunction.builder()
            .affineMap(new int[][] {{1}, {0}})
            .translate(2, 3)
            .shape(4, 4)
            .build();

    assertXmlEquals(
        IPFFormatter.renderIPF(ipf),
        """
        <table border="0" cellborder="0" cellspacing="0">
          <tr>
            <td>
              <table border="0" cellborder="0" cellspacing="0">
                <tr>
                  <td border="1" sides="LR">1</td>
                  <td border="1" sides="R">2</td>
                </tr>
                <tr>
                  <td border="1" sides="LR">0</td>
                  <td border="1" sides="R">3</td>
                </tr>
              </table>
            </td>
            <td> ⊕ [4, 4]</td>
          </tr>
        </table>
        """);
  }

  @Test
  public void test_scalar() {
    var ipf =
        IndexProjectionFunction.builder()
            .affineMap(ZAffineMap.fromMatrix(ZTensor.newZeros(0, 3)))
            .build();

    assertXmlEquals(
        IPFFormatter.renderIPF(ipf),
        """
        <table border="0" cellborder="0" cellspacing="0">
          <tr>
            <td>[−]<sub>0×3</sub> ⊕ []</td>
          </tr>
        </table>
        """);
  }
}
