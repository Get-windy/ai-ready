package cn.aiedge.notification.template;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染器
 * 支持变量占位符 ${var} 格式
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class TemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 渲染模板
     *
     * @param template 模板内容
     * @param variables 变量映射
     * @return 渲染后的内容
     */
    public String render(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template)) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);
            String replacement = value != null ? escapeReplacement(String.valueOf(value)) : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 渲染模板（带默认值）
     *
     * @param template 模板内容
     * @param variables 变量映射
     * @param defaultValues 默认值映射
     * @return 渲染后的内容
     */
    public String render(String template, Map<String, Object> variables, Map<String, String> defaultValues) {
        if (StrUtil.isBlank(template)) {
            return "";
        }

        Map<String, Object> mergedVars = new java.util.HashMap<>();
        if (defaultValues != null) {
            defaultVars.forEach((k, v) -> mergedVars.put(k, v));
        }
        if (variables != null) {
            mergedVars.putAll(variables);
        }

        return render(template, mergedVars);
    }

    /**
     * 转义替换字符串中的特殊字符
     */
    private String escapeReplacement(String str) {
        return str.replace("\\", "\\\\").replace("$", "\\$");
    }
}
