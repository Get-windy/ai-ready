package cn.aiedge.hr.salary;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资结构实体
 * 员工薪资组成配置
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_salary_structure")
public class HrSalaryStructure {

    /**
     * 薪资结构ID
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
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 绩效工资基数
     */
    private BigDecimal performanceSalary;

    /**
     * 岗位津贴
     */
    private BigDecimal positionAllowance;

    /**
     * 交通补贴
     */
    private BigDecimal transportAllowance;

    /**
     * 餐饮补贴
     */
    private BigDecimal mealAllowance;

    /**
     * 住房补贴
     */
    private BigDecimal housingAllowance;

    /**
     * 其他补贴
     */
    private BigDecimal otherAllowance;

    /**
     * 社保基数
     */
    private BigDecimal socialBase;

    /**
     * 公积金基数
     */
    private BigDecimal fundBase;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expiryDate;

    /**
     * 状态（0-失效 1-生效）
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