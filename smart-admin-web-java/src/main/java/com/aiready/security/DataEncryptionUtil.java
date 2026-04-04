package com.aiready.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 数据加密工具类
 * 用于敏感数据的加密存储和解密
 */
@Component
public class DataEncryptionUtil {

    @Value("${security.encryption.key:aes256-secret-key}")
    private String secretKey;

    private AES aes;

    @PostConstruct
    public void init() {
        // 确保密钥长度为16/24/32字节
        byte[] key = SecureUtil.generateKey("AES", secretKey.getBytes(StandardCharsets.UTF_8)).getEncoded();
        this.aes = SecureUtil.aes(key);
    }

    /**
     * 加密字符串
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        byte[] encrypted = aes.encrypt(plainText);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密字符串
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        byte[] decrypted = aes.decrypt(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 加密手机号（部分隐藏）
     */
    public String encryptPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        // 只加密后4位
        String prefix = phone.substring(0, 7);
        String suffix = encrypt(phone.substring(7));
        return prefix + "_" + suffix;
    }

    /**
     * 解密手机号
     */
    public String decryptPhone(String encryptedPhone) {
        if (encryptedPhone == null || !encryptedPhone.contains("_")) {
            return encryptedPhone;
        }
        String[] parts = encryptedPhone.split("_");
        return parts[0] + decrypt(parts[1]);
    }

    /**
     * 加密身份证号
     */
    public String encryptIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) {
            return idCard;
        }
        return encrypt(idCard);
    }

    /**
     * 解密身份证号
     */
    public String decryptIdCard(String encryptedIdCard) {
        return decrypt(encryptedIdCard);
    }

    /**
     * 加密银行卡号
     */
    public String encryptBankCard(String bankCard) {
        return encrypt(bankCard);
    }

    /**
     * 解密银行卡号
     */
    public String decryptBankCard(String encryptedBankCard) {
        return decrypt(encryptedBankCard);
    }

    /**
     * 密码加密（不可逆）
     */
    public String hashPassword(String password, String salt) {
        return SecureUtil.md5(password + salt);
    }

    /**
     * 生成随机盐
     */
    public String generateSalt() {
        return SecureUtil.randomString(16);
    }
}