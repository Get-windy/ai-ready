package cn.aiedge.agreement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 签署留痕（{@code agreement_signature}）—— §13.6「**自然人代表主体**」。
 *
 * <h3>一条签署记录必须回答的七件事</h3>
 * <pre>
 *   谁（自然人）      signerUserId + signerPersonId + signerName
 *   代表哪个主体      partyId
 *   在哪个租户        signerPartyTenantId
 *   凭什么代表        authorityBasis（授权依据文本）+ authorityEvidenceNo（外部凭证号）
 *   签了哪一版        versionId
 *   何时              signedAt
 *   签章哈希          signHash = SHA-256(该版快照正文)
 * </pre>
 *
 * <p><b>⚠️ 只记 tenant_id 或账号 ID 是不够的</b>（§13.6 原话）：司法上要回答的是
 * "谁签的、凭什么代表这家公司"。所以 {@code partyId} / {@code signerUserId} /
 * {@code signerPartyTenantId} / {@code authorityBasis} 四列在 DB 里都是 <b>NOT NULL</b>，
 * 迁移末尾的自检会在真库上断言这一点。</p>
 *
 * <h3>与"确认（confirm）"的关系</h3>
 * 签署是**更强的动作**：{@code AgreementLifecycleServiceImpl#sign} 先复用既有的
 * "本方确认"链路（同一套 {@code partyXConfirmedBy/At/SignHash} 痕迹），再额外落一条本表的记录。
 * 因此**不存在两套互相打架的确认机制**，库里的双签痕迹仍是激活的唯一依据。
 *
 * <h3>为什么 (version_id, party_side) 上没有唯一索引</h3>
 * 内容被改后原确认痕迹会被清空，必须**重新签署**，同一版同一方就会有多条记录 ——
 * 每条都是当时的留痕。"当前有效的那一条"由 {@link #signHash} 与当前快照哈希是否一致判定
 * （见 {@code AgreementSignatureRules#effectiveSignature}）。
 */
@Data
@Accessors(chain = true)
@TableName("agreement_signature")
public class AgreementSignature {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，恒为 0。 */
    private Long tenantId;

    private Long agreementId;

    /** 签了哪一版 */
    private Long versionId;

    /** 代表协议的哪一端：A / B */
    private String partySide;

    /** 代表的主体（不可空）—— 司法上回答"代表哪个主体签的" */
    private Long partyId;

    /** 在哪个租户签的（不可空）—— 主体是系统级，租户只是"哪一端的店" */
    private Long signerPartyTenantId;

    /** 签署的自然人账号（不可空） */
    private Long signerUserId;

    /** 自然人主体 ID（R1 自然人×往来单位，通用化属文档 §5.2 阶段 2，本期可空） */
    private Long signerPersonId;

    /** 签署人姓名（留痕，人读） */
    private String signerName;

    /** 凭什么代表（不可空）：授权依据文本，如"法定代表人本人"/"授权委托书（含授权范围与限额）" */
    private String authorityBasis;

    /** 授权凭证号（外部单据编号，如授权委托书编号）；可空但填了就多一份举证 */
    private String authorityEvidenceNo;

    /** 签章哈希 = SHA-256(该版快照正文)，与裁定③ 的双签哈希同口径 */
    private String signHash;

    private LocalDateTime signedAt;

    /** 签署渠道（留痕用） */
    private String signChannel;

    /** 签署类型：VERSION 版本签署（本期唯一取值；第三方电子签章只留列位不接，见裁定③） */
    private String signatureType;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
