package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.FieldDesc;

import java.util.List;
import java.util.Map;

public class TestSpecial {
    private Long id;
    @FieldDesc(columnType = "TEXT")
    private String[] stringArray;
    @FieldDesc(columnType = "TEXT")
    private Boolean[] booleans;
    private String message;
    @FieldDesc(columnType = "TEXT")
    private List<Integer> integers;
    // TEXT字段无法当作索引
    @FieldDesc(columnType = "TEXT"/*, uniqueName = "integerStringMap"*/)
    private Map<Integer, String> integerStringMap;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public Boolean[] getBooleans() {
        return booleans;
    }

    public void setBooleans(Boolean[] booleans) {
        this.booleans = booleans;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Integer> getIntegers() {
        return integers;
    }

    public void setIntegers(List<Integer> integers) {
        this.integers = integers;
    }

    public Map<Integer, String> getIntegerStringMap() {
        return integerStringMap;
    }

    public void setIntegerStringMap(Map<Integer, String> integerStringMap) {
        this.integerStringMap = integerStringMap;
    }
}
