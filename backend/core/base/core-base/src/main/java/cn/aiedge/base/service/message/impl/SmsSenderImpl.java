package cn.aiedge.base.service.message.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.message.SmsSender;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信发送通道实现（HTTP 网关，通用适配）
 *
 * 配置（四通道任一，见《对标开发技术参考文档》§5.6）：
 *   sms.enabled    : 是否启用（默认 false）
 *   sms.endpoint   : 网关地址（POST JSON）
 *   sms.access-key : 网关鉴权 Key（放 Header `X-Access-Key`，同时带 `accessKey` 字段兼容）
 *   sms.sign-name  : 短信签名
 *
 * 未配置时 {@link #send} 直接返回 false 并给出明确原因；
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

    private volatile String lastFailure;

    @Override
    public boolean configured() {
        return enabled && endpoint != null && !endpoint.isBlank();
    }

    @Override
    public boolean send(SysMessage message) {
        lastFailure = null;
        if (!configured()) {
            lastFailure = "短信通道未配置（需 sms.enabled=true 且 sms.endpoint 非空）";
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
        payload.put("signName", signName);
        payload.put("title", message.getTitle());
        if (accessKey != null && !accessKey.isBlank()) {
            payload.put("accessKey", accessKey);
        }

        try (HttpResponse response = HttpRequest.post(endpoint)
                .header("Content-Type", "application/json")
                .header("X-Access-Key", accessKey == null ? "" : accessKey)
                .body(JSONUtil.toJsonStr(payload))
                .timeout(timeoutMillis)
                .execute()) {
            boolean ok = response.isOk() && isBizSuccess(response.body());
            if (!ok) {
                lastFailure = "网关返回失败：HTTP " + response.getStatus() + " / " + safe(response.body());
                log.warn("[短信] 发送失败 messageId={}：{}", message.getId(), lastFailure);
            }
            return ok;
        } catch (Exception e) {
            lastFailure = "调用短信网关异常：" + e.getMessage();
            log.error("[短信] 调用网关异常 messageId={}", message.getId(), e);
            return false;
        }
    }

    @Override
    public String failureReason() {
        return lastFailure;
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
}
