package com.hushaorui.ssc.log;

import java.time.LocalDateTime;

public class SscLogImpl implements SscLog {
    private SscLogLevel logLevel;
    private String className;
    public SscLogImpl(Class<?> clazz) {
        this.className = clazz.getName();
        logLevel = SscLogLevel.INFO;
    }
    public SscLogImpl(Class<?> clazz, SscLogLevel logLevel) {
        this.className = clazz.getName();
        this.logLevel = logLevel;
    }
    public enum SscLogLevel {
        DEBUG("DEBUG"),
        INFO (" INFO"),
        WARN (" WARN"),
        ERROR("ERROR"),
        ;
        private String msg;

        SscLogLevel(String msg) {
            this.msg = msg;
        }
    }
    @Override
    public void debug(String data) {
        if (logLevel.ordinal() > SscLogLevel.DEBUG.ordinal()) {
            return;
        }
        printLog(data, SscLogLevel.DEBUG, null);
    }

    @Override
    public void info(String data) {
        if (logLevel.ordinal() > SscLogLevel.INFO.ordinal()) {
            return;
        }
        printLog(data, SscLogLevel.INFO, null);
    }

    @Override
    public void warn(String data) {
        if (logLevel.ordinal() > SscLogLevel.WARN.ordinal()) {
            return;
        }
        printLog(data, SscLogLevel.WARN, null);
    }

    @Override
    public void error(String data) {
        if (logLevel.ordinal() > SscLogLevel.ERROR.ordinal()) {
            return;
        }
        printLog(data, SscLogLevel.ERROR, null);
    }

    @Override
    public void debug(String formatString, Object... params) {
        if (logLevel.ordinal() > SscLogLevel.DEBUG.ordinal()) {
            return;
        }
        printLog(String.format(formatString, params), SscLogLevel.DEBUG, null);
    }

    @Override
    public void info(String formatString, Object... params) {
        if (logLevel.ordinal() > SscLogLevel.INFO.ordinal()) {
            return;
        }
        printLog(String.format(formatString, params), SscLogLevel.INFO, null);
    }

    @Override
    public void warn(String formatString, Object... params) {
        if (logLevel.ordinal() > SscLogLevel.WARN.ordinal()) {
            return;
        }
        printLog(String.format(formatString, params), SscLogLevel.WARN, null);
    }

    @Override
    public void error(String formatString, Object... params) {
        if (logLevel.ordinal() > SscLogLevel.ERROR.ordinal()) {
            return;
        }
        printLog(String.format(formatString, params), SscLogLevel.ERROR, null);
    }

    @Override
    public void error(String formatString, Exception e, Object... params) {
        if (logLevel.ordinal() > SscLogLevel.ERROR.ordinal()) {
            return;
        }
        printLog(String.format(formatString, params), SscLogLevel.ERROR, e);
    }

    @Override
    public void error(String data, Exception e) {
        if (logLevel.ordinal() > SscLogLevel.ERROR.ordinal()) {
            return;
        }
        printLog(data, SscLogLevel.ERROR, e);
    }

    protected void printLog(String data, SscLogLevel logLevel, Exception e) {
        String dateString = LocalDateTime.now().toString();
        System.out.println(String.format("%s %s [%s] - %s", dateString, logLevel.msg, className, data));
        if (e != null) {
            e.printStackTrace();
        }
    }
}
