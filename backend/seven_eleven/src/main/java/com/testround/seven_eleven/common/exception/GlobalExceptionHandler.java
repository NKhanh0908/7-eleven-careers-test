package com.testround.seven_eleven.common.exception;

import com.testround.seven_eleven.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("OPTIMISTIC_LOCK_FAILURE".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("FORBIDDEN_ACCESS".equals(ex.getErrorCode())) {
            status = HttpStatus.FORBIDDEN;
        }

        ApiResponse<Void> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage(), status.value());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Void> response = ApiResponse.error("Dữ liệu gửi lên không hợp lệ", HttpStatus.BAD_REQUEST.value(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingException(Exception ex) {
        log.error("Optimistic locking conflict: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error("OPTIMISTIC_LOCK_FAILURE", 
                "Dữ liệu đã bị thay đổi bởi luồng khác. Vui lòng tải lại trang và thực hiện lại.", 
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.error("Access denied error: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error("ACCESS_DENIED", "Bạn không có quyền truy cập tài nguyên này.", HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("System error: ", ex);
        ApiResponse<Void> response = ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi hệ thống nghiêm trọng. Vui lòng liên hệ quản trị viên.", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
