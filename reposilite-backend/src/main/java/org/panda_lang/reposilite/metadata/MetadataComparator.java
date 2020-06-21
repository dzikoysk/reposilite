package org.panda_lang.reposilite.metadata;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

final class MetadataComparator<T> implements Comparator<T> {

    private final Function<T, String> originalValue;
    private final Function<T, String[]> cachedValue;
    private final Predicate<T> isDirectory;

    MetadataComparator(Function<T, String> originalValue, Function<T, String[]> cachedValue, Predicate<T> isDirectory) {
        this.originalValue = originalValue;
        this.cachedValue = cachedValue;
        this.isDirectory = isDirectory;
    }

    @Override
    public int compare(T object, T to) {
        if (isDirectory.test(object) && !isDirectory.test(to)) {
            return -1;
        }
        else if (isDirectory.test(to) && !isDirectory.test(object)) {
            return 1;
        }

        String[] data = cachedValue.apply(object);
        String[] toData = cachedValue.apply(to);

        for (int index = 0; index < Math.max(data.length, toData.length); index++) {
            String fragment = (index < data.length) ? data[index] : "0";
            String toFragment = (index < toData.length) ? toData[index] : "0";
            int value;

            // number to string
            if (isDigit(fragment)) {
                value = 1;

                // number to number
                if (isDigit(toFragment)) {
                    value = Integer.compare(Integer.parseInt(fragment), Integer.parseInt(toFragment));
                }
            }
            // string to number
            else if (isDigit(toFragment)) {
                value = -1;
            }
            // string to string
            else {
                value = -fragment.compareTo(toFragment);
            }

            if (value != 0) {
                return -value;
            }
        }

        return -originalValue.apply(object).compareTo(originalValue.apply(to));
    }

    private boolean isDigit(String string) {
        for (char character : string.toCharArray()) {
            if (!Character.isDigit(character)) {
                return false;
            }
        }

        return true;
    }

}
