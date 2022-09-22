package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.config.IdGeneratePolicy;
import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.config.TableSplitPolicy;
import com.hushaorui.ssc.exception.SscRuntimeException;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 表的信息
 */
@Getter
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

    public SscTableInfo(DataClassDesc classDesc, SingleSqlCacheConfig config) {
        this.classDesc = classDesc;
        // id属性的名称
        String idPropName = classDesc.getIdPropName();
        // id字段的名字
        String idColumnName = classDesc.getColumnByProp(idPropName);
        Method idGetMethod = classDesc.getPropGetMethods().get(idPropName);
        // id属性的类型
        idJavaType = idGetMethod.getReturnType();
        if (classDesc.isUseIdGeneratePolicy()) {
            IdGeneratePolicy<?> idGeneratePolicy = config.getIdGeneratePolicyMap().get(idJavaType);
            if (idGeneratePolicy == null) {
                throw new SscRuntimeException(String.format("The class: %s is not supported as an id", idJavaType.getName()));
            }
        }
        TreeMap<Comparable, TableSplitPolicy<? extends Comparable>> tableSplitPolicyTreeMap = config.getTableSplitPolicyMap().get(idJavaType);
        if (tableSplitPolicyTreeMap == null) {
            throw new SscRuntimeException(String.format("There is no table split policy with class: %s", idJavaType.getName()));
        }
        String[] createTableSql = new String[classDesc.getTableCount()];
        String[] dropTableSql = new String[classDesc.getTableCount()];
        String[] insertSql = new String[classDesc.getTableCount()];
        StringBuilder selectAll = new StringBuilder();
        String[] selectByIdSql = new String[classDesc.getTableCount()];
        Map<String, StringBuilder> selectByUniqueKeySqlMap = new HashMap<>();
        String[] updateAllNotCachedByIdSql = new String[classDesc.getTableCount()];
        String[] deleteByIdSql = new String[classDesc.getTableCount()];
        String[] selectMaxIdSql = new String[classDesc.getTableCount()];
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
                create.append("unique key (");

                // 拼接按唯一键查询的sql
                StringBuilder selectByUniqueKey = selectByUniqueKeySqlMap.computeIfAbsent(uniqueName, key -> new StringBuilder());
                selectByUniqueKey.append("select ").append(realTableName).append(".* from ").append(realTableName);
                selectByUniqueKey.append(" where ");

                Iterator<String> iterator = names.iterator();
                while (iterator.hasNext()) {
                    String uniquePropName = iterator.next();
                    String uniqueColumnName = classDesc.getColumnByProp(uniquePropName);
                    create.append(uniqueColumnName);
                    selectByUniqueKey.append(uniqueColumnName).append(" = ?");
                    if (iterator.hasNext()) {
                        create.append(", ");
                        selectByUniqueKey.append(" and ");
                    }
                }
                create.append(")");

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

        this.createTableSql = createTableSql;
        this.dropTableSql = dropTableSql;
        this.insertSql = insertSql;
        this.selectAllSql = selectAll.toString();
        this.selectByIdSql = selectByIdSql;
        Map<String, String> selectByUniqueKeySql = new HashMap<>();
        selectByUniqueKeySqlMap.forEach((key, value) -> selectByUniqueKeySql.put(key, value.toString()));
        this.selectByUniqueKeySql = selectByUniqueKeySql;
        this.updateAllNotCachedByIdSql = updateAllNotCachedByIdSql;
        this.deleteByIdSql = deleteByIdSql;
        this.selectMaxIdSql = selectMaxIdSql;
    }
}
