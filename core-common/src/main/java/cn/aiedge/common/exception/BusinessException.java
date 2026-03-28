package cn.aiedge.common.exception;

/**
 * 业务异常基类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 创建常用异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException internalError(String message) {
        return new BusinessException(500, message);
    }
}