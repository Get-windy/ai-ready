package cn.aiedge.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_contract_payment")
public class ContractPayment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long contractId;

    private Integer paymentNo;

    private String paymentName;

    private Integer paymentStage;

    private String stageDesc;

    private BigDecimal planAmount;

    private BigDecimal actualAmount;

    private LocalDateTime planDate;

    private LocalDateTime actualDate;

    private Integer status;

    private Long invoiceId;

    private String invoiceNo;

    private String remark;

    private Long approvedBy;

    private LocalDateTime approvedTime;

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