package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SscData;
import com.hushaorui.ssc.config.SscField;

import java.util.*;

/**
 * 数据类描述
 */
public class DataClassDesc extends CommonClassDesc {

    /** 表名或表前缀(当分表时为前缀) */
    private String tableName;
    /** 分表的数量 */
    private int tableCount;
    /** 分表字段 */
    private String tableSplitField;
    /** 是否启用缓存 */
    private boolean cached;
    /** 第一个表，是否使用数字来拼接表名 */
    private boolean appendNumberAtFirstTable;

    /** 所有的类字段名和表字段名的映射 */
    private Map<String, String> propColumnMapping;
    /** 所有的表字段名和类字段名的映射 */
    private Map<String, String> columnPropMapping;
    /** 字段的表类型 */
    private Map<String, String> propColumnTypeMapping;
    /** 所有不为空的字段集合 */
    private Set<String> notNullProps;
    /** 所有有默认值的字段集合，影响建表语句 */
    private Map<String, String> propDefaultValues;
    /** 所有唯一键的字段集合 */
    private Map<String, Set<String>> uniqueProps;
    /** 所有不会更新的字段集合 */
    private Set<String> notUpdateProps;
    /** 所有忽略的字段集合 */
    private Set<String> ignoreProps;
    /** 分组字段 */
    private Set<String> groupProps;

    /** id字段的名称 */
    private String idPropName;

    /** 是否使用id生成策略 */
    private boolean useIdGeneratePolicy;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getTableCount() {
        return tableCount;
    }

    public void setTableCount(int tableCount) {
        this.tableCount = tableCount;
    }

    public String getTableSplitField() {
        return tableSplitField;
    }

    public void setTableSplitField(String tableSplitField) {
        this.tableSplitField = tableSplitField;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public boolean isAppendNumberAtFirstTable() {
        return appendNumberAtFirstTable;
    }

    public void setAppendNumberAtFirstTable(boolean appendNumberAtFirstTable) {
        this.appendNumberAtFirstTable = appendNumberAtFirstTable;
    }

    public Map<String, String> getPropColumnMapping() {
        return propColumnMapping;
    }

    public void setPropColumnMapping(Map<String, String> propColumnMapping) {
        this.propColumnMapping = propColumnMapping;
    }

    public Map<String, String> getColumnPropMapping() {
        return columnPropMapping;
    }

    public void setColumnPropMapping(Map<String, String> columnPropMapping) {
        this.columnPropMapping = columnPropMapping;
    }

    public Map<String, String> getPropColumnTypeMapping() {
        return propColumnTypeMapping;
    }

    public void setPropColumnTypeMapping(Map<String, String> propColumnTypeMapping) {
        this.propColumnTypeMapping = propColumnTypeMapping;
    }

    public Set<String> getNotNullProps() {
        return notNullProps;
    }

    public void setNotNullProps(Set<String> notNullProps) {
        this.notNullProps = notNullProps;
    }

    public Map<String, String> getPropDefaultValues() {
        return propDefaultValues;
    }

    public void setPropDefaultValues(Map<String, String> propDefaultValues) {
        this.propDefaultValues = propDefaultValues;
    }

    public Map<String, Set<String>> getUniqueProps() {
        return uniqueProps;
    }

    public void setUniqueProps(Map<String, Set<String>> uniqueProps) {
        this.uniqueProps = uniqueProps;
    }

    public Set<String> getNotUpdateProps() {
        return notUpdateProps;
    }

    public Set<String> getGroupProps() {
        return groupProps;
    }

    public void setNotUpdateProps(Set<String> notUpdateProps) {
        this.notUpdateProps = notUpdateProps;
    }

    public void setGroupProps(Set<String> groupProps) {
        this.groupProps = groupProps;
    }

    public Set<String> getIgnoreProps() {
        return ignoreProps;
    }

    public void setIgnoreProps(Set<String> ignoreProps) {
        this.ignoreProps = ignoreProps;
    }

    public String getIdPropName() {
        return idPropName;
    }

    public void setIdPropName(String idPropName) {
        this.idPropName = idPropName;
    }

    public boolean isUseIdGeneratePolicy() {
        return useIdGeneratePolicy;
    }

    public void setUseIdGeneratePolicy(boolean useIdGeneratePolicy) {
        this.useIdGeneratePolicy = useIdGeneratePolicy;
    }

    public String getColumnByProp(String propName) {
        return propColumnMapping.getOrDefault(propName, propName);
    }

    public static DataClassDesc transFrom(SscData data) {
        DataClassDesc classDesc = new DataClassDesc();
        classDesc.tableName = data.tableName;
        // 默认为1，不分表
        classDesc.tableCount = data.tableCount <= 0 ? 1 : data.tableCount;
        classDesc.tableSplitField = data.tableSplitField.length() == 0 ? null : data.tableSplitField;
        classDesc.idPropName = data.idPropName;
        classDesc.uniqueProps = new HashMap<>();
        data.uniqueProps.forEach((uniqueName, list) -> classDesc.uniqueProps.put(uniqueName, new HashSet<>(list)));
        // 默认为true，启用缓存
        classDesc.cached = data.cached == null ? true : data.cached;
        classDesc.appendNumberAtFirstTable = data.appendNumberAtFirstTable == null ? true : data.appendNumberAtFirstTable;
        // 默认为true，使用id自动生成策略
        classDesc.useIdGeneratePolicy = data.useIdGeneratePolicy == null ? true : data.useIdGeneratePolicy;

        Map<String, String> propColumnMapping = new HashMap<>();
        Map<String, String> propColumnTypeMapping = new HashMap<>();
        Map<String, String> propDefaultValues = new HashMap<>();
        Set<String> ignoreProps = new HashSet<>();
        Set<String> notNullProps = new HashSet<>();
        Set<String> notUpdateProps = new HashSet<>();
        Set<String> groupProps = new HashSet<>();
        if (classDesc.tableSplitField != null) {
            // 分表字段不能为null，也不能更新
            notNullProps.add(classDesc.getTableSplitField());
            notUpdateProps.add(classDesc.getTableSplitField());
        }
        Map<String, SscField> fieldMap = data.fieldMap;
        if (fieldMap != null) {
            fieldMap.forEach((propName, field) -> {
                if (field.ignore) {
                    ignoreProps.add(propName);
                    return;
                }
                propColumnMapping.put(propName, field.columnName);
                propColumnTypeMapping.put(propName, field.columnType);
                propDefaultValues.put(propName, field.defaultValue);
                if (field.notNull) {
                    notNullProps.add(propName);
                }
                if (field.notUpdate) {
                    notUpdateProps.add(propName);
                }
                if (field.group) {
                    groupProps.add(propName);
                }
            });
        }
        classDesc.setPropColumnMapping(propColumnMapping);
        classDesc.setPropColumnTypeMapping(propColumnTypeMapping);
        classDesc.setPropDefaultValues(propDefaultValues);
        classDesc.setIgnoreProps(ignoreProps);
        classDesc.setNotNullProps(notNullProps);
        classDesc.setNotUpdateProps(notUpdateProps);
        classDesc.setGroupProps(groupProps);
        return classDesc;
    }
}
