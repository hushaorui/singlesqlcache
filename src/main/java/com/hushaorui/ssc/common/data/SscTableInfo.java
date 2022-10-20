package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.config.SscValue;
import com.hushaorui.ssc.param.ValueConditionEnum;
import com.hushaorui.ssc.param.ValueIn;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表的信息
 */
public class SscTableInfo {
    /** 建表语句 */
    protected String[] createTableSql;
    /** 删表语句 */
    protected String[] dropTableSql;
    /** 数据插入语句 */
    protected String[] insertSql;
    /** 查询所有的语句 */
    protected String selectAllSql;
    /** 根据id查询指定数据 */
    protected String[] selectByIdSql;
    /** 根据唯一约束的字段查询数据 */
    protected Map<String, String> selectByUniqueKeySql;
    /** 根据指定字段查询数据 */
    protected Map<String, String> selectByConditionSql;
    /** 根据指定字段查询数据 */
    protected Map<String, String> noCachedSelectByConditionSql;

    /** 所有不需要缓存的数据更新 */
    protected String[] updateAllNotCachedByIdSql;
    /** 根据id删除数据 */
    protected String[] deleteByIdSql;
    /** 查询表中的最大id */
    protected String[] selectMaxIdSql;

    /** 数据类描述 */
    protected DataClassDesc classDesc;
    /** id的数据类型 */
    protected Class<?> idJavaType;
    protected Map<String, String> uniqueSelectSqlMap;
    /** 表名 */
    protected String[] tableNames;

    public String[] getCreateTableSql() {
        return createTableSql;
    }

    public String[] getDropTableSql() {
        return dropTableSql;
    }

    public String[] getInsertSql() {
        return insertSql;
    }

    public String getSelectAllSql() {
        return selectAllSql;
    }

    public String[] getSelectByIdSql() {
        return selectByIdSql;
    }

    public Map<String, String> getSelectByUniqueKeySql() {
        return selectByUniqueKeySql;
    }

    public Map<String, String> getSelectByConditionSql() {
        return selectByConditionSql;
    }

    public Map<String, String> getNoCachedSelectByConditionSql() {
        return noCachedSelectByConditionSql;
    }

    public String[] getUpdateAllNotCachedByIdSql() {
        return updateAllNotCachedByIdSql;
    }

    public String[] getDeleteByIdSql() {
        return deleteByIdSql;
    }

    public String[] getSelectMaxIdSql() {
        return selectMaxIdSql;
    }

    public DataClassDesc getClassDesc() {
        return classDesc;
    }

    public Class<?> getIdJavaType() {
        return idJavaType;
    }

    public Map<String, String> getUniqueSelectSqlMap() {
        return uniqueSelectSqlMap;
    }

    public String[] getTableNames() {
        return tableNames;
    }

    public SscTableInfo(DataClassDesc classDesc, SingleSqlCacheConfig config) {
        this.classDesc = classDesc;
    }

    /**
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditions 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    public String getNoCachedConditionSql(String key, List<Pair<String, Object>> conditions) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        return noCachedSelectByConditionSql.getOrDefault(key, putConditionSqlToMap(key, noCachedSelectByConditionSql, conditions));
    }

    public String getNoCachedConditionKeyByList(List<Pair<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < conditions.size(); i++) {
            // 统一使用 columnName
            Pair<String, Object> pair = conditions.get(i);
            String columnName = classDesc.getColumnByProp(pair.getKey());
            key.append(ValueConditionEnum.getKeyString(pair.getValue()));
            // 字段名不可能存在空格，这里使用空格分隔
            key.append(columnName);
            if (i != conditions.size() - 1) {
                key.append(" ");
            }
        }
        return key.toString().intern();
    }

    protected String getNoCachedConditionKey(List<SscValue> propAndTypes) {
        StringBuilder key = new StringBuilder();
        Iterator<SscValue> iterator = propAndTypes.iterator();
        while (iterator.hasNext()) {
            SscValue next = iterator.next();
            // 统一使用 columnName
            String columnName = classDesc.getColumnByProp(next.propName);
            key.append(next.condition.getKeyString());
            // 字段名不可能存在空格，这里使用空格分隔
            key.append(columnName);
            if (iterator.hasNext()) {
                key.append(" ");
            }
        }
        return key.toString().intern();
    }

    protected Map<String, String> getConditionSql() {
        Map<String, String> map = new HashMap<>();
        // select xx.* from xx where xx = ? and xx = ? union all select xx.* from xx where xx = ? and xx = ?
        Map<String, List<SscValue>> conditionProps = classDesc.getConditionProps();
        conditionProps.forEach((selectorName, propAndTypes) -> putConditionSqlToMap(propAndTypes, map));
        return map;
    }

    protected String putConditionSqlToMap(String key, Map<String, String> map, List<Pair<String, Object>> conditions) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            if (conditions != null && conditions.size() > 0) {
                builder.append(" where ");
            }
            if (conditions != null) {
                for (int c = 0; c < conditions.size(); c ++) {
                    Pair<String, Object> pair = conditions.get(c);
                    String columnName = classDesc.getColumnByProp(pair.getKey());
                    Object value = pair.getValue();
                    builder.append(columnName);
                    if (value instanceof ValueIn) {
                        builder.append(" in (");
                        ValueIn<Object> valueIn = (ValueIn<Object>) value;
                        Iterator<Object> it = valueIn.getValues().iterator();
                        while (it.hasNext()) {
                            it.next();
                            builder.append("?");
                            if (it.hasNext()) {
                                builder.append(" ,");
                            }
                        }
                        builder.append(")");
                    } else {
                        builder.append(ValueConditionEnum.getSqlString(value));
                    }
                    if (c != conditions.size() - 1) {
                        builder.append(" and ");
                    }
                }
            }
        }
        String value = builder.toString();
        if (key == null) {
            map.put(getNoCachedConditionKeyByList(conditions), value);
        } else {
            map.put(key, value);
        }
        return value;
    }
    protected void putConditionSqlToMap(List<SscValue> propAndTypes, Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            Iterator<SscValue> iterator = propAndTypes.iterator();
            if (iterator.hasNext()) {
                builder.append(" where ");
            }
            while (iterator.hasNext()) {
                SscValue next = iterator.next();
                String columnName = classDesc.getColumnByProp(next.propName);
                builder.append(columnName);
                ValueConditionEnum type = next.condition;
                if (ValueConditionEnum.ValueIn.equals(type)) {
                    // 无法确定集合的大小，这里只能象征性地放一个
                    builder.append(" in (?)");
                } else {
                    builder.append(type.getSqlString());
                }
                if (iterator.hasNext()) {
                    builder.append(" and ");
                }
            }
        }
        map.put(getNoCachedConditionKey(propAndTypes), builder.toString());
    }
}
