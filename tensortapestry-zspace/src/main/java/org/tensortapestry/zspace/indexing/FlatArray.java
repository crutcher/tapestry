package org.tensortapestry.zspace.indexing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.UtilityClass;

/**
 * A multidimensional array represented as a (shape, flat data) pair.
 */
@Data
@JsonSerialize(using = FlatArray.Serialization.Serializer.class)
@JsonDeserialize(using = FlatArray.Serialization.Deserializer.class)
public final class FlatArray {

  final int[] shape;
  final int[] data;

  @JsonIgnore
  @Getter(lazy = true)
  private final int[] strides = IndexingFns.shapeToLfsStrides(shape);

  public FlatArray(int[] shape, int[] data) {
    this.shape = shape;
    this.data = data;

    int expectedSize = IndexingFns.shapeToSize(shape);
    if (expectedSize != data.length) {
      throw new IllegalArgumentException(
        "Shape size (%d) != data length (%d): %s".formatted(
            expectedSize,
            data.length,
            Arrays.toString(shape)
          )
      );
    }
  }

  public int get(int... indices) {
    return data[IndexingFns.ravel(shape, getStrides(), indices, 0)];
  }

  @UtilityClass
  public static class Serialization {

    public final class Serializer extends StdSerializer<FlatArray> {

      public Serializer() {
        super(FlatArray.class);
      }

      @Override
      public void serialize(
        @Nonnull FlatArray value,
        @Nonnull JsonGenerator gen,
        @Nonnull SerializerProvider serializers
      ) throws IOException {
        gen.writeStartArray();

        gen.writeArray(value.getShape(), 0, value.getShape().length);
        gen.writeArray(value.getData(), 0, value.getData().length);

        gen.writeEndArray();
      }
    }

    public final class Deserializer extends StdDeserializer<FlatArray> {

      public Deserializer() {
        super(FlatArray.class);
      }

      @Override
      public FlatArray deserialize(@Nonnull JsonParser p, @Nonnull DeserializationContext context)
        throws java.io.IOException {
        int[][] shapeAndData = p.readValueAs(int[][].class);
        return new FlatArray(shapeAndData[0], shapeAndData[1]);
      }
    }
  }
}
