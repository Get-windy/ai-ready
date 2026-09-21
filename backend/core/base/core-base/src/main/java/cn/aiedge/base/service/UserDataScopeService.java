package cn.aiedge.base.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作员数据权限服务（对象清单授权）
 * <p>
 * 维度 → 授权对象清单的读写。存储为单表 {@code sys_user_data_scope}，
 * 同一（操作员, 维度）仅一行有效记录，保存采用覆盖式。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface UserDataScopeService {

    /**
     * 合法维度白名单（非法值一律拒绝，禁止写入任意字符串）
     */
    Set<String> SCOPE_KEYS = Set.of(
            "warehouse",       // 仓库权限
            "transfer",        // 调拨权限
            "department",      // 部门权限
            "partner",         // 往来单位权限
            "product",         // 商品权限
            "fund",            // 现金银行权限
            "customer_level"   // 客户级别权限
    );

    /**
     * 查询某操作员所有已设置维度 → 授权对象 id 列表
     *
     * @param userId 操作员ID
     * @return 维度 → id 列表；未设置任何维度时返回空 Map
     */
    Map<String, List<String>> getUserScopes(Long userId);

    /**
     * 覆盖式保存某操作员某维度的授权对象清单
     * <p>
     * 先按 (user_id, scope_key, deleted=0) 逻辑删除旧行，再插入新行；
     * targetIds 为空或全为空白时只删不插（等价于清除该维度）。
     * </p>
     *
     * @param userId    操作员ID
     * @param scopeKey  维度（必须在白名单内）
     * @param targetIds 授权对象 id 列表
     */
    void saveScope(Long userId, String scopeKey, List<String> targetIds);

    /**
     * 清除某操作员某维度的授权对象清单
     */
    void clearScope(Long userId, String scopeKey);

    /**
     * 校验维度是否合法
     */
    boolean isValidScopeKey(String scopeKey);

    /**
     * 查询某维度下**可授权的候选对象清单**（供前端「XX数据权限设置」弹窗渲染）
     * <p>
     * 各维度的数据源不同（仓库 / 往来单位 / 商品 / 部门 / 现金银行 / 客户级别），
     * 统一返回 {@code [{id, name, code, categoryId, categoryName}]} 结构；
     * {@code categoryName} 供前端左侧分类栏使用（无分类的维度返回 null）。
     * </p>
     * <p>
     * 🔴 租户安全：这里用 JdbcTemplate 手写 SQL，**多租户拦截器不会介入内嵌查询**，
     * 因此必须显式绑定当前会话 tenant_id；取不到会话租户时直接返回空清单，
     * 绝不退化成「不带租户条件的全表查询」（那会造成跨租户数据泄露）。
     * </p>
     *
     * @param scopeKey 维度（必须在白名单内）
     * @param keyword  名称/编号模糊匹配（可空）
     * @return 候选对象清单
     */
    List<Map<String, Object>> listTargets(String scopeKey, String keyword);
}
