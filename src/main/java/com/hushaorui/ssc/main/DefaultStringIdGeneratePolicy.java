package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.IdGeneratePolicy;
import com.hushaorui.ssc.util.SscStringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultStringIdGeneratePolicy implements IdGeneratePolicy<String> {
    private static IdGeneratePolicy<String> instance;
    public static IdGeneratePolicy<String> getInstance() {
        if (instance == null) {
            synchronized (DefaultStringIdGeneratePolicy.class) {
                if (instance == null) {
                    instance = new DefaultStringIdGeneratePolicy();
                }
            }
        }
        return instance;
    }
    Map<Class<?>, AtomicLong> map = new ConcurrentHashMap<>();
    @Override
    public String getId(Class<?> dataClass) {
        return SscStringUtils.getSameLengthString(map.computeIfAbsent(dataClass, key -> new AtomicLong(1L)).getAndIncrement(), 16);
    }
}
