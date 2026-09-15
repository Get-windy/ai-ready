package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoutePointResult {

    private Long pointId;

    private Integer pointOrder;

    private String orderId;

    private String orderNo;

    private String customerName;

    private String customerPhone;

    private String address;

    private String latitude;

    private String longitude;

    private Double distanceFromPrev;

    private Integer durationFromPrev;

    private String status;

    private LocalDateTime arriveTime;

    private LocalDateTime leaveTime;

    /** 签收人 */
    private String signee;

    /** 签收时间 */
    private LocalDateTime signTime;

    /** 配送失败原因 */
    private String failReason;

    /** 预估到达时间（按剩余点位 + 地图能力估算） */
    private LocalDateTime etaTime;

    /** 是否被催单提前过 */
    private Integer expedited;

    /** 来源单据类型 SO-销售订单 / OUT-销售出库单 / MANUAL-手工 */
    private String sourceType;

    /** 来源单据ID（由来源键 SO:{id} / OUT:{id} 解析） */
    private Long sourceId;

    private String remark;
}