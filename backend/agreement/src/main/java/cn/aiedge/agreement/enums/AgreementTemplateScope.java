package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 契约模板的两级（{@code agreement_template.scope}，DOMAIN-MODEL §13.9）。
 *
 * <ul>
 *   <li>{@link #PLATFORM} 平台模板：全员可选；{@code tenant_id} 记 <b>0</b>（系统级归属位）。</li>
 *   <li>{@link #TENANT} 租户模板：只有本租户内可选；{@code tenant_id} 记<b>本租户</b>。</li>
 * </ul>
 *
 * <p><b>⚠️ 模板是"显式选择的起点"，不是"自动套用的默认值"</b>（§13.9）：
 * 模板项不写入 {@code agreement_setting} 的"已约定"状态，只作发起时预填；
 * {@code AgreementRuntime} <b>绝不读模板</b>。理由见 {@code AgreementRuntime} 类注释。</p>
 */
public enum AgreementTemplateScope {

    /** 平台模板（全员可选）。tenant_id = 0。 */
    PLATFORM("平台模板"),

    /** 租户模板（本租户内可选）。tenant_id = 本租户。 */
    TENANT("租户模板");

    private final String label;

    AgreementTemplateScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementTemplateScope of(String name) {
        if (name == null) {
            return null;
        }
        String n = name.trim();
        for (AgreementTemplateScope s : values()) {
            if (s.name().equalsIgnoreCase(n)) {
                return s;
            }
        }
        return null;
    }

    public static AgreementTemplateScope parse(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("请选择模板级别");
        }
        AgreementTemplateScope s = of(name);
        if (s == null) {
            throw BusinessException.badRequest("模板级别「" + name + "」不支持；可选：PLATFORM（平台模板）/ TENANT（租户模板）");
        }
        return s;
    }
}
