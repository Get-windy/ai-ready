package cn.aiedge.crm.visit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CRM外勤拜访计划
 * status: 0待执行 1执行中 2已完成 3已取消
 */
@Data
@TableName("crm_visit_plan")
public class VisitPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String planNo;

    private Long customerId;

    private String customerName;

    private Long salesPersonId;

    private String salesPersonName;

    private LocalDate planDate;

    private String planTime;

    private String purpose;

    private String address;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
