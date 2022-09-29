package com.hushaorui.ssc.param;

public class ValueLike<FIELD_CLASS> implements SpecialValue {
    private FIELD_CLASS value;

    public FIELD_CLASS getValue() {
        return value;
    }

    public void setValue(FIELD_CLASS value) {
        this.value = value;
    }

    public ValueLike(FIELD_CLASS value) {
        this.value = value;
    }
}
