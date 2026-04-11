package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;

public class AbstractHLSException extends RuntimeException {

    private ErrorCodeMessage errorCodeMessage;
    private Object optionalData;

    public AbstractHLSException(final ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage.getMessage());
        this.errorCodeMessage = errorCodeMessage;
    }

    public AbstractHLSException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage.getMessage());
        this.errorCodeMessage = errorCodeMessage;
        this.optionalData = optionalData;
    }

    protected AbstractHLSException(ErrorCodeMessage errorCodeMessage, Throwable cause) {
        super(errorCodeMessage.getMessage(), cause);
        this.errorCodeMessage = errorCodeMessage;
    }


    protected AbstractHLSException(ErrorCodeMessage errorCodeMessage, Throwable cause, Object optionalData) {
        super(errorCodeMessage.getMessage(), cause);
        this.errorCodeMessage = errorCodeMessage;
        this.optionalData = optionalData;
    }

    protected AbstractHLSException(ErrorCodeMessage errorCodeMessage, String message) {
        super(message);
        this.errorCodeMessage = errorCodeMessage;
    }

    protected AbstractHLSException(ErrorCodeMessage errorCodeMessage, String message, Object optionalData) {
        super(message);
        this.errorCodeMessage = errorCodeMessage;
        this.optionalData = optionalData;
    }


    protected AbstractHLSException(String message) {
        super(message);
    }

    protected AbstractHLSException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbstractHLSException(Throwable cause) {
        super(cause);
    }

    public AbstractHLSException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ErrorCodeMessage getErrorCodeMessage() {
        return errorCodeMessage;
    }

    public void setErrorCodeMessage(ErrorCodeMessage errorCodeMessage) {
        this.errorCodeMessage = errorCodeMessage;
    }

    public Object getOptionalData() {
        return optionalData;
    }

    public void setOptionalData(Object optionalData) {
        this.optionalData = optionalData;
    }
}
