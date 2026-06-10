package cn.aiedge.erp.printing.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 自定义表达式求值器（沙箱版）
 *
 * 基于 SpEL 实现，限制全局对象访问，设置执行超时。
 * 自动兼容 JS 风格运算符（=== → ==, !== → !=）。
 *
 * 支持表达式示例：
 *   '¥' + value + '元'
 *   value * 1.5
 *   value == 80 ? 100 : value
 *   value > 0 ? value : 'N/A'
 */
@Slf4j
@Component
public class ExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "expr-eval-");
        t.setDaemon(true);
        return t;
    });

    private static final long EXPRESSION_TIMEOUT_MS = 5000;

    /**
     * 在沙箱中求值表达式
     *
     * @param expression 表达式字符串（JS 风格）
     * @param value      字段原始值（使用变量名 "value" 传入）
     * @return 求值结果
     */
    public Object evaluate(String expression, Object value) {
        // 预处理：兼容 JS 风格运算符
        String spelExpr = preprocess(expression);

        Future<Object> future = executor.submit(() -> {
            try {
                Expression exp = parser.parseExpression(spelExpr);

                // 使用 SimpleEvaluationContext 只允许读属性，禁止写、调用方法、类型引用
                EvaluationContext ctx = SimpleEvaluationContext
                        .forReadOnlyDataBinding()
                        .withRootObject(value)
                        .build();

                // 将 "value" 作为变量注入
                ctx.setVariable("value", value);

                return exp.getValue(ctx);
            } catch (Exception e) {
                log.warn("表达式求值失败: {} - {}", expression, e.getMessage());
                // 如果表达式解析失败，尝试直接作为字符串模板处理
                return evaluateAsStringTemplate(spelExpr, value);
            }
        });

        try {
            return future.get(EXPRESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("表达式求值超时(>{}ms): {}", EXPRESSION_TIMEOUT_MS, expression);
            throw new RuntimeException("表达式执行超时，请简化您的表达式");
        } catch (Exception e) {
            log.error("表达式求值异常: {}", expression, e);
            return value; // 回退返回原始值
        }
    }

    /**
     * 当 SpEL 解析失败时，尝试将表达式作为字符串模板处理
     * 例如: "¥{value}元" → "¥100元"
     */
    private Object evaluateAsStringTemplate(String expression, Object value) {
        try {
            if (expression.contains("{value}")) {
                return expression.replace("{value}", String.valueOf(value));
            }
        } catch (Exception ignored) {
        }
        return value;
    }

    /**
     * 预处理：将 JS 风格运算符转换为 SpEL
     */
    private String preprocess(String expr) {
        if (expr == null || expr.isEmpty()) {
            return "";
        }
        return expr
                .replace("===", "==")
                .replace("!==", "!=")
                .trim();
    }

    /**
     * 校验表达式是否合法
     */
    public FormulaValidationResult validate(String expression, Object sampleValue) {
        try {
            Object result = evaluate(expression, sampleValue);
            return new FormulaValidationResult(true, result, null);
        } catch (Exception e) {
            return new FormulaValidationResult(false, null, e.getMessage());
        }
    }

    /**
     * 销毁线程池
     */
    public void destroy() {
        executor.shutdown();
    }
}
