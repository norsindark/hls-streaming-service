package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends AbstractHLSException {

    public UnauthorizedException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public UnauthorizedException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }

    public UnauthorizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
