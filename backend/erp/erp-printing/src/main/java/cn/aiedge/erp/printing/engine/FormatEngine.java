package cn.aiedge.erp.printing.engine;

import java.util.Map;

/**
 * 格式化引擎接口（纯函数）
 * 输入 (value, formatConfig) → 输出 displayString
 * 前端预览和客户端渲染共用同一引擎，保证输出一致。
 */
public interface FormatEngine {

    /**
     * 格式化单个字段值
     *
     * @param value        原始值
     * @param formatConfig 格式化配置（包含 type, expression, params 等）
     * @return 格式化后的显示字符串
     */
    String format(Object value, Map<String, Object> formatConfig);

    /**
     * 校验自定义函数表达式合法性
     *
     * @param expression  用户输入的表达式字符串
     * @param sampleValue 样本值（用于试运行）
     * @return 校验结果，包含是否合法、预览结果、错误信息
     */
    FormulaValidationResult validateExpression(String expression, Object sampleValue);

    /**
     * 将整个模板 JSON + 数据 JSON 渲染为 HTML 字符串
     *
     * @param templateJson 模板 JSON（含组件、样式、布局）
     * @param dataJson     单据数据
     * @return 渲染后的完整 HTML
     */
    String renderToHtml(String templateJson, String dataJson);
}
