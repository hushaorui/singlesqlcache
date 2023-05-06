package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.common.StringOrArray;
import com.hushaorui.ssc.common.TwinsValue;
import com.hushaorui.ssc.common.em.SscDataSourceType;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.param.*;
import com.hushaorui.ssc.util.SscStringUtils;

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
    /** 根据id查询指定数据的一部分字段 */
    protected Map<String, StringOrArray> findByIdSql;
    protected Map<String, String> findByUniqueSql;
    /** 根据分表字段查询(每种数据只能有一个分表字段) */
    protected String[] selectByTableSplitFieldSql;
    /** 根据分组字段查询 */
    protected Map<String, String[]> selectByGroupFieldSql;
    protected Map<String, String> unionSelectByGroupFieldSql;
    /** 根据分表字段查询(每种数据只能有一个分表字段) */
    protected String[] selectIdByTableSplitFieldSql;

    /** 根据唯一约束的字段查询数据 */
    protected Map<String, String> selectByUniqueKeySql;
    /** 根据指定字段查询数据 */
    protected Map<String, StringOrArray> noCachedSelectByConditionSql;
    /** 根据指定字段查询数据的数量 */
    protected Map<String, StringOrArray> noCachedCountByConditionSql;
    /** 根据指定字段查询数据id */
    protected Map<String, StringOrArray> noCachedSelectIdByConditionSql;
    /** 根据指定字段删除数据 */
    protected Map<String, StringOrArray> noCachedDeleteByConditionSql;

    /** 所有不需要缓存的数据更新 */
    protected String[] updateAllNotCachedByIdSql;
    /** 根据id删除数据 */
    protected String[] deleteByIdSql;
    /** 查询表中的最大id */
    protected String[] selectMaxIdSql;

    /** 数据类描述 */
    protected DataClassDesc classDesc;
    /** 全局配置 */
    protected SscGlobalConfig config;
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

    public Map<String, String[]> getSelectByGroupFieldSql() {
        return selectByGroupFieldSql;
    }

    public Map<String, String> getUnionSelectByGroupFieldSql() {
        return unionSelectByGroupFieldSql;
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
        this.config = config;
    }

    /**
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditions 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    public SscSqlResult getNoCachedSelectConditionSql(String key, List<TwinsValue<String, Object>> conditions) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        StringOrArray sql = noCachedSelectByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedSelectByConditionSql, conditions, 1);
        }
        return getParamsAndSql(sql, conditions);
    }

    public String getFindByIdSql(String selectStr, int tableIndex) {
        if (findByIdSql == null) {
            synchronized (this) {
                if (findByIdSql == null) {
                    findByIdSql = new ConcurrentHashMap<>();
                }
            }
        }
        StringOrArray stringOrArray = findByIdSql.get(selectStr);
        if (stringOrArray == null) {
            return putNoCachedFindSqlToMap(selectStr, tableIndex, null);
        } else {
            String sql = stringOrArray.getByIndex(tableIndex);
            if (sql == null) {
                return putNoCachedFindSqlToMap(selectStr, tableIndex, stringOrArray);
            } else {
                return sql;
            }
        }
    }

    public String getUniqueFindSql(String selectStr, String uniqueName) {
        return getUniqueFindSql(selectStr, uniqueName, tableNames.length == 1 ? 0 : null);
    }

    public String getUniqueFindSql(String selectStr, String uniqueName, Integer tableIndex) {
        String columnName = classDesc.getColumnByProp(uniqueName);
        String key;
        if (tableIndex == null) {
            key = String.format("%s by %s", selectStr, columnName);
        } else {
            key = String.format("%s by %s with %s", selectStr, columnName, tableIndex);
        }
        if (findByUniqueSql == null) {
            synchronized (this) {
                if (findByUniqueSql == null) {
                    findByUniqueSql = new ConcurrentHashMap<>();
                }
            }
        }
        String sql = findByUniqueSql.get(key);
        if (sql != null) {
            return sql;
        } else {
            return putNoCachedFindUniqueSqlToMap(selectStr, columnName, key, tableIndex);
        }
    }

    public SscSqlResult getNoCachedCountConditionSql(String key, List<TwinsValue<String, Object>> conditions) {
        if (noCachedCountByConditionSql == null) {
            synchronized (this) {
                if (noCachedCountByConditionSql == null) {
                    noCachedCountByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        StringOrArray sql = noCachedCountByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedCountByConditionSql, conditions, 2);
        }
        return getParamsAndSql(sql, conditions);
    }

    public SscSqlResult getNoCachedSelectIdConditionSql(String key, List<TwinsValue<String, Object>> conditions) {
        if (noCachedSelectIdByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectIdByConditionSql == null) {
                    noCachedSelectIdByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        StringOrArray sql = noCachedSelectIdByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedSelectIdByConditionSql, conditions, 3);
        }
        return getParamsAndSql(sql, conditions);
    }

    public SscSqlResult getNoCachedDeleteConditionSql(String key, List<TwinsValue<String, Object>> conditions) {
        if (noCachedDeleteByConditionSql == null) {
            synchronized (this) {
                if (noCachedDeleteByConditionSql == null) {
                    noCachedDeleteByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        StringOrArray sql = noCachedDeleteByConditionSql.get(key);
        if (sql == null) {
            // 创建新的sql和查询条件数组，并返回
            return putNoCachedConditionSqlToMap(key, noCachedDeleteByConditionSql, conditions, 99);
        }
        return getParamsAndSql(sql, conditions);
    }

    protected SscSqlResult getParamsAndSql(StringOrArray sql, List<TwinsValue<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new SscSqlResult(sql, Collections.emptyList());
        }
        Integer firstResult = null;
        Integer maxResult = null;
        // 根据 sscResult 和 conditions，获得params
        List<Object> params = new ArrayList<>();
        int tableCount;
        if (sql.getValue() == null) {
            // 多个sql的情况，参数不需要叠在一起
            tableCount = 1;
        } else {
            tableCount = tableNames.length;
        }
        boolean isMysql = SscDataSourceType.Mysql.name().equals(config.getDataSourceType());
        JSONSerializer jsonSerializer = config.getJsonSerializer();
        for (int i = 0; i < tableCount; i ++) {
            for (TwinsValue<String, Object> pair : conditions) {
                Object value = SscStringUtils.getFieldValueAccordWithSql(pair.getValue(), jsonSerializer);
                if (value instanceof ValueIn) {
                    boolean isNot = value instanceof ValueIsNotIn;
                    ValueIn<Object> valueIn = (ValueIn<Object>) value;
                    Collection<Object> values = valueIn.getValues();
                    if (i == 0) {
                        String columnName = classDesc.getColumnByProp(pair.getKey());
                        String oldString;
                        if (isNot) {
                            oldString = columnName + " not in (?)";
                        } else {
                            oldString = columnName + " in (?)";
                        }
                        if (values == null || values.isEmpty()) {
                            // 该条件不存在
                            sql = replaceStringOrArray(sql, oldString, "");
                        } else if (values.size() > 1) {
                            StringBuilder newString = new StringBuilder(columnName);
                            if (isNot) {
                                newString.append(" not in (");
                            } else {
                                newString.append(" in (");
                            }
                            int loopCount = values.size() - 1;
                            for (int count = 0; count < loopCount; count ++) {
                                newString.append("? ,");
                            }
                            newString.append("?)");
                            // 超过一个
                            sql = replaceStringOrArray(sql, oldString, newString.toString());
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
            if (! isMysql && tableCount > 1 && firstResult != null && maxResult != null) {
                params.add(firstResult);
                params.add(maxResult);
            }
        }
        if ((isMysql || tableCount == 1) && firstResult != null && maxResult != null) {
            params.add(firstResult);
            params.add(maxResult);
        }
        return new SscSqlResult(sql, params);
    }

    private StringOrArray replaceStringOrArray(StringOrArray stringOrArray, String oldString, String newString) {
        if (stringOrArray.getValue() == null) {
            // 有数组
            String[] array = stringOrArray.getArray();
            String[] newArray = new String[array.length];
            for (int a = 0; a < array.length; a++) {
                newArray[a] = array[a].replace(oldString, newString);
            }
            return new StringOrArray(newArray);
        } else {
            // 单个字符串
            return new StringOrArray(stringOrArray.getValue().replace(oldString, newString));
        }
    }

    public String getKeyByConditionList(List<TwinsValue<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        JSONSerializer jsonSerializer = config.getJsonSerializer();
        for (int i = 0; i < conditions.size(); i++) {
            // 统一使用 columnName
            TwinsValue<String, Object> pair = conditions.get(i);
            String columnName = classDesc.getColumnByProp(pair.getKey());
            Object value = SscStringUtils.getFieldValueAccordWithSql(pair.getValue(), jsonSerializer);
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

    protected String putNoCachedFindUniqueSqlToMap(String selectStr, String uniqueName, String key, Integer tableIndex) {
        String sql;
        if (tableIndex == null) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < tableNames.length; i++) {
                if (i > 0) {
                    builder.append("\n union all \n");
                }
                builder.append("select ").append(selectStr).append(" from ").append(tableNames[i]);
                builder.append(" where ").append(uniqueName).append(" = ?");
            }
            sql = builder.toString();
        } else {
            sql = "select " + selectStr + " from " + tableNames[tableIndex] + " where " + uniqueName + " = ?";
        }
        findByUniqueSql.put(key, sql);
        return sql;
    }

    protected String putNoCachedFindSqlToMap(String selectStr, int tableIndex, StringOrArray stringOrArray) {
        String sql = "select " + selectStr + " from " + tableNames[tableIndex] + " where " +
                classDesc.getColumnByProp(classDesc.getIdPropName()) + " = ?";
        if (stringOrArray == null) {
            if (classDesc.getTableCount() == 1) {
                stringOrArray = new StringOrArray(sql);
            } else {
                String[] array = new String[classDesc.getTableCount()];
                array[tableIndex] = sql;
                stringOrArray = new StringOrArray(array);
            }
            findByIdSql.put(selectStr, stringOrArray);
        } else {
            stringOrArray.getArray()[tableIndex] = sql;
        }
        return sql;
    }
    //  1:Select   2: countByCondition  3: SelectId   99:Delete
    protected SscSqlResult putNoCachedConditionSqlToMap(String key, Map<String, StringOrArray> map, List<TwinsValue<String, Object>> conditions, int sqlType) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        List<Object> paramList = new ArrayList<>();
        SscResult sscResult = new SscResult();
        sscResult.paramList = paramList;
        List<Object> params = new ArrayList<>();
        StringBuilder iteratorBuilder = iteratorConditions(conditions, sscResult);
        String[] array;
        if (sqlType == 99) {
            array = new String[tableCount];
        } else {
            array = null;
        }
        boolean isMysql = SscDataSourceType.Mysql.name().equals(config.getDataSourceType());
        // 每个表都拼接 limit
        boolean appendInEveryTable = ! isMysql && tableCount > 1 && sscResult.firstResult != null && sscResult.maxResult != null;
        // 最后拼接 limit
        boolean appendInEnd = ! appendInEveryTable && sscResult.firstResult != null && sscResult.maxResult != null;
        boolean addParam = true;
        for (int i = 0; i < tableCount; i ++) {
            if (sqlType == 99) {
                builder.setLength(0);
            } else {
                if (i > 0) {
                    builder.append("\nunion all \n");
                }
            }
            switch (sqlType) {
                case 2:
                    builder.append("select ");
                    // count(id)
                    builder.append("count(").append(classDesc.getColumnByProp(classDesc.getIdPropName())).append(")");
                    break;
                case 3:
                    builder.append("select ");
                    // xxx.id
                    builder.append(tableNames[i]).append(".").append(classDesc.getColumnByProp(classDesc.getIdPropName()));
                    break;
                case 99:
                    builder.append("delete");
                    break;
                default:
                    builder.append("select ");
                    // xxx.*
                    builder.append(tableNames[i]).append(".*");
            }
            builder.append(" from ").append(tableNames[i]);
            if (iteratorBuilder.length() > 0) {
                builder.append(" where ");
            }
            builder.append(iteratorBuilder);
            if (addParam) {
                params.addAll(paramList);
            }
            if (appendInEveryTable) {
                appendLimitString(builder);
                if (addParam) {
                    params.add(sscResult.firstResult);
                    params.add(sscResult.maxResult);
                }
            }
            if (sqlType == 99) {
                array[i] = builder.toString();
                addParam = false;
            }
        }
        if (appendInEnd) {
            appendLimitString(builder);
            params.add(sscResult.firstResult);
            params.add(sscResult.maxResult);
        }
        StringOrArray stringOrArray;
        if (sqlType == 99) {
            stringOrArray = new StringOrArray(array);
        } else {
            stringOrArray = new StringOrArray(builder.toString());
        }
        SscSqlResult sscSqlResult = new SscSqlResult(stringOrArray, params);
        if (key == null) {
            map.put(getKeyByConditionList(conditions), stringOrArray);
        } else {
            map.put(key, stringOrArray);
        }
        return sscSqlResult;
    }

    private StringBuilder iteratorConditions(List<TwinsValue<String, Object>> conditions, SscResult sscResult) {
        StringBuilder builder = new StringBuilder();
        if (conditions == null) {
            return builder;
        }
        boolean notFirst = false;
        JSONSerializer jsonSerializer = config.getJsonSerializer();
        for (TwinsValue<String, Object> pair : conditions) {
            Object value = SscStringUtils.getFieldValueAccordWithSql(pair.getValue(), jsonSerializer);
            if (value instanceof ValueIn) {
                boolean isNot = value instanceof ValueIsNotIn;
                if (notFirst) {
                    builder.append(AND_STRING);
                } else {
                    builder.append(" where ");
                }
                notFirst = true;
                builder.append(classDesc.getColumnByProp(pair.getKey()));
                if (isNot) {
                    builder.append(" not in (");
                } else {
                    builder.append(" in (");
                }
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
                if (value instanceof SpecialValue) {
                    sscResult.paramList.add(((SpecialValue) value).getValue());
                } else {
                    sscResult.paramList.add(value);
                }
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
