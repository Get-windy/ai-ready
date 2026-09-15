package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 骑手证照明细
 *
 * <p>驾驶证 / 行驶证 / 健康证 / 从业资格证 / 其他，随实名认证台账一并提交，
 * 支持有效期到期提醒（见 {@code VerificationScheduler}）。</p>
 */
@Data
@TableName("dms_rider_certificate")
public class DmsRiderCertificate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 实名认证台账ID（dms_rider_verification.id） */
    private Long verificationId;

    /** 配送员ID */
    private Long riderId;

    /** 证照类型：1-驾驶证 2-行驶证 3-健康证 4-从业资格证 5-其他 */
    private Integer certType;

    /** 证照编号 */
    private String certNo;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 有效期至 */
    private LocalDate expireDate;

    /** 证照影像 URL */
    private String certUrl;

    /** 证照状态：0-待核验 1-有效 2-已过期 3-无效 */
    private Integer verifyStatus;

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
