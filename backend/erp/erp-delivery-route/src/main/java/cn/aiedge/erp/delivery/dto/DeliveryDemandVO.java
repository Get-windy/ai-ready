package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送需求（可入线的来源单据）
 *
 * 来源：销售出库单（已发货待配送）+ 销售订单（待发货/部分发货）。
 * 本模块**只读**这两张表，不修改销售侧数据（README 边界：配送不改库存）。
 */
@Data
public class DeliveryDemandVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源类型：SO-销售订单 / OUT-销售出库单 */
    private String sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 来源单号 */
    private String billNo;

    private Long customerId;

    private String customerName;

    /** 配送地址（单据收货地址优先，缺省取客户档案地址） */
    private String address;

    private String receiverName;

    private String receiverPhone;

    /** 客户配送坐标（GCJ-02），用于围栏判定与路线规划；为空表示未维护 */
    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDateTime billTime;

    /** 是否已在某条路线单中（入线去重） */
    private Boolean onRoute;

    /** 围栏判定结果：true-在指定的围栏内 */
    private Boolean insideFence;

    /** 距围栏中心/边界的距离（米） */
    private Double distanceMeters;
}
