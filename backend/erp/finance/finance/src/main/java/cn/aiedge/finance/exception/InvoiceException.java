package cn.aiedge.finance.exception;

/**
 * 发票业务异常
 */
public class InvoiceException extends RuntimeException {
    
    private final Integer code;
    
    public InvoiceException(String message) {
        super(message);
        this.code = 500;
    }
    
    public InvoiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public InvoiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
    
    public Integer getCode() {
        return code;
    }
}
