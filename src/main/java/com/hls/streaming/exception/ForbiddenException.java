package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends AbstractHLSException {

    public ForbiddenException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public ForbiddenException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public ForbiddenException(ErrorCodeMessage errorCodeMessage, String message) {
        super(errorCodeMessage, message);
    }

    public ForbiddenException(ErrorCodeMessage errorCodeMessage, String message, Object optionalData) {
        super(errorCodeMessage, message, optionalData);
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(Throwable cause) {
        super(cause);
    }
}
