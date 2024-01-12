package loom.common;

import lombok.RequiredArgsConstructor;
import loom.common.runtime.ExcludeFromJacocoGeneratedReport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DigestUtils {
  public static final String MD5_ALGORITHM = "MD5";

  /**
   * Convert a byte array to a hex string.
   * @param bytes The byte array to convert
   * @return The hex string
   */
  public static String bytesToHex(byte[] bytes) {
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
   * @return A MD5 MessageDigest instance
   */
  @ExcludeFromJacocoGeneratedReport
  public static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance(MD5_ALGORITHM);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the MD5 hash of a String as a hex String.
   * @param str The String to hash
   * @return The MD5 hash of the String as a hex String.
   */
  public static String toMD5HexString(String str) {
    MessageDigest md = getMD5Digest();
    md.update(str.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(md.digest());
  }
}
