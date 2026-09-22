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
 * 契约模板主档（{@code agreement_template}，§13.9）。
 *
 * <h3>两级别</h3>
 * <ul>
 *   <li>{@code PLATFORM} 平台模板：全员可选，{@code tenantId = 0}（系统级归属位）；</li>
 *   <li>{@code TENANT} 租户模板：本租户内可选，{@code tenantId = 本租户}。</li>
 * </ul>
 *
 * <h3>读权限口径（用户 2026-09-22 明确要求）</h3>
 * <b>平台拥有合规抽查读权限</b>（目的：避免非法交易）⇒ 平台可读<b>所有</b>模板（含租户模板）；
 * 租户只能读自己的 + 平台模板。条件收敛在
 * {@code cn.aiedge.agreement.domain.AgreementTemplateVisibility}（唯一构造处），
 * 不许在 Service 里各写一遍。</p>
 *
 * <h3>⚠️⚠️ 模板**不是默认值**（这是㉜的防线，改代码前先读这段）</h3>
 * 模板是**显式选择的起点**：模板项**不写入** {@code agreement_setting} 的"已约定"状态，
 * 只在"从模板发起"时作**预填**；{@code required = true} 的条款仍必须由双方在
 * **本次这一版**上显式确认（双签针对的是那一版的内容）。
 * <b>{@code AgreementRuntime} 绝不读模板</b> —— 它只认"双方签署的那一版"。
 * "模板里有 ⇒ 视为已约定"是**绝不允许**的（否则等于平台替双方定商业条款）。
 *
 * <p>模板本身**同样过合法审核**（{@code legalReviewStatus}，㊲）：违法条款无效，
 * 与条款字典的 {@code agreement_term_option.legal_review_status} 同一口径。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_template")
public class AgreementTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** PLATFORM 级恒为 0；TENANT 级记本租户（见类注释）。 */
    private Long tenantId;

    /** PLATFORM 平台模板 / TENANT 租户模板 */
    private String scope;

    private String templateName;

    /** 适用协议类型：PLATFORM_SERVICE / DISTRIBUTION / GOODS_FRAMEWORK / CONSUMER_PROMISE */
    private String agreementType;

    /** 模板用途说明（给选用的人看：这份模板适合什么场景） */
    private String description;

    /** 法务审核：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法（REJECTED 不许被用于发起） */
    private String legalReviewStatus;

    /** 1=启用 / 0=停用（停用只影响以后发起，已发起的协议不受影响） */
    private Integer status;

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
