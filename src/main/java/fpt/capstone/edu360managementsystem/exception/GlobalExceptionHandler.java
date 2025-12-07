package fpt.capstone.edu360managementsystem.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
     * Xử lý lỗi đăng nhập sai tên đăng nhập hoặc mật khẩu
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Tên đăng nhập hoặc mật khẩu không chính xác."));
    }

    /**
     * Xử lý lỗi không tìm thấy tài khoản
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Tên đăng nhập không tồn tại trong hệ thống."));
    }

    /**
     * Xử lý lỗi tài khoản bị vô hiệu hóa
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabled(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."));
    }

    /**
     * Xử lý lỗi tài khoản bị khóa
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLocked(LockedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên."));
    }

    /**
     * Xử lý lỗi RuntimeException với message cụ thể từ service layer
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        String vietnameseMessage = translateRuntimeError(message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(vietnameseMessage));
    }

    /**
     * Xử lý lỗi IllegalStateException (business logic violations)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage();
        String vietnameseMessage = translateRuntimeError(message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(vietnameseMessage));
    }

    /**
     * Xử lý lỗi IllegalArgumentException (invalid input)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        String vietnameseMessage = translateRuntimeError(message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(vietnameseMessage));
    }

    /**
     * Xử lý các exception chung
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        ex.printStackTrace(); // In stack trace để debug
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

    /**
     * Chuyển đổi RuntimeException message sang tiếng Việt
     */
    private String translateRuntimeError(String message) {
        if (message == null || message.isEmpty()) {
            return "Đã xảy ra lỗi. Vui lòng thử lại.";
        }

        // Nếu message đã là tiếng Việt (có dấu), giữ nguyên
        if (message.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) {
            return message;
        }

        // Mapping các message tiếng Anh phổ biến sang tiếng Việt
        // === Authentication / User ===
        if (message.contains("not found") || message.contains("Not found")) {
            if (message.toLowerCase().contains("teacher")) {
                return "Không tìm thấy giáo viên.";
            }
            if (message.toLowerCase().contains("student")) {
                return "Không tìm thấy học sinh.";
            }
            if (message.toLowerCase().contains("user")) {
                return "Không tìm thấy người dùng.";
            }
            if (message.toLowerCase().contains("class")) {
                return "Không tìm thấy lớp học.";
            }
            if (message.toLowerCase().contains("course")) {
                return "Không tìm thấy khóa học.";
            }
            if (message.toLowerCase().contains("subject")) {
                return "Không tìm thấy môn học.";
            }
            if (message.toLowerCase().contains("room")) {
                return "Không tìm thấy phòng học.";
            }
            if (message.toLowerCase().contains("session")) {
                return "Không tìm thấy buổi học.";
            }
            return "Không tìm thấy dữ liệu yêu cầu.";
        }

        // === Duplicate / Already exists ===
        if (message.contains("already exists") || message.contains("Already exists")) {
            if (message.toLowerCase().contains("name")) {
                return "Tên này đã tồn tại. Vui lòng chọn tên khác.";
            }
            if (message.toLowerCase().contains("email")) {
                return "Email này đã được sử dụng.";
            }
            return "Dữ liệu đã tồn tại trong hệ thống.";
        }

        // === Email related ===
        if (message.contains("Email already in use")) {
            return "Email này đã được sử dụng bởi tài khoản khác.";
        }

        // === Password related ===
        if (message.contains("Current password is incorrect")) {
            return "Mật khẩu hiện tại không chính xác.";
        }
        if (message.contains("password") && message.contains("do not match")) {
            return "Mật khẩu mới và xác nhận mật khẩu không khớp.";
        }

        // === Teacher / Class assignment ===
        if (message.contains("not assigned to this class")) {
            return "Giáo viên chưa được phân công vào lớp này.";
        }
        if (message.contains("used by active classes") || message.contains("active classes")) {
            return "Không thể thực hiện vì đang được sử dụng bởi lớp học đang hoạt động.";
        }

        // === Course / Session ===
        if (message.contains("no course linked") || message.contains("Class has no course")) {
            return "Lớp học chưa được liên kết với khóa học nào.";
        }
        if (message.contains("No session found")) {
            return "Không tìm thấy buổi học trong ngày này.";
        }

        // === Payment ===
        if (message.contains("no sessions to calculate")) {
            return "Lớp học chưa có buổi học để tính học phí.";
        }
        if (message.contains("pricePerSession is not configured")) {
            return "Lớp học chưa được cấu hình giá mỗi buổi.";
        }

        // === Cannot disable/delete ===
        if (message.contains("cannot disable") || message.contains("Cannot disable")) {
            return "Không thể vô hiệu hóa. Đang có dữ liệu liên quan đang hoạt động.";
        }
        if (message.contains("cannot delete") || message.contains("Cannot delete")) {
            return "Không thể xóa. Đang có dữ liệu liên quan.";
        }

        // === Schedule / Room / Subject conflicts ===
        if (message.contains("Room name already exists")) {
            return "Tên phòng học đã tồn tại. Vui lòng chọn tên khác.";
        }
        if (message.contains("Subject name already exists")) {
            return "Tên môn học đã tồn tại. Vui lòng chọn tên khác.";
        }
        if (message.contains("is used by active classes")) {
            return "Không thể vô hiệu hóa vì đang được sử dụng bởi các lớp học đang hoạt động.";
        }

        // === Invalid / Required ===
        if (message.contains("is required") || message.contains("cannot be null") || message.contains("cannot be empty")) {
            return "Vui lòng điền đầy đủ các trường bắt buộc.";
        }
        if (message.contains("invalid") || message.contains("Invalid")) {
            return "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.";
        }

        // Default: giữ nguyên message gốc (đã là tiếng Việt hoặc không match)
        return message;
    }
}
