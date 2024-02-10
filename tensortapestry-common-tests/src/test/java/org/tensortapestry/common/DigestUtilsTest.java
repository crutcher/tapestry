package org.tensortapestry.common;

import org.junit.jupiter.api.Test;
import org.tensortapestry.common.testing.CommonAssertions;

public class DigestUtilsTest implements CommonAssertions {

  @Test
  public void test_bytesToHex() {
    assertThat(DigestUtils.bytesToHex(new byte[] { 0x01, 0x02, 0x03, 0x04, (byte) 0xff }))
      .isEqualTo("01020304ff");
  }

  @Test
  public void test_getMD5Digest() {
    var d = DigestUtils.getMD5Digest();

    assertThat(d)
      .isInstanceOf(java.security.MessageDigest.class)
      .extracting("algorithm")
      .isEqualTo("MD5");
  }

  @Test
  public void test_toMD5HexString() {
    var hex = DigestUtils.toMD5HexString("hello world");
    assertThat(hex).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");
  }
}
