package com.sakal.binaryfileio.util;

import java.util.LinkedHashMap;

public class PrimitiveWrapperMapper {
    private static final LinkedHashMap<Class, Class> PRIMITIVE_WRAPPER_TYPE_MAP;

    static {
        PRIMITIVE_WRAPPER_TYPE_MAP = new LinkedHashMap<>();
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, short.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, long.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, float.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, double.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, char.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
    }

    public static Class map(Class wrapper) {
        return PRIMITIVE_WRAPPER_TYPE_MAP.getOrDefault(wrapper, wrapper);
    }
}
