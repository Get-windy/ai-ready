package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.UserDataScope;
import cn.aiedge.base.mapper.UserDataScopeMapper;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.base.service.UserDataScopeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 操作员数据权限服务实现
 * <p>
 * 多租户：{@code sys_user_data_scope} 有 tenant_id 且**不在**
 * MyBatisPlusConfig.IGNORE_TENANT_TABLES 白名单内 → 租户条件由拦截器自动注入，
 * 这里**不手写** tenant_id 条件（手写反而会与拦截器叠加、且在超管豁免等场景下出错）。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataScopeServiceImpl implements UserDataScopeService {

    private final UserDataScopeMapper userDataScopeMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityContext securityContext;

    @Override
    public Map<String, List<String>> getUserScopes(Long userId) {
        validateUserId(userId);
        List<UserDataScope> rows = userDataScopeMapper.selectList(
                new LambdaQueryWrapper<UserDataScope>()
                        .eq(UserDataScope::getUserId, userId)
                        .orderByAsc(UserDataScope::getScopeKey));
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (UserDataScope row : rows) {
            List<String> targets = parseTargetIds(row.getTargetIds());
            // 空清单视为未设置，不返回
            if (!targets.isEmpty()) {
                result.put(row.getScopeKey(), targets);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScope(Long userId, String scopeKey, List<String> targetIds) {
        validateUserId(userId);
        validateScopeKey(scopeKey);

        // 覆盖式保存：先逻辑删除旧行（@TableLogic 会把 deleted 置 1），再插入新行
        deleteByUserAndScope(userId, scopeKey);

        List<String> cleaned = parseTargetIds(targetIds);
        if (cleaned.isEmpty()) {
            // 只删不插 = 清除该维度
            log.info("清除操作员数据权限: userId={}, scopeKey={}", userId, scopeKey);
            return;
        }

        UserDataScope entity = new UserDataScope();
        entity.setUserId(userId);
        entity.setScopeKey(scopeKey);
        entity.setTargetIds(String.join(",", cleaned));
        userDataScopeMapper.insert(entity);
        log.info("保存操作员数据权限: userId={}, scopeKey={}, targetCount={}", userId, scopeKey, cleaned.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearScope(Long userId, String scopeKey) {
        validateUserId(userId);
        validateScopeKey(scopeKey);
        deleteByUserAndScope(userId, scopeKey);
        log.info("清除操作员数据权限: userId={}, scopeKey={}", userId, scopeKey);
    }

    @Override
    public boolean isValidScopeKey(String scopeKey) {
        return scopeKey != null && SCOPE_KEYS.contains(scopeKey);
    }

    @Override
    public List<Map<String, Object>> listTargets(String scopeKey, String keyword) {
        validateScopeKey(scopeKey);

        // 🔴 以下 SQL 由 JdbcTemplate 直接执行，多租户拦截器不会介入 → 必须显式绑定会话租户。
        //    取不到会话租户时返回空清单，绝不退化成「不带租户条件的全表查询」。
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId == null) {
            log.warn("查询数据权限候选对象时取不到会话租户，返回空清单: scopeKey={}", scopeKey);
            return List.of();
        }

        String kw = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";
        List<Object> args = new ArrayList<>();
        args.add(tenantId);

        // 各维度的对象表 / 名称列 / 编码列 / 分类表与分类外键，逐维度在这里显式声明
        String sql;
        switch (scopeKey) {
            case "warehouse":
            case "transfer":
                sql = "SELECT t.id AS id, t.warehouse_name AS name, t.warehouse_code AS code,"
                        + " t.category_id AS \"categoryId\", c.category_name AS \"categoryName\""
                        + " FROM erp_warehouse t"
                        + " LEFT JOIN erp_warehouse_category c ON c.id = t.category_id AND c.deleted = 0"
                        + " WHERE t.deleted = 0 AND t.tenant_id = ?";
                sql += likeClause(kw, args, "t.warehouse_name", "t.warehouse_code");
                sql += " ORDER BY t.sort_order NULLS LAST, t.id";
                break;
            case "department":
                sql = "SELECT t.id AS id, t.dept_name AS name, t.dept_code AS code,"
                        + " t.parent_id AS \"categoryId\", p.dept_name AS \"categoryName\""
                        + " FROM sys_department t"
                        + " LEFT JOIN sys_department p ON p.id = t.parent_id AND p.deleted = 0"
                        + " WHERE t.deleted = 0 AND t.tenant_id = ?";
                sql += likeClause(kw, args, "t.dept_name", "t.dept_code");
                sql += " ORDER BY t.sort NULLS LAST, t.id";
                break;
            case "partner":
                sql = "SELECT t.id AS id, t.partner_name AS name, t.partner_code AS code,"
                        + " t.partner_category_id AS \"categoryId\", c.category_name AS \"categoryName\""
                        + " FROM erp_partner t"
                        + " LEFT JOIN erp_partner_category c ON c.id = t.partner_category_id AND c.deleted = 0"
                        + " WHERE t.deleted = 0 AND t.tenant_id = ?";
                sql += likeClause(kw, args, "t.partner_name", "t.partner_code");
                sql += " ORDER BY t.partner_code, t.id";
                break;
            case "product":
                sql = "SELECT t.id AS id, t.product_name AS name, t.product_code AS code,"
                        + " t.category_id AS \"categoryId\", c.category_name AS \"categoryName\""
                        + " FROM erp_product t"
                        + " LEFT JOIN erp_product_category c ON c.id = t.category_id AND c.deleted = 0"
                        + " WHERE t.deleted = 0 AND t.tenant_id = ?";
                sql += likeClause(kw, args, "t.product_name", "t.product_code");
                sql += " ORDER BY t.product_code, t.id";
                break;
            case "fund":
                // finance_account 的逻辑删列是 deleted_flag（不是 deleted）
                sql = "SELECT t.id AS id, t.account_name AS name, t.bank_account AS code,"
                        + " NULL AS \"categoryId\", NULL AS \"categoryName\""
                        + " FROM finance_account t"
                        + " WHERE t.deleted_flag = 0 AND t.tenant_id = ?";
                sql += likeClause(kw, args, "t.account_name", "t.bank_account");
                sql += " ORDER BY t.sort_no NULLS LAST, t.id";
                break;
            case "customer_level":
                // erp_customer_level 无逻辑删列
                sql = "SELECT t.id AS id, t.level_name AS name, t.level_code AS code,"
                        + " NULL AS \"categoryId\", NULL AS \"categoryName\""
                        + " FROM erp_customer_level t"
                        + " WHERE t.tenant_id = ?";
                sql += likeClause(kw, args, "t.level_name", "t.level_code");
                sql += " ORDER BY t.sort_weight NULLS LAST, t.id";
                break;
            default:
                return List.of();
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args.toArray());
        // 统一 id 为字符串：本仓雪花 ID 超过 JS 安全整数范围，前端一律按字符串比较（禁止 Number(id)）
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id != null) {
                row.put("id", String.valueOf(id));
            }
        }
        return rows;
    }

    /**
     * 追加名称/编号模糊匹配条件（无关键字时返回空串），并同步压入 SQL 参数
     */
    private String likeClause(String kw, List<Object> args, String... columns) {
        if (kw == null || columns.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(columns[i]).append(" LIKE ?");
            args.add(kw);
        }
        return sb.append(")").toString();
    }

    /**
     * 逻辑删除某操作员某维度的全部有效行
     */
    private void deleteByUserAndScope(Long userId, String scopeKey) {
        userDataScopeMapper.delete(new LambdaQueryWrapper<UserDataScope>()
                .eq(UserDataScope::getUserId, userId)
                .eq(UserDataScope::getScopeKey, scopeKey));
    }

    /**
     * 解析并规整 id 列表：去空白、去空串、去重（保持顺序）
     */
    private List<String> parseTargetIds(List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : targetIds) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    /**
     * 解析库中逗号分隔的 id 串
     */
    private List<String> parseTargetIds(String targetIds) {
        if (targetIds == null || targetIds.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : targetIds.split(",")) {
            if (!id.isBlank()) {
                unique.add(id.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    /**
     * 校验操作员ID，非法抛 IllegalArgumentException（由 GlobalExceptionHandler 转 400）
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("操作员ID不能为空且必须大于0");
        }
    }

    /**
     * 校验维度白名单，非法抛 IllegalArgumentException（由 GlobalExceptionHandler 转 400）
     */
    private void validateScopeKey(String scopeKey) {
        if (!isValidScopeKey(scopeKey)) {
            throw new IllegalArgumentException("非法的数据权限维度: " + scopeKey);
        }
    }
}
