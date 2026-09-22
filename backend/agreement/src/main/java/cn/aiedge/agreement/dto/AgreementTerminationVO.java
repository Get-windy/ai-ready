package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终止记录的下发形态（§13.7）。
 *
 * <p>⚠️ 本 VO **刻意没有**任何"责任是否了结 / 是否结清 / 是否免责"的字段 ——
 * 与实体、表结构同口径（迁移 {@code V11.490.0} 的 DO $$ 用列名黑名单在真库上反证）。
 * {@link #exemptionNotice} 会把这条口径直接告诉用户，避免有人以为"终止了这笔账就算了"。</p>
 */
@Data
public class AgreementTerminationVO {

    private Long id;

    private Long agreementId;

    private String agreementNo;

    private String agreementTitle;

    /** 终止时正在执行的那一版 */
    private Long versionId;

    private Integer versionNo;

    /** 五种来源（枚举名 + 中文） */
    private String source;
    private String sourceLabel;

    /** 0=待对方确认 / 1=已终止 / 2=对方有异议 / 3=已撤回 */
    private Integer status;
    private String statusLabel;

    private Long requestedBy;
    private String requestedSide;
    private String requestedSideLabel;
    private LocalDateTime requestedAt;

    private String basisText;

    /** 是否**主张**对方违约（当事人的主张，不是平台的认定） */
    private Boolean claimCounterpartyBreach;
    private String breachNote;

    /** 「停止履行」的生效时刻 —— 本模块记的就是这个**事实** */
    private LocalDateTime stopPerformanceAt;

    private LocalDateTime effectiveAt;

    private Long counterpartyActionBy;
    private LocalDateTime counterpartyActionAt;
    private Boolean counterpartyObjection;
    private String objectionReason;

    private Long withdrawnBy;
    private LocalDateTime withdrawnAt;
    private String withdrawReason;

    private LocalDateTime createdAt;

    /**
     * 「终止 ≠ 免责」的固定提示（界面必须显示）。
     *
     * <p>用户原话强调过这一条：单方终止只记录"停止履行"这个事实，
     * 系统不自动结清、不自动免责。</p>
     */
    private String exemptionNotice;

    /** 当前会话能否对这条记录表态（确认/异议），以及是谁的表态 —— 由服务端判定，前端不用猜。 */
    private Boolean canConfirm;
    private Boolean canObject;
    private Boolean canWithdraw;
}
