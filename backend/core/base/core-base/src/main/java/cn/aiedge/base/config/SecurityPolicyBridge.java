package cn.aiedge.base.config;

import cn.aiedge.base.spi.PlatformSecuritySettingsProvider;
import cn.aiedge.base.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 把「平台设置 → 安全策略」的读取口接到 {@link PasswordPolicy} 上。
 *
 * <p>存在的唯一理由：{@code PasswordPolicy.validate(...)} 是**静态方法**、有 3 处静态调用点。
 * 与其把它们全部改成实例注入（要动构造函数、影响面大且与本次目标无关），
 * 不如在这里做一次性的桥接 —— 把 `core-api` 提供的实现（如果有）注入静态字段。
 *
 * <p>用 {@link ApplicationReadyEvent} 而不是构造器：确保容器已完全刷新，
 * `PlatformSecuritySettingsProvider` 的实现 bean（可能因模块裁剪而不存在）已就绪。
 *
 * <p>实现 bean 不存在时只打 info 日志、不报错 —— 本系统支持裁剪部署，
 * 缺这个 SPI 不是错误，会按内置默认密码规则工作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityPolicyBridge {

    private final ObjectProvider<PlatformSecuritySettingsProvider> provider;

    @EventListener(ApplicationReadyEvent.class)
    public void bind() {
        PlatformSecuritySettingsProvider p = provider.getIfAvailable();
        PasswordPolicy.setProvider(p);
        if (p == null) {
            log.info("未找到平台安全策略实现（PlatformSecuritySettingsProvider），"
                    + "密码校验按内置默认规则执行（长度 8-64，四类字符至少 3 类）");
        } else {
            log.info("平台安全策略已接入密码校验：密码长度与字符类别要求以 sys_security_policy 为准");
        }
    }
}
