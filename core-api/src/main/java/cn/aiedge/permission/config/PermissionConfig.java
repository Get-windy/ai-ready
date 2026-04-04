package cn.aiedge.permission.config;

import cn.aiedge.permission.interceptor.DataPermissionInterceptor;
import cn.aiedge.permission.service.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限模块配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class PermissionConfig {

    private final PermissionService permissionService;

    /**
     * 配置 MyBatis-Plus 数据权限拦截器
     */
    @Bean
    public DataPermissionInterceptor dataPermissionInterceptor() {
        return new DataPermissionInterceptor(permissionService);
    }
}
