package cn.aiedge.erp.finance.analytics.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件拼接器：只拼接固定 SQL 片段，所有用户输入一律以 {@code ?} 占位传参。
 */
public final class SqlWhere {

    private final StringBuilder sb = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public SqlWhere raw(String sql) {
        sb.append(" AND ").append(sql);
        return this;
    }

    public SqlWhere like(String expr, String v) {
        if (StringUtils.hasText(v)) {
            sb.append(" AND ").append(expr).append(" LIKE ?");
            params.add("%" + v.trim() + "%");
        }
        return this;
    }

    public SqlWhere eq(String expr, Object v) {
        if (v != null) {
            sb.append(" AND ").append(expr).append(" = ?");
            params.add(v);
        }
        return this;
    }

    public SqlWhere dateFrom(String expr, String v) {
        if (StringUtils.hasText(v)) {
            sb.append(" AND ").append(expr).append(" >= ?::date");
            params.add(v.trim());
        }
        return this;
    }

    public SqlWhere dateTo(String expr, String v) {
        if (StringUtils.hasText(v)) {
            sb.append(" AND ").append(expr).append(" <= ?::date");
            params.add(v.trim());
        }
        return this;
    }

    public SqlWhere addParams(Object... values) {
        for (Object v : values) {
            params.add(v);
        }
        return this;
    }

    public String sql() {
        return sb.toString();
    }

    public List<Object> list() {
        return params;
    }
}
