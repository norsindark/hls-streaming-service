package com.hls.streaming.infrastructure.config.error;

import com.hls.streaming.common.constant.ErrorConfigConstants;
import com.hls.streaming.common.enums.ErrorEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ErrorCodeConfig {

    private final Map<Long, ErrorCodeMessage> errorCodeMap;

    public ErrorCodeConfig(Map<Long, ErrorCodeMessage> errorCodeMap) {
        this.errorCodeMap = errorCodeMap;
    }

    public ErrorCodeMessage getMessage() {
        return getMessage(ErrorConfigConstants.DEFAULT_ERROR);
    }

    public ErrorCodeMessage getMessage(long errorCode) {
        return Optional.of(errorCodeMap.get(errorCode)).orElse(errorCodeMap.get(ErrorConfigConstants.DEFAULT_ERROR));
    }

    public ErrorCodeMessage getMessage(ErrorEnum errorEnum) {
        return ErrorCodeMessage.builder()
                .code(errorEnum.getCode())
                .message(errorEnum.getMessage())
                .build();
    }

    public ErrorCodeMessage getMessage(long errorCode, Object... args) {
        var errorCodeMessage =
                Optional.of(errorCodeMap.get(errorCode)).orElse(errorCodeMap.get(ErrorConfigConstants.DEFAULT_ERROR));
        return ErrorCodeMessage.builder()
                .code(errorCode)
                .message(String.format(errorCodeMessage.getMessage(), args))
                .build();
    }

    public ErrorCodeMessage getMessage(ErrorEnum errorEnum, Object... args) {
        return ErrorCodeMessage.builder()
                .code(errorEnum.getCode())
                .message(String.format(errorEnum.getMessage(), args))
                .build();
    }

    public ErrorCodeMessage internalServerErrorWithMessage(String errorMessage) {
        return ErrorCodeMessage.builder()
                .code(ErrorConfigConstants.INTERNAL_SERVER_ERROR)
                .message(errorMessage)
                .build();
    }
}
