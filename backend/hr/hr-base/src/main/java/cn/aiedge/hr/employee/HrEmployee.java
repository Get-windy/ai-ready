package cn.aiedge.hr.employee;

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
     * 员工编号
     */
    private String employeeNo;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 岗位ID
     */
    private Long positionId;

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
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 身份证号
     */
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
     * 离职日期
     */
    private LocalDate leaveDate;

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
}