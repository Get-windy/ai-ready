package com.aiready.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * XSS攻击防护工具类
 */
@Component
public class XssProtectionUtil {

    // XSS危险字符模式
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src[\r\n]*=[\r\n]*'(.*?)'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("src[\r\n]*=[\r\n]*\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    /**
     * 清除XSS危险字符
     */
    public String cleanXSS(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        String cleanValue = value;

        // 替换特殊字符
        cleanValue = cleanValue.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        cleanValue = cleanValue.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
        cleanValue = cleanValue.replaceAll("'", "&#39;");
        cleanValue = cleanValue.replaceAll("eval\\((.*)\\)", "");
        cleanValue = cleanValue.replaceAll("[\"'][\\s]*javascript:(.*)[\"']", "\"\"");
        cleanValue = cleanValue.replaceAll("[\"'][\\s]*vbscript:(.*)[\"']", "\"\"");

        // 移除危险模式
        for (Pattern pattern : XSS_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }

        return cleanValue;
    }

    /**
     * 检查是否包含XSS攻击
     */
    public boolean containsXSS(String value) {
        if (StrUtil.isEmpty(value)) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * HTML转义
     */
    public String escapeHtml(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }

    /**
     * URL编码
     */
    public String encodeUrl(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}