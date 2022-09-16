package com.hushaorui.ssc.main;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;
import com.hushaorui.ssc.common.anno.IdDesc;
import com.hushaorui.ssc.common.data.DataClassDesc;
import com.hushaorui.ssc.common.data.SingleSqlCacheConfig;
import com.hushaorui.ssc.common.em.TableNameStyle;
import com.hushaorui.ssc.exception.SscRuntimeException;
import com.hushaorui.ssc.util.SscStringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class AbstractTableOperator {
    /** 核心配置 */
    protected SingleSqlCacheConfig singleSqlCacheConfig;
    /** 数据库操作类 */
    protected JdbcTemplate jdbcTemplate;
    /** 定时器 */
    private Timer timer;
    /** 存入缓存的开关，全局性质，控制所有 */
    private static boolean saveSwitch = true;
    /** 取出缓存的开关，全局性质，控制所有 */
    private static boolean getSwitch = true;
    /** 所有的数据类描述 */
    protected static final Map<Class<Serializable>, DataClassDesc> dataClassMapping = new HashMap<>();
    protected static final Map<String, DataClassDesc> tableNameMapping = new HashMap<>();

    public AbstractTableOperator(JdbcTemplate jdbcTemplate, SingleSqlCacheConfig singleSqlCacheConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.singleSqlCacheConfig = singleSqlCacheConfig;
    }

    public AbstractTableOperator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new SingleSqlCacheConfig());
    }

    /** 注册所有的数据类 */
    public void registerDataClass(Class<Serializable> dataClass) {
        dataClassMapping.computeIfAbsent(dataClass, clazz -> {
            DataClassDesc dataClassDesc = new DataClassDesc();
            dataClassDesc.setDataClass(clazz);
            int tableCount;
            DataClass annotation = clazz.getDeclaredAnnotation(DataClass.class);
            // 表名
            String tableName;
            if (annotation == null) {
                // 默认不分表
                tableCount = 1;
                tableName = generateTableName(clazz);

            } else {
                tableCount = annotation.tableCount();
                String value = annotation.value();
                if (value.length() == 0) {
                    tableName = generateTableName(clazz);
                } else {
                    tableName = value;
                }
            }
            if (tableNameMapping.containsKey(tableName)) {
                // 表名重复
                throw new SscRuntimeException("Duplicate table name: " + tableName);
            }
            // 所有的类字段名和表字段名的映射
            Map<String, String> propColumnMapping = new LinkedHashMap<>();
            // 所有的表字段名和类字段名的映射
            Map<String, String> columnPropMapping = new LinkedHashMap<>();
            // 所有类字段名和它的get方法
            Map<String, Method> propGetMethods = new HashMap<>();
            // 所有的类字段名和它的set方法
            Map<String, Method> propSetMethods = new HashMap<>();
            // 所有不为空的字段集合
            Set<String> notNullProps = new HashSet<>();
            // 所有唯一键的字段集合
            Set<String> uniqueProps = new HashSet<>();
            // 所有不会更新的字段集合
            Set<String> notUpdateProps = new HashSet<>();
            // 所有根据某些字段查询所有符合条件的数据的集合
            Map<String, Set<String>> findAllProps = new HashMap<>();

            // id字段的名称
            String idPropName = null;
            // 分表的字段的get方法
            Method splitFieldGetMethod = null;
            Class<?> tempClass = clazz;
            while (! Object.class.equals(tempClass)) {
                Field[] declaredFields = tempClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    field.setAccessible(true);
                    if (Modifier.isStatic(field.getModifiers())) {
                        // 静态字段，不能当作属性字段
                        continue;
                    }
                    // 类字段名
                    String propName = field.getName();
                    // 表字段名
                    String columnName;
                    FieldDesc fieldDesc = field.getDeclaredAnnotation(FieldDesc.class);
                    if (fieldDesc == null) {
                        columnName = generateColumnName(propName);
                    } else {
                        if (fieldDesc.noneSave()) {
                            // 该字段不参与数据库操作
                            continue;
                        }
                        if (fieldDesc.isId()) {
                            idPropName = propName;
                        }
                        String value = fieldDesc.value();
                        if (value.length() == 0) {
                            columnName = generateColumnName(propName);
                        } else {
                            columnName = value;
                        }
                    }
                    if (columnPropMapping.containsKey(columnName)) {
                        throw new SscRuntimeException(String.format("Duplicate column name: %s in class: %s", columnName, clazz.getName()));
                    }
                    // 将类字段名和表字段名添加映射
                    propColumnMapping.put(propName, columnName);
                    columnPropMapping.put(columnName, propName);

                }
                tempClass = tempClass.getSuperclass();
            }
            dataClassDesc.setTableCount(tableCount);
            dataClassDesc.setTableName(tableName);
            dataClassDesc.setColumnPropMapping(columnPropMapping);
            dataClassDesc.setPropColumnMapping(propColumnMapping);
            dataClassDesc.setPropGetMethods(propGetMethods);
            dataClassDesc.setPropSetMethods(propSetMethods);
            dataClassDesc.setSplitFieldGetMethod(splitFieldGetMethod);
            dataClassDesc.setIdPropName(idPropName);
            dataClassDesc.setNotNullProps(notNullProps);
            dataClassDesc.setUniqueProps(uniqueProps);
            dataClassDesc.setNotUpdateProps(notUpdateProps);
            dataClassDesc.setFindAllProps(findAllProps);
            // TODO 补充，比如id

            tableNameMapping.put(tableName, dataClassDesc);
            return dataClassDesc;
        });

    }

    private String generateColumnName(String propName) {
        switch (singleSqlCacheConfig.getColumnNameStyle()) {
            case UPPERCASE_UNDERLINE:
                return SscStringUtils.humpToUnderline(propName).toUpperCase();
            case LOWERCASE:
                return propName.toLowerCase();
            case UPPERCASE:
                return propName.toUpperCase();
            default:
                return SscStringUtils.humpToUnderline(propName).toLowerCase();
        }
    }

    private String generateTableName(Class<Serializable> clazz) {
        switch (singleSqlCacheConfig.getTableNameStyle()) {
            case UPPERCASE_UNDERLINE:
                return SscStringUtils.humpToUnderline(clazz.getSimpleName()).toUpperCase();
            case SIMPLE_CLASS_NAME:
                return clazz.getSimpleName();
            case LOWERCASE_SIMPLE_CLASS_NAME:
                return clazz.getSimpleName().toLowerCase();
            case UPPERCASE_SIMPLE_CLASS_NAME:
                return clazz.getSimpleName().toUpperCase();
            case LOWERCASE_FULL_CLASS_NAME_UNDERLINE:
                return clazz.getName().replace(".", "_").toLowerCase();
            case UPPERCASE_FULL_CLASS_NAME_UNDERLINE:
                return clazz.getName().replace(".", "_").toUpperCase();
            default:
                return SscStringUtils.humpToUnderline(clazz.getSimpleName());
        }
    }
}
