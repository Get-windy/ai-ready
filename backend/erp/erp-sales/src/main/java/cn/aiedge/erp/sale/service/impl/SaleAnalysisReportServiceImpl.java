package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.sale.dto.SaleAnalysisReportQueryDTO;
import cn.aiedge.erp.sale.service.SaleAnalysisReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售分析组报表服务实现（销售业绩 / 销售分析 / 销售履约分析 / 销售欠款分析）
 *
 * <p><b>取数架构</b>：把「订货 → 销售 → 退货 → 回款 → 应收」拆成若干<b>按维度分组的小聚合块</b>，
 * 每块一条 SQL 独立分组（不跨块 JOIN，避免明细行被表头字段放大 N 倍），
 * 再在 Java 侧按键合并、派生比率列、排序、分页与合计。</p>
 *
 * <p><b>多租户</b>：本类全部走 {@link JdbcTemplate} 手写 SQL，MyBatis-Plus 的多租户拦截器<b>不生效</b>，
 * 故每条 SQL 都显式带 {@code tenant_id = ?}（参数化，非字符串拼接）。所有用户输入均以 {@code ?} 传入，
 * SQL 文本只拼接固定片段与白名单维度表达式。</p>
 *
 * <p><b>口径</b>（与《销售分析开发文档》§4 对齐）：</p>
 * <ul>
 *   <li>销售收入 = 销售额 − 退货额（净销售口径，可为负）</li>
 *   <li>销售毛利 = 销售收入 − 销售成本；毛利率 = 毛利 / 销售收入</li>
 *   <li>销售利润 = 销售毛利 − 其他费用支出 − 结算优惠；利润率 = 利润 / 销售收入</li>
 *   <li>费销比 = 其他费用支出 / 销售收入；占比类 = 行值 / 合计值</li>
 *   <li>欠款余额 = 此前欠款 + 本期新增欠款 − 本期新增收款 − 结算优惠（对标恒等式）</li>
 *   <li>已取消单据默认剔除（出库 status=12、订单 status=6、退货单 status=4、收款单 status in (3,8)）</li>
 * </ul>
 *
 * <p><b>缺口登记（确认无数据源 → 返回 null，前端显示 -，不返回 0 冒充）</b>：</p>
 * <ul>
 *   <li>业绩目标 / 完成率（销售业绩目标、销售业绩完成率、回款业绩目标、回款业绩完成率）：本系统无业绩目标配置表</li>
 *   <li>拜访统计（拜访客户数、拜访完成率）：本系统无外勤拜访记录表</li>
 *   <li>直接出库数量/金额、预估销售数量/金额：无对应业务列</li>
 *   <li>订购浮动/销售浮动数量：本系统无独立浮动数量列</li>
 *   <li>中/大单位数量、销售基本单位数量、赠品换算结果与赠品小中大数量：仅小单位数量有列（small_unit_quantity）</li>
 *   <li>大/中/小单位客单价：无对应口径列</li>
 *   <li>费用合同兑付、自定义字段1-3：无对应列</li>
 *   <li>毛利占比：单行毛利 / 合计毛利，见 grossShare（有值）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleAnalysisReportServiceImpl implements SaleAnalysisReportService {

    private final JdbcTemplate jdbcTemplate;

    // ════════════════════════════════════════════════════════════════
    //  维度定义
    // ════════════════════════════════════════════════════════════════

    /**
     * 维度上下文：每个维度在各数据块里的「分组键 / 展示名」表达式。
     * 表别名固定为：o=erp_sale_outbound、i=erp_sale_outbound_item、p=erp_product、
     * rd=erp_sale_return_doc、rdi=erp_sale_return_doc_item、so=erp_sale_order、soi=erp_sale_order_item、
     * r=erp_receipt、pr=erp_pre_receipt、bp=biz_party、fr=finance_receivable。
     * 为 null 表示该数据块不支持该维度（该块不参与本维度取数）。
     */
    private static final class DimCtx {
        String code;
        String shKey, shLabel;
        String siKey, siLabel;
        String rhKey, rhLabel;
        String riKey, riLabel;
        String ohKey, ohLabel;
        String oiKey, oiLabel;
        String rcKey, rcLabel;
        String prKey, prLabel;
        String ptKey, ptLabel;
        String rvKey, rvLabel;
    }

    /** 时间维度键：按天 / 按周 / 按月 */
    private static String dateKey(String col, String gran) {
        if ("month".equals(gran)) {
            return "to_char(" + col + "::date, 'YYYY-MM')";
        }
        if ("week".equals(gran)) {
            return "to_char(date_trunc('week', " + col + "::date), 'YYYY-MM-DD')";
        }
        return "to_char(" + col + "::date, 'YYYY-MM-DD')";
    }

    /** 时间维度展示名：在键上追加粒度后缀，避免跨粒度混淆 */
    private static String dateLabel(String col, String gran) {
        String key = dateKey(col, gran);
        if ("month".equals(gran)) {
            return key + " || '（月）'";
        }
        if ("week".equals(gran)) {
            return key + " || '（周）'";
        }
        return key;
    }

    /** 展示名表达式：分组键非聚合时统一包 MAX，保证 SELECT 内只按第 1 列 GROUP BY */
    private static String agg(String expr) {
        return "MAX(" + expr + ")";
    }

    private DimCtx dimCtx(String dim, String gran) {
        DimCtx d = new DimCtx();
        d.code = dim;
        switch (dim) {
            case "time" -> {
                d.shKey = dateKey("o.outbound_date", gran);
                d.shLabel = agg(dateLabel("o.outbound_date", gran));
                d.siKey = d.shKey;
                d.siLabel = d.shLabel;
                d.rhKey = dateKey("rd.order_date", gran);
                d.rhLabel = agg(dateLabel("rd.order_date", gran));
                d.riKey = d.rhKey;
                d.riLabel = d.rhLabel;
                d.ohKey = dateKey("so.order_date", gran);
                d.ohLabel = agg(dateLabel("so.order_date", gran));
                d.oiKey = d.ohKey;
                d.oiLabel = d.ohLabel;
                d.rcKey = dateKey("r.receipt_date", gran);
                d.rcLabel = agg(dateLabel("r.receipt_date", gran));
                d.prKey = dateKey("pr.receipt_date", gran);
                d.prLabel = agg(dateLabel("pr.receipt_date", gran));
                d.ptKey = dateKey("bp.create_time", gran);
                d.ptLabel = agg(dateLabel("bp.create_time", gran));
                d.rvKey = dateKey("fr.created_at", gran);
                d.rvLabel = agg(dateLabel("fr.created_at", gran));
            }
            case "product" -> {
                d.siKey = "COALESCE(i.product_id::text, i.product_name)";
                d.siLabel = "MAX(i.product_name)";
                d.riKey = "COALESCE(rdi.product_id::text, rdi.product_name)";
                d.riLabel = "MAX(rdi.product_name)";
                d.oiKey = "COALESCE(soi.product_id::text, soi.product_name)";
                d.oiLabel = "MAX(soi.product_name)";
            }
            case "brand" -> {
                String saleBrand = "COALESCE(NULLIF(i.brand, ''), NULLIF(p.brand, ''), '无品牌')";
                String retBrand = "COALESCE(NULLIF(rdi.brand, ''), NULLIF(p.brand, ''), '无品牌')";
                String ordBrand = "COALESCE(NULLIF(soi.brand, ''), NULLIF(p.brand, ''), '无品牌')";
                d.siKey = saleBrand;
                d.siLabel = agg(saleBrand);
                d.riKey = retBrand;
                d.riLabel = agg(retBrand);
                d.oiKey = ordBrand;
                d.oiLabel = agg(ordBrand);
            }
            case "customer" -> {
                d.shKey = "COALESCE(o.customer_id::text, o.customer_name)";
                d.shLabel = "MAX(o.customer_name)";
                d.siKey = d.shKey;
                d.siLabel = d.shLabel;
                d.rhKey = "COALESCE(rd.customer_id::text, rd.customer_name)";
                d.rhLabel = "MAX(rd.customer_name)";
                d.riKey = d.rhKey;
                d.riLabel = d.rhLabel;
                d.ohKey = "COALESCE(so.customer_id::text, so.customer_name)";
                d.ohLabel = "MAX(so.customer_name)";
                d.oiKey = d.ohKey;
                d.oiLabel = d.ohLabel;
                d.rcKey = "COALESCE(r.customer_id::text, r.customer_name)";
                d.rcLabel = "MAX(r.customer_name)";
                d.prKey = "COALESCE(pr.customer_id::text, pr.customer_name)";
                d.prLabel = "MAX(pr.customer_name)";
                d.ptKey = "bp.id::text";
                d.ptLabel = "MAX(bp.party_name)";
                d.rvKey = "COALESCE(NULLIF(fr.customer_id, ''), fr.customer_name)";
                d.rvLabel = "MAX(fr.customer_name)";
            }
            case "region" -> {
                String saleRegion = "COALESCE(NULLIF(o.region, ''), NULLIF(bp.region, ''), '无区域')";
                String retRegion = "COALESCE(NULLIF(rd.region, ''), NULLIF(bp.region, ''), '无区域')";
                String ordRegion = "COALESCE(NULLIF(so.region, ''), NULLIF(bp.region, ''), '无区域')";
                String partyRegion = "COALESCE(NULLIF(bp.region, ''), '无区域')";
                d.shKey = saleRegion;
                d.shLabel = agg(saleRegion);
                d.siKey = saleRegion;
                d.siLabel = agg(saleRegion);
                d.rhKey = retRegion;
                d.rhLabel = agg(retRegion);
                d.riKey = retRegion;
                d.riLabel = agg(retRegion);
                d.ohKey = ordRegion;
                d.ohLabel = agg(ordRegion);
                d.oiKey = ordRegion;
                d.oiLabel = agg(ordRegion);
                d.rcKey = partyRegion;
                d.rcLabel = agg(partyRegion);
                d.prKey = partyRegion;
                d.prLabel = agg(partyRegion);
                d.ptKey = partyRegion;
                d.ptLabel = agg(partyRegion);
                d.rvKey = partyRegion;
                d.rvLabel = agg(partyRegion);
            }
            case "warehouse" -> {
                String saleWh = "COALESCE(NULLIF(o.warehouse_name, ''), '未指定仓库')";
                String retWh = "COALESCE(NULLIF(rd.warehouse_name, ''), '未指定仓库')";
                String ordWh = "COALESCE(NULLIF(so.warehouse_name, ''), '未指定仓库')";
                d.shKey = saleWh;
                d.shLabel = agg(saleWh);
                d.siKey = saleWh;
                d.siLabel = agg(saleWh);
                d.rhKey = retWh;
                d.rhLabel = agg(retWh);
                d.riKey = retWh;
                d.riLabel = agg(retWh);
                d.ohKey = ordWh;
                d.ohLabel = agg(ordWh);
                d.oiKey = ordWh;
                d.oiLabel = agg(ordWh);
            }
            case "staff" -> {
                String saleStaff = "COALESCE(NULLIF(o.sales_person_name, ''), '未指定职员')";
                String retStaff = "COALESCE(NULLIF(rd.handler_name, ''), '未指定职员')";
                String ordStaff = "COALESCE(NULLIF(so.salesman_name, ''), '未指定职员')";
                String rcpStaff = "COALESCE(NULLIF(r.sales_person_name, ''), '未指定职员')";
                String preStaff = "COALESCE(NULLIF(pr.handler_name, ''), '未指定职员')";
                String partyStaff = "COALESCE(NULLIF(bp.default_handler_name, ''), '未指定职员')";
                d.shKey = saleStaff;
                d.shLabel = agg(saleStaff);
                d.siKey = saleStaff;
                d.siLabel = agg(saleStaff);
                d.rhKey = retStaff;
                d.rhLabel = agg(retStaff);
                d.riKey = retStaff;
                d.riLabel = agg(retStaff);
                d.ohKey = ordStaff;
                d.ohLabel = agg(ordStaff);
                d.oiKey = ordStaff;
                d.oiLabel = agg(ordStaff);
                d.rcKey = rcpStaff;
                d.rcLabel = agg(rcpStaff);
                d.prKey = preStaff;
                d.prLabel = agg(preStaff);
                d.ptKey = partyStaff;
                d.ptLabel = agg(partyStaff);
                d.rvKey = partyStaff;
                d.rvLabel = agg(partyStaff);
            }
            case "source" -> {
                String saleSrc = "COALESCE(NULLIF(o.generation_method, ''), NULLIF(o.source, ''), '未知来源')";
                String retSrc = "COALESCE(NULLIF(rd.generate_type, ''), '未知来源')";
                String ordSrc = "COALESCE(NULLIF(so.generation_method, ''), '未知来源')";
                d.shKey = saleSrc;
                d.shLabel = agg(saleSrc);
                d.siKey = saleSrc;
                d.siLabel = agg(saleSrc);
                d.rhKey = retSrc;
                d.rhLabel = agg(retSrc);
                d.riKey = retSrc;
                d.riLabel = agg(retSrc);
                d.ohKey = ordSrc;
                d.ohLabel = agg(ordSrc);
                d.oiKey = ordSrc;
                d.oiLabel = agg(ordSrc);
            }
            default -> throw new IllegalArgumentException("不支持的维度: " + dim);
        }
        return d;
    }

    // ════════════════════════════════════════════════════════════════
    //  条件拼装（全部参数化；SQL 文本只含固定片段）
    // ════════════════════════════════════════════════════════════════

    /** 参数化条件累加器 */
    private static final class Where {
        private final StringBuilder sb = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        void raw(String sql) {
            sb.append(" AND ").append(sql);
        }

        void eq(String expr, Object v) {
            if (expr != null && v != null) {
                sb.append(" AND ").append(expr).append(" = ?");
                params.add(v);
            }
        }

        void like(String expr, String v) {
            if (expr != null && StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" LIKE ?");
                params.add("%" + v.trim() + "%");
            }
        }

        void dateFrom(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" >= ?::date");
                params.add(v.trim());
            }
        }

        void dateTo(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" <= ?::date");
                params.add(v.trim());
            }
        }

        void addParams(Object... values) {
            for (Object v : values) {
                params.add(v);
            }
        }

        String sql() {
            return sb.toString();
        }

        List<Object> list() {
            return params;
        }
    }

    /** 数字型主键（雪花 ID 为纯数字字符串） */
    private static Long numericId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long tenantId() {
        return MyBatisPlusConfig.getCurrentTenantIdValue();
    }

    private static String today() {
        return LocalDate.now().toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  分块 FROM 片段（表别名固定，供维度表达式引用）
    // ════════════════════════════════════════════════════════════════

    private static final String SALE_HEADER_FROM =
        " FROM erp_sale_outbound o"
        + " LEFT JOIN biz_party bp ON bp.id = o.customer_id AND bp.tenant_id = o.tenant_id AND bp.deleted = 0"
        + " WHERE o.deleted = 0 AND o.tenant_id = ? AND o.status > 0 AND o.status <> 12";

    private static final String SALE_ITEM_FROM =
        " FROM erp_sale_outbound_item i"
        + " JOIN erp_sale_outbound o ON o.id = i.outbound_id AND o.tenant_id = i.tenant_id"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.tenant_id = i.tenant_id"
        + " LEFT JOIN biz_party bp ON bp.id = o.customer_id AND bp.tenant_id = o.tenant_id AND bp.deleted = 0"
        + " WHERE i.deleted = 0 AND i.tenant_id = ? AND o.deleted = 0 AND o.status > 0 AND o.status <> 12";

    private static final String RETURN_HEADER_FROM =
        " FROM erp_sale_return_doc rd"
        + " LEFT JOIN biz_party bp ON bp.id = rd.customer_id AND bp.tenant_id = rd.tenant_id AND bp.deleted = 0"
        + " WHERE rd.deleted = 0 AND rd.tenant_id = ? AND rd.status <> 4";

    private static final String RETURN_ITEM_FROM =
        " FROM erp_sale_return_doc_item rdi"
        + " JOIN erp_sale_return_doc rd ON rd.id = rdi.return_doc_id AND rd.tenant_id = rdi.tenant_id"
        + " LEFT JOIN erp_product p ON p.id = rdi.product_id AND p.tenant_id = rdi.tenant_id"
        + " LEFT JOIN biz_party bp ON bp.id = rd.customer_id AND bp.tenant_id = rd.tenant_id AND bp.deleted = 0"
        + " WHERE rdi.deleted = 0 AND rdi.tenant_id = ? AND rd.deleted = 0 AND rd.status <> 4";

    private static final String ORDER_HEADER_FROM =
        " FROM erp_sale_order so"
        + " LEFT JOIN biz_party bp ON bp.id = so.customer_id AND bp.tenant_id = so.tenant_id AND bp.deleted = 0"
        + " WHERE so.deleted = 0 AND so.tenant_id = ? AND so.status > 0 AND so.status <> 6";

    private static final String ORDER_ITEM_FROM =
        " FROM erp_sale_order_item soi"
        + " JOIN erp_sale_order so ON so.id = soi.order_id AND so.tenant_id = soi.tenant_id"
        + " LEFT JOIN erp_product p ON p.id = soi.product_id AND p.tenant_id = soi.tenant_id"
        + " LEFT JOIN biz_party bp ON bp.id = so.customer_id AND bp.tenant_id = so.tenant_id AND bp.deleted = 0"
        + " WHERE soi.tenant_id = ? AND so.deleted = 0 AND so.status > 0 AND so.status <> 6";

    private static final String RECEIPT_FROM =
        " FROM erp_receipt r"
        + " LEFT JOIN erp_sale_order so2 ON so2.id = r.order_id AND so2.tenant_id = r.tenant_id"
        + " LEFT JOIN biz_party bp ON bp.id = r.customer_id AND bp.tenant_id = r.tenant_id AND bp.deleted = 0"
        + " WHERE r.deleted = 0 AND r.tenant_id = ? AND r.status >= 2 AND r.status <> 3 AND r.status <> 8";

    private static final String PRE_RECEIPT_FROM =
        " FROM erp_pre_receipt pr"
        + " LEFT JOIN biz_party bp ON bp.id = pr.customer_id AND bp.tenant_id = pr.tenant_id AND bp.deleted = 0"
        + " WHERE pr.deleted = 0 AND pr.tenant_id = ? AND pr.status IS DISTINCT FROM 'forfeited'"
        + " AND pr.status IS DISTINCT FROM 'refunded'";

    private static final String PARTY_FROM =
        " FROM biz_party bp WHERE bp.deleted = 0 AND bp.tenant_id = ? AND bp.party_type = 2";

    private static final String RECEIVABLE_FROM =
        " FROM finance_receivable fr"
        + " LEFT JOIN biz_party bp ON bp.id::text = fr.customer_id AND bp.tenant_id = fr.tenant_id AND bp.deleted = 0"
        + " WHERE fr.deleted_flag = 0 AND fr.tenant_id = ? AND fr.remaining_amount <> 0";

    /** 价税合计：价税分离列优先，其次不含税金额、开单金额、货款金额（真库存在 0 值字段，依次回退） */
    private static final String ORDER_TAX_AMOUNT =
        "COALESCE(NULLIF(so.total_amount_with_tax,0), NULLIF(so.total_amount,0), so.bill_amount, so.product_amount, 0)";

    // ════════════════════════════════════════════════════════════════
    //  分块取数
    // ════════════════════════════════════════════════════════════════

    /** 销售块-表头：单据数 / 客户数 / 重量体积 / 随单费用 / 结算优惠 */
    private Map<String, Map<String, Object>> saleHeaderBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.shKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofHeader());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.shKey + " AS \"dimKey\", " + d.shLabel + " AS \"dimLabel\","
            + " COUNT(DISTINCT o.id) AS \"saleDocCount\","
            + " COUNT(DISTINCT o.customer_id) AS \"saleCustomerCount\","
            + " SUM(COALESCE(o.total_weight,0)) AS \"saleWeight\","
            + " SUM(COALESCE(o.total_volume,0)) AS \"saleVolume\","
            + " SUM(COALESCE(o.other_fee,0)) AS \"otherFee\","
            + " SUM(COALESCE(o.promo_discount,0)+COALESCE(o.coupon_amount,0)+COALESCE(o.direct_discount,0)) AS \"settleDiscount\""
            + dimIdentitySelect(d)
            + SALE_HEADER_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /**
     * 按维度补充的标识列（客户编号 / 结款方式 / 默认经手人 / 所属部门）。
     * 这些列仅在对应维度下有业务含义，故按维度条件拼接（无占位符，不引入参数）。
     */
    private static String dimIdentitySelect(DimCtx d) {
        if ("customer".equals(d.code)) {
            return ", MAX(bp.party_code) AS \"customerCode\","
                + " MAX(bp.settlement_type::text) AS \"settlementType\","
                + " MAX(bp.default_handler_name) AS \"defaultHandler\"";
        }
        if ("staff".equals(d.code)) {
            return ", MAX(o.department_name) AS \"deptName\"";
        }
        return "";
    }

    /**
     * 商品维度补充的标识列（货号/图片/品牌/条码/规格/型号/产地）——取自出库明细快照，缺失回退商品主数据。
     * 仅「按商品」维度有业务含义，故按维度条件拼接（无占位符）。
     */
    private static String dimProductIdentitySelect(DimCtx d) {
        if (!"product".equals(d.code)) {
            return "";
        }
        return ", MAX(i.product_code) AS \"productCode\", MAX(p.image_url) AS \"image\","
            + " MAX(COALESCE(NULLIF(i.brand,''), p.brand)) AS \"brand\","
            + " MAX(COALESCE(NULLIF(i.barcode,''), p.barcode)) AS \"barcode\","
            + " MAX(COALESCE(NULLIF(i.specification,''), p.spec)) AS \"spec\","
            + " MAX(COALESCE(NULLIF(i.model,''), p.model)) AS \"model\","
            + " MAX(COALESCE(NULLIF(i.origin,''), p.origin)) AS \"origin\"";
    }

    /** 销售块-明细：数量 / 金额 / 成本 / 毛利 / 优惠 / 赠品 / 小单位数量 */
    private Map<String, Map<String, Object>> saleItemBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.siKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofItem());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.siKey + " AS \"dimKey\", " + d.siLabel + " AS \"dimLabel\","
            + " SUM(COALESCE(i.outbound_quantity,0)) AS \"saleQty\","
            + " SUM(COALESCE(i.line_amount,0)) AS \"saleAmount\","
            + " SUM(COALESCE(i.cost_amount,0)) AS \"costAmount\","
            + " SUM(COALESCE(i.gross_profit,0)) AS \"grossProfit\","
            + " SUM(COALESCE(i.favorable_amount,0)) AS \"favorableAmount\","
            + " SUM(CASE WHEN i.gift THEN COALESCE(i.outbound_quantity,0) ELSE 0 END) AS \"giftQty\","
            + " SUM(COALESCE(i.small_unit_quantity,0)) AS \"smallQty\","
            + " SUM(COALESCE(i.weight,0)) AS \"saleWeight\","
            + " SUM(COALESCE(i.volume,0)) AS \"saleVolume\","
            + " MAX(i.conversion_result) AS \"conversionResult\""
            + dimProductIdentitySelect(d)
            + SALE_ITEM_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 退货块-表头：退货单数 / 退货重量体积 */
    private Map<String, Map<String, Object>> returnHeaderBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.rhKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofReturnHeader());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.rhKey + " AS \"dimKey\", " + d.rhLabel + " AS \"dimLabel\","
            + " COUNT(DISTINCT rd.id) AS \"returnDocCount\","
            + " SUM(COALESCE(rd.total_weight,0)) AS \"returnWeight\","
            + " SUM(COALESCE(rd.total_volume,0)) AS \"returnVolume\""
            + RETURN_HEADER_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 退货块-明细：退货数量 / 退货额 */
    private Map<String, Map<String, Object>> returnItemBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.riKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofReturnItem());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.riKey + " AS \"dimKey\", " + d.riLabel + " AS \"dimLabel\","
            + " SUM(COALESCE(rdi.return_quantity,0)) AS \"returnQty\","
            + " SUM(COALESCE(rdi.line_amount,0)) AS \"returnAmount\","
            + " SUM(COALESCE(rdi.small_unit_quantity,0)) AS \"returnSmallQty\","
            + " MAX(rdi.conversion_result) AS \"returnConversionResult\""
            + RETURN_ITEM_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 订货块-表头：订单数 / 订货客户数 / 价税合计 / 退订 / 已结 / 未结 */
    private Map<String, Map<String, Object>> orderHeaderBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.ohKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofOrderHeader());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.ohKey + " AS \"dimKey\", " + d.ohLabel + " AS \"dimLabel\","
            + " COUNT(DISTINCT so.id) AS \"orderDocCount\","
            + " COUNT(DISTINCT so.customer_id) AS \"orderCustomerCount\","
            + " SUM(" + ORDER_TAX_AMOUNT + ") AS \"orderAmount\","
            + " SUM(COALESCE(so.return_quantity,0)) AS \"orderReturnQty\","
            + " SUM(COALESCE(so.return_amount,0)) AS \"orderReturnAmount\","
            + " SUM(COALESCE(so.settled_amount, so.received_amount, 0)) AS \"orderSettled\","
            + " SUM(" + ORDER_TAX_AMOUNT + " - COALESCE(so.settled_amount, so.received_amount, 0)) AS \"unsettledAmount\""
            + ORDER_HEADER_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 订货块-明细：订货数量 / 订货金额 */
    private Map<String, Map<String, Object>> orderItemBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.oiKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofOrderItem());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.oiKey + " AS \"dimKey\", " + d.oiLabel + " AS \"dimLabel\","
            + " SUM(COALESCE(soi.quantity,0)) AS \"orderQty\","
            + " SUM(COALESCE(soi.amount,0)) AS \"orderItemAmount\""
            + ORDER_ITEM_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 回款块：回款客户数 / 回款总额 / 按期与超期拆分 / 结算优惠 */
    private Map<String, Map<String, Object>> receiptBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.rcKey == null) {
            return Map.of();
        }
        Where w = new Where();
        applyCommonFilters(w, q, CommonCols.ofReceipt());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        // 按期/超期以收款所关联订单的付款截止日（erp_sale_order.payment_date）为准；
        // 无关联订单（截止日未知）按按期计，避免把无从判断的流水一律算超期
        String sql = "SELECT " + d.rcKey + " AS \"dimKey\", " + d.rcLabel + " AS \"dimLabel\","
            + " COUNT(DISTINCT r.customer_id) AS \"receiptCustomerCount\","
            + " SUM(COALESCE(r.receipt_amount,0)) AS \"receiptAmount\","
            + " SUM(COALESCE(r.discount_amount,0)) AS \"receiptDiscount\","
            + " SUM(CASE WHEN so2.payment_date IS NULL OR r.receipt_date <= so2.payment_date"
            + "      THEN COALESCE(r.receipt_amount,0) ELSE 0 END) AS \"onTimeReceiptAmount\","
            + " SUM(CASE WHEN so2.payment_date IS NOT NULL AND r.receipt_date > so2.payment_date"
            + "      THEN COALESCE(r.receipt_amount,0) ELSE 0 END) AS \"overdueReceiptAmount\""
            + RECEIPT_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 预收款块：预收余额 */
    private Map<String, Map<String, Object>> preReceiptBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.prKey == null) {
            return Map.of();
        }
        Where w = new Where();
        w.dateFrom("pr.receipt_date", q.getStartDate());
        w.dateTo("pr.receipt_date", q.getEndDate());
        w.like("bp.region", q.getRegion());
        w.like("pr.customer_name", q.getCustomerName());
        w.like("pr.handler_name", q.getSalesmanName());
        w.like("pr.dept_name", q.getDeptName());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.prKey + " AS \"dimKey\", " + d.prLabel + " AS \"dimLabel\","
            + " SUM(COALESCE(pr.remaining_amount,0)) AS \"prepaidBalance\""
            + PRE_RECEIPT_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 客户主数据块：拓客数（期间新增客户）/ 信用额度 */
    private Map<String, Map<String, Object>> partyBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.ptKey == null) {
            return Map.of();
        }
        Where w = new Where();
        w.like("bp.region", q.getRegion());
        w.like("bp.party_name", q.getCustomerName());
        w.like("bp.default_handler_name", q.getSalesmanName());
        Where wNew = new Where();
        wNew.dateFrom("bp.create_time", q.getStartDate());
        wNew.dateTo("bp.create_time", q.getEndDate());
        // ⚠️ 参数顺序必须与 SQL 文本中 ? 的出现顺序一致：SELECT 内的新增客户条件在前，WHERE 的租户与过滤在后
        List<Object> params = new ArrayList<>();
        params.addAll(wNew.list());
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + d.ptKey + " AS \"dimKey\", " + d.ptLabel + " AS \"dimLabel\","
            + " COUNT(DISTINCT CASE WHEN 1 = 1" + wNew.sql() + " THEN bp.id END) AS \"newCustomerCount\","
            + " SUM(COALESCE(bp.credit_limit,0)) AS \"creditLimit\","
            + " SUM(COALESCE(bp.current_debt,0)) AS \"usedCredit\""
            + PARTY_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 应收块：此前欠款 / 本期新增欠款 / 超期欠款与账龄五档 */
    private Map<String, Map<String, Object>> receivableBlock(DimCtx d, SaleAnalysisReportQueryDTO q) {
        if (d.rvKey == null) {
            return Map.of();
        }
        String start = StringUtils.hasText(q.getStartDate()) ? q.getStartDate().trim() : today();
        Where w = new Where();
        w.like("bp.region", q.getRegion());
        w.like("fr.customer_name", q.getCustomerName());
        w.like("bp.default_handler_name", q.getSalesmanName());
        // ⚠️ 参数顺序同 partyBlock：SELECT 内的两个期间占位符在前
        List<Object> params = new ArrayList<>();
        params.add(start);
        params.add(start);
        params.add(tenantId());
        params.addAll(w.list());
        String overdue = "fr.due_date IS NOT NULL AND fr.due_date < CURRENT_DATE";
        String sql = "SELECT " + d.rvKey + " AS \"dimKey\", " + d.rvLabel + " AS \"dimLabel\","
            + " SUM(CASE WHEN fr.created_at::date < ?::date THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"prevDebt\","
            + " SUM(CASE WHEN fr.created_at::date >= ?::date THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"newDebt\","
            + " SUM(CASE WHEN " + overdue + " THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"overdueDebt\","
            + " SUM(CASE WHEN " + overdue + " AND CURRENT_DATE - fr.due_date < 30 THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"agingLt1m\","
            + " SUM(CASE WHEN " + overdue + " AND CURRENT_DATE - fr.due_date >= 30 AND CURRENT_DATE - fr.due_date < 90 THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"aging1to3m\","
            + " SUM(CASE WHEN " + overdue + " AND CURRENT_DATE - fr.due_date >= 90 AND CURRENT_DATE - fr.due_date < 180 THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"aging3to6m\","
            + " SUM(CASE WHEN " + overdue + " AND CURRENT_DATE - fr.due_date >= 180 AND CURRENT_DATE - fr.due_date < 365 THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"aging6to12m\","
            + " SUM(CASE WHEN " + overdue + " AND CURRENT_DATE - fr.due_date >= 365 THEN COALESCE(fr.remaining_amount,0) ELSE 0 END) AS \"agingGt12m\""
            + RECEIVABLE_FROM + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    // ════════════════════════════════════════════════════════════════
    //  公共过滤条件（按各块可用列声明，缺列的条件自动跳过）
    // ════════════════════════════════════════════════════════════════

    /** 各数据块实际可用的过滤列 */
    private record CommonCols(String dateCol, String customerIdCol, String customerNameCol,
                              String staffCol, String deptCol, String warehouseCol, String regionCol,
                              String keywordExpr, String brandExpr, String sourceCol, String saleTypeCol) {

        static CommonCols ofHeader() {
            return new CommonCols("o.outbound_date", "o.customer_id", "o.customer_name",
                "o.sales_person_name", "o.department_name", "o.warehouse_name",
                "COALESCE(NULLIF(o.region,''), bp.region)", null, null,
                "COALESCE(o.generation_method, o.source)", null);
        }

        static CommonCols ofItem() {
            return new CommonCols("o.outbound_date", "o.customer_id", "o.customer_name",
                "o.sales_person_name", "o.department_name", "o.warehouse_name",
                "COALESCE(NULLIF(o.region,''), bp.region)",
                "(i.product_name LIKE ? OR i.product_code LIKE ? OR i.barcode LIKE ?)",
                "COALESCE(NULLIF(i.brand,''), p.brand)", "COALESCE(o.generation_method, o.source)", null);
        }

        static CommonCols ofReturnHeader() {
            return new CommonCols("rd.order_date", "rd.customer_id", "rd.customer_name",
                "rd.handler_name", "rd.dept_name", "rd.warehouse_name",
                "COALESCE(NULLIF(rd.region,''), bp.region)", null, null,
                "rd.generate_type", null);
        }

        static CommonCols ofReturnItem() {
            return new CommonCols("rd.order_date", "rd.customer_id", "rd.customer_name",
                "rd.handler_name", "rd.dept_name", "rd.warehouse_name",
                "COALESCE(NULLIF(rd.region,''), bp.region)",
                "(rdi.product_name LIKE ? OR rdi.product_code LIKE ? OR rdi.barcode LIKE ?)",
                "COALESCE(NULLIF(rdi.brand,''), p.brand)", "rd.generate_type", null);
        }

        static CommonCols ofOrderHeader() {
            return new CommonCols("so.order_date", "so.customer_id", "so.customer_name",
                "so.salesman_name", "so.dept_name", "so.warehouse_name",
                "COALESCE(NULLIF(so.region,''), bp.region)", null, null,
                "so.generation_method", "so.sale_type");
        }

        static CommonCols ofOrderItem() {
            return new CommonCols("so.order_date", "so.customer_id", "so.customer_name",
                "so.salesman_name", "so.dept_name", "so.warehouse_name",
                "COALESCE(NULLIF(so.region,''), bp.region)",
                "(soi.product_name LIKE ? OR soi.product_code LIKE ? OR soi.barcode LIKE ?)",
                "COALESCE(NULLIF(soi.brand,''), p.brand)", "so.generation_method", "so.sale_type");
        }

        static CommonCols ofReceipt() {
            return new CommonCols("r.receipt_date", "r.customer_id", "r.customer_name",
                "r.sales_person_name", "r.department_name", null,
                "bp.region", null, null, null, null);
        }
    }

    /**
     * 通用过滤：日期区间 + 客户/经手人/部门/仓库/区域/商品/品牌/来源/销售类型 + 停用职员剔除。
     * <p>所有值以 {@code ?} 传入；SQL 文本只拼接固定片段。</p>
     */
    private void applyCommonFilters(Where w, SaleAnalysisReportQueryDTO q, CommonCols c) {
        w.dateFrom(c.dateCol(), q.getStartDate());
        w.dateTo(c.dateCol(), q.getEndDate());
        Long cid = numericId(q.getCustomerId());
        if (cid != null) {
            w.eq(c.customerIdCol(), cid);
        } else {
            w.like(c.customerNameCol(), q.getCustomerName());
        }
        w.like(c.staffCol(), q.getSalesmanName());
        w.like(c.deptCol(), q.getDeptName());
        w.like(c.warehouseCol(), q.getWarehouseName());
        w.like(c.regionCol(), q.getRegion());
        if (c.keywordExpr() != null && c.keywordExpr().contains("?") && StringUtils.hasText(q.getKeyword())) {
            String kw = "%" + q.getKeyword().trim() + "%";
            w.raw(c.keywordExpr());
            w.addParams(kw, kw, kw);
        }
        w.like(c.brandExpr(), q.getBrand());
        w.like(c.sourceCol(), q.getSource());
        if (c.saleTypeCol() != null && q.getSaleType() != null) {
            w.eq(c.saleTypeCol(), q.getSaleType());
        }
        if (Boolean.TRUE.equals(q.getHideDisabledStaff()) && c.staffCol() != null) {
            w.raw(c.staffCol() + " NOT IN (SELECT COALESCE(NULLIF(u.real_name,''), u.nickname)"
                + " FROM sys_user u WHERE u.deleted = 0 AND u.status <> 0 AND u.tenant_id = ?)");
            w.addParams(tenantId());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SQL 执行与合并
    // ════════════════════════════════════════════════════════════════

    /** 执行「dimKey / dimLabel + 指标列」的分组查询，按 dimKey 归并为 Map */
    private Map<String, Map<String, Object>> groupBy(String sql, List<Object> params) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : rows) {
            Object k = r.get("dimKey");
            String key = k == null ? "" : k.toString();
            if (key.isEmpty()) {
                continue;
            }
            Map<String, Object> exist = out.get(key);
            if (exist == null) {
                out.put(key, r);
            } else {
                r.forEach((ck, cv) -> {
                    if ("dimKey".equals(ck) || "dimLabel".equals(ck) || cv == null) {
                        return;
                    }
                    exist.merge(ck, cv, this::addValues);
                });
            }
        }
        return out;
    }

    private Object addValues(Object a, Object b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return toDecimal(a).add(toDecimal(b));
    }

    /** 单行聚合容器 */
    private static final class Row {
        final Map<String, Object> m = new LinkedHashMap<>();

        Row(String key) {
            m.put("dimKey", key);
        }
    }

    /** 把各块结果合并到统一的「行」上 */
    @SafeVarargs
    private final Map<String, Row> merge(Map<String, Map<String, Object>>... blocks) {
        Map<String, Row> rows = new LinkedHashMap<>();
        for (Map<String, Map<String, Object>> block : blocks) {
            block.forEach((k, v) -> {
                Row row = rows.computeIfAbsent(k, Row::new);
                Object label = v.get("dimLabel");
                if (label != null && row.m.get("dimLabel") == null) {
                    row.m.put("dimLabel", label);
                }
                v.forEach((ck, cv) -> {
                    if ("dimKey".equals(ck) || "dimLabel".equals(ck) || cv == null) {
                        return;
                    }
                    row.m.merge(ck, cv, this::addValues);
                });
            });
        }
        rows.forEach((k, r) -> {
            if (r.m.get("dimLabel") == null) {
                r.m.put("dimLabel", k);
            }
        });
        return rows;
    }

    // ════════════════════════════════════════════════════════════════
    //  数值工具
    // ════════════════════════════════════════════════════════════════

    private static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal num(Map<String, Object> m, String key) {
        return m == null ? BigDecimal.ZERO : toDecimal(m.get(key));
    }

    /** 比率（×100）：分母为 0 或缺失时返回 null（前端显示 -），不返回 0 冒充 */
    private static BigDecimal rate(Object numerator, Object denominator, int scale) {
        if (numerator == null) {
            return null;
        }
        BigDecimal den = toDecimal(denominator);
        if (den.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return toDecimal(numerator).multiply(BigDecimal.valueOf(100)).divide(den, scale, RoundingMode.HALF_UP);
    }

    /** 比值（不 ×100，如平均交易额） */
    private static BigDecimal ratio(Object numerator, Object denominator, int scale) {
        if (numerator == null) {
            return null;
        }
        BigDecimal den = toDecimal(denominator);
        if (den.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return toDecimal(numerator).divide(den, scale, RoundingMode.HALF_UP);
    }

    // ════════════════════════════════════════════════════════════════
    //  派生列（量本利全链路）
    // ════════════════════════════════════════════════════════════════

    /**
     * 由块级原始聚合值派生全部展示列。
     *
     * @param needReceipt 是否派生回款列（销售业绩 / 销售欠款）
     * @param needDebt    是否派生欠款列（销售欠款）
     */
    private void derive(Map<String, Object> m, boolean needReceipt, boolean needDebt) {
        BigDecimal saleQty = num(m, "saleQty");
        BigDecimal saleAmount = num(m, "saleAmount");
        BigDecimal returnQty = num(m, "returnQty");
        BigDecimal returnAmount = num(m, "returnAmount");
        BigDecimal revenue = saleAmount.subtract(returnAmount);        // 销售收入 = 销售额 − 退货额（可为负）
        BigDecimal cost = num(m, "costAmount");
        BigDecimal gross = revenue.subtract(cost);                     // 销售毛利
        BigDecimal otherFee = num(m, "otherFee");
        BigDecimal settle = num(m, "settleDiscount");
        BigDecimal profit = gross.subtract(otherFee).subtract(settle); // 销售利润

        m.put("saleQty", saleQty);
        m.put("saleAmount", saleAmount);
        m.put("returnQty", returnQty);
        m.put("returnAmount", returnAmount);
        m.put("revenue", revenue);
        m.put("costAmount", cost);
        m.put("grossProfit", gross);
        m.put("grossMargin", rate(gross, revenue, 2));
        m.put("otherFee", otherFee);
        m.put("settleDiscount", settle);
        m.put("expenseRatio", rate(otherFee, revenue, 4));
        m.put("saleProfit", profit);
        m.put("profitRatio", rate(profit, revenue, 2));
        // 实销（净额）块 = 销售 − 退货
        m.put("netSaleQty", saleQty.subtract(returnQty));
        m.put("netSaleAmount", revenue);
        m.put("netSaleWeight", num(m, "saleWeight").subtract(num(m, "returnWeight")));
        // 退货率类
        m.put("returnRate", rate(returnAmount, saleAmount, 2));
        m.put("amountReturnRate", rate(returnAmount, saleAmount, 2));
        m.put("qtyReturnRate", rate(returnQty, saleQty, 2));
        m.put("avgDealAmount", ratio(saleAmount, m.get("saleDocCount"), 2));
        // 订货块派生
        BigDecimal orderAmount = num(m, "orderAmount");
        BigDecimal orderReturnAmount = num(m, "orderReturnAmount");
        BigDecimal orderQty = num(m, "orderQty");
        m.put("orderAmount", orderAmount);
        m.put("orderQty", orderQty);
        m.put("orderReturnQty", num(m, "orderReturnQty"));
        m.put("orderReturnAmount", orderReturnAmount);
        m.put("netOrderAmount", orderAmount.subtract(orderReturnAmount));          // 实订金额
        m.put("orderFloatQty", orderQty.subtract(saleQty));
        m.put("saleFloatQty", saleQty.subtract(orderQty));
        m.put("orderReturnRate", rate(orderReturnAmount, orderAmount, 3));          // 退订率(%)
        if (needReceipt) {
            m.put("receiptAmount", num(m, "receiptAmount"));
            m.put("receiptCustomerCount", num(m, "receiptCustomerCount"));
            m.put("onTimeReceiptAmount", num(m, "onTimeReceiptAmount"));
            m.put("overdueReceiptAmount", num(m, "overdueReceiptAmount"));
            m.put("receiptDiscount", num(m, "receiptDiscount"));
        }
        if (needDebt) {
            BigDecimal prev = num(m, "prevDebt");
            BigDecimal added = num(m, "newDebt");
            BigDecimal received = num(m, "receiptInPeriod");
            BigDecimal discount = num(m, "settleDiscountInPeriod");
            m.put("prevDebt", prev);
            m.put("newDebt", added);
            m.put("receiptInPeriod", received);
            m.put("settleDiscountInPeriod", discount);
            // 欠款余额 = 此前欠款 + 本期新增欠款 − 本期新增收款 − 结算优惠（对标恒等式）
            m.put("debtBalance", prev.add(added).subtract(received).subtract(discount));
            m.put("overdueDebt", num(m, "overdueDebt"));
            m.put("creditUsageRate", rate(num(m, "usedCredit"), m.get("creditLimit"), 2));
        }
    }

    /** 占比类派生（需合计值，故在所有行汇总后统一回填） */
    private void deriveShares(List<Map<String, Object>> rows) {
        BigDecimal saleTotal = sum(rows, "saleAmount");
        BigDecimal revenueTotal = sum(rows, "revenue");
        BigDecimal profitTotal = sum(rows, "saleProfit");
        BigDecimal grossTotal = sum(rows, "grossProfit");
        BigDecimal customerTotal = sum(rows, "saleCustomerCount");
        BigDecimal unsettledTotal = sum(rows, "unsettledAmount");
        BigDecimal debtTotal = sum(rows, "debtBalance");
        for (Map<String, Object> m : rows) {
            m.put("saleAmountRatio", rate(num(m, "saleAmount"), saleTotal, 2));
            m.put("revenueRatio", rate(num(m, "revenue"), revenueTotal, 2));
            m.put("profitShare", rate(num(m, "saleProfit"), profitTotal, 2));
            m.put("grossShare", rate(num(m, "grossProfit"), grossTotal, 2));
            m.put("customerCountRatio", rate(num(m, "saleCustomerCount"), customerTotal, 2));
            m.put("unsettledRatio", rate(num(m, "unsettledAmount"), unsettledTotal, 2));
            m.put("debtRatio", rate(num(m, "debtBalance"), debtTotal, 2));
        }
    }

    private static BigDecimal sum(List<Map<String, Object>> rows, String key) {
        BigDecimal acc = BigDecimal.ZERO;
        for (Map<String, Object> m : rows) {
            acc = acc.add(num(m, key));
        }
        return acc;
    }

    /** 合计行：数值列求和，比率列按合计口径重算 */
    private Map<String, Object> summary(List<Map<String, Object>> rows, boolean needReceipt, boolean needDebt) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v instanceof BigDecimal || v instanceof Integer || v instanceof Long) {
                    s.merge(k, toDecimal(v), this::addValues);
                }
            });
        }
        for (String ratioKey : List.of("grossMargin", "expenseRatio", "profitRatio", "returnRate",
            "amountReturnRate", "qtyReturnRate", "avgDealAmount", "orderReturnRate", "creditUsageRate")) {
            s.remove(ratioKey);
        }
        derive(s, needReceipt, needDebt);
        // 合计行的「行占比」无意义（恒为 100%），显式置空避免误读
        for (String shareKey : List.of("saleAmountRatio", "revenueRatio", "profitShare", "grossShare",
            "customerCountRatio", "unsettledRatio", "debtRatio")) {
            s.put(shareKey, null);
        }
        return s;
    }

    // ════════════════════════════════════════════════════════════════
    //  分页输出
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> pageResult(List<Map<String, Object>> all, SaleAnalysisReportQueryDTO q) {
        int page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : q.getSize();
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", new ArrayList<>(all.subList(from, to)));
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        out.put("pages", (int) Math.ceil(total / (double) size));
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  1. 销售业绩（按时间 / 按职员）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> salesPerformance(SaleAnalysisReportQueryDTO query) {
        String dim = "staff".equals(query.getTab()) ? "staff" : "time";
        DimCtx d = dimCtx(dim, query.getGranularity());
        Map<String, Row> rows = merge(
            saleHeaderBlock(d, query),
            saleItemBlock(d, query),
            returnHeaderBlock(d, query),
            returnItemBlock(d, query),
            orderHeaderBlock(d, query),
            orderItemBlock(d, query),
            receiptBlock(d, query),
            partyBlock(d, query));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : rows.values()) {
            Map<String, Object> m = r.m;
            derive(m, true, false);
            if (isZeroQtyFiltered(query, m)) {
                continue;
            }
            out.add(m);
        }
        sortBy(out, "time".equals(dim) ? "dimKey" : "saleAmount", "time".equals(dim));
        Map<String, Object> result = pageResult(out, query);
        result.put("summary", summary(out, true, false));
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  2. 销售分析（8 维度量本利）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> salesAnalysis(SaleAnalysisReportQueryDTO query) {
        String tab = query.getTab() == null ? "time" : query.getTab();
        String dim = switch (tab) {
            case "product", "brand", "customer", "region", "warehouse", "staff", "source" -> tab;
            default -> "time";
        };
        DimCtx d = dimCtx(dim, query.getGranularity());
        Map<String, Row> rows = merge(
            saleHeaderBlock(d, query),
            saleItemBlock(d, query),
            returnHeaderBlock(d, query),
            returnItemBlock(d, query),
            orderHeaderBlock(d, query),
            orderItemBlock(d, query),
            preReceiptBlock(d, query),
            partyBlock(d, query));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : rows.values()) {
            Map<String, Object> m = r.m;
            derive(m, false, false);
            if (isZeroQtyFiltered(query, m)) {
                continue;
            }
            out.add(m);
        }
        sortBy(out, "time".equals(dim) ? "dimKey" : "saleAmount", "time".equals(dim));
        deriveShares(out);
        Map<String, Object> result = pageResult(out, query);
        result.put("summary", summary(out, false, false));
        return result;
    }

    /** 「显示销售数量大于0」勾选：过滤掉销售数量 ≤ 0 的行 */
    private boolean isZeroQtyFiltered(SaleAnalysisReportQueryDTO q, Map<String, Object> m) {
        return Boolean.TRUE.equals(q.getOnlyPositiveQty())
            && num(m, "saleQty").compareTo(BigDecimal.ZERO) <= 0;
    }

    // ════════════════════════════════════════════════════════════════
    //  3. 销售履约分析（按单据 / 按客户）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> salesFulfillment(SaleAnalysisReportQueryDTO query) {
        List<Map<String, Object>> orders = queryFulfillmentOrders(query);
        List<Map<String, Object>> rows = "customer".equals(query.getTab())
            ? groupFulfillmentByCustomer(orders)
            : orders;
        Map<String, Object> result = pageResult(rows, query);
        result.put("summary", fulfillmentSummary(rows));
        return result;
    }

    /** 履约订单明细（一行一订单） */
    private List<Map<String, Object>> queryFulfillmentOrders(SaleAnalysisReportQueryDTO q) {
        Where w = new Where();
        w.dateFrom("so.order_date", q.getStartDate());
        w.dateTo("so.order_date", q.getEndDate());
        Long cid = numericId(q.getCustomerId());
        if (cid != null) {
            w.eq("so.customer_id", cid);
        } else {
            w.like("so.customer_name", q.getCustomerName());
        }
        w.like("so.salesman_name", q.getSalesmanName());
        w.like("so.dept_name", q.getDeptName());
        w.like("so.warehouse_name", q.getWarehouseName());
        w.like("COALESCE(NULLIF(so.region,''), bp.region)", q.getRegion());
        w.like("so.generation_method", q.getSource());
        if (StringUtils.hasText(q.getCustomerCategoryIds())) {
            List<Object> catIds = new ArrayList<>();
            for (String part : q.getCustomerCategoryIds().split(",")) {
                Long id = numericId(part);
                if (id != null) {
                    catIds.add(id);
                }
            }
            if (!catIds.isEmpty()) {
                w.raw("so.customer_id IN (SELECT bp2.id FROM biz_party bp2"
                    + " WHERE bp2.deleted = 0 AND bp2.category_id IN (" + "?,".repeat(catIds.size() - 1) + "?))");
                w.addParams(catIds.toArray());
            }
        }
        if (StringUtils.hasText(q.getDocStatus())) {
            List<Object> codes = new ArrayList<>();
            for (String part : q.getDocStatus().split(",")) {
                try {
                    codes.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                    // 非法状态码忽略
                }
            }
            if (!codes.isEmpty()) {
                w.raw("so.status IN (" + "?,".repeat(codes.size() - 1) + "?)");
                w.addParams(codes.toArray());
            }
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT so.id AS \"orderId\", so.order_no AS \"docNo\","
            + " to_char(so.order_date,'YYYY-MM-DD') AS \"bizDate\", so.status AS \"orderStatus\","
            + " COALESCE(so.customer_id::text, so.customer_name) AS \"customerKey\","
            + " so.customer_name AS \"customerName\", so.customer_code AS \"customerCode\","
            + " so.settlement_method AS \"settlementMethod\","
            + " COALESCE(so.total_quantity,0) AS \"orderQty\","
            + " " + ORDER_TAX_AMOUNT + " AS \"taxAmount\","
            + " COALESCE(so.settled_amount, so.received_amount, 0) AS \"settledAmount\""
            + " FROM erp_sale_order so"
            + " LEFT JOIN biz_party bp ON bp.id = so.customer_id AND bp.tenant_id = so.tenant_id AND bp.deleted = 0"
            + " WHERE so.deleted = 0 AND so.tenant_id = ? AND so.status <> 6" + w.sql()
            + " ORDER BY so.order_date DESC NULLS LAST, so.id DESC";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, params.toArray());
        if (list.isEmpty()) {
            return list;
        }
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> r : list) {
            if (r.get("orderId") instanceof Number n) {
                ids.add(n.longValue());
            }
        }
        Map<Long, Map<String, Object>> ship = fulfillmentShipped(ids);
        Map<Long, BigDecimal> receiptByOrder = fulfillmentReceipt(ids);
        for (Map<String, Object> r : list) {
            Long oid = r.get("orderId") instanceof Number n ? n.longValue() : null;
            Map<String, Object> sp = oid == null ? null : ship.get(oid);
            BigDecimal shippedQty = sp == null ? BigDecimal.ZERO : toDecimal(sp.get("shippedQty"));
            BigDecimal shippedAmount = sp == null ? BigDecimal.ZERO : toDecimal(sp.get("shippedAmount"));
            BigDecimal tax = toDecimal(r.get("taxAmount"));
            BigDecimal settled = toDecimal(r.get("settledAmount"));
            BigDecimal receipt = oid == null ? null : receiptByOrder.get(oid);
            if (receipt != null) {
                settled = receipt;
            }
            r.put("shippedQty", shippedQty);
            r.put("shippedAmount", shippedAmount);
            r.put("unshippedQty", toDecimal(r.get("orderQty")).subtract(shippedQty));
            r.put("unshippedAmount", tax.subtract(shippedAmount));
            r.put("settledAmount", settled);
            r.put("unsettledAmount", tax.subtract(settled));
            r.put("fulfillmentRate", rate(shippedAmount, tax, 2));
            r.put("orderStatusText", orderStatusText(r.get("orderStatus")));
            // 一行一单据：单据数用于合计行口径
            r.put("orderDocCount", BigDecimal.ONE);
            if (r.get("customerKey") == null) {
                r.put("customerKey", "");
            }
        }
        return list;
    }

    /** 发货聚合：按来源订单ID */
    private Map<Long, Map<String, Object>> fulfillmentShipped(List<Long> orderIds) {
        Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
        if (orderIds.isEmpty()) {
            return out;
        }
        String sql = "SELECT o.order_id AS \"orderId\","
            + " SUM(COALESCE(o.total_quantity,0)) AS \"shippedQty\","
            + " SUM(COALESCE(o.total_amount,0)) AS \"shippedAmount\""
            + " FROM erp_sale_outbound o"
            + " WHERE o.deleted = 0 AND o.tenant_id = ? AND o.status > 0 AND o.status <> 12"
            + " AND o.order_id IN (" + "?,".repeat(orderIds.size() - 1) + "?)"
            + " GROUP BY o.order_id";
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(orderIds);
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            if (r.get("orderId") instanceof Number n) {
                out.put(n.longValue(), r);
            }
        }
        return out;
    }

    /** 已结金额：优先取该订单下收款单实收合计 */
    private Map<Long, BigDecimal> fulfillmentReceipt(List<Long> orderIds) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        if (orderIds.isEmpty()) {
            return out;
        }
        String sql = "SELECT r.order_id AS \"orderId\", SUM(COALESCE(r.receipt_amount,0)) AS \"amt\""
            + " FROM erp_receipt r WHERE r.deleted = 0 AND r.tenant_id = ?"
            + " AND r.status >= 2 AND r.status <> 3 AND r.status <> 8"
            + " AND r.order_id IN (" + "?,".repeat(orderIds.size() - 1) + "?)"
            + " GROUP BY r.order_id";
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(orderIds);
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            if (r.get("orderId") instanceof Number n) {
                out.put(n.longValue(), toDecimal(r.get("amt")));
            }
        }
        return out;
    }

    private static String orderStatusText(Object status) {
        int s = status instanceof Number n ? n.intValue() : -1;
        return switch (s) {
            case 0 -> "草稿";
            case 1 -> "待审核";
            case 2 -> "待付款";
            case 3 -> "待发货";
            case 4 -> "发货完成";
            case 5 -> "交易完成";
            case 6 -> "已取消";
            default -> "未知";
        };
    }

    /** 按客户汇总履约 */
    private List<Map<String, Object>> groupFulfillmentByCustomer(List<Map<String, Object>> orders) {
        Map<String, Map<String, Object>> acc = new LinkedHashMap<>();
        for (Map<String, Object> o : orders) {
            String key = String.valueOf(o.get("customerKey"));
            Map<String, Object> m = acc.computeIfAbsent(key, k -> {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("customerKey", k);
                n.put("customerName", o.get("customerName"));
                n.put("customerCode", o.get("customerCode"));
                n.put("settlementMethod", o.get("settlementMethod"));
                n.put("orderDocCount", BigDecimal.ZERO);
                n.put("orderQty", BigDecimal.ZERO);
                n.put("shippedQty", BigDecimal.ZERO);
                n.put("shippedAmount", BigDecimal.ZERO);
                n.put("taxAmount", BigDecimal.ZERO);
                n.put("settledAmount", BigDecimal.ZERO);
                return n;
            });
            m.put("orderDocCount", toDecimal(m.get("orderDocCount")).add(BigDecimal.ONE));
            for (String f : List.of("orderQty", "shippedQty", "shippedAmount", "taxAmount", "settledAmount")) {
                m.put(f, toDecimal(m.get(f)).add(toDecimal(o.get(f))));
            }
        }
        List<Map<String, Object>> out = new ArrayList<>(acc.values());
        for (Map<String, Object> m : out) {
            BigDecimal tax = toDecimal(m.get("taxAmount"));
            m.put("unshippedQty", toDecimal(m.get("orderQty")).subtract(toDecimal(m.get("shippedQty"))));
            m.put("unshippedAmount", tax.subtract(toDecimal(m.get("shippedAmount"))));
            m.put("unsettledAmount", tax.subtract(toDecimal(m.get("settledAmount"))));
            m.put("fulfillmentRate", rate(toDecimal(m.get("shippedAmount")), tax, 2));
        }
        sortBy(out, "taxAmount", false);
        return out;
    }

    private Map<String, Object> fulfillmentSummary(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (String f : List.of("orderDocCount", "orderQty", "shippedQty", "shippedAmount",
            "taxAmount", "settledAmount", "unshippedQty", "unshippedAmount", "unsettledAmount")) {
            s.put(f, sum(rows, f));
        }
        s.put("fulfillmentRate", rate(s.get("shippedAmount"), s.get("taxAmount"), 2));
        return s;
    }

    // ════════════════════════════════════════════════════════════════
    //  4. 销售欠款分析（按职员 / 按客户 / 按区域）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> salesDebt(SaleAnalysisReportQueryDTO query) {
        String tab = query.getTab() == null ? "staff" : query.getTab();
        String dim = switch (tab) {
            case "customer", "region" -> tab;
            default -> "staff";
        };
        DimCtx d = dimCtx(dim, null);
        Map<String, Row> rows = merge(
            saleItemBlock(d, query),
            returnHeaderBlock(d, query),
            returnItemBlock(d, query),
            receiptBlock(d, query),
            preReceiptBlock(d, query),
            partyBlock(d, query),
            receivableBlock(d, query));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : rows.values()) {
            Map<String, Object> m = r.m;
            // 本期新增收款 = 收款单实收（预收款单余额单独作「预收余额」列，不重复计入收款额）
            m.put("receiptInPeriod", num(m, "receiptAmount"));
            m.put("settleDiscountInPeriod", num(m, "receiptDiscount"));
            derive(m, true, true);
            out.add(m);
        }
        sortBy(out, "debtBalance", false);
        deriveShares(out);
        Map<String, Object> result = pageResult(out, query);
        result.put("summary", summary(out, true, true));
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  排序
    // ════════════════════════════════════════════════════════════════

    private void sortBy(List<Map<String, Object>> rows, String field, boolean asc) {
        Comparator<Map<String, Object>> cmp = (a, b) -> {
            Object va = a.get(field);
            Object vb = b.get(field);
            if (va == null && vb == null) {
                return 0;
            }
            if (va == null) {
                return 1;
            }
            if (vb == null) {
                return -1;
            }
            if (va instanceof Number && vb instanceof Number) {
                return toDecimal(va).compareTo(toDecimal(vb));
            }
            return va.toString().compareTo(vb.toString());
        };
        rows.sort(asc ? cmp : cmp.reversed());
    }
}
