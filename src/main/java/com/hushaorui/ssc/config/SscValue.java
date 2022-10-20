package com.hushaorui.ssc.config;

import com.hushaorui.ssc.param.ValueConditionEnum;

public class SscValue {
    public String propName;
    public ValueConditionEnum condition;

    public SscValue() {
    }

    public SscValue(String propName, ValueConditionEnum condition) {
        this.propName = propName;
        this.condition = condition;
    }
}
