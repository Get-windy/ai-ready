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
 * 协议版本（一行 = 一次谈定的条款快照）。
 *
 * <p><b>不变量 1（已生效版本只读）</b>：{@code status = 1} 之后
 * {@code snapshotJson} <b>禁止再写</b>；改协议的唯一合法路径是新建 DRAFT 版本（㉛ / §3.4.4d2 规定 1）。
 * 该约束由服务层硬校验 + 本类<b>永不参与全字段 UPDATE</b> 共同保证
 * （见 {@code AgreementVersionMapper} 的注释：插入必须走显式 SQL 以 CAST jsonb）。</p>
 *
 * <p><b>不变量 2（双签缺一不可）</b>：{@code partyAConfirmedAt/SignHash} 与
 * {@code partyBConfirmedAt/SignHash} <b>分开存</b>，两组都非空才允许置 ACTIVE。
 * 这是"不能单方改协议"的**技术**保证，不是流程约定。</p>
 */
@Data
@Accessors(chain = true)
@TableName("agreement_version")
public class AgreementVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long agreementId;

    /** 版本号，同一协议内不重复（1 起） */
    private Integer versionNo;

    /** 完整条款快照（JSONB）。⚠️ 一旦 ACTIVE 永久不可改。 */
    private String snapshotJson;

    /** 0=DRAFT 待对方确认 / 1=ACTIVE 生效 / 2=SUPERSEDED 已被新版取代 / 3=REJECTED 被否决 */
    private Integer status;

    /** 甲方确认痕迹（确认人 + 时间 + 内容哈希），**与乙方分开存** */
    private Long partyAConfirmedBy;
    private LocalDateTime partyAConfirmedAt;
    private String partyASignHash;

    /** 乙方确认痕迹 */
    private Long partyBConfirmedBy;
    private LocalDateTime partyBConfirmedAt;
    private String partyBSignHash;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    /** 变更原因（"为什么有这一版"；首次签订为空） */
    private String changeReason;

    // ══════════════════ 协商时间线（§13.4） ══════════════════
    // 一串 DRAFT 版本 = 协商时间线：**修改 = 反要约 = 新建一个 DRAFT 版本**。
    // 因此"这一版是谁提的、提的时候说了什么"必须落在版本行上，否则多轮协商无从回溯。

    /** 这一版是谁提的（要约方）：A / B / NONE（{@code AgreementPartySide}） */
    private String proposedBySide;

    /** 提出这一版的操作人（登录用户 ID） */
    private Long proposedByPerson;

    /** 协商留言（反要约时说明"改了什么、为什么改"）；与 change_reason 区分：后者是变更单的正式原因 */
    private String proposalNote;

    // ══════════════════ 变更单语义（§13.7 阶段修改） ══════════════════

    /** 变更单关联的**原版本**（本版从哪一版改出来的）；首次签订为空 */
    private Long originVersionId;

    /**
     * 显式声明：本次变更**不追溯**变更前已发生的单据与结算（第三条通用原则）。
     *
     * <p>写进版本行而不是只写在流程说明里 —— 这条声明要随版本一起被双方看到、一起被举证。</p>
     */
    private String noRetroactiveNote;

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
