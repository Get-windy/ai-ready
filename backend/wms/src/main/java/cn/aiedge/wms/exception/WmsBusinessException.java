package cn.aiedge.wms.exception;

import cn.aiedge.common.exception.BusinessException;

/**
 * WMS 业务异常。
 *
 * <p><b>错误码口径（2026-09-26 修正）：</b>统一取 <b>400</b>（客户端可纠正的请求问题），
 * 而非基类 {@code BusinessException(String)} 的默认 500。原因：全局处理器
 * {@code GlobalExceptionHandler#handleBusinessException} 直接拿 {@code e.getCode()}
 * 当 HTTP 状态返回，默认 500 会让「库存不足」「状态不允许」「未质检通过」这类
 * 明确的业务拒绝伪装成服务端故障 —— 前端只能显示"服务器错误"，监控也会误报。</p>
 *
 * <p><b>注意：</b>{@code BusinessException(String)} 默认 500 是**全站性设计**
 * （全仓 188 处直接 new 基类），DMS 等模块的子类同样如此；本类只统一 WMS 域口径，
 * 全站是否收敛需另行决策。</p>
 */
public class WmsBusinessException extends BusinessException {

    /** 业务拒绝默认按 400（客户端错误）返回；确需 5xx 语义时用 {@link BusinessException#BusinessException(int, String)} */
    public WmsBusinessException(String message) {
        super(400, message);
    }

    public WmsBusinessException(String message, Throwable cause) {
        super(400, message, cause);
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
