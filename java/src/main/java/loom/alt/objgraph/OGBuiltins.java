package loom.alt.objgraph;

public class OGBuiltins {
  private OGBuiltins() {}

  public static final String BUILTINS_URN = "http://loom-project.io/tapestry/v1";
  public static final JNSName TENSOR = new JNSName(BUILTINS_URN, "tensor");
  public static final JNSName TENSOR_SHAPE = new JNSName(BUILTINS_URN, "tensor.shape");
  public static final JNSName DTYPE = new JNSName(BUILTINS_URN, "dtype");
}
