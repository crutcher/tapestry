package loom.graph.export.graphviz;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import loom.common.runtime.ExcludeFromJacocoGeneratedReport;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * A fluent api for creating GraphViz HTML-Like labels.
 *
 * <p>See <a href="https://www.graphviz.org/doc/info/shapes.html#html-like">HTML-Like Labels</a>
 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class GH {

  /**
   * From <a href="https://www.graphviz.org/doc/info/shapes.html#html-like">HTML-Like Labels</a>:
   *
   * <p>As is obvious from the above description, the interpretation of white space characters is
   * one place where HTML-like labels is very different from standard HTML. In HTML, any sequence of
   * white space characters is collapsed to a single space, If the user does not want this to
   * happen, the input must use non-breaking spaces &nbsp;. This makes sense in HTML, where text
   * layout depends dynamically on the space available. In Graphviz, the layout is statically
   * determined by the input, so it is reasonable to treat ordinary space characters as
   * non-breaking. In addition, ignoring tabs and newlines allows the input text to be formatted for
   * easier reading.
   *
   * <p>HTML-Like Labels Care about whitespace, so we need to disable the default behavior of the
   * {@link OutputFormat} to trim whitespace.
   */
  public static final OutputFormat XML_OUTPUT_FORMAT;

  static {
    XML_OUTPUT_FORMAT = new OutputFormat();
    XML_OUTPUT_FORMAT.setSuppressDeclaration(true);
    XML_OUTPUT_FORMAT.setNewlines(true);
    XML_OUTPUT_FORMAT.setTrimText(false);
    XML_OUTPUT_FORMAT.setPadText(false);
  }

  public enum HorizontalAlign {
    LEFT,
    CENTER,
    RIGHT
  }

  public enum VerticalAlign {
    TOP,
    MIDDLE,
    BOTTOM
  }

  public enum TableDataAlign {
    LEFT,
    CENTER,
    RIGHT,
    TEXT
  }

  public enum ImageScale {
    FALSE,
    TRUE,
    WIDTH,
    HEIGHT,
    BOTH
  }

  /**
   * Serialize a {@code Node} using the {@link #XML_OUTPUT_FORMAT}.
   *
   * @param node the node to serialize
   * @return the serialized xml.
   */
  @ExcludeFromJacocoGeneratedReport
  public static String nodeToXml(Node node) {
    try {
      StringWriter sw = new StringWriter();
      XMLWriter xmlWriter = new XMLWriter(sw, XML_OUTPUT_FORMAT);
      xmlWriter.write(node);
      xmlWriter.close();
      return sw.toString().trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A wrapper around a {@link Node} that provides a fluent api.
   *
   * @param <T> the type of the wrapper.
   * @param <N> the type of the node.
   */
  @Getter
  @RequiredArgsConstructor
  public static class NodeWrapper<T extends NodeWrapper<T, N>, N extends Node> {
    @Nonnull protected final N node;

    @SuppressWarnings("unchecked")
    private T self() {
      return (T) this;
    }

    /**
     * Add this node to the parent as a child.
     *
     * @param parent the parent to add this node to.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T withParent(ElementWrapper<?> parent) {
      parent.add(this);
      return self();
    }

    /**
     * Serialize this node to xml.
     *
     * @return the xml.
     */
    @Override
    public String toString() {
      return nodeToXml(getNode());
    }
  }

  /**
   * A wrapper around an {@link Element} that provides a fluent api for adding children.
   *
   * @param <T> the type of the wrapper.
   */
  public static class ElementWrapper<T extends ElementWrapper<T>> extends NodeWrapper<T, Element> {

    public ElementWrapper(Element element) {
      super(element);
    }

    @SuppressWarnings("unchecked")
    private T self() {
      return (T) this;
    }

    /**
     * Get the value of an attribute on the Element.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute; {@code null} if the attribute is not set.
     */
    public String getAttr(String name) {
      return getNode().attributeValue(name);
    }

    /**
     * Set an attribute on the Element.
     *
     * @param name the name of the attribute.
     * @param value the value of the attribute; if {@code null} the attribute is not set.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T attr(String name, Object value) {
      if (value != null) {
        getNode().addAttribute(name, value.toString());
      }
      return self();
    }

    /**
     * Add a child to this element.
     *
     * @param node the child to add.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T add(Node node) {
      getNode().add(node);
      return self();
    }

    /**
     * Add a child to this element.
     *
     * @param wrapper the child to add.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T add(NodeWrapper<?, ?> wrapper) {
      add(wrapper.getNode());
      return self();
    }

    /**
     * Add a Text child to this element.
     *
     * @param text the text to add.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T add(String text) {
      add(text(text));
      return self();
    }

    /**
     * Add a collection of objects to this element.
     *
     * @param objects the objects to add.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T add(Object... objects) {
      return addAll(Arrays.asList(objects));
    }

    /**
     * Add a collection of objects to this element.
     *
     * @param objects the objects to add.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T addAll(Collection<?> objects) {
      for (var object : objects) {
        switch (object) {
          case Object[] array -> add(array);
          case List<?> list -> addAll(list);
          case String str -> add(str);
          case NodeWrapper<?, ?> wrapper -> add(wrapper);
          case Node n -> add(n);
          default -> throw new UnsupportedOperationException(
              "Cannot add instances of "
                  + object.getClass()
                  + " to an Element: %s".formatted(object));
        }
      }
      return self();
    }

    /**
     * Add a stream of objects to this element.
     *
     * @param objects the objects to add.
     * @return {@code this}
     */
    public T addAll(Stream<?> objects) {
      objects.forEach(this::add);
      return self();
    }

    /**
     * Parse xml and add the children to this element.
     *
     * @param xml the xml to parse.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T addXml(String xml) {
      Document document;
      try {
        document = DocumentHelper.parseText("<root>%s</root>".formatted(xml));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      var root = document.getRootElement();
      while (root.hasContent()) {
        add(root.content().getFirst().detach());
      }
      return self();
    }
  }

  /**
   * Create an element with the given name.
   *
   * @param name the name of the element.
   * @return an ElementWrapper for the element.
   */
  @CheckReturnValue
  private static ElementWrapper<?> element(String name) {
    return new ElementWrapper<>(DocumentHelper.createElement(name));
  }

  /**
   * Create a Text node.
   *
   * @param text the text of the node.
   * @return the Text node.
   */
  @CheckReturnValue
  public static NodeWrapper<?, Text> text(String text) {
    return new NodeWrapper<>(DocumentHelper.createText(text));
  }

  /**
   * Create a {@code <br/>} element.
   *
   * @return the {@code <br/>} element.
   */
  @CheckReturnValue
  public static ElementWrapper<?> br() {
    return element("br");
  }

  /**
   * Create a {@code <br align="align"/>} element.
   *
   * @param align the alignment of the {@code <br/>} element.
   * @return the {@code <br/>} element.
   */
  @CheckReturnValue
  public static ElementWrapper<?> br(HorizontalAlign align) {
    return br().attr("align", align);
  }

  /**
   * Create a {@code <vr/>} element.
   *
   * @return the {@code <vr/>} element.
   */
  @CheckReturnValue
  public static ElementWrapper<?> vr() {
    return element("vr");
  }

  /**
   * Create a {@code <hr/>} element.
   *
   * @return the {@code <hr/>} element.
   */
  @CheckReturnValue
  public static ElementWrapper<?> hr() {
    return element("hr");
  }

  /**
   * Create an {@code <i></i>} element wrapper.
   *
   * @return the {@code <i></i>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> italic() {
    return element("i");
  }

  /**
   * Create an {@code <i></i>} element wrapper with the given children.
   *
   * @param children the children of the {@code <i></i>} element.
   * @return the {@code <i></i>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> italic(Object... children) {
    return italic().add(children);
  }

  /**
   * Create an {@code <b></b>} element wrapper.
   *
   * @return the {@code <b></b>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> bold() {
    return element("b");
  }

  /**
   * Create an {@code <b></b>} element wrapper with the given children.
   *
   * @param children the children of the {@code <b></b>} element.
   * @return the {@code <b></b>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> bold(Object... children) {
    return bold().add(children);
  }

  /**
   * Create an {@code <u></u>} element wrapper.
   *
   * @return the {@code <u></u>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> underline() {
    return element("u");
  }

  /**
   * Create an {@code <u></u>} element wrapper with the given children.
   *
   * @param children the children of the {@code <u></u>} element.
   * @return the {@code <u></u>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> underline(Object... children) {
    return underline().add(children);
  }

  /**
   * Create an {@code <o></o>} element wrapper.
   *
   * @return the {@code <o></o>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> overline() {
    return element("o");
  }

  /**
   * Create an {@code <o></o>} element wrapper with the given children.
   *
   * @param children the children of the {@code <o></o>} element.
   * @return the {@code <o></o>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> overline(Object... children) {
    return overline().add(children);
  }

  /**
   * Create an {@code <s></s>} element wrapper.
   *
   * @return the {@code <s></s>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> strike() {
    return element("s");
  }

  /**
   * Create an {@code <s></s>} element wrapper with the given children.
   *
   * @param children the children of the {@code <s></s>} element.
   * @return the {@code <s></s>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> strike(Object... children) {
    return strike().add(children);
  }

  /**
   * Create an {@code <sub></sub>} element wrapper.
   *
   * @return the {@code <sub></sub>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> subscript() {
    return element("sub");
  }

  /**
   * Create an {@code <sub></sub>} element wrapper with the given children.
   *
   * @param children the children of the {@code <sub></sub>} element.
   * @return the {@code <sub></sub>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> subscript(Object... children) {
    return subscript().add(children);
  }

  /**
   * Create an {@code <sup></sup>} element wrapper.
   *
   * @return the {@code <sup></sup>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> superscript() {
    return element("sup");
  }

  /**
   * Create an {@code <sup></sup>} element wrapper with the given children.
   *
   * @param children the children of the {@code <sup></sup>} element.
   * @return the {@code <sup></sup>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> superscript(Object... children) {
    return superscript().add(children);
  }

  /** An extension of {@link ElementWrapper} for the {@code <font></font>} element. */
  public static final class FontWrapper extends ElementWrapper<FontWrapper> {
    public FontWrapper(Element element) {
      super(element);
      assert element.getName().equals("font");
    }

    public FontWrapper() {
      this(DocumentHelper.createElement("font"));
    }

    /**
     * Set the color of the font.
     *
     * @param color the color of the font.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public FontWrapper color(String color) {
      attr("color", color);
      return this;
    }

    /**
     * Set the face of the font.
     *
     * @param face the face of the font.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public FontWrapper face(String face) {
      attr("face", face);
      return this;
    }

    /**
     * Set the point size of the font.
     *
     * @param size the point size of the font.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public FontWrapper point_size(int size) {
      attr("point-size", size);
      return this;
    }
  }

  /**
   * Create a {@code <font></font>} element wrapper.
   *
   * @return the {@code <font></font>} element wrapper.
   */
  @CheckReturnValue
  public static FontWrapper font() {
    return new FontWrapper();
  }

  /** An extension of {@link ElementWrapper} for the {@code <img></img>} element. */
  public static final class ImgWrapper extends ElementWrapper<ImgWrapper> {
    public ImgWrapper(Element element) {
      super(element);
      assert element.getName().equals("img");
    }

    public ImgWrapper() {
      this(DocumentHelper.createElement("img"));
    }

    /**
     * Set the src of the image.
     *
     * @param src the src of the image.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public ImgWrapper src(String src) {
      attr("src", src);
      return this;
    }

    /**
     * Set the scale of the image.
     *
     * @param scale the scale of the image.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public ImgWrapper scale(ImageScale scale) {
      attr("scale", scale);
      return this;
    }

    @Override
    public ImgWrapper add(Node node) {
      throw new UnsupportedOperationException("Cannot add children to an img element");
    }
  }

  /**
   * Create a {@code <img></img>} ImgWrapper.
   *
   * @return the {@code <img></img>} ImgWrapper.
   */
  @CheckReturnValue
  public static ImgWrapper img() {
    return new ImgWrapper();
  }

  /**
   * Create a {@code <img src="src"></img>} ImgWrapper.
   *
   * @param src the src of the image.
   * @return the {@code <img src="src"></img>} ImgWrapper.
   */
  @CheckReturnValue
  public static ImgWrapper img(String src) {
    return img().src(src);
  }

  /**
   * A base class for {@link TableWrapper} and {@link TableDataWrapper}.
   *
   * @param <T> the type of the wrapper.
   */
  public abstract static class TableBaseWrapper<T extends TableBaseWrapper<T>>
      extends ElementWrapper<T> {
    public TableBaseWrapper(Element element) {
      super(element);
    }

    @SuppressWarnings("unchecked")
    private T self() {
      return (T) this;
    }

    /**
     * Set the valign of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#valign">Graphviz HTML VALIGN</a>
     *
     * @param valign the valign of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T valign(VerticalAlign valign) {
      attr("valign", valign);
      return self();
    }

    /**
     * Set the border of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#border">Graphviz HTML BORDER</a>
     *
     * @param border the border of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T border(int border) {
      attr("border", border);
      return self();
    }

    /**
     * Set the cell spacing of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#cellspacing">Graphviz HTML
     * CELLSPACING</a>
     *
     * @param spacing the cell spacing of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T cellspacing(int spacing) {
      attr("cellspacing", spacing);
      return self();
    }

    /**
     * Set the cell padding of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#cellpadding">Graphviz HTML
     * CELLPADDING</a>
     *
     * @param padding the cell padding of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T cellpadding(int padding) {
      attr("cellpadding", padding);
      return self();
    }

    /**
     * Set the color of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#color">Graphviz HTML COLOR</a>
     *
     * @param color the color of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T color(String color) {
      attr("color", color);
      return self();
    }

    /**
     * Set the bgcolor of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#bgcolor">Graphviz HTML BGCOLOR</a>
     *
     * @param color the bgcolor of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T bgcolor(String color) {
      attr("bgcolor", color);
      return self();
    }

    /**
     * Set the sides style of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#sides">Graphviz HTML SIDES</a>
     *
     * @param sides the sides style of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T sides(String sides) {
      attr("sides", sides);
      return self();
    }

    /**
     * Set the style of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#style">Graphviz HTML STYLE</a>
     *
     * @param style the style of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T style(String style) {
      attr("style", style);
      return self();
    }

    /**
     * Set if this element is fixed size.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#fixedangle">Graphviz HTML
     * FIXEDANGLE</a>
     *
     * @param fixed the fixed size of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T fixedsize(boolean fixed) {
      attr("fixedsize", fixed);
      return self();
    }

    /**
     * Set the gradient angle of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#gradientangle">Graphviz HTML
     * GRADIENTANGLE</a>
     *
     * @param angle the gradient angle of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T gradientangle(int angle) {
      attr("gradientangle", angle);
      return self();
    }

    /**
     * Set the height of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#height">Graphviz HTML HEIGHT</a>
     *
     * @param height the height of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T height(int height) {
      attr("height", height);
      return self();
    }

    /**
     * Set the width of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#width">Graphviz HTML WIDTH</a>
     *
     * @param width the width of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T width(int width) {
      attr("width", width);
      return self();
    }

    /**
     * Set the href of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#href">Graphviz HTML HREF</a>
     *
     * @param style the href of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T href(String style) {
      attr("href", style);
      return self();
    }

    /**
     * Set the target of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#target">Graphviz HTML TARGET</a>
     *
     * @param target the target of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T target(String target) {
      attr("target", target);
      return self();
    }

    /**
     * Set the port of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#port">Graphviz HTML PORT</a>
     *
     * @param port the port of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T port(String port) {
      attr("port", port);
      return self();
    }

    /**
     * Set the id of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#id">Graphviz HTML ID</a>
     *
     * @param port the id of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T id(String port) {
      attr("id", port);
      return self();
    }

    /**
     * Set the tooltip of the element.
     *
     * <p>See: <a href="https://graphviz.org/doc/info/shapes.html#title">Graphviz HTML TITLE</a>
     *
     * @param tooltip the tooltip of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public T tooltip(String tooltip) {
      attr("tooltip", tooltip);
      return self();
    }
  }

  /** An extension of {@link ElementWrapper} for the {@code <table></table>} element. */
  public static final class TableWrapper extends TableBaseWrapper<TableWrapper> {
    public TableWrapper(Element element) {
      super(element);
      assert element.getName().equals("table");
    }

    public TableWrapper() {
      this(DocumentHelper.createElement("table"));
    }

    /**
     * Set the align of the element.
     *
     * @param align the align of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableWrapper align(HorizontalAlign align) {
      attr("align", align);
      return this;
    }

    /**
     * Set the cell border of the element.
     *
     * @param border the cell border of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableWrapper cellborder(int border) {
      attr("cellborder", border);
      return this;
    }

    /**
     * Set the column separation style of the element.
     *
     * @param columns the column separation style of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableWrapper columns(String columns) {
      attr("columns", columns);
      return this;
    }

    /**
     * Set the row separation style of the element.
     *
     * @param rows the row separation style of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableWrapper rows(String rows) {
      attr("rows", rows);
      return this;
    }

    /**
     * Add a {@code <tr></tr>} element to this table.
     *
     * <p>Auto-wraps non-{@code <td/>} and {@code <vr/>} nodes in a {@code <td/>}.
     *
     * @param children the children of the {@code <tr></tr>} element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableWrapper tr(Object... children) {
      GH.tr(children).withParent(this);
      return this;
    }

    /**
     * Override to auto-box table element adds.
     *
     * <p>Table elements can only have {@code <tr></tr>} and {@code <hr/>} children;
     *
     * @param node the child to add.
     * @return {@code this}
     */
    @Override
    @CanIgnoreReturnValue
    public TableWrapper add(Node node) {
      if (node instanceof Element element) {
        if (element.getName().equals("tr") || element.getName().equals("hr")) {
          return super.add(node);
        }
      }

      return tr(node);
    }
  }

  /** An extension of {@link ElementWrapper} for the {@code <td></td>} element. */
  public static final class TableDataWrapper extends TableBaseWrapper<TableDataWrapper> {

    public TableDataWrapper(Element element) {
      super(element);
      assert element.getName().equals("td");
    }

    public TableDataWrapper() {
      this(DocumentHelper.createElement("td"));
    }

    /**
     * Set the align of the element.
     *
     * @param align the align of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableDataWrapper align(TableDataAlign align) {
      attr("align", align);
      return this;
    }

    /**
     * Set the halign of the element.
     *
     * @param align the halign of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableDataWrapper balign(HorizontalAlign align) {
      attr("balign", align);
      return this;
    }

    /**
     * Set the colspan of the element.
     *
     * @param colspan the colspan of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableDataWrapper colspan(int colspan) {
      attr("colspan", colspan);
      return this;
    }

    /**
     * Set the rowspan of the element.
     *
     * @param rowspan the rowspan of the element.
     * @return {@code this}
     */
    @CanIgnoreReturnValue
    public TableDataWrapper rowspan(int rowspan) {
      attr("rowspan", rowspan);
      return this;
    }
  }

  /**
   * Create a {@code <table></table>} element wrapper.
   *
   * @return the {@code <table></table>} element wrapper.
   */
  @CheckReturnValue
  public static TableWrapper table() {
    return new TableWrapper();
  }

  /**
   * Create a {@code <tr></tr>} element wrapper.
   *
   * @return the {@code <tr></tr>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> tr() {
    return element("tr");
  }

  /**
   * Create a {@code <tr></tr>} element wrapper with the given children.
   *
   * <p>Auto-wraps non-{@code <td/>} and {@code <vr/>} nodes in a {@code <td/>}.
   *
   * @param children the children of the {@code <tr></tr>} element.
   * @return the {@code <tr></tr>} element wrapper.
   */
  @CheckReturnValue
  public static ElementWrapper<?> tr(Object... children) {
    var tr = tr();

    for (var child : children) {
      Element element = null;
      if (child instanceof Element asElement) {
        element = asElement;
      } else if (child instanceof NodeWrapper<?, ?> wrapper) {
        if (wrapper.getNode() instanceof Element asElement) {
          element = asElement;
        }
      }
      if (element != null) {
        if (element.getName().equals("td") || element.getName().equals("vr")) {
          tr.add(element);
          continue;
        }
      }
      tr.add(GH.td(child));
    }

    return tr;
  }

  /**
   * Create a {@code <td></td>} element wrapper.
   *
   * @return the {@code <td></td>} element wrapper.
   */
  @CheckReturnValue
  public static TableDataWrapper td() {
    return new TableDataWrapper();
  }

  /**
   * Create a {@code <td></td>} element wrapper with the given children.
   *
   * @param children the children of the {@code <td></td>} element.
   * @return the {@code <td></td>} element wrapper.
   */
  @CheckReturnValue
  public static TableDataWrapper td(Object... children) {
    var t = td();
    t.add(children);
    return t;
  }

  /**
   * Chain a series of elements together as children of each other.
   *
   * @param stack the elements to chain together.
   * @return the first element in the stack.
   */
  public static ElementWrapper<?> nest(ElementWrapper<?>... stack) {
    for (int i = 1; i < stack.length; i++) {
      stack[i].withParent(stack[i - 1]);
    }
    return stack[0];
  }
}
