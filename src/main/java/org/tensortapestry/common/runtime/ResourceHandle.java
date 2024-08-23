package org.tensortapestry.common.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.Value;

/**
 * A handle to a resource.
 * <p>
 * Availability checked at class loading time.
 */
@Value
public class ResourceHandle {

  String path;
  ClassLoader classLoader;

  /**
   * Create a new resource handle.
   *
   * @param path the resource path.
   */
  public ResourceHandle(String path) {
    this(path, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Create a new resource handle.
   *
   * @param path the resource path.
   * @param classLoader the class loader.
   */
  public ResourceHandle(String path, ClassLoader classLoader) {
    this.path = path;
    this.classLoader = classLoader;

    if (getUrl() == null) {
      throw new IllegalArgumentException("Resource not found: " + path);
    }
  }

  /**
   * Get the URL of the resource.
   *
   * @return the URL.
   */
  public URL getUrl() {
    return classLoader.getResource(path);
  }

  /**
   * Get the input stream of the resource.
   *
   * @return the input stream.
   */
  public InputStream getStream() {
    return classLoader.getResourceAsStream(path);
  }

  /**
   * Get the bytes of the resource.
   *
   * @return the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public byte[] getBytes() throws IOException {
    return getStream().readAllBytes();
  }

  /**
   * Get the string of the resource.
   *
   * @return the string.
   * @throws IOException if an I/O error occurs.
   */
  public String getString() throws IOException {
    return getString(StandardCharsets.UTF_8);
  }

  /**
   * Get the resource content as a String.
   *
   * @param cs the charset.
   * @return the string.
   * @throws IOException if an I/O error occurs.
   */
  public String getString(Charset cs) throws IOException {
    return new String(getBytes(), cs);
  }
}
