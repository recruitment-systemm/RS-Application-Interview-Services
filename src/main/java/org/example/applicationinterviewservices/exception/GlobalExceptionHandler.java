package org.example.applicationinterviewservices.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.applicationinterviewservices.dto.response.ErrorDetails;
import org.example.applicationinterviewservices.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFound(ApplicationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInterviewNotFound(InterviewNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(InvalidInterviewPhaseException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInterviewPhase(InvalidInterviewPhaseException ex) {
        return build(HttpStatus.CONFLICT, "INVALID_INTERVIEW_PHASE", ex.getMessage());
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateApplication(DuplicateApplicationException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_APPLICATION", ex.getMessage());
    }

    @ExceptionHandler(DuplicateInterviewException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInterview(DuplicateInterviewException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_INTERVIEW", ex.getMessage());
    }

    @ExceptionHandler(InvalidResumeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResume(InvalidResumeException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_RESUME", ex.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException ex) {
        log.error("File upload failed", ex);
        return build(HttpStatus.BAD_REQUEST, "FILE_UPLOAD_ERROR", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Resume file exceeds the maximum allowed size");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", ex.getParameterName() + " is required");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", "Request body is missing or malformed. Send a valid JSON body.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(false, "Validation failed", new ErrorDetails("VALIDATION_ERROR", errors)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(false, message, new ErrorDetails(code, null)));
    }
}
