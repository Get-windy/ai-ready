package cn.aiedge.erp.finance.analytics.support;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务分析报表公共工具。
 *
 * <p>本包全部走 {@code JdbcTemplate} 手写 SQL，MyBatis-Plus 的多租户拦截器<b>不生效</b>，
 * 因此每条 SQL 都必须显式带 {@code tenant_id = ?}（参数化，非字符串拼接）。</p>
 */
public final class AnalyticsSupport {

    private AnalyticsSupport() {
    }

    /**
     * 当前登录会话租户；**取不到时明确拒绝，不回落到任何默认租户**。
     *
     * <p>⚠️ 2026-09-23 修复：原实现是 {@code return t == null ? 1L : t}（注释还写着"与既有分析模块实现一致"）。
     * 这是本仓已明令要根除的高危写法 —— 见 {@code DocQueryController#currentTenantId} 的注释：
     * 「一旦解析不出（旧 token、登录链路漏存 tenantId 等），回退 1 就等于**把租户 1 的数据展示给另一个租户的用户**」。</p>
     *
     * <p>本包所有端点在 Controller 层都带 {@code @SaCheckLogin}/{@code @SaCheckPermission}，
     * 正常必然解析得出租户，因此这里与 {@code DocQueryController}、{@code SetAppCenterController}
     * 保持同一口径：解析不出应当明确报「请重新登录」，而不是猜一个租户继续查。</p>
     */
    public static Long tenantId() {
        Long t = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (t == null) {
            throw new BusinessException(401, "无法确定当前租户，请重新登录");
        }
        return t;
    }

    /** invoice 表的 tenant_id 是 varchar(50)，与本系统其余表的 bigint 不同。 */
    public static String tenantIdText() {
        return String.valueOf(tenantId());
    }

    public static boolean hasText(String v) {
        return StringUtils.hasText(v);
    }

    public static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal round(BigDecimal v, int scale) {
        return nvl(v).setScale(scale, RoundingMode.HALF_UP);
    }

    /** 占比（%）：分母为 0 时返回 null（不填 0 冒充），由前端显示 `-`。 */
    public static BigDecimal ratio(BigDecimal part, BigDecimal total, int scale) {
        BigDecimal t = nvl(total);
        if (t.signum() == 0) {
            return null;
        }
        return nvl(part).multiply(BigDecimal.valueOf(100)).divide(t, scale, RoundingMode.HALF_UP);
    }

    /** 数字型主键（雪花 ID 为纯数字字符串） */
    public static Long numericId(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 聚合结果分页（内存分页：报表为分组聚合结果，行数受维度基数限制） */
    public static Map<String, Object> pageResult(List<Map<String, Object>> all, Integer pageNo, Integer pageSize) {
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : pageSize;
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

    /** 对结果集内所有数值列求和（合计行口径：按当前过滤范围，不是当前页） */
    public static Map<String, Object> sumAll(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v instanceof BigDecimal b) {
                    s.merge(k, b, (a, b2) -> ((BigDecimal) a).add((BigDecimal) b2));
                } else if (v instanceof Integer i) {
                    s.merge(k, BigDecimal.valueOf(i), (a, b2) -> ((BigDecimal) a).add((BigDecimal) b2));
                } else if (v instanceof Long l) {
                    s.merge(k, BigDecimal.valueOf(l), (a, b2) -> ((BigDecimal) a).add((BigDecimal) b2));
                }
            });
        }
        return s;
    }

    /** 按指定列降序（数值）排序，null 视为 0 */
    public static void sortDesc(List<Map<String, Object>> rows, String key) {
        rows.sort(Comparator.comparing((Map<String, Object> m) -> toDecimal(m.get(key))).reversed());
    }
}
