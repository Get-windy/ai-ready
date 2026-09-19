package cn.aiedge.workflow.model;

import lombok.Data;

import java.util.List;

/**
 * 审核设置（80622）保存某种单据类型审核规则的请求体。
 *
 * <p>请求示例：
 * <pre>
 * {
 *   "rules": [
 *     { "condition": "below_cost",
 *       "approvers": [ { "userId": "1930000000000000001", "userName": "杨生淮" } ] }
 *   ]
 * }
 * </pre>
 *
 * <p>⚠️ {@code userId} 用 String 承载：本库主键是雪花 ID（BIGINT，超出 JS 安全整数范围），
 * 前端一律按字符串处理，禁止 Number(id) 转换。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class AuditRuleSaveRequest {

    /** 该单据类型下的规则集合（只传已启用的项；未启用的项由前端不上送） */
    private List<RuleItem> rules;

    /** 单条规则：一个审核条件 + 一组审批人（多人即「或签」：任一审核即通过，见页面顶部说明条） */
    @Data
    public static class RuleItem {
        /** 审核条件编码，必为 {@link AuditRuleCatalog#CONDITIONS} 的键 */
        private String condition;
        /** 审批人集合 */
        private List<Approver> approvers;
    }

    /** 审批人（userId 为主键字符串，userName 为配置时的姓名快照） */
    @Data
    public static class Approver {
        private String userId;
        private String userName;
    }
}
