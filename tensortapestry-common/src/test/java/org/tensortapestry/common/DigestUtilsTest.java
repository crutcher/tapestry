package org.tensortapestry.common;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

public class DigestUtilsTest implements WithAssertions {

  @Test
  public void testByesToHex() {
    assertThat(DigestUtils.bytesToHex(new byte[] { 0x00, 0x01, 0x02, 0x03 })).isEqualTo("00010203");
  }

  @Test
  public void test_toMD5HexString() {
    assertThat(DigestUtils.toMd5HexString("abc")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
  }
}
