package com.hushaorui.ssc.config;

import java.util.List;

public interface JSONSerializer {
    /** 将对象转化为json字符串 */
    String toJsonString(Object object);

    Object parseObject(String jsonString, Class<?> objectClass);

    List<?> parseArray(String jsonString, Class<?> objectClass);
}
