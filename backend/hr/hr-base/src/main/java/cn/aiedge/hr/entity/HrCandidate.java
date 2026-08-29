package cn.aiedge.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 候选人实体
 * 招聘管理-候选人信息、面试流程跟踪
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_candidate")
public class HrCandidate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 关联招聘ID
     */
    private Long recruitmentId;

    /**
     * 候选人姓名
     */
    private String name;

    /**
     * 性别(0-未知 1-男 2-女)
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 学历(1-小学 2-初中 3-高中 4-大专 5-本科 6-硕士 7-博士)
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
     * 工作年限
     */
    private String experience;

    /**
     * 当前公司
     */
    private String currentCompany;

    /**
     * 当前职位
     */
    private String currentPosition;

    /**
     * 期望薪资
     */
    private BigDecimal expectedSalary;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 简历附件URL
     */
    private String resumeUrl;

    /**
     * 状态(0-简历筛选 1-初试 2-复试 3-终面 4-待录用 5-已录用 6-已拒绝 7-已入职)
     */
    private Integer status;

    /**
     * 面试官ID
     */
    private Long interviewerId;

    /**
     * 面试官
     */
    private String interviewerName;

    /**
     * 面试时间
     */
    private LocalDateTime interviewTime;

    /**
     * 面试评价
     */
    private String interviewComment;

    /**
     * 评分(1-5)
     */
    private Integer rating;

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
