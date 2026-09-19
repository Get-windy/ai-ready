package cn.aiedge.hr.change;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人事异动记录
 *
 * <p>对标 SAP 的 **Personnel Action**（一个动作 = 一组预定义 infotype 的连续维护，写入
 * Actions infotype 0000）、用友 DHR 的「变动类型 + 变动原因 + 生效日期」、金蝶 s-HR 的「人事快速异动」。</p>
 *
 * <p>本系统原本**完全没有异动模型**——入职/转正/调岗/离职都是覆盖式改 `hr_employee` 主档，
 * 改完查不到历史。本表由员工服务在建档/转正/调岗/调薪/离职时**自动写入**一条快照
 * （`before_json` / `after_json`），从而获得可回溯的变更历史；异动本身仍是即时生效，
 * 未引入审批流（审批流接入属后续项）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_employee_change")
public class HrEmployeeChange {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long employeeId;

    private String employeeNo;

    private String employeeName;

    /** 异动类型：ENTRY-入职 REGULAR-转正 TRANSFER-调岗 SALARY_ADJUST-调薪 RESIGN-离职 REHIRE-复职 UPDATE-信息变更 */
    private String changeType;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 变更前快照（JSON） */
    private String beforeJson;

    /** 变更后快照（JSON） */
    private String afterJson;

    /** 异动原因 */
    private String reason;

    private Long operatorId;

    private String operatorName;

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
