package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 零售收银交班（班次）实体
 * status: 1=营业中 2=已交班
 */
@Data
@Accessors(chain = true)
@TableName("erp_retail_shift")
public class RetailShift {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 班次号（BC+yyyyMMdd+随机） */
    private String shiftNo;

    /** 收银员 */
    private Long cashierId;
    private String cashierName;

    /** 收银仓库 */
    private Long warehouseId;

    /** 开班时间 */
    private LocalDateTime openTime;

    /** 期初现金（开班备用金） */
    private BigDecimal openingCash;

    /** 交班时间 */
    private LocalDateTime closeTime;

    /** 实点现金（交班时录入） */
    private BigDecimal closingCash;

    /** 应收现金=期初现金+现金销售-现金退款 */
    private BigDecimal expectedCash;

    /** 长短款=实点现金-应收现金 */
    private BigDecimal difference;

    /** 班次内已结算零售单数 */
    private Integer orderCount;

    /** 班次内实收总额 */
    private BigDecimal totalAmount;

    /** 现金支付合计 */
    private BigDecimal cashAmount;

    /** 扫码支付合计（支付宝+微信+聚合） */
    private BigDecimal qrAmount;

    /** 其他支付合计（银行卡/预收/转账等） */
    private BigDecimal otherAmount;

    /** 状态：1=营业中 2=已交班 */
    private Integer status;

    private String remark;

    // ═══ 系统字段 ═══
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
