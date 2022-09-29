package com.hushaorui.ssc.param;

public class ValueBetween<FIELD_CLASS> {
    private FIELD_CLASS first;
    private FIELD_CLASS last;

    public ValueBetween(FIELD_CLASS first, FIELD_CLASS last) {
        this.first = first;
        this.last = last;
    }

    public FIELD_CLASS getFirst() {
        return first;
    }

    public void setFirst(FIELD_CLASS first) {
        this.first = first;
    }

    public FIELD_CLASS getLast() {
        return last;
    }

    public void setLast(FIELD_CLASS last) {
        this.last = last;
    }
}