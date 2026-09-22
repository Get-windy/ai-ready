package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协议主档（列表 / 详情共用）。
 *
 * <p>{@code status} 与 {@code agreementType} 都是**枚举名**：DB 存数字码、字符串名或枚举名，
 * 接口一律以枚举名交互（前端 {@code type AgreementStatus = 'DRAFT' | ...}）。</p>
 *
 * <p>主体名与租户名由服务层批量补全（跨租户，不能靠 JOIN 时被租户条件过滤掉）。</p>
 */
@Data
public class AgreementVO {

    private Long id;

    private String agreementNo;

    /** 枚举名：PLATFORM_SERVICE / DISTRIBUTION / GOODS_FRAMEWORK / CONSUMER_PROMISE */
    private String agreementType;

    private String agreementTypeLabel;

    private String title;

    /** 枚举名：DRAFT / ACTIVE / SUSPENDED / TERMINATED */
    private String status;

    private String statusLabel;

    /** 当前生效版本 ID（未生效的草稿协议为空） */
    private Long currentVersionId;

    /** 当前生效版本号（列表里显示"第 N 版"） */
    private Integer currentVersionNo;

    private Long partyAId;
    private Long partyATenantId;
    private String partyAName;
    private String partyATenantName;

    private Long partyBId;
    private Long partyBTenantId;
    private String partyBName;
    private String partyBTenantName;

    /** 生效期（yyyy-MM-dd；空 = 长期有效） */
    private String effectiveFrom;
    private String effectiveTo;

    /** 终止留痕（㉝ 清退权） */
    private Long terminatedBy;
    private LocalDateTime terminatedAt;
    private String terminateReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 详情接口带出的当前版本（列表接口为空） */
    private AgreementVersionVO currentVersion;
}
