package com.paypal.http.serializer;

import com.google.gson.FieldNamingPolicy;
import com.paypal.http.HttpRequest;
import com.paypal.http.annotations.ListOf;
import com.paypal.http.exceptions.MalformedJsonException;
import com.paypal.http.exceptions.SerializeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Json implements Serializer {

    @Override
    public String contentType() {
        return "^application\\/json";
    }

    private boolean hasAncestor(Class descendant, Class ancestor) {
        if (descendant == null) {
            return false;
        } else if (ancestor.isInterface()) {
            List<Class> interfaces = Arrays.asList(descendant.getInterfaces());
            if (interfaces.contains(ancestor)) {
                return true;
            }
        }

        if (!descendant.equals(Object.class) && descendant.isAssignableFrom(ancestor)) {
            return true;
        }

        return hasAncestor(descendant.getSuperclass(), ancestor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(String source, Class<T> cls) throws IOException {
        Gson gson = new GsonBuilder().create();

        if (hasAncestor(cls, List.class) && cls.getAnnotation(ListOf.class) != null) {
            ListOf listOf = cls.getAnnotation(ListOf.class);

            List<Map<String, Object>> deserialized = gson.fromJson(source, new TypeToken<List<Map<String, Object>>>(){}.getType());
            try {
                T outlist = cls.getDeclaredConstructor().newInstance();
                for (Map<String, Object> map : deserialized) {
                    ((List) outlist).add(unmap(map, listOf.listClass()));
                }

                return outlist;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new UnsupportedEncodingException("Could not instantiate type " + cls.getSimpleName());
            }
        } else if (hasAncestor(cls, List.class) || hasAncestor(cls, Map.class)) {
            try {
                return gson.fromJson(source, cls);
            } catch (com.google.gson.JsonSyntaxException e) {
                throw new MalformedJsonException("Malformed Json " + e.getMessage());
            }
        } else {
            try {
                Map<String, Object> deserialized = gson.fromJson(source, new TypeToken<Map<String, Object>>(){}.getType());
                return unmap(deserialized, cls);
            } catch (com.google.gson.JsonSyntaxException e) {
                throw new MalformedJsonException("Malformed Json " + e.getMessage());
            }
        }
    }

    private <T> T unmap(Map<String, Object> map, Class<T> destinationClass) throws IOException {
        try {
            return ObjectMapper.unmap(map, destinationClass);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new UnsupportedEncodingException("Could not instantiate type " + destinationClass.getSimpleName());
        } catch (RuntimeException re) {
            throw new MalformedJsonException("Unable to parse Json " + re.getMessage());
        }
    }

    @Override
    public byte[] encode(HttpRequest request) throws SerializeException {
        return serialize(request.requestBody()).getBytes(UTF_8);
    }

    public String serialize(Object o) throws SerializeException {
        Gson gson = new Gson();
        if (ObjectMapper.isModel(o)) {
            try {
                return gson.toJson(ObjectMapper.map(o));
            } catch (IllegalAccessException e) {
                throw new SerializeException(e.getMessage());
            }
        } else {
            return jsonValueStringFor(o);
        }
    }

    @SuppressWarnings("unchecked")
    private String jsonValueStringFor(Object obj) throws SerializeException {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        if (obj == null || obj instanceof String || obj instanceof Number || obj instanceof Boolean ) {
            return gson.toJson(obj);
        } else if (obj instanceof Object[] || obj instanceof Collection ){
            return gson.toJson(obj, new TypeToken<List<Object>>(){}.getType());
        } else if (obj instanceof Map) {
            return gson.toJson(obj, new TypeToken<Map<String, Object>>(){}.getType());
        } else if (ObjectMapper.isModel(obj)) {
            return serialize(obj);
        } else {
            throw new SerializeException(String.format("Object of class %s could not be serialized as json", obj.getClass()));
        }
    }
}
