package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.mapper.MailConfigMapper;
import cn.aiedge.platform.model.MailConfig;
import cn.aiedge.platform.service.MailConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailConfigServiceImpl implements MailConfigService {

    private final MailConfigMapper mailConfigMapper;

    @Override
    public MailConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        MailConfig config = mailConfigMapper.selectOne(
                new LambdaQueryWrapper<MailConfig>()
                        .eq(MailConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = mailConfigMapper.selectOne(
                    new LambdaQueryWrapper<MailConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public MailConfig saveConfig(MailConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        MailConfig existing = mailConfigMapper.selectOne(
                new LambdaQueryWrapper<MailConfig>()
                        .eq(MailConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            mailConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            mailConfigMapper.updateById(config);
        }
        log.info("保存邮件配置: tenantId={}, host={}", tenantId, config.getHost());
        return config;
    }

    /**
     * 真实 SMTP 连接测试。
     *
     * <p>改造前此方法只打一行日志然后 {@code return true}，用户点「测试连接」永远成功。
     * 现按 SMTP 协议真实建连并完成认证：
     * <ol>
     *   <li>先校验必填项（host/port/username/password），缺一项直接给出缺哪项；</li>
     *   <li>{@link Transport#connect} 真连并认证 —— 这一步能同时验证
     *       「主机端口可达」与「凭据正确」；</li>
     *   <li>连接与读取超时各 5 秒，避免页面卡死；</li>
     *   <li>加密方式按配置映射：{@code SSL → ssl.enable}、{@code TLS/STARTTLS → starttls.enable}。</li>
     * </ol>
     *
     * <p><b>边界</b>：本方法只做「连接 + 认证」，**不发信**（不需要额外的测试收件人）。
     *
     * @param config 待测配置（通常来自页面表单，未落库也能测）
     */
    @Override
    public ConnectionTestResult testConnection(MailConfig config) {
        if (config == null) {
            return ConnectionTestResult.fail("没有可测试的配置");
        }
        String host = trimToNull(config.getHost());
        if (host == null) {
            return ConnectionTestResult.fail("请先填写 SMTP 服务器地址（host）");
        }
        Integer port = config.getPort();
        if (port == null || port <= 0) {
            return ConnectionTestResult.fail("请先填写 SMTP 端口（port），常见 465/587/25");
        }
        String username = trimToNull(config.getUsername());
        String password = config.getPassword();
        if (username == null || password == null || password.isEmpty()) {
            return ConnectionTestResult.fail("请先填写 SMTP 用户名与密码（用于认证）");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        // 超时：页面同步等待，必须限时，否则会一直转圈
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        String encryption = trimToNull(config.getEncryption());
        if (encryption != null) {
            switch (encryption.toUpperCase()) {
                case "SSL", "SMTPS" -> props.put("mail.smtp.ssl.enable", "true");
                case "TLS", "STARTTLS" -> props.put("mail.smtp.starttls.enable", "true");
                default -> log.debug("未知的加密方式，按明文处理: {}", encryption);
            }
        }

        Session session = Session.getInstance(props);
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(host, port, username, password);
            log.info("SMTP 连接测试成功: host={}, port={}, user={}", host, port, username);
            return ConnectionTestResult.ok("SMTP 连接成功（已通过认证），" + host + ":" + port);
        } catch (AuthenticationFailedException e) {
            // 最常见的一类失败，单独识别，否则用户会误以为是网络问题
            log.warn("SMTP 认证失败: host={}, port={}, user={}", host, port, username);
            return ConnectionTestResult.fail("SMTP 认证失败：用户名或密码错误（服务器已连通）");
        } catch (MessagingException e) {
            log.warn("SMTP 连接失败: host={}, port={}", host, port, e);
            return ConnectionTestResult.fail("SMTP 连接失败：" + rootMessage(e));
        } catch (Exception e) {
            log.warn("SMTP 测试异常: host={}, port={}", host, port, e);
            return ConnectionTestResult.fail("测试异常：" + rootMessage(e));
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 取最内层异常的原因 —— 真正的失败原因（如 UnknownHostException）在 cause 链末端 */
    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return (msg == null || msg.isBlank()) ? cur.getClass().getSimpleName() : msg;
    }
}
