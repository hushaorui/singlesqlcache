package com.hushaorui.ssc.main;

class ObjectInstanceCache extends CommonCache {
    /** 缓存对象 */
    private Object objectInstance;
    /** 分表字段的值 */
    private Comparable tableSplitFieldValue;

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
}
