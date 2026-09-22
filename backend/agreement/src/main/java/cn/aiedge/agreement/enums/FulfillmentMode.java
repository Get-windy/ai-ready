package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 履约方式（{@code agreement_fulfillment_mode.mode}，DOMAIN-MODEL §13.10）。
 *
 * <p><b>⚠️ 协议上这是「集合」而不是「单选」</b>：同一份合作协议里
 * <b>同城直发 + 异地中转可以并存</b>（用户 2026-09-22 明确要求：这才是真实交易）。
 * 因此 {@code agreement_fulfillment_mode} 一版多行并存，本枚举只负责"有哪些合法取值"，
 * <b>不承担</b>"只能选一个"的语义。</p>
 *
 * <p>下单时在这个集合内确定本单具体用哪一种（订单行粒度），并记录是谁定的（规则 / 人工）——
 * 那部分属阶段 B（订单路由接线），本次只把集合本身做成可读取的运行时数据。</p>
 *
 * <p>⚠️ 命名说明：本枚举叫 {@code FulfillmentMode} 而不是 {@code AgreementFulfillmentMode}，
 * 是为了与同名的实体类 {@code cn.aiedge.agreement.entity.AgreementFulfillmentMode}（一行 = 一个方式）
 * 区分开 —— 两者放同一个包会撞名，放不同包又会让 {@code import} 变成猜谜。</p>
 */
public enum FulfillmentMode {

    /** 直发：货从供货方直接发给最终客户，中间方不碰货（S7 代销/一件代发）。 */
    DROP_SHIP("直发"),

    /** 中转：货先经过中间方的仓，再发给客户（S9 经销中转，两段运输）。 */
    TRANSIT_STOCK("中转"),

    /** 自提：客户到指定地点自取。 */
    PICKUP("自提"),

    /** 自有库存：由销售方用自己的库存交付（S8 就近履约 / 本地仓）。 */
    LOCAL_STOCK("自有库存");

    private final String label;

    FulfillmentMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 全部合法取值（用于字典展示与"集合是否为空"的判断）。 */
    public static Set<FulfillmentMode> all() {
        return new LinkedHashSet<>(java.util.List.of(values()));
    }

    public static FulfillmentMode of(String name) {
        if (name == null) {
            return null;
        }
        String n = name.trim();
        for (FulfillmentMode m : values()) {
            if (m.name().equalsIgnoreCase(n)) {
                return m;
            }
        }
        return null;
    }

    public static String labelOf(String name) {
        FulfillmentMode m = of(name);
        return m == null ? (name == null ? "" : name) : m.label;
    }

    /**
     * 解析前端传入的履约方式名（大小写不敏感）。
     *
     * @throws BusinessException 为空或不在枚举内（中文文案，方便界面直接提示）
     */
    public static FulfillmentMode parse(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("请选择履约方式");
        }
        FulfillmentMode m = of(name);
        if (m == null) {
            throw BusinessException.badRequest("履约方式「" + name
                    + "」不支持；可选：DROP_SHIP（直发）/ TRANSIT_STOCK（中转）/ PICKUP（自提）/ LOCAL_STOCK（自有库存）");
        }
        return m;
    }
}
