package loom.zspace;

public class Utility {
  private Utility() {}

  /**
   * Return an array of integers from 0 to n - 1.
   *
   * @param n the number of integers to return.
   * @return an array of integers from 0 to n - 1.
   */
  static int[] iota(int n) {
    int[] result = new int[n];
    for (int i = 0; i < n; ++i) {
      result[i] = i;
    }
    return result;
  }

  /**
   * Return an array of integers from n - 1 to 0.
   *
   * @param n the number of integers to return.
   * @return an array of integers from n - 1 to 0.
   */
  static int[] aoti(int n) {
    int[] result = new int[n];
    for (int i = 0; i < n; ++i) {
      result[i] = n - 1 - i;
    }
    return result;
  }
}
