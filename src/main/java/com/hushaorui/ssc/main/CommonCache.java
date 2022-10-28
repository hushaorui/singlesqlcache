package com.hushaorui.ssc.main;

import com.hushaorui.ssc.common.em.CacheStatus;

class CommonCache {
    /** 在数据库中的id(分表的数据对象id会重复) */
    private Comparable id;
    /** 所属对象 */
    private Class<?> objectClass;
    /** 最后一次更新时间，也用来标记是否进行过初始化 */
    private long lastModifyTime;
    /** 最后一次使用时间 */
    private long lastUseTime;
    /** 缓存的状态 */
    private CacheStatus cacheStatus;

    public Comparable getId() {
        return id;
    }

    public void setId(Comparable id) {
        this.id = id;
    }

    public Class<?> getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(Class<?> objectClass) {
        this.objectClass = objectClass;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public CacheStatus getCacheStatus() {
        return cacheStatus;
    }

    public void setCacheStatus(CacheStatus cacheStatus) {
        this.cacheStatus = cacheStatus;
    }
}
