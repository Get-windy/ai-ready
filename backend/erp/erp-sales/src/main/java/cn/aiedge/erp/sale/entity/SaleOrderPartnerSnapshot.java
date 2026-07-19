package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单往来单位快照 (1:1)
 * 快照下单时刻的 ERP 往来单位 (biz_party) 信息
 * 客户来源是 ERP 往来单位，非 CRM 公海客户
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_partner_snapshot")
public class SaleOrderPartnerSnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID (1:1) */
    private Long orderId;

    // ═══ 往来单位信息快照 ═══
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private String customerGradeCode;
    private String customerGradeName;
    private String customerTicket;
    private String customerRemark;

    // ═══ 银行/税务快照 ═══
    private String bankName;
    private String bankAccount;
    private String taxNo;

    /** 区域 */
    private String region;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
