package cn.aiedge.base.service.message.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.message.SmsSender;
import cn.aiedge.base.spi.PlatformSmsSettingsProvider;
import cn.aiedge.base.spi.PlatformSmsSettingsProvider.PlatformSmsSettings;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信发送通道实现（HTTP 网关，通用适配）。
 *
 * <p><b>配置来源（2026-09-19 接线）</b>，按优先级：
 * <ol>
 *   <li><b>平台级数据库配置</b> {@code sys_sms_config}（「系统 → 平台设置 → 短信配置」菜单 62503 维护）
 *       —— 经 {@link PlatformSmsSettingsProvider} 读取，提供 {@code provider / accessKey / accessSecret / signName}；</li>
 *   <li><b>application yml 的 {@code sms.*}</b> —— {@code sms.enabled / sms.endpoint / sms.access-key / sms.sign-name}。</li>
 * </ol>
 *
 * <p><b>🔴 两者是两套模型，本类不做"看起来能发"的假装（重要）</b>：
 * 本类是**通用 HTTP 网关**客户端（POST JSON 到一个网关地址），
 * 而数据库里的 {@code provider = aliyun|tencent|huawei} 指的是**厂商原生 API** ——
 * 调厂商原生 API 需要厂商 SDK 与请求签名（HMAC/OAuth），本系统**未集成任何厂商 SDK**。
 * 因此：
 * <ul>
 *   <li>数据库配置里**没有**网关地址字段 → 若只配了数据库、没配 {@code sms.endpoint}，
 *       本类会**明确失败并给出可执行的处置建议**（而不是静默返回 false 让消息反复重试、
 *       也不是伪造一个 externalId 谎报成功）；</li>
 *   <li>若同时配了 {@code sms.endpoint}（自建网关中转），则网关鉴权与签名取**数据库的** accessKey/signName
 *       —— 这一条是真实生效的接线。</li>
 * </ul>
 *
 * <p>未配置时 {@link #send} 直接返回 false 并给出明确原因；
 * 由 {@code MessageSendTask} 记录 failReason 并按既有策略重试，**不静默丢消息**。
 */
@Slf4j
@Component
public class SmsSenderImpl implements SmsSender {

    @Value("${sms.enabled:false}")
    private boolean enabled;

    @Value("${sms.endpoint:}")
    private String endpoint;

    @Value("${sms.access-key:}")
    private String accessKey;

    @Value("${sms.sign-name:}")
    private String signName;

    @Value("${sms.timeout-millis:5000}")
    private int timeoutMillis;

    /** 平台级配置读取口（core-api 提供实现）；裁剪部署下可能缺失 → 允许为 null */
    @Autowired(required = false)
    private PlatformSmsSettingsProvider platformSmsSettingsProvider;

    private volatile String lastFailure;

    @Override
    public boolean configured() {
        return resolve() != null;
    }

    @Override
    public boolean send(SysMessage message) {
        lastFailure = null;
        Resolved resolved = resolve();
        if (resolved == null) {
            lastFailure = "短信通道未配置（可在「系统 → 平台设置 → 短信配置」维护，"
                    + "或在 application yml 配置 sms.enabled=true 且 sms.endpoint 非空）";
            log.warn("[短信] {}，消息 {} 保持待发送", lastFailure, message.getId());
            return false;
        }

        String phone = message.getReceiverContact();
        if (phone == null || phone.isBlank()) {
            lastFailure = "缺少接收手机号";
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("content", message.getContent());
        payload.put("signName", resolved.signName());
        payload.put("title", message.getTitle());
        if (resolved.accessKey() != null && !resolved.accessKey().isBlank()) {
            payload.put("accessKey", resolved.accessKey());
        }

        try (HttpResponse response = HttpRequest.post(resolved.endpoint())
                .header("Content-Type", "application/json")
                .header("X-Access-Key", resolved.accessKey() == null ? "" : resolved.accessKey())
                .body(JSONUtil.toJsonStr(payload))
                .timeout(timeoutMillis)
                .execute()) {
            boolean ok = response.isOk() && isBizSuccess(response.body());
            if (!ok) {
                lastFailure = "网关返回失败：HTTP " + response.getStatus() + " / " + safe(response.body());
                log.warn("[短信] 发送失败 messageId={} source={}：{}", message.getId(), resolved.source(), lastFailure);
            }
            return ok;
        } catch (Exception e) {
            lastFailure = "调用短信网关异常：" + e.getMessage();
            log.error("[短信] 调用网关异常 messageId={} source={}", message.getId(), resolved.source(), e);
            return false;
        }
    }

    @Override
    public String failureReason() {
        return lastFailure;
    }

    /**
     * 解析本次发送使用的网关参数。
     *
     * <p>关键分支：**数据库配了厂商、但没配网关地址** → 返回 {@code null} 并写入一条
     * **可执行的** `lastFailure`（说明缺什么、去哪补），交由 {@code MessageSendTask} 记录。
     * 这里刻意不"降级为 yml 配置" —— 用户在页面上明确配了厂商，静默用另一套配置发出去
     * 会让他以为厂商通道已生效，比直接报错更危险。
     *
     * @return 解析结果；不可用时返回 {@code null}
     */
    private Resolved resolve() {
        PlatformSmsSettings settings = null;
        if (platformSmsSettingsProvider != null) {
            settings = platformSmsSettingsProvider.currentSmsSettings();
        }

        if (settings != null && settings.usable()) {
            if (endpoint != null && !endpoint.isBlank()) {
                // 自建网关中转：网关地址来自 yml，鉴权与签名取数据库配置 —— 真实生效
                return new Resolved(endpoint, settings.accessKey(), settings.signName(), "db:sys_sms_config");
            }
            lastFailure = "已在「平台设置 → 短信配置」配置服务商「" + settings.provider()
                    + "」，但本系统未集成该厂商 SDK（需请求签名），无法直连其原生 API。"
                    + "请改配 application yml 的 sms.endpoint 指向自建短信网关，或等待厂商 SDK 接入。";
            log.warn("[短信] {}", lastFailure);
            return null;
        }

        if (enabled && endpoint != null && !endpoint.isBlank()) {
            return new Resolved(endpoint, accessKey, signName, "yml:sms");
        }
        return null;
    }

    /** 兼容常见网关返回体：{"code":0|200,"success":true} */
    private boolean isBizSuccess(String body) {
        if (body == null || body.isBlank()) {
            return true;
        }
        try {
            if (!JSONUtil.isTypeJSONObject(body)) {
                return true;
            }
            Object code = JSONUtil.parseObj(body).get("code");
            Object success = JSONUtil.parseObj(body).get("success");
            if (code != null) {
                String c = String.valueOf(code);
                return "0".equals(c) || "200".equals(c) || "true".equalsIgnoreCase(c);
            }
            if (success != null) {
                return Boolean.parseBoolean(String.valueOf(success));
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private String safe(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    /** 本次发送解析出的网关参数 + 来源（来源仅用于日志定位问题） */
    private record Resolved(String endpoint, String accessKey, String signName, String source) {
    }
}
