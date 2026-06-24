package cn.aiedge.hr.performance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绩效考核实体
 * 员工绩效考核记录
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_performance")
public class HrPerformance {

    /**
     * 绩效ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 考核周期（yyyy-MM 或 yyyy-Q1 等）
     */
    private String reviewPeriod;

    /**
     * 考核类型（MONTHLY-月度 QUARTERLY-季度 YEARLY-年度）
     */
    private String reviewType;

    /**
     * 考核评分（0-100）
     */
    private BigDecimal score;

    /**
     * 考核等级（S-A-B-C-D）
     */
    private String level;

    /**
     * 工作态度评分
     */
    private BigDecimal attitudeScore;

    /**
     * 工作能力评分
     */
    private BigDecimal abilityScore;

    /**
     * 工作业绩评分
     */
    private BigDecimal achievementScore;

    /**
     * 综合评语
     */
    private String comment;

    /**
     * 考核人ID
     */
    private Long reviewerId;

    /**
     * 考核人姓名
     */
    private String reviewerName;

    /**
     * 考核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 状态（0-待考核 1-已考核 2-已确认）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}