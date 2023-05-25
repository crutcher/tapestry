package loom.expressions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A block expression defines a number of tensor slice results in terms of bound tensor selection
 * inputs.
 */
@Jacksonized
@Builder
@Data
public class BlockExpression {
    @Nonnull
    public final NamedZRange index;

    @Nonnull
    public final List<BoundSlice> inputs;
    @Nonnull
    public final List<BoundSlice> outputs;

    //

    // Validate;
    // - all input, output match the index.
    // - output is coherent?

    @JsonCreator
    public BlockExpression(
            @Nonnull NamedZRange index,
            @Nonnull List<BoundSlice> inputs,
            @Nonnull List<BoundSlice> outputs) {
        this.index = index;
        this.inputs = ImmutableList.copyOf(inputs);
        this.outputs = ImmutableList.copyOf(outputs);

        if (inputs.size() != inputs.stream().map(i -> i.name).collect(Collectors.toSet()).size()) {
            throw new IllegalArgumentException("Input names must be unique: " + inputs);
        }
        if (outputs.size() != outputs.stream().map(i -> i.name).collect(Collectors.toSet()).size()) {
            throw new IllegalArgumentException("output names must be unique: " + outputs);
        }

        inputs.forEach(i -> i.validateAgainstIndex(index));
        outputs.forEach(i -> i.validateAgainstIndex(index));
    }
}
