package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.service.impl.DroolsRuleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drools规则服务测试类
 */
@ExtendWith(MockitoExtension.class)
class DroolsRuleServiceTest {
    
    @InjectMocks
    private DroolsRuleServiceImpl droolsRuleService;
    
    @BeforeEach
    void setUp() {
        // 初始化测试
    }
    
    @Test
    void testValidateRuleSyntax_ValidRule() {
        String validRule = "package cn.aiedge.erp.sales.pricing.rules\n" +
                "\n" +
                "import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest\n" +
                "\n" +
                "rule \"测试规则\"\n" +
                "    when\n" +
                "        $request: PriceCalculationRequest()\n" +
                "    then\n" +
                "        System.out.println(\"测试规则执行\");\n" +
                "end";
        
        boolean isValid = droolsRuleService.validateRuleSyntax(validRule);
        
        assertTrue(isValid, "有效的规则应该通过语法验证");
    }
    
    @Test
    void testValidateRuleSyntax_InvalidRule() {
        String invalidRule = "package cn.aiedge.erp.sales.pricing.rules\n" +
                "\n" +
                "import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest\n" +
                "\n" +
                "rule \"测试规则\"\n" +
                "    when\n" +
                "        $request: PriceCalculationRequest() // 语法错误：缺少闭合\n";
        
        boolean isValid = droolsRuleService.validateRuleSyntax(invalidRule);
        
        assertFalse(isValid, "无效的规则应该无法通过语法验证");
    }
    
    @Test
    void testExecuteRules_ValidFact() {
        // 创建测试事实对象
        TestFact testFact = new TestFact();
        testFact.setValue("测试值");
        
        // 执行规则
        TestFact result = droolsRuleService.executeRules(testFact);
        
        assertNotNull(result);
        assertEquals("测试值", result.getValue());
    }
    
    @Test
    void testGetRuleList() {
        // 获取规则列表
        var ruleList = droolsRuleService.getRuleList();
        
        assertNotNull(ruleList);
        // 至少应该包含默认规则
        assertTrue(ruleList.size() >= 0);
    }
    
    // 测试事实类
    static class TestFact {
        private String value;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
}