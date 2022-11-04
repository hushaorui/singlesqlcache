package com.hushaorui.ssc.log;

public interface SscLog {
    void debug(String data);
    void info(String data);
    void warn(String data);
    void error(String data);
    void debug(String formatString, Object... params);
    void info(String formatString, Object... params);
    void warn(String formatString, Object... params);
    void error(String formatString, Object... params);
    void error(Throwable e, String formatString, Object... params);
    void error(String data, Throwable e);
}
