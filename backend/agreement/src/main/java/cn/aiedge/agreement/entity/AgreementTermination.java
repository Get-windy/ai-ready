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
 * 终止留痕（{@code agreement_termination}）—— §13.7。
 *
 * <h3>⚠️⚠️ 终止 ≠ 免责（用户原话强调）</h3>
 * 单方终止**只记录「停止履行」这个事实**（{@link #stopPerformanceAt}）；
 * 系统<b>不自动结清、不自动免责</b>。是否违约、赔多少走 §13.8（平台不裁判）。
 *
 * <p>这条不只是注释：本实体**刻意没有任何**"责任已了结 / 已结清 / 已免责"语义的字段，
 * 迁移 {@code V11.490.0} 的 {@code DO $$} 在真库上对列名集合做黑名单断言
 * （命中 {@code settle|clear|liabilit|exempt|disclaim|release|indemn|...} 即整体回滚），
 * 后人想"顺手加个 settled_at"会在迁移阶段就被拦下。</p>
 *
 * <h3>五种来源（㉝，{@code AgreementTerminationSource}）</h3>
 * 协商一致 / 自然到期 / 单方终止 / 因对方违约 / 平台清退。
 *
 * <h3>状态推进（{@code AgreementTerminationStatus}）</h3>
 * <pre>
 *   协商一致：PENDING --对方确认--> CONFIRMED ；--对方异议--> OBJECTED（协议不终止）；--发起方撤回--> WITHDRAWN
 *   其余四种：发起即 CONFIRMED（"停止履行"已是事实，系统不假装它没发生），
 *             对方的异议作为留痕 recorded 在 counterpartyObjection/objectionReason，不回滚终止。
 * </pre>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_termination")
public class AgreementTermination {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统级归属位，恒为 0。 */
    private Long tenantId;

    private Long agreementId;

    /** 终止发生时正在执行的那一版（"依据哪一版的约定终止"）；草稿协议尚无生效版本时为空 */
    private Long versionId;

    /** 五种来源，见 {@code AgreementTerminationSource} */
    private String source;

    /** {@code AgreementTerminationStatus}：0=待对方确认 / 1=已终止 / 2=对方有异议 / 3=已撤回 */
    private Integer status;

    /** 谁、何时、代表哪一端发起 */
    private Long requestedBy;
    private String requestedSide;
    private LocalDateTime requestedAt;

    /** 依据：引用协议条款 / 法定情形。**必填** —— 凭什么终止必须说得出来。 */
    private String basisText;

    /** 是否**主张**对方违约。⚠️ 当事人的主张，不是平台的认定（§13.8）。 */
    private Boolean claimCounterpartyBreach;

    private String breachNote;

    /** 「停止履行」这个事实的生效时刻（⚠️ 只是事实：不结清、不免责、不回写既有单据） */
    private LocalDateTime stopPerformanceAt;

    /** 本条终止记录生效的时刻（协商一致 = 对方确认时刻；其余 = 发起时刻） */
    private LocalDateTime effectiveAt;

    /** 对方表态留痕 */
    private Long counterpartyActionBy;
    private LocalDateTime counterpartyActionAt;
    private Boolean counterpartyObjection;
    private String objectionReason;

    /** 撤回留痕（发起方在对方确认前可撤回） */
    private Long withdrawnBy;
    private LocalDateTime withdrawnAt;
    private String withdrawReason;

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
