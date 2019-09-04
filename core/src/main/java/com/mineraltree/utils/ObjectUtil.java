package com.mineraltree.utils;

/** Helper methods that apply to general objects. */
public class ObjectUtil {

  /**
   * Finds and returns the fist item in the supplied list which is not-null. Useful for supplying
   * default or fallback choices.
   *
   * @param items the items to search through
   * @return the first item found which is non-null
   * @throws NullPointerException if all of the supplied items are {@code null}
   */
  public static <T> T firstNonNull(T... items) {
    for (T item : items) {
      if (null != item) {
        return item;
      }
    }
    throw new NullPointerException("All  " + items.length + " of the supplied items were null");
  }
}
