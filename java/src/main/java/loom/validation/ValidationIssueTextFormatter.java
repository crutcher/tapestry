package loom.validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import loom.common.json.JsonUtil;
import loom.common.text.IndentUtils;

public class ValidationIssueTextFormatter implements ValidationIssueFormatter {
  @Override
  public String formatIssueList(@Nullable List<ValidationIssue> issues) {
    if (issues == null || issues.isEmpty()) {
      return "No Validation Issues";
    }

    return "Validation failed with "
        + issues.size()
        + " issues:\n\n"
        + issues.stream().map(this::formatIssue).collect(Collectors.joining("\n\n"))
        + "\n";
  }

  @Override
  public String formatIssue(ValidationIssue issue) {
    var sb = new StringBuilder();
    sb.append("* Error [").append(issue.getType()).append("]: ").append(issue.getSummary());

    Map<String, String> params = issue.getParams();
    if (params != null && !params.isEmpty()) {
      sb.append("\n")
          .append(
              params.entrySet().stream()
                  .sorted(Map.Entry.comparingByKey())
                  .map(e -> "   └> %s: %s".formatted(e.getKey(), e.getValue()))
                  .collect(Collectors.joining("\n")));
    }

    String message = issue.getMessage();
    if (message != null) {
      sb.append("\n\n").append(IndentUtils.reindent(2, message));
    }

    List<ValidationIssue.Context> contexts = issue.getContexts();
    if (contexts != null) {
      for (var context : contexts) {
        sb.append("\n\n").append(IndentUtils.reindent(2, formatContext(context)));
      }
    }

    return sb.toString();
  }

  @Override
  public String formatContext(ValidationIssue.Context context) {
    var sb = new StringBuilder();

    sb.append("- %s::".formatted(context.getName()));
    if (context.getJsonpath() != null) {
      sb.append(" ").append(context.getJsonpath());
    }

    String message = context.getMessage();
    if (message != null) {
      var m = message.trim();
      if (!m.isEmpty())
        sb.append("\n\n").append(IndentUtils.indent(2, IndentUtils.splitAndRemoveCommonIndent(m)));
    }

    Object data = context.getData();
    if (data != null) {
      sb.append("\n\n").append(IndentUtils.indent("  |> ", JsonUtil.toPrettyJson(data)));
    }

    return sb.toString();
  }
}
