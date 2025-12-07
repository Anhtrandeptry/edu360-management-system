package fpt.capstone.edu360managementsystem.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Xử lý lỗi không tìm thấy buổi học (session)
     */
    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<?> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý lỗi DataIntegrityViolationException (Duplicate entry, constraint
     * violation)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        String vietnameseMessage = "Đã xảy ra lỗi khi lưu dữ liệu";

        // Parse SQL error để hiển thị message tiếng Việt
        if (message != null) {
            if (message.contains("Duplicate entry") && message.contains("for key")) {
                // Extract email/username từ message nếu có
                if (message.contains("'email'") || message.contains("email")) {
                    vietnameseMessage = "Email này đã được sử dụng bởi tài khoản khác. Vui lòng sử dụng email khác.";
                } else if (message.contains("'username'") || message.contains("username")) {
                    vietnameseMessage = "Tên đăng nhập này đã tồn tại. Vui lòng chọn tên khác.";
                } else if (message.contains("'phone_number'") || message.contains("phone")) {
                    vietnameseMessage = "Số điện thoại này đã được sử dụng. Vui lòng sử dụng số khác.";
                } else {
                    vietnameseMessage = "Thông tin bạn nhập đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.";
                }
            } else if (message.contains("Duplicate entry")) {
                vietnameseMessage = "Dữ liệu đã tồn tại trong hệ thống. Vui lòng kiểm tra và thử lại.";
            }
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(vietnameseMessage));
    }

    /**
     * Xử lý lỗi validation (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();

            // Chuyển sang tiếng Việt
            String vietnameseMessage = translateValidationError(fieldName, errorMessage);
            errors.put(fieldName, vietnameseMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.",
                        "errors", errors
                ));
    }

    /**
     * Xử lý các exception chung
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        logger.error("Unhandled exception occurred: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
    }

    /**
     * Chuyển đổi validation error sang tiếng Việt
     */
    @SuppressWarnings("unused")
    private String translateValidationError(String fieldName, String errorMessage) {
        // Có thể customize dựa trên field và error trong tương lai
        if (errorMessage.contains("must not be blank")) {
            return "Trường này không được để trống";
        }
        if (errorMessage.contains("must not be empty")) {
            return "Trường này không được để trống";
        }
        if (errorMessage.contains("must be a well-formed email")) {
            return "Email không đúng định dạng";
        }
        if (errorMessage.contains("size must be between")) {
            return "Độ dài không hợp lệ";
        }
        return errorMessage;
    }
}
