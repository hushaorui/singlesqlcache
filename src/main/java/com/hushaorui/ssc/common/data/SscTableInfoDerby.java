package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.exception.SscRuntimeException;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 表的信息
 */
public class SscTableInfoDerby extends SscTableInfo {

    public SscTableInfoDerby(DataClassDesc classDesc, SscGlobalConfig config) {
        super(classDesc, config);
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
        String tableSplitField = classDesc.getTableSplitField();
        {
            Class<?> tableSplitFieldType;
            if (tableSplitField == null) {
                tableSplitFieldType = idJavaType;
            } else {
                tableSplitFieldType = propGetMethods.get(tableSplitField).getReturnType();
            }
            if (! config.getTableSplitPolicyMap().containsKey(tableSplitFieldType)) {
                throw new SscRuntimeException(String.format("There is no table split policy with class: %s, prop: %s", idJavaType.getName(), idPropName));
            }
        }
        int tableCount = classDesc.getTableCount();
        String[] tableNames = new String[tableCount];
        String[] createTableSql = new String[tableCount];
        String[] dropTableSql = new String[tableCount];
        String[] insertSql = new String[tableCount];
        //StringBuilder selectAll = new StringBuilder();
        String[] selectByIdSql = new String[tableCount];
        String[] selectByTableSplitFieldSql = new String[tableCount];
        Map<String, String[]> selectByGroupFieldSql = new HashMap<>();
        Map<String, StringBuilder> unionSelectByGroupFieldSql = new HashMap<>();
        String[] selectIdByTableSplitFieldSql = new String[tableCount];
        Map<String, StringBuilder> selectByUniqueKeySqlMap = new HashMap<>();

        String[] updateAllNotCachedByIdSql = new String[tableCount];
        String[] deleteByIdSql = new String[tableCount];
        String[] selectMaxIdSql = new String[tableCount];
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
            StringBuilder create = new StringBuilder("create table ");
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
                Set<String> uniqueKeySet = classDesc.getUniqueProps().get(propName);
                if (uniqueKeySet != null && uniqueKeySet.size() == 1) {
                    // 该字段唯一
                    create.append(" unique");
                }
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
                //create.append(",\n    ");
                //create.append("unique key(");

                StringBuilder[] uniqueSelectSql = uniqueSelectSqlTempMap.computeIfAbsent(uniqueName, k -> new StringBuilder[tableCount]);
                uniqueSelectSql[i] = new StringBuilder();
                uniqueSelectSql[i].append("select * from ").append(realTableName).append(" where ");

                // 拼接按唯一键查询的sql
                StringBuilder selectByUniqueKey = selectByUniqueKeySqlMap.computeIfAbsent(uniqueName, key -> new StringBuilder());
                selectByUniqueKey.append("select ").append(realTableName).append(".* from ").append(realTableName);
                selectByUniqueKey.append(" where ");

                Iterator<String> iterator = names.iterator();
                while (iterator.hasNext()) {
                    String uniquePropName = iterator.next();
                    String uniqueColumnName = classDesc.getColumnByProp(uniquePropName);
                    //create.append(uniqueColumnName);
                    selectByUniqueKey.append(uniqueColumnName).append(" = ?");
                    uniqueSelectSql[i].append(uniqueColumnName).append(" = ?");

                    if (iterator.hasNext()) {
                        //create.append(", ");
                        selectByUniqueKey.append(AND_STRING);
                        uniqueSelectSql[i].append(AND_STRING);
                    }
                }
                //create.append(")");

                if (i < createTableSql.length - 1) {
                    // 不是最后一个表
                    selectByUniqueKey.append("\n union all \n");
                }
            }
            create.append("\n)");
            createTableSql[i] = create.toString();
            dropTableSql[i] = "drop table " + realTableName;

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

            /*selectAll.append("select ").append(realTableName).append(".* from ").append(realTableName);
            // 根据id排序
            selectAll.append(" order by ").append(idColumnName);
            if (i < createTableSql.length - 1) {
                // 不是最后一个表
                selectAll.append("\n union all \n");
            }*/
            selectByIdSql[i] = String.format("select * from %s where %s = ?", realTableName, idColumnName);
            if (tableSplitField != null) {
                selectByTableSplitFieldSql[i] = String.format("select * from %s where %s = ?", realTableName, classDesc.getColumnByProp(tableSplitField));
                selectIdByTableSplitFieldSql[i] = String.format("select %s from %s where %s = ?", idColumnName, realTableName, classDesc.getColumnByProp(tableSplitField));
            }
            final int tableIndex = i;
            classDesc.getGroupProps().forEach(groupedField -> {
                if (groupedField.equals(tableSplitField)) {
                    // 分表字段不应该再另起一个缓存
                    return;
                }
                String tempSql = String.format("select * from %s where %s = ?", realTableName, classDesc.getColumnByProp(groupedField));
                selectByGroupFieldSql.computeIfAbsent(groupedField, k -> new String[tableCount])[tableIndex] = tempSql;
                StringBuilder builder = unionSelectByGroupFieldSql.computeIfAbsent(groupedField, k -> new StringBuilder());
                if (builder.length() > 0) {
                    builder.append("\n union all \n");
                }
                builder.append(tempSql);
            });

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
        //this.selectAllSql = selectAll.toString();
        //this.selectAllIdSql = this.selectAllSql.replace("* from ", idColumnName + " from");
        this.selectByIdSql = selectByIdSql;
        this.selectByTableSplitFieldSql = selectByTableSplitFieldSql;
        this.selectIdByTableSplitFieldSql = selectIdByTableSplitFieldSql;
        this.selectByGroupFieldSql = selectByGroupFieldSql;
        this.unionSelectByGroupFieldSql = new HashMap<>();
        unionSelectByGroupFieldSql.forEach((k, v) -> {
            this.unionSelectByGroupFieldSql.put(k, v.toString());
        });
        Map<String, String> selectByUniqueKeySql = new HashMap<>();
        selectByUniqueKeySqlMap.forEach((key, value) -> selectByUniqueKeySql.put(key, value.toString()));
        this.selectByUniqueKeySql = selectByUniqueKeySql;

        this.updateAllNotCachedByIdSql = updateAllNotCachedByIdSql;
        this.deleteByIdSql = deleteByIdSql;
        this.selectMaxIdSql = selectMaxIdSql;
        Map<String, String> uniqueSelectSqlMap;
        Map<String, String[]> uniqueAndFieldSelectSqlMap;
        if (uniqueSelectSqlTempMap.isEmpty()) {
            uniqueSelectSqlMap = Collections.emptyMap();
            uniqueAndFieldSelectSqlMap = Collections.emptyMap();
        } else {
            uniqueSelectSqlMap = new HashMap<>();
            uniqueAndFieldSelectSqlMap = new HashMap<>();
            uniqueSelectSqlTempMap.forEach((key, value) -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < value.length; i++) {
                    uniqueAndFieldSelectSqlMap.computeIfAbsent(key, k -> new String[tableCount])[i] = value[i].toString();
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
        this.uniqueAndFieldSelectSqlMap = uniqueAndFieldSelectSqlMap;
    }

    @Override
    protected void appendLimitString(StringBuilder builder) {
        builder.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ");
    }
}
