package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.config.SscValue;
import com.hushaorui.ssc.param.*;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表的信息
 */
public abstract class SscTableInfo {
    protected static final String AND_STRING = " and ";
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

    public SscSqlResult getSelectByConditionSql(String key, List<Pair<String, Object>> conditions) {
        String sql  = selectByConditionSql.get(key);
        if (sql == null) {
            return null;
        }
        return getParamsAndSql(sql, conditions);
    }

    /**
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditions 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    public SscSqlResult getNoCachedConditionSql(String key, List<Pair<String, Object>> conditions) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        String sql = noCachedSelectByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedSelectByConditionSql, conditions);
        }
        return getParamsAndSql(sql, conditions);
    }

    protected SscSqlResult getParamsAndSql(String sql, List<Pair<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new SscSqlResult(sql, Collections.emptyList());
        }
        Integer firstResult = null;
        Integer maxResult = null;
        String orderBy = null;
        // 根据 sscResult 和 conditions，获得params
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < tableNames.length; i ++) {
            for (Pair<String, Object> pair : conditions) {
                Object value = pair.getValue();
                if (value instanceof ValueIn) {
                    ValueIn<Object> valueIn = (ValueIn<Object>) value;
                    Collection<Object> values = valueIn.getValues();
                    if (i == 0) {
                        String columnName = classDesc.getColumnByProp(pair.getKey());
                        String oldString = columnName + " in (?)";
                        if (values == null || values.isEmpty()) {
                            // 该条件不存在
                            sql = sql.replace(oldString, "");
                        } else if (values.size() > 1) {
                            StringBuilder newString = new StringBuilder(columnName);
                            newString.append(" in (");
                            int loopCount = values.size() - 1;
                            for (int count = 0; count < loopCount; count ++) {
                                newString.append("? ,");
                            }
                            newString.append("?)");
                            // 超过一个
                            sql = sql.replace(oldString, newString.toString());
                        }
                    }
                    if (values != null) {
                        params.addAll(values);
                    }
                    continue;
                } else if (value instanceof ValueBetween) {
                    ValueBetween<Object> valueBetween = (ValueBetween<Object>) value;
                    params.add(valueBetween.getFirst());
                    params.add(valueBetween.getLast());
                    continue;
                } else if ((value instanceof ValueIsNotNull) || (value instanceof ValueIsNull)) {
                    continue;
                }
                if (value instanceof ValueFirstResult) {
                    firstResult = ((ValueFirstResult) value).getValue();
                } else if (value instanceof ValueMaxResult) {
                    maxResult = ((ValueMaxResult) value).getValue();
                } else if (value instanceof ValueOrderBy) {
                    orderBy = ((ValueOrderBy) value).getValue();
                } else if (value instanceof SpecialValue) {
                    params.add(((SpecialValue) value).getValue());
                } else {
                    params.add(value);
                }
            }
            if (tableNames.length > 1 && firstResult != null && maxResult != null) {
                params.add(firstResult);
                params.add(maxResult);
            }
        }
        if (orderBy != null) {
            params.add(orderBy);
        }
        if (tableNames.length == 1 && firstResult != null && maxResult != null) {
            params.add(firstResult);
            params.add(maxResult);
        }
        return new SscSqlResult(sql, params);
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
            Object value = pair.getValue();
            key.append(ValueConditionEnum.getKeyString(value));
            // 字段名不可能存在空格，这里使用空格分隔
            key.append(columnName);
            if (i != conditions.size() - 1) {
                key.append(" ");
            }
        }
        return key.toString().intern();
    }

    protected String getConditionKey(List<SscValue> propAndTypes) {
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

    protected abstract void appendLimitString(StringBuilder builder);

    protected SscSqlResult putNoCachedConditionSqlToMap(String key, Map<String, String> map, List<Pair<String, Object>> conditions) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        List<Object> paramList = new ArrayList<>();
        SscResult sscResult = new SscResult();
        sscResult.paramList = paramList;
        List<Object> params = new ArrayList<>();
        StringBuilder iteratorBuilder = iteratorConditions(conditions, sscResult);
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            builder.append(iteratorBuilder);
            params.addAll(paramList);
            if (tableCount > 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
                appendLimitString(builder);
                params.add(sscResult.firstResult);
                params.add(sscResult.maxResult);
            }
        }
        if (sscResult.orderBy != null) {
            builder.append(" order by ?");
            params.add(sscResult.orderBy);
        }
        if (tableCount == 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
            appendLimitString(builder);
            params.add(sscResult.firstResult);
            params.add(sscResult.maxResult);
        }
        String sql = builder.toString();
        SscSqlResult sscSqlResult = new SscSqlResult(sql, params);
        if (key == null) {
            map.put(getNoCachedConditionKeyByList(conditions), builder.toString());
        } else {
            map.put(key, builder.toString());
        }
        return sscSqlResult;
    }

    private StringBuilder iteratorConditions(List<Pair<String, Object>> conditions, SscResult sscResult) {
        StringBuilder builder = new StringBuilder();
        if (conditions == null) {
            return builder;
        }
        boolean notFirst = false;
        for (Pair<String, Object> pair : conditions) {
            Object value = pair.getValue();
            if (value instanceof ValueIn) {
                if (notFirst) {
                    builder.append(AND_STRING);
                } else {
                    builder.append(" where ");
                }
                notFirst = true;
                builder.append(classDesc.getColumnByProp(pair.getKey()));
                builder.append(" in (");
                ValueIn<Object> valueIn = (ValueIn) value;
                Iterator<Object> it = valueIn.getValues().iterator();
                while (it.hasNext()) {
                    Object listItem = it.next();
                    sscResult.paramList.add(listItem);
                    builder.append("?");
                    if (it.hasNext()) {
                        builder.append(" ,");
                    }
                }
                builder.append(")");
            } else if (value instanceof ValueFirstResult) {
                sscResult.firstResult = ((ValueFirstResult) value).getValue();
            } else if (value instanceof ValueMaxResult) {
                sscResult.maxResult = ((ValueMaxResult) value).getValue();
            } else if (value instanceof ValueOrderBy) {
                sscResult.orderBy = ((ValueOrderBy) value).getValue();
            } else {
                if (notFirst) {
                    builder.append(AND_STRING);
                }
                notFirst = true;
                builder.append(classDesc.getColumnByProp(pair.getKey()));
                builder.append(ValueConditionEnum.getSqlString(value));
                sscResult.paramList.add(value);
            }
        }
        return builder;
    }

    protected void putConditionSqlToMap(List<SscValue> propAndTypes, Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        SscResult sscResult = new SscResult();
        int tableCount = classDesc.getTableCount();
        StringBuilder iteratorBuilder = iteratorPropAndTypes(propAndTypes, sscResult);
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            builder.append(iteratorBuilder);
            if (tableCount > 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
                appendLimitString(builder);
            }
        }
        if (sscResult.orderBy != null) {
            builder.append(" order by ?");
        }
        if (tableCount == 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
            appendLimitString(builder);
        }
        map.put(getConditionKey(propAndTypes), builder.toString());
    }

    private StringBuilder iteratorPropAndTypes(List<SscValue> propAndTypes, SscResult sscResult) {
        StringBuilder builder = new StringBuilder();
        Iterator<SscValue> iterator = propAndTypes.iterator();
        boolean notFirst = false;
        while (iterator.hasNext()) {
            SscValue next = iterator.next();
            ValueConditionEnum type = next.condition;
            if (ValueConditionEnum.ValueIn.equals(type)) {
                if (notFirst) {
                    builder.append(AND_STRING);
                } else {
                    builder.append(" where ");
                }
                notFirst = true;
                builder.append(classDesc.getColumnByProp(next.propName));
                // 无法确定集合的大小，这里只能象征性地放一个
                builder.append(" in (?)");
            } else if (ValueConditionEnum.FirstResult.equals(type)) {
                sscResult.firstResult = 0;
            } else if (ValueConditionEnum.MaxResult.equals(type)) {
                sscResult.maxResult = 0;
            } else if (ValueConditionEnum.OrderBy.equals(type)) {
                sscResult.orderBy = "";
            } else {
                if (notFirst) {
                    builder.append(AND_STRING);
                }
                notFirst = true;
                builder.append(classDesc.getColumnByProp(next.propName));
                builder.append(type.getSqlString());
            }
        }
        return builder;
    }
}
