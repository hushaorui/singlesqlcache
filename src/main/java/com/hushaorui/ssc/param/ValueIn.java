package com.hushaorui.ssc.param;

import java.util.Collection;

public class ValueIn<FIELD_CLASS> {
    private Collection<FIELD_CLASS> values;

    public Collection<FIELD_CLASS> getValues() {
        return values;
    }

    public void setValues(Collection<FIELD_CLASS> values) {
        this.values = values;
    }

    public ValueIn(Collection<FIELD_CLASS> values) {
        this.values = values;
    }
}
