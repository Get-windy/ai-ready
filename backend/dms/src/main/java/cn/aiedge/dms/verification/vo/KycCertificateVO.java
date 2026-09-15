package cn.aiedge.dms.verification.vo;

import cn.aiedge.dms.verification.entity.DmsRiderCertificate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 证照核验行（证照 + 持证人快照 + 到期派生字段）
 *
 * <p>「人员核验 → 证照核验」Tab 的行结构：运营每天要看的是**到期清单**，
 * 因此按证照维度（而非台账维度）平铺，并给出剩余天数与告警色。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KycCertificateVO extends DmsRiderCertificate {

    /** 持证人姓名 */
    private String riderName;

    /** 持证人手机号 */
    private String riderPhone;

    /** 证照类型文本 */
    private String certTypeText;

    /** 证照状态文本 */
    private String verifyStatusText;

    /** 距到期天数（负数=已过期；无有效期时为 null） */
    private Long daysToExpire;

    /** 是否到期预警（已过期 或 在提醒期内） */
    private Boolean expiring;
}
