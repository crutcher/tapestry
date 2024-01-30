package org.tensortapestry.common.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.Getter;

@Getter
public class ResourceHandle {

  private final String path;
  private final ClassLoader classLoader;

  public ResourceHandle(String path) {
    this(path, Thread.currentThread().getContextClassLoader());
  }

  public ResourceHandle(String path, ClassLoader classLoader) {
    this.path = path;
    this.classLoader = classLoader;

    if (getURL() == null) {
      throw new IllegalArgumentException("Resource not found: " + path);
    }
  }

  public URL getURL() {
    return classLoader.getResource(path);
  }

  public InputStream getStream() {
    return classLoader.getResourceAsStream(path);
  }

  public byte[] getBytes() throws IOException {
    return getStream().readAllBytes();
  }

  public String getString(Charset cs) throws IOException {
    return new String(getBytes(), cs);
  }

  public String getString() throws IOException {
    return getString(StandardCharsets.UTF_8);
  }
}
