package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单物流信息 (1:N)
 * 支持配送/快递/自提多种方式，支持多包裹
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_logistics")
public class SaleOrderLogistics {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 物流类型: DELIVERY(配送) / EXPRESS(快递) / PICKUP(自提) */
    private String logisticsType;

    // ═══ 配送信息 ═══
    private String deliveryMethod;
    private String deliveryRoute;
    private Long deliveryRouteId;
    private Long driverId;
    private String driverName;
    private String deliveryVehicle;

    // ═══ 快递信息 ═══
    /** 物流公司名称（快照） */
    private String logisticsCompany;
    /** 物流公司档案ID（biz_party.id，partnerType=LOGISTICS） */
    private Long logisticsCompanyId;
    private String logisticsNo;
    private String waybillNo;

    // ═══ 包裹/运单层（一单多包：一条记录 = 一个包裹） ═══
    /** 包裹号/箱号（同一订单内序号，如 P1/P2；为空视为默认单包裹） */
    private String packageNo;
    /** 件数 */
    private Integer packageCount;
    /** 包裹重量(kg) */
    private BigDecimal packageWeight;
    /** 包裹体积(m³) */
    private BigDecimal packageVolume;
    /** 包裹状态：0-待发货 1-已发货 2-已签收 9-异常 */
    private Integer packageStatus;

    /** 物流/包裹备注 */
    private String remark;

    // ═══ 费用 ═══
    private String freightPayer;
    private BigDecimal shippingFee;
    private BigDecimal codAmount;

    /** 承运商账单金额（对账用，人工/导入录入） */
    private BigDecimal freightBillAmount;
    /** 运费差异 = 账单金额 − 我方计费 */
    private BigDecimal freightDiff;
    /** 对账状态：0-未对账 1-已对账 */
    private Integer freightReconciled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
