package loom.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.experimental.UtilityClass;
import loom.common.runtime.ExcludeFromJacocoGeneratedReport;

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
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /**
   * Get a MD5 MessageDigest instance.
   *
   * @return A MD5 MessageDigest instance
   */
  @ExcludeFromJacocoGeneratedReport
  public MessageDigest getMD5Digest() {
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
  public String toMD5HexString(String str) {
    MessageDigest md = getMD5Digest();
    md.update(str.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(md.digest());
  }
}
