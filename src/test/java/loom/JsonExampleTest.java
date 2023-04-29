package loom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class JsonExampleTest implements WithAssertions {

  @Test
  public void testGetId() {
    var ex = JsonExample.builder().id(12).build();
    ex.getId();
  }

  @Test
  public void testJson() throws Exception {
    var ex = JsonExample.builder().id(12).build();

    var mapper = new ObjectMapper();
    String json = mapper.writer().writeValueAsString(ex);

    assertThat(json).isEqualTo("{\"id\":12}");
  }
}
