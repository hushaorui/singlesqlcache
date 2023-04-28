package com.hushaorui.ssc.common;

public class StringOrArray {
    private String value;
    private String[] array;
    public StringOrArray(String value) {
        this.value = value;
    }
    public StringOrArray(String[] array) {
        this.array = array;
    }

    public String getValue() {
        return value;
    }

    public String[] getArray() {
        return array;
    }

    public String getByIndex(int index) {
        if (array != null) {
            return array[index];
        } else {
            return value;
        }
    }
}
