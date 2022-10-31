package com.hushaorui.ssc.log;

public class SscLogFactoryImpl implements SscLogFactory {
    private static final SscLogFactory instance = new SscLogFactoryImpl();
    private SscLogImpl.SscLogLevel logLevel;
    public static SscLogFactory getInstance() {
        return instance;
    }
    @Override
    public SscLog getLog(Class<?> clazz) {
        if (logLevel == null) {
            return new SscLogImpl(clazz);
        }
        return new SscLogImpl(clazz, logLevel);
    }

    @Override
    public void setLogLevel(String logLevel) {
        this.logLevel = SscLogImpl.SscLogLevel.valueOf(logLevel);
    }
}
