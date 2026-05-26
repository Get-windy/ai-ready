package cn.aiedge.erp.batch.service.exception;

import java.math.BigDecimal;

/**
 * 批次业务异常
 * 用于表示业务逻辑相关的异常
 */
public class BatchBusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BatchBusinessException(String message) {
        super(message);
        this.errorCode = "BATCH_BUSINESS_ERROR";
        this.args = null;
    }

    public BatchBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BatchBusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BatchBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BATCH_BUSINESS_ERROR";
        this.args = null;
    }

    public BatchBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }

    /**
     * 批次未找到异常
     */
    public static BatchBusinessException batchNotFound(Long batchId) {
        return new BatchBusinessException("BATCH_NOT_FOUND", 
            String.format("批次不存在: id=%d", batchId));
    }

    /**
     * 批次号已存在异常
     */
    public static BatchBusinessException batchNoExists(String batchNo) {
        return new BatchBusinessException("BATCH_NO_EXISTS", 
            String.format("批次号已存在: %s", batchNo));
    }

    /**
     * 批次状态无效异常
     */
    public static BatchBusinessException invalidBatchStatus(String currentStatus, String targetStatus) {
        return new BatchBusinessException("INVALID_BATCH_STATUS", 
            String.format("无效的状态转换: %s -> %s", currentStatus, targetStatus));
    }

    /**
     * 批次数量不足异常
     */
    public static BatchBusinessException insufficientQuantity(Long batchId, 
            BigDecimal available, BigDecimal required) {
        return new BatchBusinessException("INSUFFICIENT_QUANTITY", 
            String.format("批次可用数量不足: batchId=%d, available=%s, required=%s", 
                batchId, available, required));
    }

    /**
     * 批次已过期异常
     */
    public static BatchBusinessException batchExpired(Long batchId, String batchNo) {
        return new BatchBusinessException("BATCH_EXPIRED", 
            String.format("批次已过期: batchId=%d, batchNo=%s", batchId, batchNo));
    }

    /**
     * 批次被隔离异常
     */
    public static BatchBusinessException batchQuarantined(Long batchId, String batchNo) {
        return new BatchBusinessException("BATCH_QUARANTINED", 
            String.format("批次被隔离: batchId=%d, batchNo=%s", batchId, batchNo));
    }

    /**
     * 批次已作废异常
     */
    public static BatchBusinessException batchCancelled(Long batchId, String batchNo) {
        return new BatchBusinessException("BATCH_CANCELLED", 
            String.format("批次已作废: batchId=%d, batchNo=%s", batchId, batchNo));
    }

    /**
     * 操作无权限异常
     */
    public static BatchBusinessException operationNotPermitted(String operation, String userRole) {
        return new BatchBusinessException("OPERATION_NOT_PERMITTED", 
            String.format("操作无权限: operation=%s, userRole=%s", operation, userRole));
    }

    /**
     * 参数验证失败异常
     */
    public static BatchBusinessException validationFailed(String field, String message) {
        return new BatchBusinessException("VALIDATION_FAILED", 
            String.format("参数验证失败: field=%s, message=%s", field, message));
    }

    /**
     * 并发操作异常
     */
    public static BatchBusinessException concurrentModification(Long batchId, Integer version) {
        return new BatchBusinessException("CONCURRENT_MODIFICATION", 
            String.format("数据已被修改，请刷新后重试: batchId=%d, version=%d", batchId, version));
    }
}