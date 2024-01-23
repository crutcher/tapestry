package org.tensortapestry.loom.zspace;

/** A ZDimError is thrown when ZDim objects do not match the expected number of dimensions. */
public class ZDimMissMatchError extends ZSpaceError {

  ZDimMissMatchError(String message) {
    super(message);
  }
}
