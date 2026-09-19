package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城拼团 →「拼团订单」Tab 行（对标 11 列）：
 * 拼团编号 / 客户名称 / 商品名称 / 订单编号 / 提交时间 / 单据时间 / 商品金额 / 订单金额 / 活动名称 / 活动ID / 拼团状态
 */
@Data
public class GroupBuyOrderRowVO {

    private Long id;
    /** 拼团编号 */
    private String groupId;
    /** 客户名称 */
    private String customerName;
    /** 商品名称 */
    private String productName;
    /** 订单编号 */
    private String orderNo;
    /** 提交时间 */
    private LocalDateTime submitTime;
    /** 单据时间 */
    private LocalDateTime billDate;
    /** 商品金额 */
    private BigDecimal productAmount;
    /** 订单金额 */
    private BigDecimal orderAmount;
    /** 活动名称 */
    private String activityName;
    /** 活动ID */
    private String activityCode;
    /** 拼团状态 */
    private String groupStatus;

    private Long activityId;
    private Long orderId;
}
