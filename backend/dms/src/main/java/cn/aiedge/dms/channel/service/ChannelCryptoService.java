package cn.aiedge.dms.channel.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 渠道对接凭据的字段级加密（《渠道管理开发文档》§7.4：密钥加密存储）
 *
 * <p>口径：只加密 {@code dms_channel.config_json} 中**敏感键名**（secret/token/password/...）对应的值，
 * 落库形如 {@code ENCv1:<base64(iv||ciphertext)>}（AES-256-GCM，随机 IV）；非敏感键（appKey/回调地址/网关）
 * 保持明文可读。出参仍由 {@link ChannelService} 统一脱敏（******），二者互不替代。</p>
 *
 * <p>密钥解析顺序：① 环境变量/启动参数 {@code DMS_CHANNEL_ENCRYPT_KEY}（映射 {@code dms.channel.encrypt-key}）
 * → ② 配置中心 {@code dms.channel.config.encrypt-key} → ③ 内置开发默认密钥（**仅开发/演示**，此时打 WARN）。
 * 生产环境必须显式配置；更换密钥后旧密文不可解（需重新保存渠道凭据）。</p>
 *
 * <p>失败策略：**fail closed** —— 加密失败拒绝保存、解密失败拒绝使用，绝不静默降级为明文。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelCryptoService {

    /** 密文前缀（版本化，便于后续换算法；历史明文无前缀 → 原样返回） */
    public static final String PREFIX = "ENCv1:";

    /** 需要加密的配置项键名（大小写不敏感，含子串匹配，与脱敏口径一致） */
    public static final Set<String> SECRET_KEY_PARTS = Set.of(
            "secret", "token", "password", "privatekey", "accesskey", "signkey");

    private static final String CFG_ENCRYPT_ON = "dms.channel.config.encrypt";
    private static final String CFG_ENCRYPT_KEY = "dms.channel.config.encrypt-key";
    /** 环境变量/启动参数键名（core-api application.yml 的 dms.channel.encrypt-key） */
    private static final String ENV_ENCRYPT_KEY = "dms.channel.encrypt-key";
    /** 内置默认密钥：仅开发/演示，生产必须覆盖 */
    private static final String DEV_DEFAULT_KEY = "ai-ready-dms-channel-insecure-default-key";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final ConfigService configService;
    private final Environment environment;

    private volatile boolean devKeyWarned = false;

    // ══════════════════════════════════════════════════════════
    // 开关与密钥
    // ══════════════════════════════════════════════════════════

    /** 是否启用凭据加密（配置 dms.channel.config.encrypt，缺失/异常回落 true） */
    public boolean enabled() {
        try {
            Boolean value = configService.getBoolean(MyBatisPlusConfig.getCurrentTenantIdValue(), CFG_ENCRYPT_ON);
            return value == null || value;
        } catch (Exception e) {
            return true;
        }
    }

    /** 单值加密（已加密/空值原样返回） */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (DmsBusinessException e) {
            throw e;
        } catch (Exception e) {
            // fail closed：加密失败拒绝保存，绝不明文落库
            throw new DmsBusinessException("渠道凭据加密失败，已拒绝保存：" + e.getMessage());
        }
    }

    /** 单值解密（无前缀=历史明文，原样返回） */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(raw, GCM_IV_BYTES, raw.length - GCM_IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 密钥变更/密文损坏：明确报错，绝不把密文当明文喂给适配器
            throw new DmsBusinessException("渠道凭据解密失败（可能已更换加密密钥，请重新保存渠道凭据）：" + e.getMessage());
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** AES-256 密钥：SHA-256(密钥材料) */
    private SecretKey key() {
        return new SecretKeySpec(sha256(keyMaterial()), "AES");
    }

    private byte[] sha256(String material) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new DmsBusinessException("渠道凭据密钥派生失败：" + e.getMessage());
        }
    }

    private String keyMaterial() {
        String fromEnv = environment == null ? null : environment.getProperty(ENV_ENCRYPT_KEY);
        if (hasText(fromEnv)) {
            return fromEnv.trim();
        }
        try {
            String fromConfig = configService.getString(MyBatisPlusConfig.getCurrentTenantIdValue(), CFG_ENCRYPT_KEY);
            if (hasText(fromConfig)) {
                return fromConfig.trim();
            }
        } catch (Exception e) {
            log.debug("渠道加密密钥未在配置中心登记，回落内置默认：{}", e.getMessage());
        }
        if (!devKeyWarned) {
            devKeyWarned = true;
            log.warn("未配置 dms.channel.config.encrypt-key / DMS_CHANNEL_ENCRYPT_KEY，"
                    + "渠道凭据使用**内置开发默认密钥**加密（仅开发/演示，生产必须显式配置）");
        }
        return DEV_DEFAULT_KEY;
    }

    // ══════════════════════════════════════════════════════════
    // 配置 JSON 级处理
    // ══════════════════════════════════════════════════════════

    /** 落库前：加密 config_json 中的敏感值（开关关闭时原样返回） */
    public String encryptConfigJson(String json) {
        if (json == null || json.isBlank() || !enabled()) {
            return json;
        }
        try {
            JsonNode root = JSON.readTree(json);
            transform(root, true);
            return JSON.writeValueAsString(root);
        } catch (DmsBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new DmsBusinessException("渠道对接配置不是合法 JSON，无法加密保存：" + e.getMessage());
        }
    }

    /** 使用前：解密 config_json 中的敏感值（供适配器 / 回调验签使用，结果不出服务端） */
    public String decryptConfigJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode root = JSON.readTree(json);
            transform(root, false);
            return JSON.writeValueAsString(root);
        } catch (DmsBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new DmsBusinessException("渠道对接配置无法解析：" + e.getMessage());
        }
    }

    /**
     * 掩码回写合并：入参中值为掩码（******）或未出现的敏感键，保留库中原值；
     * 其余键以入参为准（普通字段可正常修改）。
     *
     * @param storedDecrypted 库中原配置（已解密）
     * @param incoming        前端回传配置（敏感值为掩码）
     */
    public String mergeMasked(String storedDecrypted, String incoming, String mask) {
        if (incoming == null || incoming.isBlank()) {
            return storedDecrypted;
        }
        JsonNode incomingNode;
        try {
            incomingNode = JSON.readTree(incoming);
        } catch (Exception e) {
            // 非法 JSON：若含掩码则保留原值，否则拒绝（防止把脏数据写库）
            if (incoming.contains(mask)) {
                return storedDecrypted;
            }
            throw new DmsBusinessException("渠道对接配置不是合法 JSON：" + e.getMessage());
        }
        if (!incomingNode.isObject()) {
            return incoming;
        }
        JsonNode storedNode = null;
        if (storedDecrypted != null && !storedDecrypted.isBlank()) {
            try {
                storedNode = JSON.readTree(storedDecrypted);
            } catch (Exception ignored) {
                storedNode = null;
            }
        }
        mergeObject((ObjectNode) incomingNode, storedNode);
        return incomingNode.toString();
    }

    private void mergeObject(ObjectNode target, JsonNode stored) {
        List<String> names = new ArrayList<>();
        target.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            JsonNode value = target.get(name);
            JsonNode storedValue = stored == null ? null : stored.get(name);
            if (value == null || value.isNull()) {
                if (storedValue != null) {
                    target.set(name, storedValue);
                }
                continue;
            }
            if (value.isObject()) {
                mergeObject((ObjectNode) value, storedValue);
            } else if (value.isContainerNode()) {
                continue;
            } else {
                boolean masked = value.isTextual() && value.asText().contains(ChannelService.MASK);
                boolean blank = value.isTextual() && value.asText().isBlank();
                if ((masked || (blank && isSecretKey(name))) && storedValue != null) {
                    target.set(name, storedValue);
                }
            }
        }
    }

    /** 递归加解密敏感值；{@code encrypt=true} 加密，false 解密 */
    private void transform(JsonNode node, boolean encrypt) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode child = obj.get(name);
                if (child.isValueNode()) {
                    if (isSecretKey(name) && child.isTextual() && hasText(child.asText())) {
                        String value = child.asText();
                        obj.put(name, encrypt ? encrypt(value) : decrypt(value));
                    }
                } else {
                    transform(child, encrypt);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(child -> transform(child, encrypt));
        }
    }

    /** 敏感键名判定（与脱敏同一口径，避免「加密了但没脱敏」或反之） */
    public static boolean isSecretKey(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return SECRET_KEY_PARTS.stream().anyMatch(lower::contains);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
