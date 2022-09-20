package com.hushaorui.ssc.exception;

public class SscRuntimeException extends RuntimeException {
    public SscRuntimeException(String message) {
        super(message);
    }

    public SscRuntimeException(Throwable cause) {
        super(cause);
    }
}
