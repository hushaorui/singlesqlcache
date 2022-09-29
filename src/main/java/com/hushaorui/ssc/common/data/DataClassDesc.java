package com.hushaorui.ssc.common.data;

import java.util.Map;
import java.util.Set;

/**
 * 数据类描述
 */
public class DataClassDesc extends CommonClassDesc {

    /** 表名或表前缀(当分表时为前缀) */
    private String tableName;
    /** 分表的数量 */
    private int tableCount;

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
    /** 所有需要添加缓存的条件查询字段集合 */
    private Map<String, Set<String>> conditionProps;
    /** 所有不会更新的字段集合 */
    private Set<String> notUpdateProps;
    /** 所有不需要缓存的字段集合 */
    private Set<String> notCachedProps;

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

    public Map<String, Set<String>> getConditionProps() {
        return conditionProps;
    }

    public void setConditionProps(Map<String, Set<String>> conditionProps) {
        this.conditionProps = conditionProps;
    }

    public Set<String> getNotUpdateProps() {
        return notUpdateProps;
    }

    public void setNotUpdateProps(Set<String> notUpdateProps) {
        this.notUpdateProps = notUpdateProps;
    }

    public Set<String> getNotCachedProps() {
        return notCachedProps;
    }

    public void setNotCachedProps(Set<String> notCachedProps) {
        this.notCachedProps = notCachedProps;
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
}
