package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 采购订单供应商快照 (1:1)
 * 对标 SaleOrderPartnerSnapshot
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_partner_snapshot")
public class PurchaseOrderPartnerSnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 供应商名称 */
    private String supplierName;

    /** 供应商编号 */
    private String supplierCode;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 联系地址 */
    private String contactAddress;

    /** 开户行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 税号 */
    private String taxNo;

    /** 供应商备注 */
    private String supplierRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
