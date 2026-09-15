package cn.aiedge.dms.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * dms_task 条件聚合中间结果（Mapper 出参，非接口返回）
 *
 * @author AI-Ready Team
 */
@Data
public class TaskAggVO {

    private Long orderCount;
    private Long pendingOrders;
    private Long assignedOrders;
    private Long inTransitOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long exceptionOrders;
    /** 超时未签收（已过截止时间且尚未签收：状态 0~4） */
    private Long overdueOrders;

    /** 准时签收量（completed_time <= deadline_time） */
    private Long onTimeCount;
    /** 准时率分母（有截止时间的已签收量） */
    private Long onTimeBase;

    private BigDecimal avgDeliveryMinutes;
    private BigDecimal deliveryFee;
    private BigDecimal collectOnDelivery;
    private BigDecimal goodsAmount;
}
