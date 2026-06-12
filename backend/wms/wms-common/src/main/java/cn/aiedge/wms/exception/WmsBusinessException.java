package cn.aiedge.wms.exception;

import cn.aiedge.common.exception.BusinessException;

/**
 * WMS 业务异常
 */
public class WmsBusinessException extends BusinessException {

    public WmsBusinessException(String message) {
        super(message);
    }

    public WmsBusinessException(String message, Throwable cause) {
        super(500, message, cause);
    }

    /**
     * 库存不足
     */
    public static WmsBusinessException insufficientStock(String productCode, String locationCode) {
        return new WmsBusinessException(String.format("库存不足: 商品[%s] 货位[%s]", productCode, locationCode));
    }

    /**
     * 货位已满
     */
    public static WmsBusinessException locationFull(String locationCode) {
        return new WmsBusinessException(String.format("货位已满: %s", locationCode));
    }

    /**
     * 状态不允许操作
     */
    public static WmsBusinessException invalidStatus(String taskNo, int currentStatus, int expectedStatus) {
        return new WmsBusinessException(String.format("任务[%s]当前状态[%d]不允许此操作，需要状态[%d]", taskNo, currentStatus, expectedStatus));
    }

    /**
     * 租户未启用WMS
     */
    public static WmsBusinessException wmsNotEnabled(Long tenantId) {
        return new WmsBusinessException(String.format("租户[%d]未启用WMS增值服务", tenantId));
    }

    /**
     * 任务不存在
     */
    public static WmsBusinessException taskNotFound(Long taskId, String taskType) {
        return new WmsBusinessException(String.format("%s任务[%d]不存在", taskType, taskId));
    }

    /**
     * 任务不存在（通用）
     */
    public static WmsBusinessException taskNotFound(Long taskId) {
        return new WmsBusinessException(String.format("任务[%d]不存在", taskId));
    }
}
