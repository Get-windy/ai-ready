package com.aiready.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * API请求签名工具类
 */
@Component
public class ApiSignatureUtil {

    @Value("${security.signature.secret:aiready-api-secret}")
    private String apiSecret;

    @Value("${security.signature.expire:300000}")
    private Long signatureExpire; // 签名有效期，默认5分钟

    /**
     * 生成签名
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param path 请求路径
     * @param params 请求参数
     * @return 签名字符串
     */
    public String generateSignature(Long timestamp, String nonce, String path, Map<String, String> params) {
        // 构建签名字符串
        StringBuilder signStr = new StringBuilder();
        signStr.append(apiSecret);
        signStr.append(timestamp);
        signStr.append(nonce);
        signStr.append(path);

        // 参数按key排序后拼接
        if (params != null && !params.isEmpty()) {
            List<String> keys = new ArrayList<>(params.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                String value = params.get(key);
                if (StrUtil.isNotEmpty(value)) {
                    signStr.append(key).append("=").append(value).append("&");
                }
            }
        }

        // MD5加密
        return SecureUtil.md5(signStr.toString()).toUpperCase();
    }

    /**
     * 验证签名
     */
    public boolean verifySignature(Long timestamp, String nonce, String path, 
                                   Map<String, String> params, String signature) {
        // 检查时间戳是否过期
        if (System.currentTimeMillis() - timestamp > signatureExpire) {
            return false;
        }

        // 检查nonce是否重复（防止重放攻击）
        // 实际应用中应使用Redis存储已使用的nonce
        
        // 生成签名并比较
        String expectedSignature = generateSignature(timestamp, nonce, path, params);
        return expectedSignature.equals(signature);
    }

    /**
     * 生成随机nonce
     */
    public String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}