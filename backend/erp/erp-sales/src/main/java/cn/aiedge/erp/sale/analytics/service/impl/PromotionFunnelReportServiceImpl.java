package cn.aiedge.erp.sale.analytics.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.sale.analytics.dto.PromotionFunnelQueryDTO;
import cn.aiedge.erp.sale.analytics.service.PromotionFunnelReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 推广分析（职员分享推广漏斗）报表服务实现
 *
 * <p><b>数据源</b>：{@code mkt_share_record}（营销域「推广分享触达台账」，一行 = 一次分享及其触达效果，
 * 由《营销推广》的「我要推广」写入）。本页是<b>只读消费方</b>，不新增埋点表、不臆造埋点数据。</p>
 *
 * <p><b>行来源</b>：{@code sys_user}（启用职员，LEFT JOIN 分享台账）——对标本期出现「全 0 指标」的职员行，
 * 其语义即「已开通推广但本期无触达数据」，故行来源必须是职员档案而不是分享记录本身，
 * 否则无分享记录的职员会整行消失。</p>
 *
 * <p><b>口径</b>（《推广分析开发文档》§4 四段漏斗）：</p>
 * <ul>
 *   <li>分享次数 = 分享台账行数；浏览次数 = SUM(view_count)；下单笔数 = SUM(order_count)；下单金额 = SUM(order_amount)</li>
 *   <li>分享类型下拉 → {@code mkt_share_record.share_type} 白名单过滤</li>
 * </ul>
 *
 * <p><b>缺口登记（确认无数据源 → 返回 null，前端显示 -，不返回 0 冒充）</b>：</p>
 * <ul>
 *   <li><b>新客注册</b>：分享台账无「新客注册数」列，全库亦无「分享链接来源 → 客户注册」归因链路
 *       （已核 information_schema：mkt_share_record 仅有浏览/领取/下单计数器）。本列如实返回 null。</li>
 * </ul>
 *
 * <p><b>多租户</b>：手写 SQL 显式带 {@code tenant_id = ?}；用户输入一律以 {@code ?} 传入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionFunnelReportServiceImpl implements PromotionFunnelReportService {

    private final JdbcTemplate jdbcTemplate;

    /** 分享类型白名单（防注入：只允许登记在册的类型值） */
    private static final List<String> SHARE_TYPES =
        List.of("PRODUCT", "COUPON", "PROMOTION", "GROUP_BUY", "FLASH_SALE");

    /**
     * 当前登录会话租户；**取不到时明确拒绝**，不把 null 传进 SQL。
     *
     * <p>⚠️ 2026-09-23 修复：原实现直接返回可能为 null 的租户，而 SQL 里的 {@code tenant_id = null}
     * 恒不成立 ⇒ 列表**静默返回 0 行**，"数据凭空少了"却没有任何报错。
     * 按本仓已确立口径（{@code DocQueryController#currentTenantId}），解析不出租户应当明确报「请重新登录」。</p>
     */
    private static Long tenantId() {
        Long t = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (t == null) {
            throw new cn.aiedge.common.exception.BusinessException(401, "无法确定当前租户，请重新登录");
        }
        return t;
    }

    @Override
    public Map<String, Object> page(PromotionFunnelQueryDTO q) {
        Long tid = tenantId();
        // ⚠️ 参数必须严格按 SQL 文本中出现顺序累加：ON 子句条件在前，WHERE 的租户与职员条件在后
        List<Object> params = new ArrayList<>();
        // 分享台账侧条件（挂在 LEFT JOIN 条件里，保证「本期无分享」的职员仍出行）
        StringBuilder on = new StringBuilder(" AND s.sharer_id = u.id");
        if (StringUtils.hasText(q.getStartDate())) {
            on.append(" AND s.share_time >= ?::date");
            params.add(q.getStartDate().trim());
        }
        if (StringUtils.hasText(q.getEndDate())) {
            on.append(" AND s.share_time < ?::date + 1");
            params.add(q.getEndDate().trim());
        }
        if (StringUtils.hasText(q.getShareType()) && SHARE_TYPES.contains(q.getShareType().trim().toUpperCase())) {
            on.append(" AND s.share_type = ?");
            params.add(q.getShareType().trim().toUpperCase());
        }
        // ON 子句条件已就位，之后才是 WHERE 段占位符
        params.add(tid);
        StringBuilder where = new StringBuilder();
        if (StringUtils.hasText(q.getStaffName())) {
            where.append(" AND (u.real_name LIKE ? OR u.nickname LIKE ? OR u.username LIKE ?)");
            String kw = "%" + q.getStaffName().trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        String sql = "SELECT u.id::text AS \"staffId\", u.username AS \"staffCode\","
            + " COALESCE(NULLIF(u.real_name,''), u.nickname, u.username) AS \"dimLabel\","
            + " COUNT(s.id) AS \"shareCount\","
            + " COALESCE(SUM(s.view_count),0) AS \"viewCount\","
            + " COALESCE(SUM(s.order_count),0) AS \"orderCount\","
            + " COALESCE(SUM(s.order_amount),0) AS \"orderAmount\""
            + " FROM sys_user u"
            + " LEFT JOIN mkt_share_record s ON s.deleted = 0 AND s.tenant_id = u.tenant_id" + on
            + " WHERE u.deleted = 0 AND u.tenant_id = ? AND u.status = 1" + where
            + " GROUP BY u.id, u.username, u.nickname, u.real_name"
            + " ORDER BY COUNT(s.id) DESC, u.id";
        List<Map<String, Object>> all = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : all) {
            // 无「新客注册」归因链路：显式 null（前端显示 -），不返回 0 冒充
            r.put("newCustomerCount", null);
            r.put("rowKey", String.valueOf(r.get("staffId")));
        }
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
        out.put("summary", summary(all));
        return out;
    }

    private Map<String, Object> summary(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v instanceof BigDecimal || v instanceof Number) {
                    s.merge(k, toDecimal(v), (a, b) -> toDecimal(a).add(toDecimal(b)));
                }
            });
        }
        s.remove("staffId");
        s.put("newCustomerCount", null);
        return s;
    }

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

    /** 供排序扩展（当前 SQL 已排序，保留以避免子类重复实现） */
    @SuppressWarnings("unused")
    private static Comparator<Map<String, Object>> byNum(String key) {
        return Comparator.comparing(m -> toDecimal(m.get(key)));
    }
}
