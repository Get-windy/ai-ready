package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.CleanupRule;

import java.util.List;
import java.util.Map;

/**
 * 数据清理规则服务接口
 */
public interface CleanupRuleService {

    List<CleanupRule> list(Long tenantId);

    CleanupRule create(CleanupRule rule, Long tenantId, String createBy);

    CleanupRule update(Long id, CleanupRule rule, Long tenantId, String updateBy);

    boolean delete(Long id);

    /**
     * 立即执行清理（真实按保留天数删除目标表历史数据）
     *
     * <p>返回 {@code Map} 而非 boolean —— 调用方必须拿到「预统计多少行 / 实际删了多少行 /
     * 是否触及单次上限 / 为什么没执行」，否则页面只能猜，而猜就是上一版的「谎报已触发」。
     *
     * @param confirm 必须显式 true；缺省拒绝（破坏性动作的二次确认闸门）
     * @param dryRun  仅预统计不删除（文档 §5.6 ⑧「干跑/预演」），用于执行前确认影响面
     */
    Map<String, Object> execute(Long id, boolean confirm, boolean dryRun, Long tenantId, String operator);

    /** 当前生效的清理白名单（供页面/运维发现「哪些表允许清理」，避免盲填表名后被拒） */
    List<Map<String, Object>> allowedTables();
}
