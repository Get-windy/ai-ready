package cn.aiedge.permission.exception;

/**
 * 权限拒绝异常
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
