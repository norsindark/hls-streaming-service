package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends AbstractHLSException {

    public BadRequestException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public BadRequestException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public BadRequestException(ErrorCodeMessage errorCodeMessage, Throwable cause) {
        super(errorCodeMessage, cause);
    }

    public BadRequestException(ErrorCodeMessage errorCodeMessage, Throwable cause, Object optionalData) {
        super(errorCodeMessage, cause, optionalData);
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
