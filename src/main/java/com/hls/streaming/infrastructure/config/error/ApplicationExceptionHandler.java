package com.hls.streaming.infrastructure.config.error;

import com.hls.streaming.common.constant.ErrorConfigConstants;
import com.hls.streaming.common.exception.AbstractHLSException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {


    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception exception,
            Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request) {

        if (exception instanceof MethodArgumentNotValidException) {
            return handleMethodArgumentNotValidException((MethodArgumentNotValidException) exception, request);
        }

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .httpStatus(HttpStatus.valueOf(statusCode.value()))
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            WebRequest request) {

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .build());
    }

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<Object> handleDataAccessException(
            DataAccessException exception,
            WebRequest request) {

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            WebRequest request) {

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException exception,
            WebRequest request) {

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .errorFields(mapFromConstraintViolations(exception.getConstraintViolations()))
                        .build());
    }

    private ResponseEntity<Object> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            WebRequest request) {

        final var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.ErrorField.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .toList();

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .customMessage("Validation Failed")
                        .request(request)
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .errorFields(fieldErrors)
                        .build());
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(
            RuntimeException exception,
            WebRequest request) {

        return handleAllExceptions(
                ErrorParams.builder()
                        .exception(exception)
                        .request(request)
                        .build());
    }

    private List<ErrorResponse.ErrorField> mapFromConstraintViolations(
            Set<ConstraintViolation<?>> constraintViolations) {

        return constraintViolations.stream()
                .map(error -> ErrorResponse.ErrorField.builder()
                        .field(error.getPropertyPath().toString())
                        .message(error.getMessage())
                        .rejectedValue(error.getInvalidValue())
                        .build())
                .toList();
    }

    private ResponseEntity<Object> handleAllExceptions(ErrorParams params) {

        final var exception = params.getException();
        final var servletWebRequest = (ServletWebRequest) params.getRequest();
        final var path = servletWebRequest.getRequest().getServletPath();

        if (log.isDebugEnabled()) {
            log.debug("Handling exception {} at {}", exception.getClass().getSimpleName(), path, exception);
        }

        final var status = Optional.ofNullable(params.getHttpStatus())
                .orElse(Optional.ofNullable(exception.getClass().getAnnotation(ResponseStatus.class))
                        .map(ResponseStatus::value)
                        .orElse(HttpStatus.INTERNAL_SERVER_ERROR));

        AbstractHLSException abstractException = null;
        Object optionalData = null;

        if (exception instanceof AbstractHLSException) {
            abstractException = (AbstractHLSException) exception;
            optionalData = abstractException.getOptionalData();

            abstractException.setErrorCodeMessage(
                    Optional.ofNullable(abstractException.getErrorCodeMessage())
                            .orElseGet(() -> ErrorCodeMessage.builder()
                                    .code(ErrorConfigConstants.INTERNAL_SERVER_ERROR)
                                    .message(Optional.ofNullable(exception.getMessage())
                                            .orElse(exception.getLocalizedMessage()))
                                    .build()));
        } else if (exception instanceof DataAccessException) {
            final var message = ErrorCodeMessage.builder()
                    .code(ErrorConfigConstants.INTERNAL_SERVER_ERROR)
                    .message(Optional.ofNullable(exception.getMessage())
                            .orElse(exception.getLocalizedMessage()))
                    .build();

            abstractException = new AbstractHLSException(message);
        }

        final var error = Optional.ofNullable(abstractException)
                .map(AbstractHLSException::getErrorCodeMessage)
                .orElse(ErrorCodeMessage.builder()
                        .code(ErrorConfigConstants.INTERNAL_SERVER_ERROR)
                        .message(Optional.ofNullable(exception.getMessage())
                                .orElse(exception.getLocalizedMessage()))
                        .build());

        if (ErrorConfigConstants.INTERNAL_SERVER_ERROR == error.getCode()) {
            log.error("Internal error: {}", error.getMessage(), exception);
        }

        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .errorCode(error.getCode())
                        .error(status.getReasonPhrase())
                        .message(error.getMessage())
                        .optionalData(optionalData)
                        .path(path)
                        .timestamp(Instant.now())
                        .details(params.getErrorFields())
                        .build());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorParams {
        private Exception exception;
        private String customMessage;
        private WebRequest request;
        private HttpStatus httpStatus;
        private List<ErrorResponse.ErrorField> errorFields;
    }
}
