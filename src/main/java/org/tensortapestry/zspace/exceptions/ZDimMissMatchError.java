package org.tensortapestry.zspace.exceptions;

/** A ZDimError is thrown when ZDim objects do not match the expected number of dimensions. */
public class ZDimMissMatchError extends ZSpaceError {

  public ZDimMissMatchError(String message) {
    super(message);
  }
}
