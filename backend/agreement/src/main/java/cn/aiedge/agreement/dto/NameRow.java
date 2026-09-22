package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 「ID → 名称」的通用查询行（往来单位名 / 租户名）。
 *
 * <p>为什么单独查名称而不是在分页 SQL 里 JOIN：往来单位在 {@code biz_party}（租户维度表，
 * 会被租户拦截器过滤，而协议两端**天然跨租户**），租户在 {@code sys_tenant}（系统表）。
 * 两张表口径不同、且协议列表已带租户可见性条件，JOIN 进去会让可见性判定与展示耦合。
 * 这里按页内出现过的 id 各查一次，避免逐行 N+1。</p>
 */
@Data
public class NameRow {

    private Long id;

    /** 别名统一为 name，避免与实体字段名耦合 */
    private String name;
}
