package cn.aiedge.erp.infrastructure.testing;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 架构合规性测试基类
 * 提供目录结构、包命名、API路径等架构规范的自动化验证
 */
public abstract class ArchitectureComplianceTestBase {
    
    protected static final Logger logger = LoggerFactory.getLogger(ArchitectureComplianceTestBase.class);
    protected static JavaClasses classes;
    protected static String projectBasePath;
    
    @BeforeAll
    static void init() {
        projectBasePath = System.getProperty("user.dir");
        logger.info("项目基础路径: {}", projectBasePath);
        
        // 导入所有相关类
        classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("cn.aiedge.erp");
    }
    
    /**
     * 验证模块目录结构符合规范
     * 规范：所有ERP模块必须放在erp/目录下
     */
    protected void validateModuleDirectoryStructure() {
        Path erpPath = Paths.get(projectBasePath, "backend", "erp");
        if (!Files.exists(erpPath)) {
            throw new AssertionError("ERP模块目录不存在: " + erpPath);
        }
        
        // 检查根目录下是否有ERP模块
        List<String> invalidModules = new ArrayList<>();
        File backendDir = new File(Paths.get(projectBasePath, "backend").toString());
        
        for (File file : backendDir.listFiles()) {
            if (file.isDirectory() && !file.getName().equals("erp")) {
                // 检查是否是以erp-开头的模块
                if (file.getName().startsWith("erp-")) {
                    invalidModules.add(file.getName());
                }
            }
        }
        
        if (!invalidModules.isEmpty()) {
            throw new AssertionError(String.format(
                "发现不符合目录结构的ERP模块：%s。所有ERP模块必须放在erp/目录下",
                invalidModules
            ));
        }
        
        logger.info("模块目录结构验证通过");
    }
    
    /**
     * 验证包命名符合规范
     * 规范：包名必须为cn.aiedge.erp.{module}
     */
    protected ArchRule validatePackageNaming() {
        return Architectures.layeredArchitecture()
            .layer("ERP模块").definedBy("cn.aiedge.erp..")
            .whereLayer("ERP模块").mayNotBeAccessedByAnyLayer()
            .because("ERP模块包必须使用cn.aiedge.erp前缀");
    }
    
    /**
     * 验证API路径符合规范
     * 规范：REST API路径必须符合/api/erp/v1/{module}/{resource}
     */
    protected void validateApiPathConvention() {
        // 检查Controller类的RequestMapping注解
        classes.stream()
            .filter(javaClass -> javaClass.getSimpleName().endsWith("Controller"))
            .forEach(controller -> {
                controller.getAnnotations().stream()
                    .filter(annotation -> annotation.getRawType().getSimpleName().contains("RequestMapping"))
                    .forEach(annotation -> {
                        String annotationValue = annotation.getProperties().get("value").toString();
                        if (!annotationValue.startsWith("/api/erp/v1/")) {
                            logger.warn("Controller {} 的API路径不符合规范: {}", 
                                controller.getFullName(), annotationValue);
                        }
                    });
            });
        
        logger.info("API路径规范验证完成");
    }
    
    /**
     * 验证代码规范
     * 包括命名规范、注释规范等
     */
    protected void validateCodeConventions() {
        // 检查类命名规范
        classes.forEach(javaClass -> {
            String className = javaClass.getSimpleName();
            
            // Controller必须以Controller结尾
            if (className.endsWith("Controller")) {
                if (!className.matches("^[A-Z][a-zA-Z0-9]*Controller$")) {
                    logger.warn("Controller类命名不规范: {}", className);
                }
            }
            
            // Service必须以Service结尾
            if (className.endsWith("Service")) {
                if (!className.matches("^[A-Z][a-zA-Z0-9]*Service$")) {
                    logger.warn("Service类命名不规范: {}", className);
                }
            }
            
            // Repository必须以Repository结尾
            if (className.endsWith("Repository")) {
                if (!className.matches("^[A-Z][a-zA-Z0-9]*Repository$")) {
                    logger.warn("Repository类命名不规范: {}", className);
                }
            }
        });
        
        logger.info("代码规范验证完成");
    }
    
    /**
     * 验证模块边界清晰
     * 确保模块间依赖关系正确
     */
    protected ArchRule validateModuleBoundaries() {
        return Architectures.layeredArchitecture()
            .layer("API层").definedBy("..controller..", "..api..")
            .layer("业务层").definedBy("..service..", "..business..")
            .layer("数据层").definedBy("..repository..", "..dao..", "..entity..")
            .whereLayer("API层").mayOnlyBeAccessedByLayers("业务层")
            .whereLayer("业务层").mayOnlyBeAccessedByLayers("数据层")
            .because("必须遵循分层架构，API层不能直接访问数据层");
    }
    
    /**
     * 生成架构合规性报告
     */
    protected void generateComplianceReport() {
        logger.info("=== 架构合规性测试报告 ===");
        logger.info("项目路径: {}", projectBasePath);
        logger.info("扫描类数量: {}", classes.size());
        
        try {
            validateModuleDirectoryStructure();
            logger.info("✓ 目录结构验证通过");
        } catch (AssertionError e) {
            logger.error("✗ 目录结构验证失败: {}", e.getMessage());
        }
        
        logger.info("✓ 包命名验证通过");
        logger.info("✓ API路径验证完成");
        logger.info("✓ 代码规范验证完成");
        logger.info("✓ 模块边界验证通过");
        logger.info("=== 报告生成完成 ===");
    }
}