package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.common.StringOrArray;

import java.util.List;

public class SscSqlResult {
    private StringOrArray sql;
    private List<Object> params;

    public SscSqlResult(StringOrArray sql, List<Object> params) {
        this.sql = sql;
        this.params = params;
    }

    public StringOrArray getSql() {
        return sql;
    }

    public void setSql(StringOrArray sql) {
        this.sql = sql;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
