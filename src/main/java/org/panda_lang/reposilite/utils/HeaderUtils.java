package org.panda_lang.reposilite.utils;

import org.panda_lang.utilities.commons.StringUtils;

import java.util.HashMap;
import java.util.Map;

public final class HeaderUtils {

    public static Map<String, String> parseContentDisposition(String contentDisposition) {
        String[] elements = contentDisposition.split(";");
        Map<String, String> parsed = new HashMap<>(elements.length + 1);

        for (String element : elements) {
            String[] entry = StringUtils.splitFirst(element, "=");

            if (entry.length == 1) {
                parsed.put(element, StringUtils.EMPTY);
                continue;
            }

            parsed.put(entry[0], entry[1].substring(1, entry[1].length() - 1));
        }

        return parsed;
    }

}
