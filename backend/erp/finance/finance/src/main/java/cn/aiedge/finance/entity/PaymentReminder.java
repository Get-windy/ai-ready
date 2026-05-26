package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("finance_payment_reminder")
public class PaymentReminder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String reminderNo;
    
    private Long receivableId;
    
    private String receivableNo;
    
    private Long customerId;
    
    private String customerName;
    
    private Long orderId;
    
    private String orderNo;
    
    private BigDecimal pendingAmount;
    
    private BigDecimal overdueAmount;
    
    private Integer overdueDays;
    
    private LocalDate dueDate;
    
    private Integer reminderType;
    
    private Integer reminderLevel;
    
    private String reminderMethod;
    
    private String reminderContent;
    
    private LocalDateTime reminderTime;
    
    private Long reminderBy;
    
    private String reminderByName;
    
    private Integer status;
    
    private LocalDateTime responseTime;
    
    private String responseContent;
    
    private Long responseBy;
    
    private String responseByName;
    
    private LocalDateTime nextReminderTime;
    
    private Integer reminderCount;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;
}