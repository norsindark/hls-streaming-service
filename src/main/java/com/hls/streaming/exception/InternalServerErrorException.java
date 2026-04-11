package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends AbstractHLSException {
    public InternalServerErrorException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public InternalServerErrorException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public InternalServerErrorException(ErrorCodeMessage errorCodeMessage, Throwable cause) {
        super(errorCodeMessage, cause);
    }

    public InternalServerErrorException(ErrorCodeMessage errorCodeMessage, Throwable cause, Object optionalData) {
        super(errorCodeMessage, cause, optionalData);
    }

    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerErrorException(Throwable cause) {
        super(cause);
    }

    public InternalServerErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
