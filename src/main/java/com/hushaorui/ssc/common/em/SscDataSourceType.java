package com.hushaorui.ssc.common.em;

import com.hushaorui.ssc.common.data.SscHashMap;
import com.hushaorui.ssc.common.data.SscTableInfo;
import com.hushaorui.ssc.common.data.SscTableInfoDerby;
import com.hushaorui.ssc.common.data.SscTableInfoMysql;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 支持的数据源的类型
 */
public enum SscDataSourceType {
    Mysql(SscTableInfoMysql.class, Collections.unmodifiableMap(SscHashMap.of(
            String.class, "VARCHAR"
            ,byte.class, "TINYINT"
            ,Byte.class, "TINYINT"
            ,short.class, "SMALLINT"
            ,Short.class, "SMALLINT"
            ,int.class, "INT"
            ,Integer.class, "INT"
            ,long.class, "BIGINT"
            ,Long.class, "BIGINT"
            ,float.class, "FLOAT"
            ,Float.class, "FLOAT"
            ,double.class, "DOUBLE"
            ,Double.class, "DOUBLE"
            ,boolean.class, "BOOL"
            ,Boolean.class, "BOOL"
            ,char.class, "VARCHAR"
            ,Character.class, "VARCHAR"
            ,Timestamp.class, "TIMESTAMP"
            ,Date.class, "DATETIME"
            ,byte[].class, "BLOB"
    ))),
    Derby(SscTableInfoDerby.class, Collections.unmodifiableMap(SscHashMap.of(
            String.class, "VARCHAR"
            ,byte.class, "SMALLINT"
            ,Byte.class, "SMALLINT"
            ,short.class, "SMALLINT"
            ,Short.class, "SMALLINT"
            ,int.class, "INT"
            ,Integer.class, "INT"
            ,long.class, "BIGINT"
            ,Long.class, "BIGINT"
            ,float.class, "FLOAT"
            ,Float.class, "FLOAT"
            ,double.class, "DOUBLE"
            ,Double.class, "DOUBLE"
            ,boolean.class, "INT"
            ,Boolean.class, "INT"
            ,char.class, "VARCHAR"
            ,Character.class, "VARCHAR"
            ,Timestamp.class, "TIMESTAMP"
            ,Date.class, "DATETIME"
            ,byte[].class, "BLOB"
    ))),
    ;
    /** sql 拼接的实现类 */
    private Class<? extends SscTableInfo> tableInfoClass;
    /** java类型和表类型的映射 */
    private Map<Class<?>, String> javaTypeToTableTypeMap;
    SscDataSourceType(Class<? extends SscTableInfo> tableInfoClass, Map<Class<?>, String> javaTypeToTableTypeMap) {
        this.tableInfoClass = tableInfoClass;
        this.javaTypeToTableTypeMap = javaTypeToTableTypeMap;
    }

    public Class<? extends SscTableInfo> getTableInfoClass() {
        return tableInfoClass;
    }

    public Map<Class<?>, String> getJavaTypeToTableTypeMap() {
        return new HashMap<>(javaTypeToTableTypeMap);
    }
}
