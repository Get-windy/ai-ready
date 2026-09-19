package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.mapper.SmsConfigMapper;
import cn.aiedge.platform.model.SmsConfig;
import cn.aiedge.platform.service.SmsConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigServiceImpl implements SmsConfigService {

    private final SmsConfigMapper smsConfigMapper;

    @Override
    public SmsConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SmsConfig config = smsConfigMapper.selectOne(
                new LambdaQueryWrapper<SmsConfig>()
                        .eq(SmsConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = smsConfigMapper.selectOne(
                    new LambdaQueryWrapper<SmsConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public SmsConfig saveConfig(SmsConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SmsConfig existing = smsConfigMapper.selectOne(
                new LambdaQueryWrapper<SmsConfig>()
                        .eq(SmsConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            smsConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            smsConfigMapper.updateById(config);
        }
        log.info("保存短信配置: tenantId={}, provider={}", tenantId, config.getProvider());
        return config;
    }

    /**
     * 各主流短信服务商的 API 端点（用于连通性探测）。
     * <p>键为 {@code provider} 字段的小写值；值是 {@code host:port}。
     */
    private static final Map<String, String> PROVIDER_ENDPOINTS = Map.of(
            "aliyun", "dysmsapi.aliyuncs.com:443",
            "aliyunsms", "dysmsapi.aliyuncs.com:443",
            "aliyun_sms", "dysmsapi.aliyuncs.com:443",
            "tencent", "sms.tencentcloudapi.com:443",
            "tencentsms", "sms.tencentcloudapi.com:443",
            "tencent_sms", "sms.tencentcloudapi.com:443",
            "huawei", "smsapi.cn-north-1.myhuaweicloud.com:443",
            "huawei_sms", "smsapi.cn-north-1.myhuaweicloud.com:443"
    );

    /**
     * 短信服务连通性测试。
     *
     * <p>改造前此方法只打日志然后 {@code return true}。
     *
     * <p><b>能力的诚实边界（重要）</b>：本系统**未集成任何短信 SDK**，
     * 因此无法真正验证 AccessKey/签名是否有效（那需要调用服务商的
     * {@code SendSms} 接口，会产生真实费用与真实短信）。
     * 本方法只做两件确定的事：
     * <ol>
     *   <li><b>配置完整性校验</b> —— provider / accessKey / accessSecret / signName 缺哪项报哪项；</li>
     *   <li><b>网络连通性探测</b> —— 对已知服务商的 API 端点做 TCP 建连（5 秒超时）。</li>
     * </ol>
     * 返回文案会**明确区分**这两者，不会把"网络能通"说成"短信可用"。
     * 未知 {@code provider} 直接判失败并列出支持的值（不猜端点）。
     */
    @Override
    public ConnectionTestResult testConnection(SmsConfig config) {
        if (config == null) {
            return ConnectionTestResult.fail("没有可测试的配置");
        }
        String provider = trimToNull(config.getProvider());
        if (provider == null) {
            return ConnectionTestResult.fail("请先选择短信服务商（provider）");
        }
        if (trimToNull(config.getAccessKey()) == null) {
            return ConnectionTestResult.fail("请先填写 AccessKey");
        }
        if (trimToNull(config.getAccessSecret()) == null) {
            return ConnectionTestResult.fail("请先填写 AccessSecret");
        }
        if (trimToNull(config.getSignName()) == null) {
            return ConnectionTestResult.fail("请先填写短信签名（signName）");
        }

        String endpoint = PROVIDER_ENDPOINTS.get(provider.toLowerCase(Locale.ROOT));
        if (endpoint == null) {
            return ConnectionTestResult.fail(
                    "未内置服务商「" + provider + "」的 API 端点，无法探测连通性。"
                            + "当前支持：" + String.join(" / ", PROVIDER_ENDPOINTS.keySet()));
        }

        String host = endpoint.substring(0, endpoint.indexOf(':'));
        int port = Integer.parseInt(endpoint.substring(endpoint.indexOf(':') + 1));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            log.info("短信端点连通: provider={}, endpoint={}", provider, endpoint);
            return ConnectionTestResult.ok(
                    "服务商端点连通（" + endpoint + "）。"
                            + "注意：仅验证了网络连通与配置完整性，"
                            + "未调用服务商 API 校验 AccessKey 与签名是否有效。");
        } catch (Exception e) {
            log.warn("短信端点不可达: provider={}, endpoint={}", provider, endpoint, e);
            return ConnectionTestResult.fail(
                    "服务商端点不可达（" + endpoint + "）：" + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " - " + e.getMessage()));
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
