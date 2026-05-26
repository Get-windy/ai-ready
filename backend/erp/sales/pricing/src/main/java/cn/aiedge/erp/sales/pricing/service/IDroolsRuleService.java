package cn.aiedge.erp.sales.pricing.service;

import org.kie.api.runtime.KieSession;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Drools规则服务接口
 */
public interface IDroolsRuleService {
    
    /**
     * 初始化规则引擎
     */
    void initRulesEngine();
    
    /**
     * 重新加载规则
     */
    void reloadRules();
    
    /**
     * 获取规则会话
     */
    KieSession getRuleSession();
    
    /**
     * 执行规则计算
     */
    <T> T executeRules(T fact);
    
    /**
     * 批量执行规则计算
     */
    <T> T executeBatchRules(T facts);
    
    /**
     * 验证规则语法
     */
    boolean validateRuleSyntax(String ruleContent);
    
    /**
     * 添加新规则
     */
    void addRule(String ruleName, String ruleContent);
    
    /**
     * 更新规则
     */
    void updateRule(String ruleName, String newRuleContent);
    
    /**
     * 删除规则
     */
    void deleteRule(String ruleName);
    
    /**
     * 获取规则列表
     */
    List<RuleInfo> getRuleList();
    
    /**
     * 规则信息
     */
    class RuleInfo {
        private String ruleName;
        private String ruleContent;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private boolean active;
        
        // getters and setters
    }
}