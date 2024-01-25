package org.tensortapestry.loom.zspace.serialization;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParseUtil {

  /**
   * Split a string by commas.
   * @param s the string
   * @return the list of strings
   */
  public List<String> splitCommas(String s) {
    var res = new ArrayList<String>();
    for (var part : s.split(",", -1)) {
      res.add(part.trim());
    }
    return res;
  }

  /**
   * Split a string by colons.
   * @param s the string
   * @return the list of strings
   */
  public List<String> splitColons(String s) {
    var res = new ArrayList<String>();
    for (var part : s.split(":", -1)) {
      res.add(part.trim());
    }
    return res;
  }
}
