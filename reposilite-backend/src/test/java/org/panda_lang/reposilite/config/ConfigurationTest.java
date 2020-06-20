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
            if (method.getName().startsWith("get")) {
                assertNotNull(method.invoke(configuration));
            }
        }
    }

    @Test
    void shouldContainSetterForEveryGetter() throws NoSuchMethodException {
        for (Method method : Configuration.class.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                assertNotNull(Configuration.class.getDeclaredMethod(StringUtils.replaceFirst(method.getName(), "get", "set"), method.getReturnType()));
            }
        }
    }

}