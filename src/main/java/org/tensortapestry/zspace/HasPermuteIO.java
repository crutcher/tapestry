package org.tensortapestry.zspace;

public interface HasPermuteIO<T extends HasPermuteIO<T>>
  extends HasPermuteInput<T>, HasPermuteOutput<T> {}
