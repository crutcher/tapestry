package org.tensortapestry.loom.graph.export.graphviz;

import org.dom4j.DocumentHelper;
import org.dom4j.Text;
import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.BaseTestClass;
import org.tensortapestry.common.testing.XmlAssertions;

public class GHTest extends BaseTestClass implements XmlAssertions {

  @Test
  public void test_NodeWrapper() {
    var wrapper = new GH.NodeWrapper<>(DocumentHelper.createText("hello"));

    var b = GH.bold();
    wrapper.withParent(b);

    assertThat(b.toString()).isEqualTo("<b>hello</b>");
  }

  @Test
  public void test_ElementWrapper() {
    var hello = new GH.ElementWrapper<>(DocumentHelper.createElement("hello"))
      .attr("foo", "bar")
      .add("some text", GH.br(), GH.bold())
      .add(new Object[] { GH.vr() });

    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> hello.add(new Object()))
      .withMessageContaining(
        "Cannot add instances of class java.lang.Object to an Element: java.lang.Object@"
      );

    hello.addXml("<font color=\"red\">red text</font>abc<br/><vr/>");

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> hello.addXml("<x>abc"));

    assertThat(hello.getAttr("foo")).isEqualTo("bar");

    assertXmlEquals(
      hello,
      "<hello foo=\"bar\">some text<br/><b/><vr/><font color=\"red\">red text</font>abc<br/><vr/></hello>"
    );
  }

  @Test
  public void test_text() {
    assertThat(GH.text("hello"))
      .isInstanceOf(GH.NodeWrapper.class)
      .extracting(GH.NodeWrapper::getNode)
      .isInstanceOf(Text.class);
  }

  @Test
  public void test_rules() {
    assertThat(GH.br()).isInstanceOf(GH.ElementWrapper.class).hasToString("<br/>");

    assertThat(GH.br(GH.HorizontalAlign.CENTER)).hasToString("<br align=\"CENTER\"/>");

    assertThat(GH.vr()).isInstanceOf(GH.ElementWrapper.class).hasToString("<vr/>");

    assertThat(GH.hr()).isInstanceOf(GH.ElementWrapper.class).hasToString("<hr/>");
  }

  @Test
  public void test_style_containers() {
    assertThat(GH.italic()).isInstanceOf(GH.ElementWrapper.class).hasToString("<i/>");
    assertXmlEquals(GH.italic("foo", GH.br()), "<i>foo<br/></i>");

    assertThat(GH.bold()).isInstanceOf(GH.ElementWrapper.class).hasToString("<b/>");
    assertXmlEquals(GH.bold("foo", GH.br()), "<b>foo<br/></b>");

    assertThat(GH.underline()).isInstanceOf(GH.ElementWrapper.class).hasToString("<u/>");
    assertXmlEquals(GH.underline("foo", GH.br()), "<u>foo<br/></u>");

    assertThat(GH.overline()).isInstanceOf(GH.ElementWrapper.class).hasToString("<o/>");
    assertXmlEquals(GH.overline("foo", GH.br()), "<o>foo<br/></o>");

    assertThat(GH.strike()).isInstanceOf(GH.ElementWrapper.class).hasToString("<s/>");
    assertXmlEquals(GH.strike("foo", GH.br()), "<s>foo<br/></s>");

    assertThat(GH.superscript()).isInstanceOf(GH.ElementWrapper.class).hasToString("<sup/>");
    assertXmlEquals(GH.superscript("foo", GH.br()), "<sup>foo<br/></sup>");

    assertThat(GH.subscript()).isInstanceOf(GH.ElementWrapper.class).hasToString("<sub/>");
    assertXmlEquals(GH.subscript("foo", GH.br()), "<sub>foo<br/></sub>");
  }

  @Test
  public void test_font() {
    assertThat(GH.font()).isInstanceOf(GH.ElementWrapper.class).hasToString("<font/>");

    assertXmlEquals(
      GH.font().color("red").face("Helvetica").pointSize(12).add("abc"),
      "<font color=\"red\" face=\"Helvetica\" point-size=\"12\">abc</font>"
    );
  }

  @Test
  public void test_img() {
    assertThat(GH.img()).isInstanceOf(GH.ElementWrapper.class).hasToString("<img/>");

    assertThatExceptionOfType(UnsupportedOperationException.class)
      .isThrownBy(() -> GH.img().add("foo"))
      .withMessage("Cannot add children to an img element");

    assertThat(GH.img("foo.png"))
      .isInstanceOf(GH.ElementWrapper.class)
      .hasToString("<img src=\"foo.png\"/>");

    assertXmlEquals(
      GH.img().src("foo.png").scale(GH.ImageScale.HEIGHT),
      "<img src=\"foo.png\" scale=\"HEIGHT\"/>"
    );
  }

  @Test
  public void test_tables() {
    assertThat(GH.table()).isInstanceOf(GH.TableWrapper.class).hasToString("<table/>");

    var table = GH
      .table()
      .align(GH.HorizontalAlign.CENTER)
      .valign(GH.VerticalAlign.TOP)
      .border(1)
      .cellborder(4)
      .cellspacing(2)
      .cellpadding(3)
      .columns("*")
      .rows("*")
      .color("red")
      .bgcolor("blue")
      .sides("LR")
      .style("dotted")
      .fixedsize(true)
      .gradientangle(45)
      .height(100)
      .width(200)
      .href("http://example.com")
      .target("foo")
      .port("xyz")
      .id("12345")
      .tooltip("I like tables")
      .tr(DocumentHelper.createElement("br"), GH.td().colspan(2).add("xyz"), "c")
      .tr(
        GH
          .td()
          .align(GH.TableDataAlign.TEXT)
          .balign(GH.HorizontalAlign.CENTER)
          .colspan(3)
          .rowspan(3)
      );

    assertXmlEquals(
      table,
      """
                        <table
                            align="CENTER"
                            valign="TOP"
                            border="1"
                            cellborder="4"
                            cellspacing="2"
                            cellpadding="3"
                            columns="*"
                            rows="*"
                            color="red"
                            bgcolor="blue"
                            sides="LR"
                            style="dotted"
                            fixedsize="true"
                            gradientangle="45"
                            height="100"
                            width="200"
                            href="http://example.com"
                            target="foo"
                            port="xyz"
                            id="12345"
                            tooltip="I like tables"
                          >
                          <tr><td><br/></td><td colspan="2">xyz</td><td>c</td></tr>
                          <tr><td align="TEXT" balign="CENTER" colspan="3" rowspan="3"/></tr>
                        </table>"""
    );
  }

  @Test
  public void test_nest() {
    var stack = GH.nest(GH.font().color("red"), GH.bold(), GH.italic("hello"));

    assertXmlEquals(stack, "<font color=\"red\"><b><i>hello</i></b></font>");
  }
}
