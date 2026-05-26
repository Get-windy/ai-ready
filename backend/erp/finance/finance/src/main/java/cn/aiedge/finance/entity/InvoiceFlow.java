package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 发票流转记录实体类
 * 记录发票状态变更历史
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_invoice_flow")
public class InvoiceFlow {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 发票ID
     */
    private Long invoiceId;
    
    /**
     * 操作类型：1-创建 2-提交 3-审核通过 4-审核拒绝 5-开具 6-作废 7-红冲 8-归档 9-打印 10-认证
     */
    private Integer operationType;
    
    /**
     * 操作前状态
     */
    private Integer beforeStatus;
    
    /**
     * 操作后状态
     */
    private Integer afterStatus;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 操作备注
     */
    private String remark;
    
    /**
     * 附件URL
     */
    private String attachmentUrl;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
