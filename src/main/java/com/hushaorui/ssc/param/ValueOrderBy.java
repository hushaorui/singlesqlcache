package com.hushaorui.ssc.param;

public class ValueOrderBy implements SpecialValue {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValueOrderBy(String value) {
        this.value = value;
    }
}
