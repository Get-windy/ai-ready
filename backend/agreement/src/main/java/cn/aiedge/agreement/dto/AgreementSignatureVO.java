package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签署记录的下发形态（§13.6）。
 *
 * <p>字段逐条对应"司法上要回答的问题"：谁签的（{@code signerUserId/PersonId/Name}）·
 * 代表哪个主体（{@code partyId/partyName}）· 在哪个租户（{@code signerPartyTenantId/Name}）·
 * 凭什么代表（{@code authorityBasis/EvidenceNo}）· 签了哪一版（{@code versionId/versionNo}）·
 * 何时（{@code signedAt}）· 签章哈希（{@code signHash}）。</p>
 */
@Data
public class AgreementSignatureVO {

    private Long id;

    private Long agreementId;

    private Long versionId;

    private Integer versionNo;

    /** A / B */
    private String partySide;

    private String partySideLabel;

    private Long partyId;
    private String partyName;

    private Long signerPartyTenantId;
    private String signerPartyTenantName;

    private Long signerUserId;
    private Long signerPersonId;
    private String signerName;

    private String authorityBasis;
    private String authorityEvidenceNo;

    private String signHash;

    private LocalDateTime signedAt;

    private String signChannel;

    private String signatureType;

    private String remark;

    /** 这条签署对**当前**快照是否仍然有效（哈希一致）。内容被改过则为 false，需重新签署。 */
    private Boolean effective;

    /** 有效性的人读说明（无效时告诉用户"需要重新签署"） */
    private String effectiveNote;
}
