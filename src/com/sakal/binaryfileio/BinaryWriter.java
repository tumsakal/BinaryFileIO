package com.sakal.binaryfileio;

import com.sakal.binaryfileio.util.PrimitiveWrapperMapper;
import com.sakal.binaryfileio.util.ReflectionUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BinaryWriter {

    private DataOutputStream _dataOutputStream;
    private Class _dataClass;
    private LinkedHashMap<String, Class> _fieldOrder;
    private HashMap<String, Function<?, ?>> _typeResolvers;
    private LinkedHashMap<Class, Method> _methodWrites;

    public static ConfigurationBuilder newConfigurationBuilder() {
        return new ConfigurationBuilder();
    }

    public <T> void write(T objectData) {
        for (String field : _fieldOrder.keySet()) {
            Class fieldType = _fieldOrder.get(field);
            Method method = _methodWrites.get(fieldType);
            Object dataArgument = null;
            if (method != null) {
                if (fieldType == method.getParameterTypes()[0])
                    try {
                        dataArgument = _dataClass.getDeclaredField(field).get(objectData);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                else {
                    Function func = _typeResolvers.get(field);
                    if (func != null) {
                        try {
                            //noinspection unchecked
                            dataArgument = func.apply(_dataClass.getDeclaredField(field).get(objectData));
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    method.invoke(_dataOutputStream, dataArgument);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        try {
            _dataOutputStream.flush();
            _dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public static class ConfigurationBuilder {
        private String _fileName;
        private boolean _isAppend;
        private Class _dataClass;
        private List<String> _dataClassFields;
        private LinkedHashMap<String, Class> _fieldOrder;
        private HashMap<String, Function<?, ?>> _typeResolvers;
        private LinkedHashMap<Class, Method> _methodWrites;
        private HashMap<String, Class> _resolveTypeMap;

        private ConfigurationBuilder() {
            _fieldOrder = new LinkedHashMap<>();
            _dataClassFields = new ArrayList<>();
            _typeResolvers = new HashMap<>();
            _methodWrites = new LinkedHashMap<>();
            _resolveTypeMap = new HashMap<>();
            _isAppend = false;
        }

        public ConfigurationBuilder sourceFile(String fileName) {
            _fileName = fileName;
            return this;
        }

        public ConfigurationBuilder writeAppend() {
            _isAppend = true;
            return this;
        }

        public ConfigurationBuilder forWriteObjectOf(Class dataClass) {
            _dataClass = dataClass;
            _dataClassFields.addAll(Arrays.stream(_dataClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toList()));
            return this;
        }


        private Method getWriteMethodFor(Class argumentType) {
            if (argumentType == String.class)
                return ReflectionUtil.getMethod(DataOutputStream.class, method -> method.getName().compareTo("writeUTF") == 0 && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class);
            else
                return ReflectionUtil.getMethod(DataOutputStream.class, method -> (method.getName().startsWith("write") && method.getName().length() > "write".length() && method.getParameterCount() == 1 && method.getParameterTypes()[0] == PrimitiveWrapperMapper.map(argumentType)));
        }

        public ConfigurationBuilder thenWrite(String fieldName, Class fieldType) {
            if (_dataClassFields.contains(fieldName)) {
                _fieldOrder.put(fieldName, fieldType);
            }
            return this;
        }

        public ConfigurationBuilder thenWrite(String fieldName, Class fieldType, Class resolveType, Function<?, ?> typeResolver) {
            if (_dataClassFields.contains(fieldName)) {
                _fieldOrder.put(fieldName, fieldType);
                _typeResolvers.put(fieldName, typeResolver);
                _resolveTypeMap.put(fieldName, resolveType);
            }
            return this;
        }

        public BinaryWriter build() {
            BinaryWriter writer = new BinaryWriter();
            writer._dataClass = _dataClass;
            writer._fieldOrder = _fieldOrder;
            writer._typeResolvers = _typeResolvers;
            try {
                File f = new File(_fileName);
                FileOutputStream fileOutputStream = (_isAppend) ? new FileOutputStream(f, true) : new FileOutputStream(f);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                writer._dataOutputStream = new DataOutputStream(bufferedOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            for (String key : _fieldOrder.keySet()) {
                Class fieldType = _fieldOrder.get(key);
                Method method = getWriteMethodFor(fieldType);
                if (method != null) {
                    if (!_methodWrites.containsKey(fieldType))
                        _methodWrites.put(fieldType, method);
                } else {
                    Class resolveType = _resolveTypeMap.get(key);
                    method = getWriteMethodFor(resolveType);
                    if (!_methodWrites.containsKey(resolveType))
                        _methodWrites.put(fieldType, method);
                }
            }
            writer._methodWrites = _methodWrites;
            return writer;
        }
    }
}