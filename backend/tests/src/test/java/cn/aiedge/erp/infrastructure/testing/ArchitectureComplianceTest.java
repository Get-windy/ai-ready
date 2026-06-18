package cn.aiedge.erp.infrastructure.testing;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构合规性测试
 * 验证ERP系统架构规范是否得到遵守
 */
public class ArchitectureComplianceTest extends ArchitectureComplianceTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(ArchitectureComplianceTest.class);
    
    @Test
    void testModuleDirectoryStructure() {
        logger.info("开始测试模块目录结构...");
        validateModuleDirectoryStructure();
        logger.info("模块目录结构测试通过");
    }
    
    @Test
    void testPackageNamingConvention() {
        logger.info("开始测试包命名规范...");
        
        // 所有ERP模块包必须使用cn.aiedge.erp前缀
        ArchRule packageRule = classes()
            .that().resideInAPackage("..erp..")
            .should().resideInAPackage("cn.aiedge.erp..")
            .because("所有ERP模块必须使用cn.aiedge.erp包前缀");
        
        packageRule.check(classes);
        logger.info("包命名规范测试通过");
    }
    
    @Test
    void testApiPathConvention() {
        logger.info("开始测试API路径规范...");
        validateApiPathConvention();
        logger.info("API路径规范测试通过");
    }
    
    @Test
    void testCodeConventions() {
        logger.info("开始测试代码规范...");
        validateCodeConventions();
        logger.info("代码规范测试通过");
    }
    
    @Test
    void testModuleBoundaries() {
        logger.info("开始测试模块边界...");
        
        // 分层架构验证
        ArchRule layeredArchitectureRule = layeredArchitecture()
            .layer("Controller层").definedBy("..controller..")
            .layer("Service层").definedBy("..service..")
            .layer("Repository层").definedBy("..repository..")
            .layer("Entity层").definedBy("..entity..", "..model..", "..domain..")
            
            .whereLayer("Controller层").mayOnlyBeAccessedByLayers("Service层")
            .whereLayer("Service层").mayOnlyBeAccessedByLayers("Controller层", "Repository层")
            .whereLayer("Repository层").mayOnlyBeAccessedByLayers("Service层")
            .whereLayer("Entity层").mayOnlyBeAccessedByLayers("Repository层", "Service层", "Controller层");
        
        layeredArchitectureRule.check(classes);
        logger.info("模块边界测试通过");
    }
    
    @Test
    void testControllerNamingConvention() {
        logger.info("开始测试Controller命名规范...");
        
        ArchRule controllerRule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().haveSimpleNameContaining("Controller")
            .andShould().resideInAPackage("..controller..")
            .because("Controller类必须放在controller包中并以Controller结尾");
        
        controllerRule.check(classes);
        logger.info("Controller命名规范测试通过");
    }
    
    @Test
    void testServiceNamingConvention() {
        logger.info("开始测试Service命名规范...");
        
        ArchRule serviceRule = classes()
            .that().haveSimpleNameEndingWith("Service")
            .or().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage("..service..")
            .because("Service类必须放在service包中");
        
        serviceRule.check(classes);
        logger.info("Service命名规范测试通过");
    }
    
    @Test
    void testRepositoryNamingConvention() {
        logger.info("开始测试Repository命名规范...");
        
        ArchRule repositoryRule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..repository..")
            .because("Repository类必须放在repository包中");
        
        repositoryRule.check(classes);
        logger.info("Repository命名规范测试通过");
    }
    
    @Test
    void testNoCircularDependencies() {
        logger.info("开始测试循环依赖...");
        
        ArchRule noCircularDependenciesRule = slices()
            .matching("cn.aiedge.erp.(*)..")
            .should().beFreeOfCycles()
            .because("模块间不应该存在循环依赖");
        
        noCircularDependenciesRule.check(classes);
        logger.info("循环依赖测试通过");
    }
    
    @Test
    void testNoJavaUtilLogging() {
        logger.info("开始测试日志规范...");
        
        ArchRule loggingRule = noClasses()
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("java.util.logging..")
            .because("应该使用SLF4J而不是java.util.logging");
        
        loggingRule.check(classes);
        logger.info("日志规范测试通过");
    }
    
    @Test
    void testApiLayerDependencies() {
        logger.info("开始测试API层依赖...");
        
        ArchRule apiDependenciesRule = classes()
            .that().resideInAPackage("..controller..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "java..",
                "javax..",
                "jakarta..",
                "org.springframework..",
                "cn.aiedge.erp..service..",
                "cn.aiedge.erp..model..",
                "cn.aiedge.erp..dto.."
            )
            .because("Controller层应该只依赖必要的包");
        
        apiDependenciesRule.check(classes);
        logger.info("API层依赖测试通过");
    }
    
    @Test
    void testGenerateComplianceReport() {
        logger.info("开始生成架构合规性报告...");
        generateComplianceReport();
        logger.info("架构合规性报告生成完成");
    }
    
    @Test
    void testAllComplianceRules() {
        logger.info("开始执行所有架构合规性测试...");
        
        testModuleDirectoryStructure();
        testPackageNamingConvention();
        testApiPathConvention();
        testCodeConventions();
        testModuleBoundaries();
        testControllerNamingConvention();
        testServiceNamingConvention();
        testRepositoryNamingConvention();
        testNoCircularDependencies();
        testNoJavaUtilLogging();
        testApiLayerDependencies();
        
        logger.info("所有架构合规性测试通过");
    }
}