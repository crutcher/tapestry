package loom.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class ValidationReport {
  @Builder
  public static class Issue {
    String xpath;
    @Builder.Default int lineNumber = -1;
    String type;
    String summary;
    String message;

    @Nullable String context;

    // Keep the details in a LinkedHashMap so that they are ordered.
    @Builder.Default Map<String, String> details = new LinkedHashMap<>();
  }

  @Nullable String uri;

  List<Issue> issues = new ArrayList<>();

  public void addIssue(Issue.IssueBuilder builder) {
    addIssue(builder.build());
  }

  public void addIssue(Issue issue) {
    issues.add(issue);
  }

  public boolean isValid() {
    return issues.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Validation failed with %d issues:\n".formatted(issues.size()));

    for (int i = 0; i < issues.size(); i++) {
      var issue = issues.get(i);
      sb.append("\n--\n\n");

      sb.append("Error [%s]: %s::\n".formatted(issue.type, issue.summary));
      if (issue.lineNumber >= 0) {
        if (uri == null) {
          sb.append(" - line: %d\n".formatted(issue.lineNumber));
        } else {
          sb.append(" - line: %s:%s\n".formatted(uri, issue.lineNumber));
        }
      }

      if (issue.xpath != null) {
        sb.append(" - xpath: %s\n".formatted(issue.xpath));
      }
      for (var entry : issue.details.entrySet()) {
        sb.append(" - %s: %s\n".formatted(entry.getKey(), entry.getValue()));
      }

      sb.append("\n%s\n".formatted(issue.message.trim()));

      if (issue.context != null) {
        sb.append("\nSource Context:\n%s\n".formatted(issue.context));
      }
    }

    return sb.toString();
  }
}
