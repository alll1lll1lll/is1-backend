package ru.itmo.is1.web;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.is1.dto.common.ErrorResponse;
import ru.itmo.is1.exception.ApiException;
import ru.itmo.is1.exception.DatabaseConstraintMessages;
import ru.itmo.is1.exception.ErrorCode;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String VALIDATION_MESSAGE = "Validation failed";
    private static final String REF_VALIDITY_SUFFIX = ".validRef";
    private static final Map<Class<?>, String> TYPE_DESCRIPTIONS = Map.of(
            Integer.class, "an integer",
            Long.class, "an integer",
            Float.class, "a number",
            Double.class, "a number",
            Boolean.class, "true or false");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        log.atDebug()
                .setMessage("Request rejected")
                .addKeyValue("errorCode", ex.getCode())
                .addKeyValue("reason", ex.getMessage())
                .addKeyValue("path", requestPath(request))
                .log();
        return respond(ex.getCode(), ex.getMessage(), null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return respond(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password", null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return respond(ErrorCode.FORBIDDEN, "Access denied", null, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                addError(errors, lastPathElement(violation.getPropertyPath().toString()), violation.getMessage()));
        return validationFailed(errors, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        ConstraintViolationException violation = findCause(ex, ConstraintViolationException.class);
        if (violation != null) {
            return handleConstraintViolation(violation, request);
        }
        SQLException sqlException = findCause(ex, SQLException.class);
        if (sqlException != null && DatabaseConstraintMessages.isIntegrityViolation(sqlException)) {
            log.atWarn()
                    .setMessage("Database constraint violated")
                    .addKeyValue("sqlState", sqlException.getSQLState())
                    .addKeyValue("reason", sqlException.getMessage())
                    .addKeyValue("path", requestPath(request))
                    .log();
            return respond(ErrorCode.CONSTRAINT_VIOLATION, DatabaseConstraintMessages.describe(sqlException),
                    null, request);
        }
        log.atError()
                .setMessage("Unhandled exception")
                .setCause(ex)
                .addKeyValue("exception", ex.getClass().getName())
                .addKeyValue("path", requestPath(request))
                .log();
        return respond(ErrorCode.INTERNAL_ERROR, "Internal server error", null, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                addError(errors, stripRefSuffix(error.getField()), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                addError(errors, error.getObjectName(), error.getDefaultMessage()));
        return validationFailed(errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> addError(errors, parameter, error.getDefaultMessage()));
        });
        return validationFailed(errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        return validationFailed(Map.of(parameter, "expected " + describeType(ex.getRequiredType())), request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        return validationFailed(Map.of(ex.getParameterName(), "required parameter"), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        JsonMappingException mappingException = findCause(ex, JsonMappingException.class);
        if (mappingException == null || mappingException.getPath().isEmpty()) {
            return respond(ErrorCode.MALFORMED_REQUEST, "Malformed JSON request body", null, request);
        }
        String field = mappingException.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."));
        String problem = mappingException instanceof InvalidFormatException invalidFormat
                ? "expected " + describeType(invalidFormat.getTargetType())
                : "invalid value";
        return validationFailed(Map.of(field, problem), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ErrorResponse error = buildError(status, status.name(), status.getReasonPhrase(), null, request);
        return new ResponseEntity<>(error, headers, statusCode);
    }

    private ResponseEntity<Object> validationFailed(Map<String, String> errors, WebRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, VALIDATION_MESSAGE, errors, request);
    }

    private ResponseEntity<Object> respond(ErrorCode code, String message, Map<String, String> fieldErrors,
                                           WebRequest request) {
        ErrorResponse error = buildError(code.getStatus(), code.name(), message, fieldErrors, request);
        return ResponseEntity.status(code.getStatus()).body(error);
    }

    private static ErrorResponse buildError(HttpStatus status, String code, String message,
                                            Map<String, String> fieldErrors, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(code)
                .message(message)
                .path(requestPath(request))
                .fieldErrors(fieldErrors)
                .build();
    }

    private static String requestPath(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : null;
    }

    private static void addError(Map<String, String> errors, String field, String message) {
        errors.merge(field, message, (first, second) -> first + "; " + second);
    }

    private static String stripRefSuffix(String field) {
        return field.endsWith(REF_VALIDITY_SUFFIX)
                ? field.substring(0, field.length() - REF_VALIDITY_SUFFIX.length())
                : field;
    }

    private static String lastPathElement(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private static String describeType(Class<?> type) {
        if (type == null) {
            return "a value of another type";
        }
        if (type.isEnum()) {
            return "one of: " + Arrays.toString(type.getEnumConstants());
        }
        Class<?> boxed = ClassUtils.resolvePrimitiveIfNecessary(type);
        return TYPE_DESCRIPTIONS.getOrDefault(boxed, "a value of type " + boxed.getSimpleName());
    }

    private static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }
}
