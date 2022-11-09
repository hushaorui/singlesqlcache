package com.hushaorui.ssc.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObjectInstanceCache extends CommonCache {
    /** 缓存对象 */
    private Object objectInstance;
    /** 分表字段的值 */
    private Comparable tableSplitFieldValue;
    /** 每个分组字段的值 */
    private Map<String, Comparable> groupFieldValues;

    public Object getObjectInstance() {
        return objectInstance;
    }

    public void setObjectInstance(Object objectInstance) {
        this.objectInstance = objectInstance;
    }

    public Comparable getTableSplitFieldValue() {
        return tableSplitFieldValue;
    }

    public void setTableSplitFieldValue(Comparable tableSplitFieldValue) {
        this.tableSplitFieldValue = tableSplitFieldValue;
    }

    public Comparable getGroupFieldValue(String propName) {
        if (groupFieldValues == null) {
            return NullObject.INSTANCE;
        }
        return groupFieldValues.getOrDefault(propName, NullObject.INSTANCE);
    }

    public void setGroupFieldValue(String propName, Comparable groupFieldValue) {
        if (groupFieldValues == null) {
            synchronized (this) {
                if (groupFieldValues == null) {
                    groupFieldValues = new ConcurrentHashMap<>();
                }
            }
        }
        groupFieldValues.put(propName, groupFieldValue == null ? NullObject.INSTANCE : groupFieldValue);
    }
}
