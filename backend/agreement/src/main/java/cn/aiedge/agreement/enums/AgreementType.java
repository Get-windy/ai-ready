package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 协议类型（§十二 12.1 表格，裁定①：**一张主档 + 一个类型列**承载三类协议）。
 *
 * <p>为什么不分表：分表会让「版本不可变」「双签缺一不可」「必填条款校验」三套不变量
 * 各写一遍，而这三条恰恰是最贵、最不能各写一遍的东西。</p>
 */
public enum AgreementType {

    /** 平台 ↔ 租户（入驻 / 服务协议）。甲方固定为平台主体 + 系统租户 1。 */
    PLATFORM_SERVICE("平台服务协议"),

    /** 租户 ↔ 租户：一方供货、另一方代销（佣金 / 结算 / 退货责任）。 */
    DISTRIBUTION("代销协议"),

    /** 租户 ↔ 租户：长期按框架购销（价格口径 / 账期 / 交付 / 质量责任）。 */
    GOODS_FRAMEWORK("购销框架协议"),

    /** 租户 → 不特定消费者：单方公开承诺，**只能加码不能缩水**（§3.4.4d3）。 */
    CONSUMER_PROMISE("消费者单方承诺");

    private final String label;

    AgreementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 乙方是否为「不特定消费者」（此类协议 partyBId / partyBTenantId 允许为空）。 */
    public boolean isConsumerFacing() {
        return this == CONSUMER_PROMISE;
    }

    /** 本类型属于哪个协议范围（列表页两个入口的筛选口径，见 {@link AgreementScope}）。 */
    public AgreementScope scope() {
        return this == PLATFORM_SERVICE ? AgreementScope.PLATFORM : AgreementScope.TENANT;
    }

    /**
     * 解析前端传入的类型名（**大小写不敏感**，前端传枚举名而不是数字码）。
     *
     * <p>校验只此一处：服务层建协议（{@code create}）与列表过滤（{@code AgreementListConditions}）
     * 都调它，避免两处各写一份文案、日后改一处漏一处。</p>
     *
     * @throws BusinessException 为空或取值不在枚举内（中文文案；不用 RuntimeException，
     *                           否则会被兜底 advice 吞成 HTTP 500「系统异常」）
     */
    public static AgreementType parse(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("请选择协议类型");
        }
        String n = name.trim();
        for (AgreementType t : values()) {
            if (t.name().equalsIgnoreCase(n)) {
                return t;
            }
        }
        throw BusinessException.badRequest("协议类型「" + name + "」不支持；可选：PLATFORM_SERVICE / DISTRIBUTION "
                + "/ GOODS_FRAMEWORK / CONSUMER_PROMISE");
    }
}
