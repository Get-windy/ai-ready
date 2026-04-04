package cn.aiedge.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件存储配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

    @Value("${storage.local.base-path:./uploads}")
    private String localBasePath;

    /**
     * 配置本地文件静态资源访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地文件访问映射
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:" + localBasePath + "/");
    }
}
