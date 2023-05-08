package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.FieldDesc;

public class TestByteArray {
    private Long id;
    private byte[] field1;
    private Byte[] field2;
    @FieldDesc(uniqueName = "name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getField1() {
        return field1;
    }

    public void setField1(byte[] field1) {
        this.field1 = field1;
    }

    public Byte[] getField2() {
        return field2;
    }

    public void setField2(Byte[] field2) {
        this.field2 = field2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
