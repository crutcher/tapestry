package loom.validation;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import loom.common.lazy.LazyString;

public interface HasValidate<T extends HasValidate<T>> {

  /**
   * Validate the object.
   *
   * @throws LoomValidationError if the object is invalid.
   */
  @CanIgnoreReturnValue
  default T checkValid() {
    var collector = new ListValidationIssueCollector();
    validate(null, collector, null);
    collector.check();

    @SuppressWarnings("unchecked")
    var self = (T) this;
    return self;
  }

  /**
   * Validate the object.
   *
   * @param jsonPathPrefix the jsonpath prefix.
   * @param issueCollector the issue collector.
   * @param contextSupplier the context supplier.
   */
  void validate(
      @Nullable LazyString jsonPathPrefix,
      ValidationIssueCollector issueCollector,
      @Nullable Supplier<List<ValidationIssue.Context>> contextSupplier);
}
