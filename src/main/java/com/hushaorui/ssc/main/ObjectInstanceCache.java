package com.hushaorui.ssc.main;

import com.hushaorui.ssc.common.em.CacheStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ObjectInstanceCache {
    /** 在数据库中的id(分表的数据对象id会重复) */
    private Serializable id;
    /** 所属对象 */
    private Class<?> objectClass;
    /** 缓存对象 */
    private Object objectInstance;
    /** 最后一次更新时间，也用来标记是否进行过初始化 */
    private long lastModifyTime;
    /** 最后一次使用时间 */
    private long lastUseTime;
    /** 缓存的状态 */
    private CacheStatus cacheStatus;
}
