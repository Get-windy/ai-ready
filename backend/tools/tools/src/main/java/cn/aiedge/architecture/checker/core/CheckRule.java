package cn.aiedge.architecture.checker.core;

import cn.aiedge.architecture.checker.model.CheckResult;
import cn.aiedge.architecture.checker.model.ProjectContext;

/**
 * 检查规则接口
 * 
 * <p>所有检查规则必须实现此接口，定义检查逻辑</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
public interface CheckRule {

    /**
     * 获取规则ID
     * 
     * @return 规则唯一标识符
     */
    String getId();

    /**
     * 获取规则名称
     * 
     * @return 规则名称
     */
    String getName();

    /**
     * 获取规则描述
     * 
     * @return 规则详细描述
     */
    String getDescription();

    /**
     * 获取规则类别
     * 
     * @return 规则类别（如directory, package, api, code等）
     */
    String getCategory();

    /**
     * 获取规则严重级别
     * 
     * @return 严重级别（ERROR, WARNING, INFO）
     */
    Severity getSeverity();

    /**
     * 获取规则优先级（数值越小优先级越高）
     * 
     * @return 优先级数值
     */
    int getPriority();

    /**
     * 检查是否启用此规则
     * 
     * @return true表示启用，false表示禁用
     */
    boolean isEnabled();

    /**
     * 执行检查
     * 
     * @param context 项目上下文
     * @return 检查结果
     */
    CheckResult check(ProjectContext context);

    /**
     * 获取修复建议
     * 
     * @param context 项目上下文
     * @return 修复建议描述
     */
    String getFixSuggestion(ProjectContext context);

    /**
     * 严重级别枚举
     */
    enum Severity {
        /**
         * 错误级别，必须修复
         */
        ERROR(3),
        
        /**
         * 警告级别，建议修复
         */
        WARNING(2),
        
        /**
         * 信息级别，仅供参考
         */
        INFO(1);
        
        private final int level;
        
        Severity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
        
        /**
         * 比较严重级别
         * 
         * @param other 其他严重级别
         * @return true表示当前级别高于或等于其他级别
         */
        public boolean isAtLeast(Severity other) {
            return this.level >= other.level;
        }
    }
}