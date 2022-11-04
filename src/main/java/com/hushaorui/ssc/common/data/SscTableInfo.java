package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SscGlobalConfig;
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
    ///** 查询所有的语句 */
    //protected String selectAllSql;
    ///** 查询所有id的语句 */
    //protected String selectAllIdSql;
    /** 根据id查询指定数据 */
    protected String[] selectByIdSql;
    /** 根据分表字段查询(每种数据只能有一个分表字段) */
    protected String[] selectByTableSplitFieldSql;
    /** 根据分表字段查询(每种数据只能有一个分表字段) */
    protected String[] selectIdByTableSplitFieldSql;

    /** 根据唯一约束的字段查询数据 */
    protected Map<String, String> selectByUniqueKeySql;
    /** 根据指定字段查询数据 */
    protected Map<String, String> noCachedSelectByConditionSql;
    /** 根据指定字段查询数据的数量 */
    protected Map<String, String> noCachedCountByConditionSql;
    protected Map<String, String> noCachedSelectIdByConditionSql;


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
    protected Map<String, String[]> uniqueAndFieldSelectSqlMap;
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

    public String[] getSelectByIdSql() {
        return selectByIdSql;
    }

    public String[] getSelectByTableSplitFieldSql() {
        return selectByTableSplitFieldSql;
    }
    public String[] getSelectIdByTableSplitFieldSql() {
        return selectIdByTableSplitFieldSql;
    }

    /*public String getSelectAllIdSql() {
        return selectAllIdSql;
    }*/
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

    public Map<String, String[]> getUniqueAndFieldSelectSqlMap() {
        return uniqueAndFieldSelectSqlMap;
    }

    public String[] getTableNames() {
        return tableNames;
    }

    public SscTableInfo(DataClassDesc classDesc, SscGlobalConfig config) {
        this.classDesc = classDesc;
    }

    /**
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditions 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    public SscSqlResult getNoCachedSelectConditionSql(String key, List<Pair<String, Object>> conditions) {
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
            return putNoCachedConditionSqlToMap(key, noCachedSelectByConditionSql, conditions, 1);
        }
        return getParamsAndSql(sql, conditions);
    }


    public SscSqlResult getNoCachedCountConditionSql(String key, List<Pair<String, Object>> conditions) {
        if (noCachedCountByConditionSql == null) {
            synchronized (this) {
                if (noCachedCountByConditionSql == null) {
                    noCachedCountByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        String sql = noCachedCountByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedCountByConditionSql, conditions, 2);
        }
        return getParamsAndSql(sql, conditions);
    }

    public SscSqlResult getNoCachedSelectIdConditionSql(String key, List<Pair<String, Object>> conditions) {
        if (noCachedSelectIdByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectIdByConditionSql == null) {
                    noCachedSelectIdByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        String sql = noCachedSelectIdByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedSelectIdByConditionSql, conditions, 3);
        }
        return getParamsAndSql(sql, conditions);
    }

    protected SscSqlResult getParamsAndSql(String sql, List<Pair<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new SscSqlResult(sql, Collections.emptyList());
        }
        Integer firstResult = null;
        Integer maxResult = null;
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
        if (tableNames.length == 1 && firstResult != null && maxResult != null) {
            params.add(firstResult);
            params.add(maxResult);
        }
        return new SscSqlResult(sql, params);
    }

    public String getKeyByConditionList(List<Pair<String, Object>> conditions) {
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

    /*protected String getConditionKey(List<SscValue> propAndTypes) {
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
    }*/

    protected abstract void appendLimitString(StringBuilder builder);

    protected SscSqlResult putNoCachedConditionSqlToMap(String key, Map<String, String> map, List<Pair<String, Object>> conditions, int sqlType) {
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
            builder.append("select ");
            switch (sqlType) {
                case 2:
                    // count(id)
                    builder.append("count(").append(classDesc.getColumnByProp(classDesc.getIdPropName())).append(")");
                    break;
                case 3:
                    // xxx.id
                    builder.append(tableNames[i]).append(".").append(classDesc.getColumnByProp(classDesc.getIdPropName()));
                    break;
                default:
                    // xxx.*
                    builder.append(tableNames[i]).append(".*");
            }
            builder.append(" from ").append(tableNames[i]);
            builder.append(iteratorBuilder);
            params.addAll(paramList);
            if (tableCount > 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
                appendLimitString(builder);
                params.add(sscResult.firstResult);
                params.add(sscResult.maxResult);
            }
        }
        if (tableCount == 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
            appendLimitString(builder);
            params.add(sscResult.firstResult);
            params.add(sscResult.maxResult);
        }
        String sql = builder.toString();
        SscSqlResult sscSqlResult = new SscSqlResult(sql, params);
        if (key == null) {
            map.put(getKeyByConditionList(conditions), builder.toString());
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

    /*protected void putConditionSqlToMap(List<SscValue> propAndTypes, Map<String, String> map, int sqlType) {
        StringBuilder builder = new StringBuilder();
        SscResult sscResult = new SscResult();
        int tableCount = classDesc.getTableCount();
        StringBuilder iteratorBuilder = iteratorPropAndTypes(propAndTypes, sscResult);
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ");
            switch (sqlType) {
                case 2:
                    // count(id)
                    builder.append("count(").append(classDesc.getColumnByProp(classDesc.getIdPropName())).append(")");
                    break;
                case 3:
                    // xxx.id
                    builder.append(tableNames[i]).append(".").append(classDesc.getColumnByProp(classDesc.getIdPropName()));
                    break;
                default:
                    // xxx.*
                    builder.append(tableNames[i]).append(".*");
            }
            builder.append(" from ").append(tableNames[i]);
            builder.append(iteratorBuilder);
            if (tableCount > 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
                appendLimitString(builder);
            }
        }
        if (tableCount == 1 && sscResult.firstResult != null && sscResult.maxResult != null) {
            appendLimitString(builder);
        }
        map.put(getConditionKey(propAndTypes), builder.toString());
    }*/

    /*private StringBuilder iteratorPropAndTypes(List<SscValue> propAndTypes, SscResult sscResult) {
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
    }*/
}
