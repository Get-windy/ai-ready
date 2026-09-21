package cn.aiedge.architecture;

import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 切点表达式「软引用」安全网。
 *
 * <p><b>为什么需要它</b>：AOP 切点里的类名是**字符串**，编译器不校验。删掉
 * {@code DataPermissionController} 后，{@code PermissionAuditAspect} 里的
 * {@code execution(* cn.aiedge.base.controller.DataPermissionController.*(..))} 照常编译通过、
 * 照常打包成功，**只有启动时**才炸：</p>
 *
 * <pre>
 * IllegalArgumentException: warning no match for this type name:
 *   cn.aiedge.base.controller.DataPermissionController [Xlint:invalidAbsoluteTypeName]
 * </pre>
 *
 * <p>Spring 在 context refresh 阶段**急切解析**切点，且**没有任何配置可以忽略**该错误
 * （{@code @Conditional} 也无效——错误发生在条件评估之前）。本仓 2026-09-20 实踩过一次，
 * 连续四次重启才定位。</p>
 *
 * <p>本测试不启动 Spring 上下文（秒级、CI 必定可跑），只做一件事：
 * 把每个 {@code @Aspect} 上的切点字符串里的**全限定类名**抽出来，验证它在 classpath 上存在。
 * 通配写法（{@code execution(* cn.aiedge..controller.*Controller.*(..))}）天然安全，会被跳过。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.21
 */
class PointcutTargetExistenceTest {

    /** 匹配 execution(* cn.aiedge.xxx.Yyy.method(..)) / execution(* cn.aiedge.xxx.Yyy.*(..)) */
    private static final Pattern EXECUTION_TYPE = Pattern.compile(
            "execution\\s*\\(\\s*[^\\s]+\\s+([\\w$.]+)\\.[\\w$*]+\\s*\\(");

    /** 匹配 @annotation(cn.aiedge.xxx.Yyy) / @within(...) / @target(...) */
    private static final Pattern ANNOTATION_TYPE = Pattern.compile(
            "@(?:annotation|within|target)\\s*\\(\\s*([\\w$.]+)\\s*\\)");

    @Test
    @DisplayName("所有切点引用的类型都必须存在（否则应用启动即失败）")
    void pointcutExpressionsMustReferenceExistingTypes() {
        List<String> missing = new ArrayList<>();
        List<Class<?>> aspects = scanAspectClasses();

        // 防呆：一个 @Aspect 都没扫到说明扫描逻辑或 classpath 有问题，不能让测试"空过"
        assertThat(aspects)
                .as("未扫描到任何 @Aspect 类，本测试将失去意义（检查 classpath 或扫描范围）")
                .isNotEmpty();

        for (Class<?> aspectClass : aspects) {
            for (Method method : aspectClass.getDeclaredMethods()) {
                for (Annotation annotation : method.getAnnotations()) {
                    for (String expression : pointcutExpressionsOf(annotation)) {
                        collectMissing(expression, EXECUTION_TYPE, aspectClass, method, missing);
                        collectMissing(expression, ANNOTATION_TYPE, aspectClass, method, missing);
                    }
                }
            }
        }

        assertThat(missing)
                .as("""
                        以下切点引用的类型不存在。编译器不会报错，但 Spring 启动时解析切点会抛
                        [Xlint:invalidAbsoluteTypeName] 导致应用起不来（无配置可忽略）。
                        修法：① 修正切点里的类名；或 ② 改成通配写法如
                        execution(* cn.aiedge..controller.*Controller.*(..))""")
                .isEmpty();
    }

    private void collectMissing(String expression, Pattern pattern,
                                Class<?> aspectClass, Method method, List<String> missing) {
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            // 含通配符的切点天然不会因删类而失效，跳过
            if (typeName.contains("*") || typeName.contains("..")) {
                continue;
            }
            // 至少要像个全限定名（含包名）；否则视为正则误抓，避免假失败
            if (!typeName.contains(".")) {
                continue;
            }
            if (!typeExists(typeName)) {
                missing.add(aspectClass.getSimpleName() + "#" + method.getName() + " → " + typeName);
            }
        }
    }

    /** 扫描 classpath 上所有 @Aspect 类 */
    private List<Class<?>> scanAspectClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Aspect.class));

        Set<Class<?>> classes = new LinkedHashSet<>();
        for (var definition : scanner.findCandidateComponents("cn.aiedge")) {
            String className = definition.getBeanClassName();
            if (className == null) {
                continue;
            }
            try {
                classes.add(Class.forName(className, false, getClass().getClassLoader()));
            } catch (Throwable ignored) {
                // 加载不到就跳过：本测试只关心「切点引用的类型是否存在」
            }
        }
        return new ArrayList<>(classes);
    }

    /** 取注解里的切点表达式：@Around/@Before/… 用 value 或 pointcut，@Pointcut 用 value */
    private List<String> pointcutExpressionsOf(Annotation annotation) {
        List<String> expressions = new ArrayList<>();
        for (String attribute : List.of("value", "pointcut")) {
            try {
                Object value = annotation.annotationType().getMethod(attribute).invoke(annotation);
                if (value instanceof String text && !text.isBlank()) {
                    expressions.add(text);
                }
            } catch (Exception ignored) {
                // 该注解没有这个属性，正常
            }
        }
        return expressions;
    }

    private boolean typeExists(String typeName) {
        try {
            Class.forName(typeName, false, getClass().getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
