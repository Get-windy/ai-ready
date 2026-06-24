package cn.aiedge.hr.employee;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 劳动合同实体
 * 员工劳动合同信息
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_contract")
public class HrContract {

    /**
     * 合同ID
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
     * 合同类型（1-固定期限 2-无固定期限 3-试用期）
     */
    private Integer contractType;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 约定薪资
     */
    private BigDecimal salaryAmount;

    /**
     * 签订日期
     */
    private LocalDate signDate;

    /**
     * 合同状态（0-待签 1-生效 2-到期 3-终止）
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