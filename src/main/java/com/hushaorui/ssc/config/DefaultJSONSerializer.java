package com.hushaorui.ssc.config;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

public class DefaultJSONSerializer implements JSONSerializer {
    @Override
    public String toJsonString(Object object) {
        return JSONArray.toJSONString(object);
    }

    @Override
    public Object parseObject(String jsonString, Class<?> objectClass) {
        return JSONArray.parseObject(jsonString, objectClass);
    }

    @Override
    public List<?> parseArray(String jsonString, Class<?> objectClass) {
        return JSONArray.parseArray(jsonString, objectClass);
    }
}
