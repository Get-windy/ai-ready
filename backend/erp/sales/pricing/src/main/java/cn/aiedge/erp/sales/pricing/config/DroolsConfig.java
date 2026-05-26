package cn.aiedge.erp.sales.pricing.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;

/**
 * Drools规则引擎配置类
 */
@Configuration
public class DroolsConfig {
    
    private static final String RULES_PATH = "rules/";
    
    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        KieRepository kieRepository = kieServices.getRepository();
        
        // 加载规则文件
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resourcePatternResolver.getResources("classpath*:" + RULES_PATH + "**/*.drl");
        
        for (Resource resource : resources) {
            String path = RULES_PATH + resource.getFilename();
            kieFileSystem.write(ResourceFactory.newClassPathResource(path, "UTF-8"));
        }
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        return kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
    }
    
    @Bean
    public KieSession kieSession() throws IOException {
        return kieContainer().newKieSession();
    }
}