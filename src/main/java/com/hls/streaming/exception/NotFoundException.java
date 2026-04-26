package com.hls.streaming.exception;

import com.hls.streaming.config.error.ErrorCodeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends AbstractHLSException {

    @Serial
    private static final long serialVersionUID = -5954414858196851579L;

    public NotFoundException(ErrorCodeMessage errorCodeMessage) {
        super(errorCodeMessage);
    }

    public NotFoundException(ErrorCodeMessage errorCodeMessage, Object optionalData) {
        super(errorCodeMessage, optionalData);
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    public NotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
