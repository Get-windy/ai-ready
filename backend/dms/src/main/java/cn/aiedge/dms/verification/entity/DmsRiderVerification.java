package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 骑手实名认证（KYC / 资质）台账
 *
 * <p>一人一条（租户内骑手唯一）。自有员工走企业自审，外部平台配送员由渠道方背书，
 * 系统只记录背书结果与有效期，不重复采集原始证件影像（敏感信息最小化）。</p>
 *
 * <p>本表是审核留痕的唯一事实来源，审核通过/驳回后回写 {@code dms_rider.verify_status}。</p>
 */
@Data
@TableName("dms_rider_verification")
public class DmsRiderVerification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 配送员ID（dms_rider.id） */
    private Long riderId;

    /** 配送员姓名快照 */
    private String riderName;

    /** 配送员类型：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机 */
    private Integer riderType;

    /** 外部平台渠道ID */
    private Long channelId;

    /** 渠道名称快照 */
    private String channelName;

    /** 证件姓名（实名核验通过后的真实姓名） */
    private String realName;

    /** 身份证号（脱敏存储：前 3 后 4，不落原文） */
    private String idCardNo;

    /** 身份证件照 URL（JSON：{"front":"...","back":"..."}） */
    private String idCardUrls;

    /** 背书渠道 / 背景审查机构 */
    private String endorseOrg;

    /** 背书 / 背景审查结论：1-通过 0-未通过 */
    private Integer endorseResult;

    /** 背书 / 资质有效期 */
    private LocalDate endorseExpireDate;

    /** 认证状态：0-待提交 1-待审核 2-已通过 3-已驳回 4-已过期 */
    private Integer verifyStatus;

    /** 审核人 */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 生效时间（审核通过时写入，审计留痕） */
    private LocalDateTime effectiveTime;

    /** 备注 */
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

    @Version
    private Integer version;
}
