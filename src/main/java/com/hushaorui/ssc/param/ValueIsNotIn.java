package com.hushaorui.ssc.param;

import java.util.Collection;

public class ValueIsNotIn<FIELD_CLASS> extends ValueIn<FIELD_CLASS> {
    public ValueIsNotIn(Collection<FIELD_CLASS> values) {
        super(values);
    }
}
