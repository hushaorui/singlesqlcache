package com.hushaorui.ssc.config;

/**
 * id生成策略
 * @param <ID_TYPE> id属性的类型
 */
public interface IdGeneratePolicy<ID_TYPE> {
    /**
     * 获取id
     * @param dataClass 数据类型
     * @return id
     */
    ID_TYPE getId(Class<?> dataClass);
}
