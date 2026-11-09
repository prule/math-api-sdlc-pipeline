package com.paulrule.mathapi.adapter.in.web;

import com.paulrule.mathapi.domain.DivisionByZeroException;
import com.paulrule.mathapi.domain.UnknownOperationException;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single advice mapping failures to RFC 9457 Problem Details (PRD §3.2). Spring MVC's own errors come from the superclass
 * with {@code type} {@code about:blank} (R-7).
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI INVALID_REQUEST = URI.create("/problems/invalid-request");
    private static final URI UNKNOWN_OPERATION = URI.create("/problems/unknown-operation");
    private static final URI DIVISION_BY_ZERO = URI.create("/problems/division-by-zero");
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    // Spring Framework 7 leaves ProblemDetail.type null, which omits it from the JSON; R-7 wants it explicit.
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getType() == null) {
            problem.setType(ABOUT_BLANK);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // The parser's own message names Jackson classes, so it is never copied (R-7).
        return ResponseEntity.badRequest()
                .body(invalidRequest("The request body is not valid JSON, or an Operand is not a decimal number."));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var field = ex.getBindingResult().getFieldErrors().getFirst().getField();
        return ResponseEntity.badRequest().body(invalidRequest("Field '" + field + "' is required."));
    }

    @ExceptionHandler(UnknownOperationException.class)
    ProblemDetail handleUnknownOperation(UnknownOperationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(UNKNOWN_OPERATION);
        problem.setTitle("Unknown operation");
        return problem;
    }

    @ExceptionHandler(DivisionByZeroException.class)
    ProblemDetail handleDivisionByZero(DivisionByZeroException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setType(DIVISION_BY_ZERO);
        problem.setTitle("Division by zero");
        return problem;
    }

    private static ProblemDetail invalidRequest(String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(INVALID_REQUEST);
        problem.setTitle("Invalid request");
        return problem;
    }
}
