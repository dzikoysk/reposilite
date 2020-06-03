package org.panda_lang.reposilite.utils;

public final class ArrayUtils {

    private ArrayUtils() { }

    public static <T> T getLatest(T[] elements) {
        return elements.length > 0 ? elements[0] : null;
    }

    public static <T> T getLast(T[] elements) {
        return  elements.length == 0 ? null : elements[elements.length - 1];
    }

}
