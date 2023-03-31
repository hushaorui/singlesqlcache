package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;

@DataClass(tableCount = 4)
public class TestUseDb {
    @FieldDesc(isId = true)
    private Long dbId;
    private String dbName;
    private Integer type;
    private Long largeNum;

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getLargeNum() {
        return largeNum;
    }

    public void setLargeNum(Long largeNum) {
        this.largeNum = largeNum;
    }
}
