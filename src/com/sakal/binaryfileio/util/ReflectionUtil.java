package com.sakal.binaryfileio.util;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static Method getMethod(Class type, Predicate<Method> predicate) {
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods)
            if (predicate.test(method)) {
                return method;
            }
        return null;
    }
}
