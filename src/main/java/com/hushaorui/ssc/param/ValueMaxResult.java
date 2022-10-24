package com.hushaorui.ssc.param;

public class ValueMaxResult implements SpecialValue {
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public ValueMaxResult(Integer value) {
        this.value = value;
    }
}
