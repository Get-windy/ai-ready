package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 契约模板（列表行）。
 *
 * <p>{@code manageable} 是**服务端算好的**：前端据此决定"改/删"按钮是否可用 ——
 * 口径只有一处（{@code AgreementTemplateVisibility.assertManageable}），
 * 不让前端自己猜"我能不能改这个模板"。</p>
 */
@Data
public class AgreementTemplateVO {

    private Long id;

    /** PLATFORM / TENANT */
    private String scope;

    private String scopeLabel;

    /** PLATFORM 级为 0；TENANT 级为归属租户 */
    private Long tenantId;

    private String tenantName;

    private String templateName;

    private String agreementType;

    private String agreementTypeLabel;

    private String description;

    /** PENDING / APPROVED / REJECTED */
    private String legalReviewStatus;

    private String legalReviewStatusLabel;

    private Integer status;

    private String statusLabel;

    /** 当前会话能否修改/删除它（读权限与写权限不对称，见 AgreementTemplateVisibility） */
    private Boolean manageable;

    /** 不能改时的原因（直接展示） */
    private String manageableHint;

    /** 预填内容条数（列表页快速判断"是不是空模板"） */
    private Integer settingCount;

    private Integer termCount;

    private Integer narrativeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
