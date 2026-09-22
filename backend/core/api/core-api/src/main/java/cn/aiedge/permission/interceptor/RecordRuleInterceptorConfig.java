package cn.aiedge.permission.interceptor;

import cn.aiedge.permission.service.PermissionService;
import cn.aiedge.permission.service.RecordRuleService;
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
 * 记录规则拦截器的装配（放在 interceptor 包内，与拦截器同生命周期、同测试面）。
 *
 * <p><b>注册顺序</b>：插件链由 core-base 的 {@code MyBatisPlusConfig} 装配
 * （租户 → 分页 → 乐观锁），本拦截器必须插在 {@code PaginationInnerInterceptor} **之前**，
 * 否则分页插件额外生成的 count 语句漏掉记录规则条件，出现
 * 「total 含无权记录、records 只有有权记录」的口径不一致（多租户插件踩过同一个坑）。
 * 顺序依据见类注释第 3 条。</p>
 *
 * <p>用 {@link SmartInitializingSingleton} 的原因与 {@code PermissionConfig#dataPermissionInterceptorRegistrar}
 * 相同：插件链在 core-base 装配、本类在 core-api，二者没有装配顺序上的从属关系，
 * 等所有单例就绪后再插入最稳。未配置任何记录规则时拦截器在 {@code decideInjection} 首行短路，
 * 对现有行为零影响。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecordRuleInterceptorConfig {

    private final RecordRuleService recordRuleService;
    private final PermissionService permissionService;

    @Bean
    public RecordRuleInterceptor recordRuleInterceptor() {
        return new RecordRuleInterceptor(recordRuleService, permissionService);
    }

    @Bean
    public SmartInitializingSingleton recordRuleInterceptorRegistrar(
            MybatisPlusInterceptor mybatisPlusInterceptor,
            RecordRuleInterceptor recordRuleInterceptor) {
        return () -> {
            // ⚠️ getInterceptors() 返回不可变视图，直接 add 会 UnsupportedOperationException（PermissionConfig 已踩）
            List<InnerInterceptor> chain = new ArrayList<>(mybatisPlusInterceptor.getInterceptors());
            if (chain.contains(recordRuleInterceptor)) {
                return;
            }
            int insertAt = chain.size();
            for (int i = 0; i < chain.size(); i++) {
                if (chain.get(i) instanceof PaginationInnerInterceptor) {
                    insertAt = i;
                    break;
                }
            }
            chain.add(insertAt, recordRuleInterceptor);
            mybatisPlusInterceptor.setInterceptors(chain);
            log.info("记录规则拦截器已挂载到 MyBatis 插件链：位置 {}（分页插件之前），链路共 {} 个插件",
                    insertAt, chain.size());
        };
    }
}
