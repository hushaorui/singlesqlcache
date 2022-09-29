package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.common.em.SscConditionEnum;
import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.exception.SscRuntimeException;
import com.hushaorui.ssc.param.SpecialValueEnum;
import com.hushaorui.ssc.param.ValueIn;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表的信息
 */
public class SscTableInfo {
    /** 建表语句 */
    private String[] createTableSql;
    /** 删表语句 */
    private String[] dropTableSql;
    /** 数据插入语句 */
    private String[] insertSql;
    /** 查询所有的语句 */
    private String selectAllSql;
    /** 根据id查询指定数据 */
    private String[] selectByIdSql;
    /** 根据唯一约束的字段查询数据 */
    private Map<String, String> selectByUniqueKeySql;
    /** 根据指定字段查询数据 */
    private Map<String, String> selectByConditionSql;
    /** 根据指定字段查询数据 */
    private Map<String, String> noCachedSelectByConditionSql;

    /** 所有不需要缓存的数据更新 */
    private String[] updateAllNotCachedByIdSql;
    /** 根据id删除数据 */
    private String[] deleteByIdSql;
    /** 查询表中的最大id */
    private String[] selectMaxIdSql;

    /** 数据类描述 */
    private DataClassDesc classDesc;
    /** id的数据类型 */
    private Class<?> idJavaType;

    /** 唯一键辅助表创建sql (分表时才会用到) */
    private Map<String, String[]> uniqueCreateSqlMap;
    /** 唯一键查询sql */
    private Map<String, String> uniqueSelectSqlMap;

    /** 表名 */
    private String[] tableNames;

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

    public Map<String, String[]> getUniqueCreateSqlMap() {
        return uniqueCreateSqlMap;
    }

    public Map<String, String> getUniqueSelectSqlMap() {
        return uniqueSelectSqlMap;
    }

    public String[] getTableNames() {
        return tableNames;
    }

    public SscTableInfo(DataClassDesc classDesc, SingleSqlCacheConfig config) {
        this.classDesc = classDesc;
        // id属性的名称
        String idPropName = classDesc.getIdPropName();
        // id字段的名字
        String idColumnName = classDesc.getColumnByProp(idPropName);
        Map<String, Method> propGetMethods = classDesc.getPropGetMethods();
        Method idGetMethod = propGetMethods.get(idPropName);
        // id属性的类型
        idJavaType = idGetMethod.getReturnType();
        if (classDesc.isUseIdGeneratePolicy()) {
            if (! config.getIdGeneratePolicyMap().containsKey(idJavaType)) {
                throw new SscRuntimeException(String.format("The class: %s is not supported as an id", idJavaType.getName()));
            }
        }
        if (! config.getTableSplitPolicyMap().containsKey(idJavaType)) {
            throw new SscRuntimeException(String.format("There is no table split policy with class: %s, prop: %s", idJavaType.getName(), idPropName));
        }
        // 唯一键的类型也必须能够找到对应的分表策略
        /*classDesc.getUniqueProps().values().forEach(propNames -> {
            for (String propName : propNames) {
                Class<?> propType = propGetMethods.get(propName).getReturnType();
                if (! config.getTableSplitPolicyMap().containsKey(propType)) {
                    throw new SscRuntimeException(String.format("There is no table split policy with class: %s, prop: %s", propType.getName(), propName));
                }
            }
        });*/
        int tableCount = classDesc.getTableCount();
        String[] tableNames = new String[tableCount];
        String[] createTableSql = new String[tableCount];
        String[] dropTableSql = new String[tableCount];
        String[] insertSql = new String[tableCount];
        StringBuilder selectAll = new StringBuilder();
        String[] selectByIdSql = new String[tableCount];
        Map<String, StringBuilder> selectByUniqueKeySqlMap = new HashMap<>();

        String[] updateAllNotCachedByIdSql = new String[tableCount];
        String[] deleteByIdSql = new String[tableCount];
        String[] selectMaxIdSql = new String[tableCount];
        Map<String, StringBuilder[]> uniqueCreateSqlTempMap;
        if (tableCount > 1) {
            uniqueCreateSqlTempMap = new HashMap<>();
        } else {
            uniqueCreateSqlTempMap = Collections.emptyMap();
        }
        Map<String, StringBuilder[]> uniqueSelectSqlTempMap = new HashMap<>();
        // 决定表名后面拼接的数字长度
        int numberLength;
        if (createTableSql.length < 100) {
            numberLength = 2;
        } else {
            numberLength = 0;
            int tempLength = createTableSql.length;
            while (tempLength > 1) {
                numberLength ++;
                tempLength /= 10;
            }
        }
        for (int i = 0; i < createTableSql.length; i++) {
            StringBuilder create = new StringBuilder("create table if not exists ");
            String indexString = String.valueOf(i);
            StringBuilder realTableNameBuilder = new StringBuilder(classDesc.getTableName()).append("_");
            for (int j = indexString.length(); j < numberLength; j++) {
                realTableNameBuilder.append("0");
            }
            realTableNameBuilder.append(indexString);
            // 真正的表名
            String realTableName = realTableNameBuilder.toString();
            tableNames[i] = realTableName;
            create.append(realTableName);
            create.append("(\n");
            create.append("    ").append(idColumnName).append(" ").append(classDesc.getPropColumnTypeMapping().get(idPropName));
            create.append(" primary key");
            // 其他字段
            classDesc.getPropColumnMapping().forEach((propName, columnName) -> {
                if (idPropName.equals(propName)) {
                    return;
                }
                create.append(",\n    ");
                create.append(columnName).append(" ").append(classDesc.getPropColumnTypeMapping().get(propName));
                if (classDesc.getNotNullProps().contains(propName)) {
                    // 该字段非空
                    create.append(" not null");
                } else {
                    // 默认值
                    String defaultValue = classDesc.getPropDefaultValues().get(propName);
                    if (defaultValue != null) {
                        create.append("default ").append(defaultValue);
                    }
                }
            });
            // 唯一键
            for (Map.Entry<String, Set<String>> next : classDesc.getUniqueProps().entrySet()) {
                String uniqueName = next.getKey();
                Set<String> names = next.getValue();
                create.append(",\n    ");
                create.append("unique key(");

                StringBuilder[] uniqueCreateSql;
                if (tableCount > 1) {
                    uniqueCreateSql = uniqueCreateSqlTempMap.computeIfAbsent(uniqueName, k -> new StringBuilder[tableCount]);
                    uniqueCreateSql[i] = new StringBuilder();
                    uniqueCreateSql[i].append("create table if not exists ").append(realTableName).append("_")
                            .append(uniqueName).append(config.getUniqueTableNameSuffix()).append("(\n    ");
                    // 可能多个字段联合唯一，新建的辅助表只能使用自增主键，id名称使用 uniqueName，类型使用长整型
                    String uniqueIdName = config.getUniqueTableIdName();
                    uniqueCreateSql[i].append(uniqueIdName).append(" bigint").append(" primary key,\n    ").append(idColumnName).append(" ")
                            .append(classDesc.getPropColumnTypeMapping().get(idPropName)).append(" unique not null,\n    ");
                } else {
                    uniqueCreateSql = null;
                }
                StringBuilder[] uniqueSelectSql = uniqueSelectSqlTempMap.computeIfAbsent(uniqueName, k -> new StringBuilder[tableCount]);
                uniqueSelectSql[i] = new StringBuilder();
                uniqueSelectSql[i].append("select * from ").append(realTableName).append(" where ");

                // 拼接按唯一键查询的sql
                StringBuilder selectByUniqueKey = selectByUniqueKeySqlMap.computeIfAbsent(uniqueName, key -> new StringBuilder());
                selectByUniqueKey.append("select ").append(realTableName).append(".* from ").append(realTableName);
                selectByUniqueKey.append(" where ");

                Iterator<String> iterator = names.iterator();
                StringBuilder namesBuilder = new StringBuilder();
                while (iterator.hasNext()) {
                    String uniquePropName = iterator.next();
                    String uniqueColumnName = classDesc.getColumnByProp(uniquePropName);
                    create.append(uniqueColumnName);
                    selectByUniqueKey.append(uniqueColumnName).append(" = ?");
                    namesBuilder.append(uniqueColumnName);
                    uniqueSelectSql[i].append(uniqueColumnName).append(" = ?");

                    if (tableCount > 1) {
                        uniqueCreateSql[i].append(uniqueColumnName).append(" ").append(classDesc.getPropColumnTypeMapping().get(uniquePropName));
                        if (classDesc.getNotNullProps().contains(uniquePropName)) {
                            uniqueCreateSql[i].append(" not null");
                        }
                    }

                    if (iterator.hasNext()) {
                        create.append(", ");
                        namesBuilder.append(", ");
                        selectByUniqueKey.append(" and ");
                        uniqueSelectSql[i].append(" and ");
                    }
                    if (tableCount > 1) {
                        uniqueCreateSql[i].append(",\n    ");
                    }
                }
                create.append(")");

                if (tableCount > 1) {
                    uniqueCreateSql[i].append("unique key(").append(namesBuilder).append(")\n)");
                }

                if (i < createTableSql.length - 1) {
                    // 不是最后一个表
                    selectByUniqueKey.append("\n union all \n");
                }
            }
            create.append("\n)");
            createTableSql[i] = create.toString();
            dropTableSql[i] = "drop table if exists " + realTableName;

            {
                StringBuilder insert = new StringBuilder("insert into ");
                insert.append(realTableName).append("(");
                boolean isNotFirst = false;
                Collection<String> columnNames = classDesc.getPropColumnMapping().values();
                for (String columnName : columnNames) {
                    if (isNotFirst) {
                        insert.append(", ");
                    } else {
                        isNotFirst = true;
                    }
                    insert.append(columnName);
                }
                insert.append(") values(");
                for (int k = 0; k < columnNames.size(); k++) {
                    if (k != 0) {
                        insert.append(", ");
                    }
                    insert.append("?");
                }
                insert.append(")");
                insertSql[i] = insert.toString();
            }

            selectAll.append("select ").append(realTableName).append(".* from ").append(realTableName);
            // 根据id排序
            selectAll.append(" order by ").append(idColumnName);
            if (i < createTableSql.length - 1) {
                // 不是最后一个表
                selectAll.append("\n union all \n");
            }
            selectByIdSql[i] = String.format("select * from %s where %s = ?", realTableName, idColumnName);

            deleteByIdSql[i] = String.format("delete from %s where %s = ?", realTableName, idColumnName);

            StringBuilder updateAllNotCachedById = new StringBuilder();
            updateAllNotCachedById.append("update ").append(realTableName).append("\nset\n");
            boolean notFirst = false;
            for (Map.Entry<String, String> entry : classDesc.getPropColumnMapping().entrySet()) {
                String columnName = entry.getValue();
                if (classDesc.getNotUpdateProps().contains(entry.getKey())) {
                    // id 和不需要更新的字段(id已经在该集合中)，跳过
                    continue;
                }
                if (notFirst) {
                    updateAllNotCachedById.append(",\n");
                } else {
                    notFirst = true;
                }
                updateAllNotCachedById.append("    ").append(columnName).append(" = ?");
            }
            updateAllNotCachedById.append("\nwhere ").append(idColumnName).append(" = ?");
            updateAllNotCachedByIdSql[i] = updateAllNotCachedById.toString();

            if (String.class.equals(idJavaType)) {
                selectMaxIdSql[i] = String.format("select max(cast(%s as signed)) from %s", idColumnName, realTableName);
            } else {
                selectMaxIdSql[i] = String.format("select max(%s) from %s", idColumnName, realTableName);
            }
        }

        this.tableNames = tableNames;
        this.createTableSql = createTableSql;
        this.dropTableSql = dropTableSql;
        this.insertSql = insertSql;
        this.selectAllSql = selectAll.toString();
        this.selectByIdSql = selectByIdSql;
        Map<String, String> selectByUniqueKeySql = new HashMap<>();
        selectByUniqueKeySqlMap.forEach((key, value) -> selectByUniqueKeySql.put(key, value.toString()));
        this.selectByUniqueKeySql = selectByUniqueKeySql;

        this.selectByConditionSql = getConditionSql();
        this.updateAllNotCachedByIdSql = updateAllNotCachedByIdSql;
        this.deleteByIdSql = deleteByIdSql;
        this.selectMaxIdSql = selectMaxIdSql;
        // 辅助表，弃用
        /*Map<String, String[]> uniqueCreateSqlMap = new HashMap<>();
        uniqueCreateSqlTempMap.forEach((key, value) -> {
            String[] array = new String[value.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = value[i].toString();
                System.out.println(array[i]);
            }
            uniqueCreateSqlMap.put(key, array);
        });
        this.uniqueCreateSqlMap = uniqueCreateSqlMap;*/
        Map<String, String> uniqueSelectSqlMap;
        if (uniqueSelectSqlTempMap.isEmpty()) {
            uniqueSelectSqlMap = Collections.emptyMap();
        } else {
            uniqueSelectSqlMap = new HashMap<>();
            uniqueSelectSqlTempMap.forEach((key, value) -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < value.length; i++) {
                    builder.append(value[i]);
                    if (i != value.length - 1) {
                        builder.append("\n union all \n");
                    }
                }
                uniqueSelectSqlMap.put(key, builder.toString());
            });
        }
        //uniqueSelectSqlMap.values().forEach(System.out::println);
        this.uniqueSelectSqlMap = uniqueSelectSqlMap;
    }

    /**
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditionMap 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    public String getNoCachedConditionSql(String key, HashMap<String, Object> conditionMap) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        return noCachedSelectByConditionSql.getOrDefault(key, putConditionSqlToMap(key, conditionMap, noCachedSelectByConditionSql));
    }
    /*
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param propOrColumnNames 属性名或字段名集合(可混合)
     * @return 查询sql
     */
    /*public String getNoCachedConditionSql(String key, Set<String> propOrColumnNames) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        return noCachedSelectByConditionSql.getOrDefault(key, putConditionSqlToMap(key, propOrColumnNames, noCachedSelectByConditionSql));
    }*/

    /*
     * 根据参与条件查询的属性名或字段名集合，找到对应的查询sql
     * @param conditions 条件对象
     * @return 查询sql
     */
    /*public String getNoCachedConditionSql(String key, Map<Class<?>, SscTableInfo> tableInfoMapping, SscCondition... conditions) {
        if (noCachedSelectByConditionSql == null) {
            synchronized (this) {
                if (noCachedSelectByConditionSql == null) {
                    noCachedSelectByConditionSql = new ConcurrentHashMap<>();
                }
            }
        }
        return noCachedSelectByConditionSql.getOrDefault(key, putConditionSqlToMap(key, noCachedSelectByConditionSql, tableInfoMapping, conditions));
    }*/

    public String getNoCachedConditionKey(HashMap<String, Object> conditionMap) {
        StringBuilder key = new StringBuilder();
        Iterator<Map.Entry<String, Object>> iterator = conditionMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            // 统一使用 columnName
            String columnName = classDesc.getColumnByProp(next.getKey());
            key.append(SpecialValueEnum.getKeyString(next.getValue()));
            // 字段名不可能存在空格，这里使用空格分隔
            key.append(columnName);
            if (iterator.hasNext()) {
                key.append(" ");
            }
        }
        return key.toString().intern();
    }

    private String getNoCachedConditionKey(Map<String, SpecialValueEnum> propAndTypes) {
        StringBuilder key = new StringBuilder();
        Iterator<Map.Entry<String, SpecialValueEnum>> iterator = propAndTypes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, SpecialValueEnum> next = iterator.next();
            // 统一使用 columnName
            String columnName = classDesc.getColumnByProp(next.getKey());
            key.append(next.getValue().getKeyString());
            // 字段名不可能存在空格，这里使用空格分隔
            key.append(columnName);
            if (iterator.hasNext()) {
                key.append(" ");
            }
        }
        return key.toString().intern();
    }

    private Map<String, String> getConditionSql() {
        Map<String, String> map = new HashMap<>();
        // select xx.* from xx where xx = ? and xx = ? union all select xx.* from xx where xx = ? and xx = ?
        Map<String, Map<String, SpecialValueEnum>> conditionProps = classDesc.getConditionProps();
        conditionProps.forEach((selectorName, propAndTypes) -> putConditionSqlToMap(propAndTypes, map));
        return map;
    }

    /*private String putConditionSqlToMap(String key, Map<String, String> map, Map<Class<?>, SscTableInfo> tableInfoMapping, SscCondition... conditions) {
        StringBuilder builder = new StringBuilder();
        for (SscCondition condition : conditions) {
            Class<?> joinClass = condition.getJoinClass();
            SscTableInfo sscTableInfo;
            if (joinClass == null) {
                sscTableInfo = this;
            } else {
                sscTableInfo = tableInfoMapping.get(joinClass);
                if (sscTableInfo == null) {
                    throw new SscRuntimeException("该类未注册：" + joinClass.getName());
                }
            }
            splicingConditionSql(builder, tableInfoMapping, condition, sscTableInfo);
        }
        String value = builder.toString();
        map.put(key, value);
        System.out.println(String.format("key: %s, value: %s", key, value));
        return value;
    }*/

    /*private void splicingConditionSql(StringBuilder builder, Map<Class<?>, SscTableInfo> tableInfoMapping, SscCondition condition, SscTableInfo sscTableInfo) {
        SscConditionEnum type = condition.getType();
        if (SscConditionEnum.FIRST.equals(type)) {
            builder.append(" limit ").append(condition.getNumber());
        } else if (SscConditionEnum.MAX.equals(type)) {
            builder.append(", ").append(condition.getNumber());
        } else if (SscConditionEnum.ORDER_BY.equals(type)) {
            builder.append(" order by ").append(condition.getOrderBy());
        } else {
            DataClassDesc desc = sscTableInfo.getClassDesc();
            if (SscConditionEnum.LEFT_OUTER_JOIN.equals(type)) {
                builder.append(" left outer join ").append(desc.getTableName());
            } else if (SscConditionEnum.RIGHT_OUTER_JOIN.equals(type)) {
                builder.append(" right outer join ").append(desc.getTableName());
            } else if (SscConditionEnum.INNER_JOIN.equals(type)) {
                builder.append(" inner join ").append(desc.getTableName());
            } else if (SscConditionEnum.ON.equals(type)) {
                builder.append(" on");
                // TODO_LIST 连接条件
            } else {
                HashMap<String, Object> fieldMap = condition.getFieldMap();
                if (fieldMap != null) {
                    int tableCount = desc.getTableCount();
                    for (int i = 0; i < tableCount; i ++) {
                        if (i > 0) {
                            builder.append("\nunion all \n");
                        }
                        String tableName = sscTableInfo.getTableNames()[i];
                        builder.append("select ").append(tableName).append(".* from ").append(tableName);
                        Iterator<String> iterator = fieldMap.keySet().iterator();
                        if (iterator.hasNext()) {
                            builder.append(" where ");
                        }
                        while (iterator.hasNext()) {
                            String propOrColumnName = iterator.next();
                            String columnName = classDesc.getColumnByProp(propOrColumnName);
                            builder.append(columnName).append(" = ?");
                            if (iterator.hasNext()) {
                                builder.append(" and ");
                            }
                        }
                    }
                }
            }
        }
    }*/

    private String putConditionSqlToMap(String key, HashMap<String, Object> conditionMap, Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            Iterator<Map.Entry<String, Object>> iterator = conditionMap.entrySet().iterator();
            if (iterator.hasNext()) {
                builder.append(" where ");
            }
            while (iterator.hasNext()) {
                Map.Entry<String, Object> next = iterator.next();
                String columnName = classDesc.getColumnByProp(next.getKey());
                Object value = next.getValue();
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
                    builder.append(SpecialValueEnum.getSqlString(value));
                }
                if (iterator.hasNext()) {
                    builder.append(" and ");
                }
            }
        }
        String value = builder.toString();
        if (key == null) {
            map.put(getNoCachedConditionKey(conditionMap), value);
        } else {
            map.put(key, value);
        }
        return value;
    }
    private void putConditionSqlToMap(Map<String, SpecialValueEnum> propAndTypes, Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        int tableCount = classDesc.getTableCount();
        for (int i = 0; i < tableCount; i ++) {
            if (i > 0) {
                builder.append("\nunion all \n");
            }
            builder.append("select ").append(tableNames[i]).append(".* from ").append(tableNames[i]);
            Iterator<Map.Entry<String, SpecialValueEnum>> iterator = propAndTypes.entrySet().iterator();
            if (iterator.hasNext()) {
                builder.append(" where ");
            }
            while (iterator.hasNext()) {
                Map.Entry<String, SpecialValueEnum> next = iterator.next();
                String columnName = classDesc.getColumnByProp(next.getKey());
                builder.append(columnName);
                SpecialValueEnum type = next.getValue();
                if (SpecialValueEnum.ValueIn.equals(type)) {
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
