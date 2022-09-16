package com.hushaorui.ssc.util;

import java.lang.reflect.Field;

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
}
