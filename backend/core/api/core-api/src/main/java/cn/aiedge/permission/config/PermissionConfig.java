package cn.aiedge.permission.config;

import cn.aiedge.permission.interceptor.DataPermissionInterceptor;
import cn.aiedge.permission.service.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限模块配置
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
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

    /**
     * 把数据权限拦截器**真正挂进 MyBatis 插件链**，且必须插在分页插件之前。
     *
     * <p>此前这里只有上面那个 {@code @Bean} 声明，从未 {@code addInnerInterceptor}——
     * 即基于 {@code @DataPermission} 注解的行级数据权限（含 {@code CUSTOM_SQL} 自定义范围）
     * 从来没有生效过（2026-09-20 盘点确认）。</p>
     *
     * <p>为什么必须插在分页之前：分页插件会额外生成 count 语句，若数据权限排在它后面，
     * count 不带数据权限条件，会出现「total 含无权记录、records 只有有权记录」的口径不一致
     * ——多租户插件踩过同一个坑，见 {@code MyBatisPlusConfig#mybatisPlusInterceptor} 注释。</p>
     *
     * <p>用 {@link SmartInitializingSingleton} 是因为插件链由 core-base 装配、
     * 本类在 core-api，二者没有装配顺序上的从属关系；等所有单例就绪后再插入最稳。
     * 未加 {@code @DataPermission} 注解的 Mapper 方法在拦截器里会立即返回，
     * 故本次挂载对现有行为**零影响**。</p>
     */
    @Bean
    public SmartInitializingSingleton dataPermissionInterceptorRegistrar(
            MybatisPlusInterceptor mybatisPlusInterceptor,
            DataPermissionInterceptor dataPermissionInterceptor) {
        return () -> {
            // ⚠️ `getInterceptors()` 返回的是**不可变视图**，直接 add 会抛
            // UnsupportedOperationException 导致应用启动失败（本轮实踩：编译期完全看不出来，
            // 只有启动才暴露）。必须复制一份、改完再用 setInterceptors 写回。
            List<InnerInterceptor> chain = new ArrayList<>(mybatisPlusInterceptor.getInterceptors());
            if (chain.contains(dataPermissionInterceptor)) {
                return;
            }
            // 插到分页插件之前；链路里没有分页插件时追加到末尾
            int insertAt = chain.size();
            for (int i = 0; i < chain.size(); i++) {
                if (chain.get(i) instanceof PaginationInnerInterceptor) {
                    insertAt = i;
                    break;
                }
            }
            chain.add(insertAt, dataPermissionInterceptor);
            mybatisPlusInterceptor.setInterceptors(chain);
            log.info("数据权限拦截器已挂载到 MyBatis 插件链：位置 {}，链路共 {} 个插件", insertAt, chain.size());
        };
    }
}
