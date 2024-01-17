package loom.testing;

import org.xmlunit.assertj3.XmlAssert;

public interface XmlAssertions extends CommonAssertions {
  default void assertXmlEquals(Object actual, Object expected) {
    XmlAssert.assertThat(actual.toString())
        .and(expected.toString())
        .ignoreWhitespace()
        .areIdentical();
  }
}
