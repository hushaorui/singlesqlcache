package com.hushaorui.ssc.log;

public interface SscLogFactory {
    SscLog getLog(Class<?> clazz);
    /** 日志级别， 如 DEBUG, INFO WARN ERROR ... */
    void setLogLevel(String logLevel);
}
