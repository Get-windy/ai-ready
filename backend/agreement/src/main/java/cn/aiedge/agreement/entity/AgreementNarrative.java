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
 * 文字版（{@code agreement_narrative}）：一行 = 某一版的一段文字条款。
 *
 * <h3>⚠️ 文字版**只留痕举证，系统永不自动执行**（§13.1 / §13.2）</h3>
 * 争议解决与管辖、保密、不可抗力、特别约定、违约责任的具体文字表述 ——
 * <b>没有消费方</b>，不解析、不自动执行，只做双方确认 + 哈希留痕，供举证。
 * 接口返回里必须带显式标记（{@code autoExecutable = false} + 一句中文说明），
 * 让前端能明确告诉用户"此类条款系统不会自动执行，需人工处理"（§13.2 硬要求）。
 *
 * <h3>为什么要存 {@code contentText} 与 {@code contentHash} 两份</h3>
 * {@code contentText} 是给人看的举证内容；{@code contentHash} 是给机器核的 ——
 * 双方确认的是"这一段文字"，确认之后若正文被改，哈希对不上，
 * 就能证明"确认的内容被换过"（与 {@code agreement_version} 的 sign_hash 同一手法）。
 * 哈希口径：SHA-256(段落正文)。
 */
@Data
@Accessors(chain = true)
@TableName("agreement_narrative")
public class AgreementNarrative {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，恒为 0。 */
    private Long tenantId;

    private Long agreementId;

    /** 文字条款挂在**版本**上，与设定版同口径（判"当时写的是什么"靠版本 + 本表）。 */
    private Long versionId;

    /** 段落类别：DISPUTE / CONFIDENTIALITY / FORCE_MAJEURE / SPECIAL_TERMS / BREACH_LIABILITY_TEXT */
    private String sectionCode;

    /** 段落名（冗余存下当时的名称，字典日后调整不影响历史版本的展示与举证） */
    private String sectionTitle;

    private String contentText;

    /** 正文哈希（SHA-256），用于证明"确认之后没被改过" */
    private String contentHash;

    /** 甲方确认痕迹（确认人 + 时间），与乙方**分开存**；两者都非空才算这一段双方都认过 */
    private Long partyAConfirmedBy;
    private LocalDateTime partyAConfirmedAt;

    /** 乙方确认痕迹 */
    private Long partyBConfirmedBy;
    private LocalDateTime partyBConfirmedAt;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
