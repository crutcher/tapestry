package loom.common.xml.dom4j;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.dom4j.Node;
import org.dom4j.XPath;

@Builder
@Value
public class XPathNamespaceContext {

  @Singular Map<String, String> namespaces;

  /**
   * Create an XPath instance with the given expression and namespaces.
   *
   * <p>Binds the XPath instance to the namespaces.
   *
   * @param node The node to create the XPath from.
   * @param xpathExpression The XPath expression.
   * @return The XPath instance.
   */
  public XPath createXPath(Node node, String xpathExpression) {
    var xp = node.createXPath(xpathExpression);
    xp.setNamespaceURIs(namespaces);
    return xp;
  }

  /**
   * Wrapper for {@link Node#selectNodes(String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the list of nodes.
   */
  public List<Node> selectNodes(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).selectNodes(node);
  }

  /**
   * Wrapper for {@link Node#selectObject(String)}; {@link XPath#evaluate(Object)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the result of the evaluation.
   */
  public Object evaluate(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).evaluate(node);
  }

  /**
   * Wrapper for {@link Node#selectNodes(String, String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @param sortXPath the sort xpath expression.
   * @return the list of nodes.
   */
  public List<Node> selectNodes(Node node, String xpathExpression, String sortXPath) {
    return createXPath(node, xpathExpression).selectNodes(node, createXPath(node, sortXPath));
  }

  /**
   * Wrapper for {@link Node#selectSingleNode(String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the node, or null.
   */
  public Node selectSingleNode(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).selectSingleNode(node);
  }

  /**
   * Wrapper for {@link Node#valueOf(String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the value of the node.
   */
  public String valueOf(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).valueOf(node);
  }

  /**
   * Wrapper for {@link Node#numberValueOf(String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the value of the node.
   */
  public Number numberValueOf(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).numberValueOf(node);
  }

  /**
   * Wrapper for {@link XPath#booleanValueOf(Object)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the value of the node.
   */
  public boolean booleanValueOf(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).booleanValueOf(node);
  }

  /**
   * Wrapper for {@link Node#matches(String)}.
   *
   * @param node the node to select from.
   * @param xpathExpression the xpath expression.
   * @return the value of the node.
   */
  public boolean matches(Node node, String xpathExpression) {
    return createXPath(node, xpathExpression).matches(node);
  }
}
