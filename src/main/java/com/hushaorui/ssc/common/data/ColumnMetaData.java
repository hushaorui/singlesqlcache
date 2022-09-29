package com.hushaorui.ssc.common.data;

import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.io.Serializable;

public class ColumnMetaData implements Serializable {

    private String name; // 字段名称
    private int type; // 字段类型
    private String typeName; // 字段类型名称
    //private String catalogName; // 库名 没有必要
    private int columnDisplaySize; // 字段设置的长度 如 VARCHAR(64) 长度为64
    private String columnClassName; // 字段类型对应java类的class名称，如 java.lang.String
    private String columnLabel; // 目前同 name ，待定
    private String schemaName; // 约束名称 x 目前没用

    public ColumnMetaData(SqlRowSetMetaData metaData, int i) {
        //ColumnMetaData(name=id, type=-5, typeName=BIGINT, columnDisplaySize=20, columnClassName=java.lang.Long, columnLabel=id, schemaName=)
        this.name = metaData.getColumnName(i);
        this.type = metaData.getColumnType(i);
        this.typeName = metaData.getColumnTypeName(i);
        this.columnDisplaySize = metaData.getColumnDisplaySize(i);
        this.columnClassName = metaData.getColumnClassName(i);
        this.columnLabel = metaData.getColumnLabel(i);
        this.schemaName = metaData.getSchemaName(i);
    }

    @Override
    public String toString() {
        return "ColumnMetaData{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", typeName='" + typeName + '\'' +
                ", columnDisplaySize=" + columnDisplaySize +
                ", columnClassName='" + columnClassName + '\'' +
                ", columnLabel='" + columnLabel + '\'' +
                ", schemaName='" + schemaName + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getColumnDisplaySize() {
        return columnDisplaySize;
    }

    public String getColumnClassName() {
        return columnClassName;
    }

    public String getColumnLabel() {
        return columnLabel;
    }

    public String getSchemaName() {
        return schemaName;
    }
}