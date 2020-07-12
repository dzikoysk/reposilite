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

package org.panda_lang.reposilite.config;

import org.junit.jupiter.api.Test;
import org.panda_lang.utilities.commons.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigurationTest {

    @Test
    void shouldNotContainNullValues() throws IllegalAccessException {
        Configuration configuration = new Configuration();

        for (Field field : configuration.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            assertNotNull(field.get(configuration));
        }
    }

    @Test
    void shouldNotContainNullResults() throws InvocationTargetException, IllegalAccessException {
        Configuration configuration = new Configuration();

        for (Method method : configuration.getClass().getDeclaredMethods()) {
            if (isGetter(method)) {
                assertNotNull(method.invoke(configuration));
            }
        }
    }

    @Test
    void shouldContainSetterForEveryGetter() throws NoSuchMethodException {
        for (Method method : Configuration.class.getDeclaredMethods()) {
            if (isGetter(method)) {
                String name = StringUtils.replaceFirst(method.getName(), "get", "set");
                name = StringUtils.replaceFirst(name, "is", "set");
                assertNotNull(Configuration.class.getDeclaredMethod(name, method.getReturnType()));
            }
        }
    }

    private boolean isGetter(Method method) {
        return method.getName().startsWith("get") || method.getName().startsWith("is");
    }

}