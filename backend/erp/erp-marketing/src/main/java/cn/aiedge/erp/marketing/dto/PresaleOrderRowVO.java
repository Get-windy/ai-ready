package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城预售 →「预售订单」Tab 行（对标 8 列）：
 * 客户名称 / 商品名称 / 单据编号 / 单据日期 / 商品金额 / 订单金额 / 活动名称 / 活动状态
 */
@Data
public class PresaleOrderRowVO {

    private Long id;
    private String customerName;
    private String productName;
    private String orderNo;
    private LocalDateTime billDate;
    /** 商品金额 */
    private BigDecimal productAmount;
    /** 订单金额 */
    private BigDecimal orderAmount;
    private String activityName;
    /** 活动状态（预售活动状态文案） */
    private String activityStatus;

    private Long presaleId;
    private Long orderId;
    private Integer depositPaid;
    private Integer finalPaid;
}
