package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文字条款（下发前端）。
 *
 * <h3>⚠️ {@code autoExecutable} / {@code manualNotice} 是**硬要求**，不许省</h3>
 * §13.2 规定：文字条款"永不自动执行"，界面必须**显式告知**"此类条款系统不会自动执行，需人工处理"。
 * 否则用户会以为写了「逾期按日万分之五」系统就会自动扣钱，于是既不自己主张、也错过时效。
 *
 * <p>因此这两个字段由服务端下发（不是前端写死文案）：口径只有一处，
 * 将来若真做了"半自动"的能力，改这里就够，前端不用跟着改。</p>
 */
@Data
public class AgreementNarrativeVO {

    private Long id;

    /** DISPUTE / CONFIDENTIALITY / FORCE_MAJEURE / SPECIAL_TERMS / BREACH_LIABILITY_TEXT */
    private String sectionCode;

    private String sectionTitle;

    private String contentText;

    /** 正文哈希（SHA-256）：证明"双方确认之后这段文字没被改过" */
    private String contentHash;

    /** ⚠️ 恒为 false：文字条款系统不会自动执行 */
    private Boolean autoExecutable;

    /** 与 autoExecutable 配套的中文说明，前端直接展示 */
    private String manualNotice;

    private Long partyAConfirmedBy;

    private LocalDateTime partyAConfirmedAt;

    private Long partyBConfirmedBy;

    private LocalDateTime partyBConfirmedAt;

    private Integer sort;
}
