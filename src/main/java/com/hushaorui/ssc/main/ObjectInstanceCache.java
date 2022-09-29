package com.hushaorui.ssc.main;

class ObjectInstanceCache extends CommonCache {
    /** 缓存对象 */
    private Object objectInstance;

    public Object getObjectInstance() {
        return objectInstance;
    }

    public void setObjectInstance(Object objectInstance) {
        this.objectInstance = objectInstance;
    }
}
