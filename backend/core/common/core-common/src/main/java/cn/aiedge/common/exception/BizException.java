package cn.aiedge.common.exception;

/**
 * 业务异常类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class BizException extends RuntimeException {
    
    private String code;
    
    public BizException(String message) {
        super(message);
    }
    
    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}