package org.tensortapestry.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.experimental.UtilityClass;
import org.tensortapestry.common.runtime.ExcludeFromJacocoGeneratedReport;

@UtilityClass
public class DigestUtils {

  public final String MD5_ALGORITHM = "MD5";

  /**
   * Convert a byte array to a hex string.
   *
   * @param bytes The byte array to convert
   * @return The hex string
   */
  public String bytesToHex(byte[] bytes) {
    var sb = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString();
  }

  /**
   * Get a MD5 MessageDigest instance.
   *
   * @return A MD5 MessageDigest instance
   */
  @ExcludeFromJacocoGeneratedReport
  public MessageDigest getMd5Digest() {
    try {
      return MessageDigest.getInstance(MD5_ALGORITHM);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the MD5 hash of a String as a hex String.
   *
   * @param str The String to hash
   * @return The MD5 hash of the String as a hex String.
   */
  public String toMd5HexString(String str) {
    MessageDigest md = getMd5Digest();
    md.update(str.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(md.digest());
  }
}
