package org.tensortapestry.zspace.indexing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
public class FlatArray {

  int[] shape;
  int[] data;

  @JsonIgnore
  @Getter(lazy = true)
  private final int[] strides = IndexingFns.shapeToLSFStrides(shape);

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

    public class Deserializer extends StdDeserializer<FlatArray> {

      public Deserializer() {
        super(FlatArray.class);
      }

      @Override
      public FlatArray deserialize(@Nonnull JsonParser p, @Nonnull DeserializationContext context)
        throws java.io.IOException {
        if (p.nextToken() != JsonToken.START_ARRAY) {
          throw new IOException("Expected start of array");
        }

        int[] shape = p.readValueAs(int[].class);
        int[] data = p.readValueAs(int[].class);

        if (p.nextToken() != JsonToken.END_ARRAY) {
          throw new IOException("Expected end of array");
        }
        return new FlatArray(shape, data);
      }
    }
  }
}
