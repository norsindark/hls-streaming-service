package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AuthorizationException extends AbstractHLSException {

    public AuthorizationException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public AuthorizationException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    public AuthorizationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
