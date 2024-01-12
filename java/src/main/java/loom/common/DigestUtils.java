package loom.common;

import java.security.MessageDigest;

public class DigestUtils {
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

  private static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String toMD5HexString(String str) {
    MessageDigest md = getMD5Digest();
    md.update(str.getBytes());
    return bytesToHex(md.digest());
  }
}
