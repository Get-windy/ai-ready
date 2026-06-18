package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sales.pricing.service.IDroolsRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Drools规则服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroolsRuleServiceImpl implements IDroolsRuleService {
    
    private KieContainer kieContainer;
    private KieSession kieSession;
    private final KieServices kieServices = KieServices.Factory.get();
    
    @Override
    public void initRulesEngine() {
        log.info("初始化Drools规则引擎");
        
        try {
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            KieRepository kieRepository = kieServices.getRepository();
            
            // 加载默认规则文件
            loadDefaultRules(kieFileSystem);
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                log.error("规则编译错误: {}", kieBuilder.getResults().getMessages());
                throw BusinessException.badRequest("规则编译失败");
            }
            
            kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
            kieSession = kieContainer.newKieSession();
            
            log.info("Drools规则引擎初始化完成");
        } catch (Exception e) {
            log.error("Drools规则引擎初始化失败", e);
            throw BusinessException.badRequest("规则引擎初始化失败");
        }
    }
    
    private void loadDefaultRules(KieFileSystem kieFileSystem) {
        // 基础价格计算规则
        String basicPriceRule = "package cn.aiedge.erp.sales.pricing.rules\n" +
                "\n" +
                "import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest\n" +
                "import cn.aiedge.erp.sales.pricing.dto.PriceCalculationResult\n" +
                "\n" +
                "rule \"基础价格规则\"\n" +
                "    when\n" +
                "        $request: PriceCalculationRequest()\n" +
                "    then\n" +
                "        System.out.println(\"应用基础价格规则\");\n" +
                "end\n" +
                "\n" +
                "rule \"数量折扣规则\"\n" +
                "    when\n" +
                "        $request: PriceCalculationRequest(quantity >= 100)\n" +
                "    then\n" +
                "        System.out.println(\"应用数量折扣规则: 数量 >= 100\");\n" +
                "end\n" +
                "\n" +
                "rule \"VIP客户折扣规则\"\n" +
                "    when\n" +
                "        $request: PriceCalculationRequest(customerLevel == \"VIP\")\n" +
                "    then\n" +
                "        System.out.println(\"应用VIP客户折扣规则\");\n" +
                "end";
        
        kieFileSystem.write("src/main/resources/rules/basic-pricing-rules.drl", basicPriceRule);
    }
    
    @Override
    public void reloadRules() {
        log.info("重新加载Drools规则");
        
        if (kieSession != null) {
            kieSession.dispose();
        }
        
        initRulesEngine();
    }
    
    @Override
    public KieSession getRuleSession() {
        if (kieSession == null) {
            initRulesEngine();
        }
        return kieSession;
    }
    
    @Override
    public <T> T executeRules(T fact) {
        log.debug("执行Drools规则计算: factType={}", fact.getClass().getSimpleName());
        
        KieSession session = getRuleSession();
        session.insert(fact);
        session.fireAllRules();
        
        return fact;
    }
    
    @Override
    public <T> T executeBatchRules(T facts) {
        log.debug("批量执行Drools规则计算");
        
        if (facts instanceof Iterable) {
            KieSession session = getRuleSession();
            for (Object fact : (Iterable<?>) facts) {
                session.insert(fact);
            }
            session.fireAllRules();
        }
        
        return facts;
    }
    
    @Override
    public boolean validateRuleSyntax(String ruleContent) {
        try {
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            kieFileSystem.write("src/main/resources/temp-rule-validation.drl", ruleContent);
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            return !kieBuilder.getResults().hasMessages(Message.Level.ERROR);
        } catch (Exception e) {
            log.error("规则语法验证失败", e);
            return false;
        }
    }
    
    @Override
    public void addRule(String ruleName, String ruleContent) {
        log.info("添加新规则: ruleName={}", ruleName);
        
        if (!validateRuleSyntax(ruleContent)) {
            throw BusinessException.badRequest("规则语法错误");
        }
        
        try {
            Path rulePath = Paths.get("src/main/resources/rules", ruleName + ".drl");
            Files.createDirectories(rulePath.getParent());
            Files.writeString(rulePath, ruleContent);
            
            reloadRules();
        } catch (IOException e) {
            log.error("保存规则文件失败", e);
            throw BusinessException.badRequest("保存规则文件失败");
        }
    }
    
    @Override
    public void updateRule(String ruleName, String newRuleContent) {
        log.info("更新规则: ruleName={}", ruleName);
        
        if (!validateRuleSyntax(newRuleContent)) {
            throw BusinessException.badRequest("规则语法错误");
        }
        
        try {
            Path rulePath = Paths.get("src/main/resources/rules", ruleName + ".drl");
            Files.writeString(rulePath, newRuleContent);
            
            reloadRules();
        } catch (IOException e) {
            log.error("更新规则文件失败", e);
            throw BusinessException.badRequest("更新规则文件失败");
        }
    }
    
    @Override
    public void deleteRule(String ruleName) {
        log.info("删除规则: ruleName={}", ruleName);
        
        try {
            Path rulePath = Paths.get("src/main/resources/rules", ruleName + ".drl");
            Files.deleteIfExists(rulePath);
            
            reloadRules();
        } catch (IOException e) {
            log.error("删除规则文件失败", e);
            throw BusinessException.badRequest("删除规则文件失败");
        }
    }
    
    @Override
    public List<RuleInfo> getRuleList() {
        List<RuleInfo> ruleList = new ArrayList<>();
        
        try {
            Path rulesDir = Paths.get("src/main/resources/rules");
            if (Files.exists(rulesDir)) {
                Files.list(rulesDir)
                        .filter(path -> path.toString().endsWith(".drl"))
                        .forEach(path -> {
                            try {
                                RuleInfo ruleInfo = new RuleInfo();
                                ruleInfo.setRuleName(path.getFileName().toString().replace(".drl", ""));
                                ruleInfo.setRuleContent(Files.readString(path));
                                ruleInfo.setCreateTime(LocalDateTime.now()); // 简化处理
                                ruleInfo.setUpdateTime(LocalDateTime.now());
                                ruleInfo.setActive(true);
                                
                                ruleList.add(ruleInfo);
                            } catch (IOException e) {
                                log.error("读取规则文件失败: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error("获取规则列表失败", e);
        }
        
        return ruleList;
    }
}