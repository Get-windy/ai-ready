package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 协议列表查询条件（契约：{@code GET /api/agreement/page?current=&size=&agreementScope=&agreementType=&status=&keyword=}）。
 *
 * <p>{@code agreementScope} / {@code agreementType} / {@code status} 都收**枚举名**
 * （如 {@code TENANT} / {@code DISTRIBUTION} / {@code DRAFT}），与前端交互一致；转数字码在服务层完成。</p>
 *
 * <p>{@code agreementScope} 是<b>向后兼容的新增可选参数</b>（两个入口共用一份列表页：
 * 「协议列表」= {@code TENANT}、「平台协议」= {@code PLATFORM}）：
 * {@code PLATFORM} ⇒ 只查 {@code agreement_type = 'PLATFORM_SERVICE'}；
 * {@code TENANT} ⇒ 查 {@code agreement_type <> 'PLATFORM_SERVICE'}；<b>不传 ⇒ 不加任何类型条件</b>。
 * 与 {@code agreementType} 同时传时以 {@code agreementType} 为准（理由见
 * {@code cn.aiedge.agreement.domain.AgreementListConditions} 类注释）。</p>
 */
@Data
public class AgreementQuery {

    private Long current = 1L;

    private Long size = 20L;

    /** 协议范围枚举名：TENANT（租户级，排除平台服务协议）/ PLATFORM（平台级，仅平台服务协议）；可空 */
    private String agreementScope;

    /** 协议类型枚举名 */
    private String agreementType;

    /** 主档状态枚举名 */
    private String status;

    /** 关键字：协议编号 / 标题 */
    private String keyword;
}
