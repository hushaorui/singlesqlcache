package com.hushaorui.ssc.log;

public class SscLogFactoryImpl implements SscLogFactory {
    private static final SscLogFactory instance = new SscLogFactoryImpl();
    public static SscLogFactory getInstance() {
        return instance;
    }
    @Override
    public SscLog getLog(Class<?> clazz) {
        return new SscLogImpl(clazz);
    }
}
