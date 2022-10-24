package com.hushaorui.ssc.param;

public class ValueFirstResult implements SpecialValue {
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public ValueFirstResult(Integer value) {
        this.value = value;
    }
}
