package loom.zspace;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Data;

import java.util.Arrays;

@Data
@JsonDeserialize(using = ZPoint.Deserializer.class)
public class ZPoint {
  static String formatLabeledCoord(String label, int[] coordinate) {
    return String.format("%s:%s", label, Arrays.toString(coordinate));
  }

  public static void verifyZPointSameNDims(int[] start, int[] end) {
    if (start.length != end.length) {
      throw new IllegalArgumentException(
          String.format(
              "%s and %s differ in dimensions",
              formatLabeledCoord("start", start), formatLabeledCoord("end", end)));
    }
  }

  /**
   * Verify that start is <= end in all dimensions.
   *
   * @param start the start of the range.
   * @param end the end of the range.
   * @throws IllegalArgumentException if start and end are not the same dims and start is not <= all
   *     elements of end.
   */
  public static void verifyZPointLE(int[] start, int[] end) {
    verifyZPointSameNDims(start, end);
    for (int i = 0; i < start.length; i++) {
      if (start[i] > end[i]) {
        throw new IllegalArgumentException(
            String.format(
                "%s is not <= %s",
                formatLabeledCoord("start", start), formatLabeledCoord("end", end)));
      }
    }
  }

  static class Deserializer extends StdDeserializer<ZPoint> {
    public Deserializer() {
      super(ZPoint.class);
    }

    @Override
    public ZPoint deserialize(
        com.fasterxml.jackson.core.JsonParser p,
        com.fasterxml.jackson.databind.DeserializationContext ctxt)
        throws java.io.IOException, com.fasterxml.jackson.core.JsonProcessingException {
      int[] coords = p.readValueAs(int[].class);
      return new ZPoint(coords);
    }
  }

  @JsonValue private final int[] coords;

  public ZPoint(int... coords) {
    this.coords = coords;
  }

  public int ndim() {
    return coords.length;
  }

  @Override
  public String toString() {
    return "<" + java.util.Arrays.toString(coords) + ">";
  }

  public int get(int i) {
    return coords[i];
  }

  public int[] copyCoords() {
    return this.coords.clone();
  }
}
