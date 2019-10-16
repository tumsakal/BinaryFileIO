package com.sakal.binaryfileio;


import com.sakal.binaryfileio.util.PrimitiveWrapperMapper;
import com.sakal.binaryfileio.util.ReflectionUtil;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BinaryReader {
    private DataInputStream _dataInputStream;
    private Class _dataClass;
    private LinkedHashMap<String, Class> _fieldOrder;
    private HashMap<String, Function<?, ?>> _typeResolvers;
    private LinkedHashMap<Class, Method> _methodReads;
    private HashMap<String, Class> _resolveTypeMap;

    private BinaryReader() {
    }

    public static ConfigurationBuilder newConfigurationBuilder() {
        return new ConfigurationBuilder();
    }

    public Object read() {
        try {
            if (_dataInputStream.available() <= 0)
                return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object result = null;
        try {
            result = _dataClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        for (String fieldName : _fieldOrder.keySet()) {
            Class fieldType = _fieldOrder.get(fieldName);
            Method method = _methodReads.get(fieldType);
            Object returnData;
            if (method != null) {
                try {
                    returnData = method.invoke(_dataInputStream);
                    if (returnData != null) {
                        if (method.getReturnType() == fieldType)
                            _dataClass.getField(fieldName).set(result, returnData);
                        else {
                            Function func = _typeResolvers.get(fieldName);
                            Object resolveData = func.apply(returnData);
                            _dataClass.getField(fieldName).set(result, resolveData);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public void close() {
        try {
            _dataInputStream.close();
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
        private Class _dataClass;
        private List<String> _dataClassFields;
        private LinkedHashMap<String, Class> _fieldOrders;
        private HashMap<String, Function<?, ?>> _typeResolvers;
        private LinkedHashMap<Class, Method> _methodReads;
        private HashMap<String, Class> _resolveTypeMap;

        private ConfigurationBuilder() {
            _dataClassFields = new ArrayList<>();
            _fieldOrders = new LinkedHashMap<>();
            _typeResolvers = new HashMap<>();
            _methodReads = new LinkedHashMap<>();
            _resolveTypeMap = new HashMap<>();
        }

        public ConfigurationBuilder sourceFile(String fileName) {
            _fileName = fileName;
            return this;
        }

        public ConfigurationBuilder forReadObjectOf(Class dataClass) {
            _dataClass = dataClass;
            _dataClassFields.addAll(Arrays.stream(_dataClass.getDeclaredFields()).map(field -> field.getName()).collect(Collectors.toList()));
            return this;
        }

        private Method getMethodReadFor(Class argumentType) {
            if (argumentType == String.class)
                return ReflectionUtil.getMethod(DataInputStream.class, method -> method.getName().compareTo("readUTF") == 0 && method.getParameterCount() == 0 && method.getReturnType() == String.class);
            else
                return ReflectionUtil.getMethod(DataInputStream.class, method -> (method.getName().startsWith("read") && method.getName().length() > "read".length() && method.getParameterCount() == 0 && method.getReturnType() == PrimitiveWrapperMapper.map(argumentType)));
        }

        public ConfigurationBuilder setReadOrder(String fieldName, Class fieldType) {
            if (_dataClassFields.contains(fieldName))
                _fieldOrders.put(fieldName, fieldType);
            return this;
        }

        public ConfigurationBuilder setReadOrder(String fieldName, Class fieldType, Class resolveType, Function typeResolver) {
            if (_dataClassFields.contains(fieldName)) {
                _fieldOrders.put(fieldName, fieldType);
                _typeResolvers.put(fieldName, typeResolver);
                _resolveTypeMap.put(fieldName, resolveType);
            }
            return this;
        }

        public BinaryReader build() {
            BinaryReader reader = new BinaryReader();
            reader._dataClass = this._dataClass;
            reader._fieldOrder = this._fieldOrders;
            reader._typeResolvers = this._typeResolvers;
            reader._resolveTypeMap = this._resolveTypeMap;
            //
            try {
                File file = new File(_fileName);
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                reader._dataInputStream = new DataInputStream(bufferedInputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //
            for (String key : _fieldOrders.keySet()) {
                Class fieldType = _fieldOrders.get(key);
                Method method = getMethodReadFor(fieldType);
                if (method != null) {
                    if (!_methodReads.containsKey(fieldType))
                        _methodReads.put(fieldType, method);
                } else {
                    Class resolveType = _resolveTypeMap.get(key);
                    method = getMethodReadFor(resolveType);
                    if (!_methodReads.containsKey(resolveType))
                        _methodReads.put(fieldType, method);
                }
            }
            reader._methodReads = this._methodReads;
            return reader;
        }
    }
}
