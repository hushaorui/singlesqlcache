package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.exception.SscRuntimeException;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 表的信息
 */
public class SscTableInfoDerby extends SscTableInfo {

    public SscTableInfoDerby(DataClassDesc classDesc, SingleSqlCacheConfig config) {
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
        if (! config.getTableSplitPolicyMap().containsKey(idJavaType)) {
            throw new SscRuntimeException(String.format("There is no table split policy with class: %s, prop: %s", idJavaType.getName(), idPropName));
        }
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
                    uniqueCreateSql[i].append(uniqueIdName).append(" BIGINT").append(" primary key,\n    ").append(idColumnName).append(" ")
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
}
