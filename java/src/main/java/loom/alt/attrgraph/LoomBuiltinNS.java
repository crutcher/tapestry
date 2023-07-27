package loom.alt.attrgraph;

public class LoomBuiltinNS {
  private LoomBuiltinNS() {}

  public static final String BUILTINS_URN = "http://loom-project.io/tapestry/v1";
  public static final NSName TENSOR = new NSName(BUILTINS_URN, "tensor");
  public static final NSName SHAPE = new NSName(BUILTINS_URN, "shape");
  public static final NSName DTYPE = new NSName(BUILTINS_URN, "dtype");
  public static final NSName OPERATION = new NSName(BUILTINS_URN, "operation");
  public static final NSName OP = new NSName(BUILTINS_URN, "op");
  public static final NSName INPUTS = new NSName(BUILTINS_URN, "inputs");
  public static final NSName RESULTS = new NSName(BUILTINS_URN, "results");
  public static final NSName SIGNATURE = new NSName(BUILTINS_URN, "signature");
  public static final NSName EXTERNAL = new NSName(BUILTINS_URN, "external");

  public static final NSName CONCAT = new NSName(BUILTINS_URN, "concat");
}
