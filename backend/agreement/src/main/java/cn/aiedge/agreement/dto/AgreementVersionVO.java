package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 协议版本（下发形态）。
 *
 * <p>{@code status} 是**枚举名**（DRAFT / ACTIVE / SUPERSEDED / REJECTED），
 * DB 里存的是数字码 —— 前端按名字判断（{@code status === 'DRAFT'}）。</p>
 *
 * <p>{@code snapshotJson} 原样下发（不做美化）：它是取证材料，
 * 前端"查看快照"弹窗直接展示原文；{@code terms} 是同一份快照解析后的明细，便于渲染。</p>
 */
@Data
public class AgreementVersionVO {

    private Long id;

    private Long agreementId;

    private Integer versionNo;

    /** 枚举名：DRAFT / ACTIVE / SUPERSEDED / REJECTED */
    private String status;

    /** 状态中文名 */
    private String statusLabel;

    /** 变更原因（"为什么有这一版"；首次签订为空） */
    private String changeReason;

    /** 这一版是谁提的（要约方）：A / B / NONE（协商时间线，§13.4） */
    private String proposedBySide;

    /** 提案方中文名：甲方 / 乙方 / 非缔约方 */
    private String proposedBySideLabel;

    /** 提出这一版的操作人（登录用户 ID） */
    private Long proposedByPerson;

    /** 协商留言（反要约时说明"改了什么、为什么改"） */
    private String proposalNote;

    /** 变更单关联的**原版本**（本版从哪一版改出来的）；首次签订为空 */
    private Long originVersionId;

    /** 显式声明：本变更**不追溯**已发生的单据与结算（§7 阶段修改的不追溯口径） */
    private String noRetroactiveNote;

    private Long partyAConfirmedBy;
    private LocalDateTime partyAConfirmedAt;

    private Long partyBConfirmedBy;
    private LocalDateTime partyBConfirmedAt;

    /** 完整条款快照原文（不可变，司法举证用） */
    private String snapshotJson;

    private String effectiveFrom;
    private String effectiveTo;

    private Long createdBy;
    private LocalDateTime createdAt;

    private LocalDateTime updateTime;

    /** 该版本的条款明细（由快照解析得出，与 snapshotJson 同源） */
    private List<AgreementTermVO> terms;
}
