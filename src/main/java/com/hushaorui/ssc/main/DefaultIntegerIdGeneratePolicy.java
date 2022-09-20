package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.IdGeneratePolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultIntegerIdGeneratePolicy implements IdGeneratePolicy<Integer> {
    private static IdGeneratePolicy<Integer> instance;
    public static IdGeneratePolicy<Integer> getInstance() {
        if (instance == null) {
            synchronized (DefaultIntegerIdGeneratePolicy.class) {
                if (instance == null) {
                    instance = new DefaultIntegerIdGeneratePolicy();
                }
            }
        }
        return instance;
    }
    Map<Class<?>, AtomicInteger> map = new ConcurrentHashMap<>();
    @Override
    public Integer getId(Class<?> dataClass) {
        return map.computeIfAbsent(dataClass, key -> new AtomicInteger(1)).getAndIncrement();
    }
}
