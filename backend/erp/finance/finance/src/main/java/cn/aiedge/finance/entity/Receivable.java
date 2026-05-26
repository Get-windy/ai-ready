package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("finance_receivable")
public class Receivable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String receivableNo;

    private Long customerId;

    private String customerName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate receivableDate;

    private LocalDate dueDate;

    private Integer status;

    private BigDecimal originalAmount;

    private BigDecimal receivedAmount;

    private BigDecimal pendingAmount;

    private BigDecimal overdueAmount;

    private Integer overdueDays;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

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