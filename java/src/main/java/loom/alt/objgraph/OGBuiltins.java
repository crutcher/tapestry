package loom.alt.objgraph;

public class OGBuiltins {
  private OGBuiltins() {}

  public static final String BUILTINS_URN = "http://loom-project.io/tapestry/v1";
  public static final JNSName TENSOR = new JNSName(BUILTINS_URN, "tensor");
  public static final JNSName SHAPE = new JNSName(BUILTINS_URN, "shape");
  public static final JNSName DTYPE = new JNSName(BUILTINS_URN, "dtype");
  public static final JNSName OPERATION = new JNSName(BUILTINS_URN, "operation");
  public static final JNSName INPUTS = new JNSName(BUILTINS_URN, "inputs");
  public static final JNSName SIGNATURE = new JNSName(BUILTINS_URN, "signature");
  public static final JNSName EXTERNAL = new JNSName(BUILTINS_URN, "external");
}
