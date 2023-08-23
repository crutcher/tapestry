package loom.graph;

import org.jetbrains.annotations.NotNull;

public class LoomValidationException extends RuntimeException {
  public long serialVersionUID = 1L;

  @NotNull public ValidationReport report;

  public LoomValidationException(ValidationReport report) {
    super(report.toString());
    this.report = report;
  }
}
