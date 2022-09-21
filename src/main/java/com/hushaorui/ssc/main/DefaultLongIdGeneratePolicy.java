package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.IdGeneratePolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultLongIdGeneratePolicy implements IdGeneratePolicy<Long> {
    private static IdGeneratePolicy<Long> instance;
    public static IdGeneratePolicy<Long> getInstance() {
        if (instance == null) {
            synchronized (DefaultLongIdGeneratePolicy.class) {
                if (instance == null) {
                    instance = new DefaultLongIdGeneratePolicy();
                }
            }
        }
        return instance;
    }
    private DefaultLongIdGeneratePolicy() {}
    Map<Class<?>, AtomicLong> map = new ConcurrentHashMap<>();
    @Override
    public Long getId(Class<?> dataClass) {
        return map.computeIfAbsent(dataClass, key -> new AtomicLong(1L)).getAndIncrement();
    }
}
