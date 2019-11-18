package com.paypal.http.serializer;

import com.paypal.http.annotations.Model;
import com.paypal.http.annotations.SerializedName;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectMapper {

	public static Map<String, Object> map(Object o) throws IllegalAccessException {
		Map<String, Object> serialized = new HashMap<>();
		Class oClass = o.getClass();
		for (Field f : oClass.getDeclaredFields()) {
			if (!Modifier.isTransient(f.getModifiers())) {
				f.setAccessible(true);

				SerializedName sn = f.getAnnotation(SerializedName.class);
				if (sn == null) {
					continue;
				}

				String key = sn.value();

				Object value = f.get(o);
				if (value != null) {
					if (isPrimitive(value)) {
						serialized.put(key, value);
					} else if (value instanceof List) {
						List valueList = new ArrayList();
						for (Object subValue : (List) value) {
							if (isPrimitive(subValue)) {
								valueList.add(subValue);
							} else {
								valueList.add(map(subValue));
							}
						}
						serialized.put(key, valueList);
					} else {
						serialized.put(key, map(value));
					}
				}
			}
		}

		return serialized;
	}

	@SuppressWarnings("unchecked")
	public static <T> T unmap(Map<String, Object> inputData, Class<T> cls) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		T instance = cls.getConstructor().newInstance();

		for (String key : inputData.keySet()) {
			Object value = inputData.get(key);

			Field f = fieldForSerializedName(key, cls);
			if (f == null || value == null) {
				continue;
			}

			SerializedName sn = f.getAnnotation(SerializedName.class);
			f.setAccessible(true);

			if (isPrimitive(value)) {
				if (isNumeric(f.getType())) {
					f.set(instance, numericCast(f.getType(), (Number) value));
				} else {
					f.set(instance, value);
				}
			} else if (value instanceof List) {
				Class listClass = sn.listClass();
				if (listClass.equals(Void.class)) {
					throw new InstantiationException("Generated array properties must set the listClass property on SerializedName");
				}
				List destList = new ArrayList();
				if (isPrimitive(listClass)) {
					List sourceList = (List) value;
					for (Object sourceListValue : sourceList) {
						destList.add(listClass.cast(sourceListValue));
					}
				} else {
					List<Map<String, Object>> sourceList = (List<Map<String, Object>>) value;
					for (Map<String, Object> subValue : sourceList) {
						destList.add(unmap(subValue, listClass));
					}
				}

				f.set(instance, destList);
			} else {
				f.set(instance, unmap((Map<String, Object>) value, f.getType()));
			}
		}

		return instance;
	}

	private static Field fieldForSerializedName(String name, Class cls) {
		Field target = null;
		for (Field f : cls.getDeclaredFields()) {
			SerializedName sn = f.getAnnotation(SerializedName.class);
			if (sn != null && name.equals(sn.value())) {
				target = f;
				break;
			}
		}

		return target;
	}

	public static boolean isModel(Object o) {
		return o.getClass().getAnnotation(Model.class) != null;
	}

	private static boolean isPrimitive(Object o) {
		return o != null && isPrimitive(o.getClass());
	}

	private static boolean isPrimitive(Class cls) {
		return cls.isPrimitive() || isWrapperType(cls);
	}

	private static boolean isNumeric(Class cls) {
		return NUMERIC_TYPES.contains(cls);
	}

	private static Object numericCast(Class dest, Number o) {
		if (dest.equals(Byte.class)) {
			return o.byteValue();
		} else if (dest.equals(Short.class)) {
			return o.shortValue();
		} else if (dest.equals(Integer.class)) {
			return o.intValue();
	 	} else if (dest.equals(Long.class)) {
			return o.longValue();
		} else if (dest.equals(Float.class)) {
			return o.floatValue();
		} else if (dest.equals(Double.class)) {
			return o.doubleValue();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static final Set<Class> WRAPPER_TYPES = new HashSet(Arrays.asList(
			Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, String.class, Void.class));

	@SuppressWarnings("unchecked")
	private static final Set<Class> NUMERIC_TYPES = new HashSet(Arrays.asList(
			Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class));

	private static boolean isWrapperType(Class cls) {
		return WRAPPER_TYPES.contains(cls);
	}
}
