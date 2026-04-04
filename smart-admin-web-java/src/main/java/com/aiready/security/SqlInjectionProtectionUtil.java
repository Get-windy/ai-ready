package com.aiready.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * SQL注入防护工具类
 */
@Component
public class SqlInjectionProtectionUtil {

    // SQL注入关键字模式
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        Pattern.compile("(?i)(\\b(select|insert|update|delete|drop|truncate|exec|execute|xp_cmdshell)\\b)"),
        Pattern.compile("(?i)(\\b(union|join|inner|outer|left|right)\\b.*\\b(select|from)\\b)"),
        Pattern.compile("(?i)(\\b(or|and)\\b\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?)"),
        Pattern.compile("(?i)(\\b(or|and)\\b\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?)"),
        Pattern.compile("(?i)(--|\\/\\*|\\*\\/|#)"),
        Pattern.compile("(?i)(;\\s*$|;\\s*--)"),
        Pattern.compile("(?i)(@@|char\\(|nchar\\(|varchar\\(|nvarchar\\()"),
        Pattern.compile("(?i)(master\\.\\.sys|sysobjects|syscolumns)"),
        Pattern.compile("(?i)('.*\\b(or|and)\\b.*')"),
        Pattern.compile("(?i)(\\bwaitfor\\b.*\\bdelay\\b)")
    };

    /**
     * 检查是否包含SQL注入
     */
    public boolean containsSqlInjection(String value) {
        if (StrUtil.isEmpty(value)) {
            return false;
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清除SQL注入危险字符
     */
    public String cleanSqlInjection(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        String cleanValue = value;

        // 转义单引号
        cleanValue = cleanValue.replace("'", "''");

        // 移除危险关键字（仅移除，不转义）
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }

        return cleanValue;
    }

    /**
     * 验证参数是否安全
     * @param paramName 参数名
     * @param paramValue 参数值
     * @return 是否安全
     */
    public boolean validateParam(String paramName, String paramValue) {
        if (StrUtil.isEmpty(paramValue)) {
            return true;
        }

        // 检查SQL注入
        if (containsSqlInjection(paramValue)) {
            return false;
        }

        // 检查长度限制
        if (paramValue.length() > 10000) {
            return false;
        }

        return true;
    }

    /**
     * 转义like查询中的特殊字符
     */
    public String escapeLike(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        return value.replace("%", "\\%")
                   .replace("_", "\\_")
                   .replace("\\", "\\\\");
    }
}