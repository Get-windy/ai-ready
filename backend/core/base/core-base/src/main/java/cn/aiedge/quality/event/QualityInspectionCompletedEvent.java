package cn.aiedge.quality.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 质检单完成事件（PASS/CONCESSION/FAIL 驱动库存放行/冻结）
 * 由 QualityInspectionServiceImpl.complete() 发布，wms/erp-stock 监听。
 * 事件类放 core-base（quality 侧），监听器放消费方模块以避免 core-base 反向依赖。
 */
@Getter
@AllArgsConstructor
public class QualityInspectionCompletedEvent {

    private final Long inspectionId;

    private final String qualityNo;

    private final String bizType;

    private final String bizNo;

    private final Long productId;

    private final String productName;

    private final String batchNo;

    /** 仓库ID(冻结/放行按 仓库+批次+数量 定位库存) */
    private final Long warehouseId;

    /** PASS / FAIL / CONCESSION */
    private final String result;

    /** 合格数量 */
    private final BigDecimal passQuantity;

    /** 不合格数量 */
    private final BigDecimal failQuantity;
}
