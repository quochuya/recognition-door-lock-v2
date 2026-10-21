package com.example.lock.config;

import lombok.Builder;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Không có handler này, lỗi 403 (thiếu quyền) và 401 (sai mật khẩu) sẽ trả về
 * trang HTML lỗi mặc định của Spring Boot thay vì JSON — khó cho frontend xử lý.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Bắt lỗi xoá/sửa dữ liệu đang bị bảng khác tham chiếu bằng khoá ngoại
     * (vd xoá 1 Floor còn Room bên trong, xoá User còn RfidCard/AccessLog).
     * Không có handler này, Postgres ném lỗi kỹ thuật khó hiểu ra thẳng frontend.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "Không thể thực hiện vì dữ liệu này đang được tham chiếu bởi dữ liệu khác (vd còn tầng/phòng/thẻ bên trong)");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Getter
    @Builder
    public static class ApiError {
        private int status;
        private String message;
        private LocalDateTime timestamp;
    }
}
