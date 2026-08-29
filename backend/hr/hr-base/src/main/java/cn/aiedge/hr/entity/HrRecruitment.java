package cn.aiedge.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘职位实体
 * 招聘管理-发布职位、跟踪应聘进度
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_recruitment")
public class HrRecruitment {

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
     * 招聘岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 招聘人数
     */
    private Integer headcount;

    /**
     * 招聘渠道(ONLINE/HEADHUNTER/REFERRAL/CAMPUS/OTHER)
     */
    private String channel;

    /**
     * 紧急程度(1-普通 2-紧急 3-特急)
     */
    private Integer urgency;

    /**
     * 状态(0-待审批 1-招聘中 2-已暂停 3-已完成 4-已关闭)
     */
    private Integer status;

    /**
     * 学历要求(同员工学历枚举: 1-小学 2-初中 3-高中 4-大专 5-本科 6-硕士 7-博士)
     */
    private Integer requiredEducation;

    /**
     * 工作经验要求
     */
    private String requiredExperience;

    /**
     * 薪资下限
     */
    private BigDecimal salaryMin;

    /**
     * 薪资上限
     */
    private BigDecimal salaryMax;

    /**
     * 岗位描述
     */
    private String description;

    /**
     * 任职要求
     */
    private String requirements;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人
     */
    private String publisherName;

    /**
     * 发布日期
     */
    private LocalDate publishDate;

    /**
     * 截止日期
     */
    private LocalDate expireDate;

    /**
     * 应聘人数
     */
    private Integer applicantCount;

    /**
     * 录用人数
     */
    private Integer hiredCount;

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
