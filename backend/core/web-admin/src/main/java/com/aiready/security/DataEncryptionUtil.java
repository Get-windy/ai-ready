package com.aiready.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Base64;

/**
 * 数据加密工具类
 * 用于敏感数据的加密存储和解密
 */
@Component
public class DataEncryptionUtil {
    
    @Value("${encryption.key:defaultkey12345678}")
    private String encryptionKey;
    
    private AES aes;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        this.aes = SecureUtil.aes(encryptionKey.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 加密数据
     */
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(aes.encrypt(data));
    }
    
    /**
     * 解密数据
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        return aes.decryptStr(Base64.getDecoder().decode(encryptedData));
    }
    
    /**
     * 加密敏感字段（脱敏）
     */
    public String encryptSensitive(String data, int showLastChars) {
        if (data == null || data.length() <= showLastChars) {
            return "***";
        }
        String encrypted = encrypt(data);
        int length = encrypted.length();
        return encrypted.substring(0, 2) + "***" + encrypted.substring(length - showLastChars);
    }
    
    /**
     * 生成哈希值
     */
    public String hashPassword(String password, String salt) {
        return SecureUtil.md5(password + salt);
    }
    
    /**
     * 生成随机盐
     */
    public String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}