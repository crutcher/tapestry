package org.tensortapestry.loom.graph;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public abstract class AbstractNodeWrapper<
  WrapperT extends AbstractNodeWrapper<WrapperT, BodyT>, BodyT
  >
  implements LoomNodeWrapper<WrapperT> {

  public abstract static class AbstractNodeWrapperBuilder<WrapperT, BuilderT, BodyT, BodyBuilderT> {

    private final LoomNode.Builder nodeBuilder;

    private final Supplier<BodyBuilderT> createBodyBuilder;
    private final Function<BodyBuilderT, BodyT> bodyBuilderBuild;
    private final Function<LoomNode, WrapperT> wrapper;

    protected AbstractNodeWrapperBuilder(
      @Nonnull String type,
      @Nonnull Supplier<BodyBuilderT> createBodyBuilder,
      @Nonnull Function<BodyBuilderT, BodyT> bodyBuilderBuild,
      @Nonnull Function<LoomNode, WrapperT> wrapper
    ) {
      this.nodeBuilder = LoomNode.builder().type(type);
      this.createBodyBuilder = createBodyBuilder;
      this.bodyBuilderBuild = bodyBuilderBuild;
      this.wrapper = wrapper;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public final BuilderT self() {
      return (BuilderT) this;
    }

    @Nonnull
    public final BuilderT graph(@Nullable LoomGraph graph) {
      nodeBuilder.graph(graph);
      return self();
    }

    @Nonnull
    public final BuilderT id(@Nonnull UUID id) {
      nodeBuilder.id(id);
      return self();
    }

    @Nonnull
    public final BuilderT label(@Nonnull String label) {
      nodeBuilder.label(label);
      return self();
    }

    @Nonnull
    public final BuilderT body(@Nonnull BodyT body) {
      nodeBuilder.body(body);
      return self();
    }

    @Nonnull
    public final BuilderT body(@Nonnull Consumer<BodyBuilderT> body) {
      var bodyBuilder = createBodyBuilder.get();
      body.accept(bodyBuilder);
      nodeBuilder.body(bodyBuilderBuild.apply(bodyBuilder));
      return self();
    }

    @Nonnull
    public final BuilderT annotation(@Nonnull String type, @Nonnull Object value) {
      nodeBuilder.tag(type, value);
      return self();
    }

    @Nonnull
    public WrapperT build() {
      return wrapper.apply(nodeBuilder.build());
    }
  }

  @Nonnull
  @Delegate(excludes = LoomNodeWrapper.class)
  @JsonValue
  protected final LoomNode node;

  @Nonnull
  protected final Class<BodyT> bodyClass;

  @Override
  @Nonnull
  public final LoomNode unwrap() {
    return node;
  }

  @Override
  public final String toString() {
    return unwrap().toString();
  }

  @Override
  public final int hashCode() {
    return unwrap().hashCode();
  }

  @Override
  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public final boolean equals(Object other) {
    return unwrap().equals(other);
  }

  @Nonnull
  public final BodyT getBody() {
    return node.viewBodyAs(bodyClass);
  }
}
