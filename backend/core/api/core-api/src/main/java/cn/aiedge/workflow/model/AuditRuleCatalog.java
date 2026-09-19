package cn.aiedge.workflow.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审核设置（设置 → 系统配置 → 审核设置，菜单 80622）的静态目录。
 *
 * <p>两目录都取自 ql361「审核设置」页实测（2026-09-18，证据
 * {@code tool-results/ql361/设置-live/审核设置.json} + {@code .png}），**逐字照录、不增不减**：
 * <ul>
 *   <li>{@link #DOC_TYPES}：16 类单据（行序即 ql361 的页面行序，用 LinkedHashMap 固定）；</li>
 *   <li>{@link #CONDITIONS}：7 类审核条件（摘要文案里的「…时提交给[X]审核」的「…」部分）。</li>
 * </ul>
 *
 * <p>为什么不落库：这两份是**枚举常量**而非业务数据（ql361 也是固定 16 行），落库反而会
 * 引入「新租户没有种子就看不到行」的运维负担；只把「租户配置了什么规则」落
 * {@code sys_audit_rule}，未配置的单据在接口里以「摘要为空」返回（与 ql361 实测一致：
 * 16 行中仅 3 行有摘要）。
 *
 * <p>⚠️ 未证实项（如实登记）：ql361 各单据**各自**适用哪些条件，实测只拿到 3 张单据的证据
 * （销售出库单 6 条 / 销售订单 6 条 / 调拨单 1 条），其余 13 类单据的条件子集**无证据**。
 * 因此本系统**不做按单据裁剪条件**，7 类条件对 16 类单据统一可选 —— 需要时由用户按需勾选，
 * 不替用户预设。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public final class AuditRuleCatalog {

    private AuditRuleCatalog() {
    }

    /** 单据类型：编码 → 中文名（中文名逐字取自 ql361 实测；编码为技术键，不落展示） */
    public static final Map<String, String> DOC_TYPES;

    /** 审核条件：编码 → 中文描述（中文描述逐字取自 ql361 摘要文案） */
    public static final Map<String, String> CONDITIONS;

    static {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("sale_outbound", "销售出库单");
        docs.put("sale_order", "销售订单");
        docs.put("sale_return_apply", "销售退货申请单");
        docs.put("mall_order_cancel", "商城订单取消");
        docs.put("purchase_inbound", "采购入库单");
        docs.put("purchase_order", "采购订单");
        docs.put("expense_doc", "费用单");
        docs.put("receipt_doc", "收款单");
        docs.put("payment_doc", "付款单");
        docs.put("advance_receipt", "预收款单");
        docs.put("advance_payment", "预付款单");
        docs.put("voucher", "会计凭证");
        docs.put("pre_order", "预订货单");
        docs.put("stock_transfer", "调拨单");
        docs.put("expense_contract", "费用合同");
        docs.put("transfer_apply", "调拨申请单");
        DOC_TYPES = Collections.unmodifiableMap(docs);

        Map<String, String> conditions = new LinkedHashMap<>();
        conditions.put("below_cost", "商品低于成本价");
        conditions.put("below_min_discount", "商品折扣低于最低折扣");
        conditions.put("has_gift", "有赠品");
        conditions.put("not_customer_default_price", "售价不是客户默认价");
        conditions.put("below_min_sale_price", "商品售价低于最低售价");
        conditions.put("book_stock_insufficient", "账面库存不足");
        conditions.put("available_stock_insufficient", "可用库存不足");
        CONDITIONS = Collections.unmodifiableMap(conditions);
    }
}
