package cn.aiedge.dms.verification.vo;

import cn.aiedge.dms.verification.entity.DmsRiderCertificate;
import cn.aiedge.dms.verification.entity.DmsRiderVerification;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 实名认证台账行（台账 + 证照 + 派生字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KycVO extends DmsRiderVerification {

    /** 配送员类型文本 */
    private String riderTypeText;

    /** 认证状态文本 */
    private String verifyStatusText;

    /** 是否具备接单资质（唯一放行口径：认证已通过且证照/背书未过期） */
    private Boolean eligible;

    /** 不具备资质的原因（逗号分隔） */
    private String ineligibleReason;

    /** 手机号（来自 dms_rider 快照） */
    private String riderPhone;

    /** 证照明细 */
    private List<DmsRiderCertificate> certificates;

    /** 证照总数 */
    private Integer certCount;

    /** 到期预警数（N 天内到期或已过期） */
    private Integer expiringCertCount;
}
