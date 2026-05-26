package cn.aiedge.erp.sales.pricing.exception;

/**
 * 价格计算异常类
 */
public class PriceCalculationException extends RuntimeException {
    
    private final String errorCode;
    private final String errorDetail;
    private final Object errorData;
    
    public PriceCalculationException(String message) {
        super(message);
        this.errorCode = "PRICE_CALCULATION_ERROR";
        this.errorDetail = message;
        this.errorData = null;
    }
    
    public PriceCalculationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = message;
        this.errorData = null;
    }
    
    public PriceCalculationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetail = message;
        this.errorData = null;
    }
    
    public PriceCalculationException(String errorCode, String message, String errorDetail) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
        this.errorData = null;
    }
    
    public PriceCalculationException(String errorCode, String message, String errorDetail, Object errorData) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
        this.errorData = errorData;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorDetail() {
        return errorDetail;
    }
    
    public Object getErrorData() {
        return errorData;
    }
    
    /**
     * 价格计算异常错误码
     */
    public static class ErrorCodes {
        // 产品相关错误
        public static final String PRODUCT_NOT_FOUND = "PRICE_PRODUCT_001";
        public static final String PRODUCT_INACTIVE = "PRICE_PRODUCT_002";
        public static final String PRODUCT_PRICE_MISSING = "PRICE_PRODUCT_003";
        
        // 客户相关错误
        public static final String CUSTOMER_NOT_FOUND = "PRICE_CUSTOMER_001";
        public static final String CUSTOMER_INACTIVE = "PRICE_CUSTOMER_002";
        public static final String CUSTOMER_CREDIT_LIMIT = "PRICE_CUSTOMER_003";
        
        // 策略相关错误
        public static final String STRATEGY_NOT_FOUND = "PRICE_STRATEGY_001";
        public static final String STRATEGY_INACTIVE = "PRICE_STRATEGY_002";
        public static final String STRATEGY_EXPIRED = "PRICE_STRATEGY_003";
        public static final String STRATEGY_CONFLICT = "PRICE_STRATEGY_004";
        public static final String NO_APPLICABLE_STRATEGY = "PRICE_STRATEGY_005";
        
        // 规则相关错误
        public static final String RULE_PARSING_ERROR = "PRICE_RULE_001";
        public static final String RULE_EXECUTION_ERROR = "PRICE_RULE_002";
        public static final String RULE_ENGINE_ERROR = "PRICE_RULE_003";
        
        // 计算相关错误
        public static final String CALCULATION_ERROR = "PRICE_CALCULATION_001";
        public static final String INVALID_PARAMETERS = "PRICE_CALCULATION_002";
        public static final String PRICE_OUT_OF_RANGE = "PRICE_CALCULATION_003";
        public static final String NEGATIVE_PRICE = "PRICE_CALCULATION_004";
        
        // 验证相关错误
        public static final String VALIDATION_FAILED = "PRICE_VALIDATION_001";
        public static final String PRICE_APPROVAL_REQUIRED = "PRICE_VALIDATION_002";
        public static final String PRICE_OUT_OF_BOUNDS = "PRICE_VALIDATION_003";
        
        // 系统相关错误
        public static final String SYSTEM_ERROR = "PRICE_SYSTEM_001";
        public static final String CACHE_ERROR = "PRICE_SYSTEM_002";
        public static final String PERFORMANCE_ERROR = "PRICE_SYSTEM_003";
        public static final String TIMEOUT_ERROR = "PRICE_SYSTEM_004";
    }
    
    /**
     * 异常工厂方法
     */
    public static class Factory {
        
        public static PriceCalculationException productNotFound(String productCode) {
            return new PriceCalculationException(
                ErrorCodes.PRODUCT_NOT_FOUND,
                "产品不存在",
                String.format("产品代码 '%s' 不存在", productCode)
            );
        }
        
        public static PriceCalculationException noApplicableStrategy(PriceCalculationRequest request) {
            return new PriceCalculationException(
                ErrorCodes.NO_APPLICABLE_STRATEGY,
                "未找到适用的价格策略",
                String.format("未找到适用于产品 '%s', 客户 '%s' 的价格策略", 
                    request.getProductCode(), request.getCustomerId())
            );
        }
        
        public static PriceCalculationException strategyConflict(String strategyName1, String strategyName2) {
            return new PriceCalculationException(
                ErrorCodes.STRATEGY_CONFLICT,
                "价格策略冲突",
                String.format("策略 '%s' 和 '%s' 存在冲突", strategyName1, strategyName2)
            );
        }
        
        public static PriceCalculationException priceOutOfRange(BigDecimal price, BigDecimal min, BigDecimal max) {
            return new PriceCalculationException(
                ErrorCodes.PRICE_OUT_OF_RANGE,
                "价格超出允许范围",
                String.format("计算价格 %s 超出允许范围 [%s, %s]", price, min, max)
            );
        }
        
        public static PriceCalculationException negativePrice(BigDecimal price) {
            return new PriceCalculationException(
                ErrorCodes.NEGATIVE_PRICE,
                "计算结果为负价格",
                String.format("计算得到负价格: %s", price)
            );
        }
    }
}