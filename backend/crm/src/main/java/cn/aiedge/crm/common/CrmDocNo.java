package cn.aiedge.crm.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * CRM 业务单号生成支持。
 *
 * <p>本模块历史实现用 {@code selectCount(...) + 1} 拼号，存在两个缺陷
 * （见 CRM 模块 README §7 P0「号段生成器会撞号」）：
 * <ol>
 *   <li>「删一条再建一条」必撞唯一约束 —— 唯一索引不含 deleted，已逻辑删除的行仍占着号；</li>
 *   <li>序号是全局流水而非当日流水，跨天后仍接着昨天的数字增长。</li>
 * </ol>
 *
 * <p>统一改为：取当日同前缀单号的<b>最大值 + 1</b>，且取值 SQL <b>不带 deleted 条件</b>
 * （即包含已逻辑删除的行），因此号码永不复用、永不撞索引。
 *
 * <p>各 Mapper 负责提供「同前缀单号最大值」的查询（表名/列名不同，SQL 写死在注解里，
 * 不做动态拼接，避免 SQL 注入），解析与格式化逻辑集中在本类。
 */
public final class CrmDocNo {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CrmDocNo() {
    }

    /**
     * 当日号前缀，例如传入 {@code "LEAD-"} 返回 {@code "LEAD-20260918"}。
     */
    public static String prefixOf(String prefix) {
        return prefix + LocalDate.now().format(DAY);
    }

    /**
     * 由当日已有单号的最大值顺延出下一个单号。
     *
     * @param prefix  当日号前缀，形如 {@code "LEAD-20260918"}
     * @param maxCode 库中同前缀单号的最大值（可含已逻辑删除行），可为 null
     * @param width   流水位数，如 4 表示 {@code 0001}
     */
    public static String next(String prefix, String maxCode, int width) {
        int seq = 1;
        if (maxCode != null && maxCode.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%0" + width + "d", seq);
    }
}
