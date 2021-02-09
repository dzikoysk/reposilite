/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.metadata;

import java.math.BigInteger;
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
                    try {
                        value = Long.compare(Long.parseLong(fragment), Long.parseLong(toFragment));
                    } catch (NumberFormatException numberFormatException) {
                        value = new BigInteger(fragment).compareTo(new BigInteger(toFragment));
                    }
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
