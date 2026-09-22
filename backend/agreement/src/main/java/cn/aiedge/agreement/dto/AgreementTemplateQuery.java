package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 契约模板列表查询条件。
 *
 * <p><b>可见性不在这里，也不接受前端传</b>：由会话租户 + 是否平台侧在服务端判定
 * （{@code AgreementTemplateVisibility}）—— 前端若能传"看谁的模板"，
 * 就等于把跨租户读取的开关交给了客户端。</p>
 */
@Data
public class AgreementTemplateQuery {

    private Long current;

    private Long size;

    /** PLATFORM 平台模板 / TENANT 租户模板；不传 = 不限 */
    private String scope;

    /** 适用协议类型；不传 = 不限 */
    private String agreementType;

    /** 模板名 / 说明 里的关键字 */
    private String keyword;

    /** 1=启用 / 0=停用；不传 = 不限（停用模板在列表里仍可见，只是不能用于发起） */
    private Integer status;
}
