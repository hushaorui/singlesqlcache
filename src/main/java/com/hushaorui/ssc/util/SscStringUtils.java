package com.hushaorui.ssc.util;

import com.hushaorui.ssc.config.JSONSerializer;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public abstract class SscStringUtils {

    /** 驼峰转下划线 */
    public static String humpToUnderline(String content) {
        StringBuilder builder = new StringBuilder();
        char[] chars = content.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            String string = String.valueOf(chars[i]);
            String lower = string.toLowerCase();
            if (i != 0 && !string.equals(lower)) {
                // 不是第一个字母且是大写字母，拼接下划线
                builder.append("_");
            }
            builder.append(lower);
        }
        return builder.toString();
    }

    /**
     * 获取字段对应的get方法方法名
     * @param field 字段
     * @return get方法名
     */
    public static String getGetMethodNameByFieldName(Field field) {
        String fieldName = field.getName();
        String getMethodName;
        if (boolean.class.equals(field.getType())) {
            if (fieldName.startsWith("is")) {
                // 字段命名一般不要以is开头，但是既然命名了，就不能让它出错
                getMethodName = fieldName;
            } else if (fieldName.length() == 1) {
                getMethodName = "is" + fieldName.toUpperCase();
            } else {
                getMethodName = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            }
        } else if (fieldName.length() == 1) {
            getMethodName = "get" + fieldName.toUpperCase();
        } else {
            getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        return getMethodName;
    }

    /**
     * 获取字段对应的set方法方法名
     * @param field 字段
     * @return set方法名
     */
    public static String getSetMethodNameByFieldName(Field field) {
        String fieldName = field.getName();
        String setMethodName;
        if (fieldName.length() == 1) {
            setMethodName = "set" + fieldName.toUpperCase();
        } else {
            setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        return setMethodName;
    }

    /**
     * 格式化输出一定长度的数字
     * @param number 需要输出的数字
     * @param length 输出的最小长度
     * @return 格式化后的字符串
     */
    public static String getSameLengthString(long number, int length) {
        return String.format("%0" + length + "d", number);
    }

    /**
     * 获取md5字符串
     * @param sourceStr 原字符串
     * @return md5字符串
     */
    public static String md5Encode(String sourceStr) {
        String signStr = null;
        try {
            byte[] bytes = sourceStr.getBytes(StandardCharsets.UTF_8.name());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            byte[] md5Byte = md5.digest();
            if (md5Byte != null) {
                signStr = HexBin.encode(md5Byte);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return signStr == null ? sourceStr : signStr.toLowerCase();
    }

    /**
     * 获取字符串同步锁
     *
     * @param id       确保在同一个dataType下唯一
     * @param dataType 数据类型
     * @return 同步锁
     */
    public static String getLock(Object id, Class<?> dataType) {
        return (id + dataType.getSimpleName()).intern();
    }

    /** 获取类的泛型 */
    public static <T> Class<T> getGenericType(Class<T> clazz, int index) {
        try {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            Type firstType;
            if (genericInterfaces.length == 0) {
                firstType = clazz.getGenericSuperclass();
            } else {
                firstType = genericInterfaces[0];
            }
            if (firstType instanceof Class) {
                firstType = ((Class) firstType).getGenericInterfaces()[0];
            }
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) firstType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (index >= actualTypeArguments.length) {
                index = actualTypeArguments.length - 1;
            }
            Type type = actualTypeArguments[index];
            return (Class<T>) Class.forName(type.getTypeName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 所有可以直接存入数据库的类型集合
     */
    public static final Set<Class<?>> staticTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class,
            boolean.class, Boolean.class, char.class, Character.class, float.class, Float.class, double.class, Double.class,
            byte[].class, Byte[].class,
            String.class
    )));
    /**
     * 将字段的值转化为符合sql规定的值
     * @param fieldValue 字段的值
     * @return 转化后的值
     */
    public static Object getFieldValueAccordWithSql(Object fieldValue, JSONSerializer jsonSerializer) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof Date) {
            // Date 类型不需要额外序列化
            return fieldValue;
        }
        if (staticTypes.contains(fieldValue.getClass())) {
            return fieldValue;
        }
        // 其他的都需要序列化
        return jsonSerializer.toJsonString(fieldValue);
    }

    /**
     * 比较两个对象
     * @param object1 对象1
     * @param object2 对象2
     * @return 是否相等
     */
    public static boolean objectEquals(Object object1, Object object2) {
        if (object1 == null) {
            return object2 == null;
        } else {
            return object1.equals(object2);
        }
    }
}
