package org.panda_lang.reposilite.metadata;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

final class MetadataComparator<T> implements Comparator<T> {

    private final Function<T, String[]> mapper;
    private final Predicate<T> isDirectory;

    MetadataComparator(Function<T, String[]> mapper, Predicate<T> isDirectory) {
        this.mapper = mapper;
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

        String[] data = mapper.apply(object);
        String[] toData = mapper.apply(to);

        for (int index = 0; index < data.length; index++) {
            if (toData.length <= index) {
                return 1;
            }

            String fragment = data[index];
            String toFragment = toData[index];
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

        return data.length == toData.length ? 0 : 1;
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
