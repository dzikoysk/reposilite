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

package org.panda_lang.reposilite.auth;

import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.text.Joiner;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Permission {

    MANAGER("m", false),
    WRITE("w", false),
    // Stub permission
    // ~ https://github.com/dzikoysk/reposilite/issues/362
    READ("r", true);

    private final String name;
    private final boolean isDefault;

    Permission(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getName() {
        return name;
    }

    public static Set<Permission> getDefaultPermissions() {
        return Arrays.stream(values())
                .filter(Permission::isDefault)
                .collect(Collectors.toSet());
    }

    public static String toString(Iterable<? extends Permission> permissions) {
        return Joiner.on(StringUtils.EMPTY)
                .join(permissions)
                .toString();
    }

}
