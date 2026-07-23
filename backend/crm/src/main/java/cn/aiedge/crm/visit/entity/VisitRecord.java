package cn.aiedge.crm.visit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CRM外勤拜访执行记录（签到打卡）
 * visitType: 1上门 2电话 3其他
 * result: 1有意向 2一般 3无意向
 */
@Data
@TableName("crm_visit_record")
public class VisitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 关联拜访计划ID，可空=无计划临时拜访 */
    private Long planId;

    private Long customerId;

    private String customerName;

    private Long salesPersonId;

    private String salesPersonName;

    private LocalDateTime visitTime;

    private Integer visitType;

    private String location;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String content;

    private Integer result;

    private String nextAction;

    private LocalDate nextVisitDate;

    /** 附件URL json数组 */
    private String attachments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
