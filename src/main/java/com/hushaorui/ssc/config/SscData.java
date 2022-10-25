package com.hushaorui.ssc.config;

import com.hushaorui.ssc.common.data.DataClassDesc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SscData {

    /** 表名或表前缀(当分表时为前缀) */
    public String tableName;
    /** 分表的数量 */
    public int tableCount;
    /** 是否启用缓存 */
    public Boolean cached;
    /** 所有需要添加缓存的条件查询字段集合 */
    public Map<String, List<SscValue>> conditionProps;
    /** 所有唯一键的字段集合 */
    public Map<String, List<String>> uniqueProps;
    /** id字段的名称 */
    public String idPropName;
    /** 是否使用id生成策略 */
    public Boolean useIdGeneratePolicy;
    /** 字段 */
    public Map<String, SscField> fieldMap;

    public static SscData transFrom(DataClassDesc classDesc) {
        SscData sscData = new SscData();
        sscData.tableName = classDesc.getTableName();
        sscData.tableCount = classDesc.getTableCount();
        sscData.cached = classDesc.isCached();
        sscData.idPropName = classDesc.getIdPropName();
        sscData.conditionProps = classDesc.getConditionProps();
        sscData.uniqueProps = new HashMap<>();
        classDesc.getUniqueProps().forEach((uniqueName, set) -> sscData.uniqueProps.put(uniqueName, new ArrayList<>(set)));
        sscData.useIdGeneratePolicy = classDesc.isUseIdGeneratePolicy();
        Map<String, SscField> fieldMap = new HashMap<>();
        sscData.fieldMap = fieldMap;
        for (String propName : classDesc.getPropColumnMapping().keySet()) {
            SscField sscField = new SscField();
            sscField.columnName = classDesc.getColumnByProp(propName);
            sscField.columnType = classDesc.getPropColumnTypeMapping().get(propName);
            sscField.defaultValue = classDesc.getPropDefaultValues().get(propName);
            sscField.ignore = classDesc.getIgnoreProps().contains(propName);
            sscField.notNull = classDesc.getNotNullProps().contains(propName);
            sscField.notUpdate = classDesc.getNotUpdateProps().contains(propName);
            fieldMap.put(propName, sscField);
        }
        return sscData;
    }
}
