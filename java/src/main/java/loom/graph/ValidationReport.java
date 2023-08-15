package loom.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathConstants;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Data
public class ValidationReport {
  @Builder
  public static class Issue {
    String xpath;
    @Builder.Default int lineNumber = -1;
    String type;
    String summary;
    String message;

    // Keep the details in a LinkedHashMap so that they are ordered.
    @Builder.Default Map<String, String> details = new LinkedHashMap<>();
  }

  List<Issue> issues = new ArrayList<>();

  public boolean isValid() {
    return issues.isEmpty();
  }

  @Override
  public String toString() {
    return toCollatedString(null);
  }

  public String toCollatedString(@NotNull Document doc) {
    StringBuilder sb = new StringBuilder();
    sb.append("Validation failed with %d issues:\n".formatted(issues.size()));

    String uri = doc.getDocumentURI();

    for (int i = 0; i < issues.size(); i++) {
      var issue = issues.get(i);
      sb.append("\n--\n\n");

      sb.append("Error [%s]: %s::\n".formatted(issue.type, issue.summary));
      if (uri != null && issue.lineNumber >= 0) {
        sb.append(" - location: %s\n".formatted("%s:%d".formatted(uri, issue.lineNumber)));
      }

      if (issue.xpath != null) {
        sb.append(" - xpath: %s\n".formatted(issue.xpath));
      }
      for (var entry : issue.details.entrySet()) {
        sb.append(" - %s: %s\n".formatted(entry.getKey(), entry.getValue()));
      }

      sb.append("\n%s\n".formatted(issue.message.trim()));

      if (issue.xpath != null && doc != null) {
        try {
          var node = (Node) XGraphUtils.xpath.evaluate(issue.xpath, doc, XPathConstants.NODE);
          if (node != null) {
            sb.append("\nSource Node:\n %s\n".formatted(XGraphUtils.documentToString(node).trim()));
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    return sb.toString();
  }
}
