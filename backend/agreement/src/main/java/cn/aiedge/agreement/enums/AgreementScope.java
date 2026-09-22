package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 协议范围（列表页两个入口的筛选口径，§十二 12.1 的三类划分收敛成「平台级 / 租户级」两档）。
 *
 * <p>为什么需要它：{@code agreement_type} 有四类，但菜单只有两个入口
 * （「设置 → 协议列表」看三类租户级、「系统 → 平台协议」只看平台服务协议）。
 * 只按**单类型等值**过滤表达不了「排除平台服务协议」，
 * 前端若取回后本地剔除，分页 {@code total} 会仍是服务端口径 ⇒ 显示行数与「共 N 条」对不上。
 * 因此把范围判定放进服务端 SQL 条件（{@link cn.aiedge.agreement.domain.AgreementListConditions}），
 * 由分页拦截器按同一份条件算 count，两者天然一致。</p>
 *
 * <p><b>不传</b>（{@code null} / 空串）时<b>不追加任何类型条件</b> —— 与本次扩展之前的行为逐字一致，
 * 保证既有调用方零感知。</p>
 */
public enum AgreementScope {

    /** 租户级：代销 / 购销框架 / 消费者单方承诺（即 {@code agreement_type <> 'PLATFORM_SERVICE'}）。 */
    TENANT("租户级协议", "代销 / 购销框架 / 消费者单方承诺"),

    /** 平台级：平台 ↔ 租户的服务协议（即 {@code agreement_type = 'PLATFORM_SERVICE'}）。 */
    PLATFORM("平台级协议", "平台服务协议");

    private final String label;

    private final String description;

    AgreementScope(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** 该类型是否属于本范围（PLATFORM_SERVICE 是唯一的平台级类型，见 §十二 12.1）。 */
    public boolean contains(AgreementType type) {
        if (type == null) {
            return false;
        }
        return (this == PLATFORM) == (type == AgreementType.PLATFORM_SERVICE);
    }

    /**
     * 解析前端传入的范围名（**大小写不敏感**，与 {@code agreementType} 的既有口径一致）。
     *
     * @param value 前端传值；为 {@code null} / 空白表示"不限制范围"，返回 {@code null}
     * @throws BusinessException 取值不在枚举内（配中文文案，禁用 RuntimeException：
     *                           后者会被兜底 advice 吞成 HTTP 500「系统异常」，把用户可自解的问题误导成服务故障）
     */
    public static AgreementScope parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        for (AgreementScope s : values()) {
            if (s.name().equalsIgnoreCase(v)) {
                return s;
            }
        }
        throw BusinessException.badRequest("协议范围「" + value + "」不支持；可选：TENANT（租户级）"
                + " / PLATFORM（平台级）");
    }
}
