package com.hushaorui.ssc.common.data;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 数据类描述
 */
@Getter
@Setter
public class DataClassDesc {
    /** 镜像 */
    private Class<?> dataClass;
    /** 表名或表前缀(当分表时为前缀) */
    private String tableName;
    /** 分表的数量 */
    private int tableCount;

    /** 所有的类字段名和表字段名的映射 */
    private Map<String, String> propColumnMapping;
    /** 所有的表字段名和类字段名的映射 */
    private Map<String, String> columnPropMapping;
    /** 所有类字段名和它的get方法 */
    private Map<String, Method> propGetMethods;
    /** 所有的类字段名和它的set方法 */
    private Map<String, Method> propSetMethods;
    /** 所有不为空的字段集合 */
    private Set<String> notNullProps;
    /** 所有唯一键的字段集合 */
    private Set<String> uniqueProps;
    /** 所有不会更新的字段集合 */
    private Set<String> notUpdateProps;
    /** 所有根据某些字段查询所有符合条件的数据的集合 */
    private Map<String, Set<String>> findAllProps;

    /** id字段的名称 */
    private String idPropName;
    /** 分表的字段的get方法 */
    private Method splitFieldGetMethod;

    public String getColumnByProp(String columnName) {
        return propColumnMapping.getOrDefault(columnName, columnName);
    }
    public String getPropByColumn(String propName) {
        return columnPropMapping.getOrDefault(propName, propName);
    }
}
