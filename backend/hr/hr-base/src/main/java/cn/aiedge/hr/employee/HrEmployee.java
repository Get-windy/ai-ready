package cn.aiedge.hr.employee;

import cn.aiedge.base.annotation.DataMask;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工档案实体
 * 员工基本信息、合同、证件等
 *
 * <p>员工编号 `employee_no` 由号段 `EMP` 生成（见 `HrEmployeeServiceImpl#nextEmployeeNo`），
 * 租户内唯一；`status` 的流转受状态机约束（试用 2 → 在职 1 → 离职 0）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_employee")
public class HrEmployee {

    /**
     * 员工ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 员工编号（号段生成，租户内唯一）
     */
    private String employeeNo;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 部门ID（引用 sys_department，系统管理域）
     */
    private Long deptId;

    /**
     * 岗位ID（引用 hr_position）
     */
    private Long positionId;

    /**
     * 关联系统账号ID（sys_user）；为空表示未开通账号
     */
    private Long userId;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 手机号
     */
    @DataMask(table = "hr_employee")
    private String phone;

    /**
     * 邮箱
     */
    @DataMask(table = "hr_employee")
    private String email;

    /**
     * 身份证号
     */
    @DataMask(table = "hr_employee")
    private String idCard;

    /**
     * 学历（1-小学 2-初中 3-高中 4-大专 5-本科 6-硕士 7-博士）
     */
    private Integer education;

    /**
     * 毕业院校
     */
    private String school;

    /**
     * 专业
     */
    private String major;

    /**
     * 入职日期
     */
    private LocalDate hireDate;

    /**
     * 转正日期（转正动作写入）
     */
    private LocalDate regularDate;

    /**
     * 离职日期 / 最后工作日
     */
    private LocalDate leaveDate;

    /**
     * 离职类型（1-主动离职 2-协商解除 3-辞退 4-合同到期 5-退休 6-其他）
     */
    private Integer resignType;

    /**
     * 离职原因
     */
    private String resignReason;

    /**
     * 员工类型（1-全职 2-兼职 3-实习 4-外包）
     */
    private Integer employeeType;

    /**
     * 员工状态（0-离职 1-在职 2-试用）
     */
    private Integer status;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 户籍地址
     */
    private String hometownAddress;

    /**
     * 现居住地址
     */
    private String currentAddress;

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

    // ── 以下为展示用联表字段（非表列） ──

    /** 部门名称（联 sys_department，列表展示用） */
    @TableField(exist = false)
    private String deptName;

    /** 岗位名称（联 hr_position，列表展示用） */
    @TableField(exist = false)
    private String positionName;

    /** 系统账号登录名（联 sys_user，列表展示用） */
    @TableField(exist = false)
    private String userLoginName;

    /** 工龄（年，按 hire_date 计算，列表展示用） */
    @TableField(exist = false)
    private BigDecimal workYears;
}
