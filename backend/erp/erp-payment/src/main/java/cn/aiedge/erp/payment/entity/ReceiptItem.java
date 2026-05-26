package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_receipt_item")
public class ReceiptItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long receiptId;

    private Integer lineNo;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private BigDecimal orderAmount;

    private BigDecimal invoiceAmount;

    private BigDecimal pendingAmount;

    private BigDecimal verifyAmount;

    private BigDecimal verifiedAmount;

    private Integer verifyStatus;

    private LocalDateTime verifiedTime;

    private Long verifiedBy;

    private String remark;

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
}