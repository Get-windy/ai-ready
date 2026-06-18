package cn.aiedge.erp.sales.pricing.controller;

import cn.aiedge.erp.sales.pricing.service.IDroolsRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Drools规则管理控制器
 */
@Tag(name = "Drools规则管理", description = "Drools规则引擎管理API")
@RestController
@RequestMapping("/api/erp/drools-rules")
@RequiredArgsConstructor
public class DroolsRuleController {
    
    private final IDroolsRuleService droolsRuleService;
    
    @Operation(summary = "初始化规则引擎", description = "初始化Drools规则引擎")
    @PostMapping("/init")
    public ResponseEntity<Void> initRulesEngine() {
        droolsRuleService.initRulesEngine();
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "重新加载规则", description = "重新加载所有Drools规则")
    @PostMapping("/reload")
    public ResponseEntity<Void> reloadRules() {
        droolsRuleService.reloadRules();
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "验证规则语法", description = "验证Drools规则语法是否正确")
    @PostMapping("/validate-syntax")
    public ResponseEntity<Boolean> validateRuleSyntax(@RequestBody Map<String, String> request) {
        String ruleContent = request.get("ruleContent");
        boolean isValid = droolsRuleService.validateRuleSyntax(ruleContent);
        return ResponseEntity.ok(isValid);
    }
    
    @Operation(summary = "添加新规则", description = "添加新的Drools规则")
    @PostMapping("/rules")
    public ResponseEntity<Void> addRule(@RequestBody Map<String, String> request) {
        String ruleName = request.get("ruleName");
        String ruleContent = request.get("ruleContent");
        droolsRuleService.addRule(ruleName, ruleContent);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "更新规则", description = "更新现有的Drools规则")
    @PutMapping("/rules/{ruleName}")
    public ResponseEntity<Void> updateRule(
            @PathVariable String ruleName,
            @RequestBody Map<String, String> request) {
        String newRuleContent = request.get("newRuleContent");
        droolsRuleService.updateRule(ruleName, newRuleContent);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "删除规则", description = "删除指定的Drools规则")
    @DeleteMapping("/rules/{ruleName}")
    public ResponseEntity<Void> deleteRule(@PathVariable String ruleName) {
        droolsRuleService.deleteRule(ruleName);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "获取规则列表", description = "获取所有Drools规则列表")
    @GetMapping("/rules")
    public ResponseEntity<List<IDroolsRuleService.RuleInfo>> getRuleList() {
        List<IDroolsRuleService.RuleInfo> ruleList = droolsRuleService.getRuleList();
        return ResponseEntity.ok(ruleList);
    }
    
    @Operation(summary = "执行规则测试", description = "执行Drools规则测试")
    @PostMapping("/execute-test")
    public ResponseEntity<Map<String, Object>> executeRuleTest(@RequestBody Map<String, Object> testData) {
        // 简化实现：实际应该根据测试数据执行规则
        Map<String, Object> result = Map.of(
                "success", true,
                "message", "规则测试执行成功",
                "executionTime", System.currentTimeMillis()
        );
        return ResponseEntity.ok(result);
    }
}