package cn.aiedge.architecture;

import cn.aiedge.AiReadyApplication;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 组件扫描装配门禁。
 *
 * <p>背景：本项目用显式 {@code scanBasePackages} 白名单（不是扫描 {@code cn.aiedge} 全包）。
 * 漏配一个包既不会编译报错也不会启动报错，而 {@code @MapperScan} 是通配的，
 * 于是表现为「表能建、点不动」——Mapper 在、Service/Controller 不在容器里，端点一律 404。
 * 2026-09-14（scheduler）、2026-09-18（module/monitor/platform/datasource/export/devtool）
 * 两次修复都是人工逐个 404 排查出来的。本测试把这类漏配变成 CI 红灯。
 *
 * <p>判定：任何含 {@code @RestController}/{@code @Controller} 的包，必须落在
 * 「{@link AiReadyApplication} 的 scanBasePackages + 被覆盖的 {@code @ComponentScan} 闭包」内，
 * 或在基线清单 {@code src/test/resources/known-unscanned-controller-packages.txt} 里显式豁免。
 *
 * <p>基线是「棘轮」：消除一个就删一行；新增一行必须在 CLEANUP_AUDIT_REPORT.md 里写明理由。
 */
public class ComponentScanCoverageTest {

    /** 全部业务代码都在该包下 */
    private static final String SCAN_ROOT = "cn.aiedge";

    /**
     * 控制器数量下限：低于此值说明 classpath 不完整（例如测试被裁剪执行），
     * 门禁会退化成"什么都没扫到所以全都合规"的装饰。2026-09-19 实测 394 个。
     */
    private static final int MIN_EXPECTED_CONTROLLERS = 300;

    private static final String BASELINE_RESOURCE = "known-unscanned-controller-packages.txt";

    /**
     * mapper 包数量下限：与控制器同理，低于此值说明扫描失效（门禁会退化成"什么都没扫到所以全合规"）。
     * 2026-09-23 实测该值为 40+（`**.mapper` / `**.dao` 是主仓命名惯例）。
     */
    private static final int MIN_EXPECTED_MAPPER_PACKAGES = 20;

    @Test
    @DisplayName("装配门禁：所有控制器所在包必须在组件扫描范围内（否则端点必然 404）")
    void allControllerPackagesAreComponentScanned() {
        Set<String> scanPackages = effectiveScanPackages();
        Set<String> baseline = baselinePackages();

        Set<String> violations = new TreeSet<>();
        for (String className : controllerClasses()) {
            String pkg = packageOf(className);
            if (!isCovered(pkg, scanPackages) && !baseline.contains(pkg)) {
                violations.add(pkg + "    ← " + className);
            }
        }

        if (!violations.isEmpty()) {
            fail("""
                发现未装配的控制器包（"表能建、点不动"的根因）：
                %s

                处理方式二选一：
                  a) 要这个功能 → 把包加进 AiReadyApplication 的 scanBasePackages（或某个已被扫描的 @ComponentScan），
                     并补一条能打到该端点的冒烟；
                  b) 不要这个功能 → 删除代码，数据库表按 CLEANUP_AUDIT_REPORT.md 的孤儿表流程处理。
                确需暂时豁免时，才在 src/test/resources/%s 里加一行并写明理由。
                """.formatted(String.join("\n", violations), BASELINE_RESOURCE));
        }
    }

    @Test
    @DisplayName("装配门禁：基线清单不得过期（已装配的包必须从基线里删掉）")
    void baselineMustNotBeStale() {
        Set<String> scanPackages = effectiveScanPackages();
        Set<String> liveControllerPackages = new TreeSet<>();
        for (String className : controllerClasses()) {
            liveControllerPackages.add(packageOf(className));
        }

        List<String> stale = new ArrayList<>();
        for (String pkg : baselinePackages()) {
            if (isCovered(pkg, scanPackages)) {
                stale.add(pkg + "    （已装配）");
            } else if (!liveControllerPackages.contains(pkg)) {
                stale.add(pkg + "    （包里已没有控制器）");
            }
        }

        if (!stale.isEmpty()) {
            fail("""
                基线清单已过期，请从 src/test/resources/%s 删除以下行：
                %s
                基线只允许保留"确实未装配且仍存在"的包，否则它会掩盖真实状态。
                """.formatted(BASELINE_RESOURCE, String.join("\n", stale)));
        }
    }

    @Test
    @DisplayName("装配门禁：扫描到的控制器数量不得异常偏低（防止门禁空跑成装饰）")
    void scanMustNotBeVacuous() {
        int found = controllerClasses().size();
        assertTrue(found >= MIN_EXPECTED_CONTROLLERS,
            "只扫描到 " + found + " 个控制器（期望 ≥ " + MIN_EXPECTED_CONTROLLERS + "）："
                + "说明测试 classpath 不完整，装配门禁已失去意义，请先修好扫描本身。");
    }

    @Test
    @DisplayName("装配门禁：所有 @Mapper 接口所在包必须在 @MapperScan 范围内（否则 Bean 不存在 ⇒ 整个应用起不来）")
    void allMapperInterfacesAreMapperScanned() {
        Set<String> mapperPackages = new TreeSet<>();
        Set<String> violations = new TreeSet<>();
        for (String className : mapperClasses()) {
            String pkg = packageOf(className);
            mapperPackages.add(pkg);
            if (!isMapperScanned(pkg)) {
                violations.add(pkg + "    ← " + className);
            }
        }

        // 防"门禁空跑成装饰"：扫不到东西时它不是通过，而是失效
        assertTrue(mapperPackages.size() >= MIN_EXPECTED_MAPPER_PACKAGES,
            "只扫到 " + mapperPackages.size() + " 个 mapper 包（期望 ≥ " + MIN_EXPECTED_MAPPER_PACKAGES
                + "）：测试 classpath 不完整，本门禁已失去意义，请先修好扫描本身。");

        if (!violations.isEmpty()) {
            fail("""
                发现**不会被注册**的 @Mapper 接口（所在包不在 @MapperScan 范围内）：
                %s

                为什么要命：本仓用的是**显式** @MapperScan，而**一旦存在显式 @MapperScan**，
                MyBatis 的"自动扫 @Mapper 接口"就会退让（AutoConfiguredMapperScannerRegistrar 不生效）
                ⇒ 接口上写了 @Mapper 也**不会**成为 Bean ⇒ 依赖它的 @Component 起不来
                ⇒ **整个应用启动失败**，而不是某个端点 404。
                （2026-09-23 实测：cn.aiedge.erp.purchase.replenishment.ReplenishmentProductMapper，
                 报 NoSuchBeanDefinitionException / APPLICATION FAILED TO START。）

                两种修法（选一）：
                  ① 把接口移进 `xxx.mapper` / `xxx.dao` 包（推荐，通配规则自动覆盖）；
                  ② 在 AiReadyApplication 的 @MapperScan 上**显式登记**该包（并写明理由）。
                """.formatted(String.join("\n", violations)));
        }
    }

    // ────────────────────────── 内部实现 ──────────────────────────

    /** 扫描 classpath 上的所有控制器类名 */
    private Set<String> controllerClasses() {
        ClassPathScanningCandidateComponentProvider scanner = scanner(RestController.class, Controller.class);
        Set<String> names = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_ROOT)) {
            names.add(bd.getBeanClassName());
        }
        return names;
    }

    /** 扫描 classpath 上所有 {@code @Mapper} 接口的类名 */
    private Set<String> mapperClasses() {
        ClassPathScanningCandidateComponentProvider scanner = scanner(Mapper.class);
        Set<String> names = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_ROOT)) {
            names.add(bd.getBeanClassName());
        }
        return names;
    }

    /**
     * {@code @MapperScan} 声明的包模式（含别名 {@code basePackages} 与可重复注解）。
     *
     * <p>**从注解本身读**，不写死一份副本：写死的话，那边加了包、这边门禁却看不见，
     * 就又变回"装饰"。</p>
     */
    private static Set<String> mapperScanPatterns() {
        Set<String> patterns = new LinkedHashSet<>();
        for (MapperScan scan : AiReadyApplication.class.getAnnotationsByType(MapperScan.class)) {
            for (String p : scan.value()) {
                if (!p.isBlank()) {
                    patterns.add(p.trim());
                }
            }
            for (String p : scan.basePackages()) {
                if (!p.isBlank()) {
                    patterns.add(p.trim());
                }
            }
        }
        return patterns;
    }

    /**
     * 包是否落在某个 {@code @MapperScan} 模式内（含子包）。
     *
     * <p>只实现本仓实际用到的两种形态，够用且不猜：</p>
     * <ul>
     *   <li>字面包：{@code cn.aiedge.erp.supplier.repository} ⇒ 相等或为其子包；</li>
     *   <li>带 {@code **} 的模式：{@code cn.aiedge.**.mapper} ⇒ 前缀 {@code cn.aiedge.} 且后缀 {@code .mapper}
     *       （{@code **} 匹配零或多层包，与 MyBatis 解析口径一致）。</li>
     * </ul>
     */
    private static boolean isMapperScanned(String pkg) {
        for (String pattern : mapperScanPatterns()) {
            if (pattern.contains("**")) {
                int idx = pattern.indexOf("**");
                String prefix = pattern.substring(0, idx).replaceAll("\\.$", "");
                String suffix = pattern.substring(idx + 2);
                if (pkg.startsWith(prefix + ".") && (suffix.isEmpty() || pkg.endsWith(suffix))) {
                    return true;
                }
            } else if (pkg.equals(pattern) || pkg.startsWith(pattern + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算「有效扫描包」= 主应用声明的包 + 其 @ComponentScan 闭包。
     * 关键在于闭包必须迭代到不动点：只有声明者自身所在包已被覆盖时，它的 @ComponentScan 才会生效
     * （未被扫描的配置类不会注册，其 @ComponentScan 自然也不会执行）。
     */
    private Set<String> effectiveScanPackages() {
        Set<String> covered = new LinkedHashSet<>();

        // 根：主应用。@SpringBootApplication 未显式声明包时，默认是主应用自身所在包。
        for (Class<?> config : List.of(AiReadyApplication.class)) {
            covered.addAll(declaredScanPackages(config.getName(), SpringBootApplication.class.getName(), true));
        }

        List<AnnotationMetadata> configs = configScanMetadata();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (AnnotationMetadata metadata : configs) {
                if (!isCovered(packageOf(metadata.getClassName()), covered)) {
                    continue;
                }
                for (String pkg : declaredScanPackagesFromMetadata(metadata)) {
                    if (covered.add(pkg)) {
                        changed = true;
                    }
                }
            }
        }
        return covered;
    }

    /** 收集 classpath 上所有 @ComponentScan / @SpringBootApplication 配置类的注解元数据（不加载类） */
    private List<AnnotationMetadata> configScanMetadata() {
        ClassPathScanningCandidateComponentProvider scanner =
            scanner(ComponentScan.class, SpringBootApplication.class);
        List<AnnotationMetadata> result = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_ROOT)) {
            if (bd instanceof AnnotatedBeanDefinition annotated) {
                result.add(annotated.getMetadata());
            }
        }
        return result;
    }

    private Set<String> declaredScanPackages(String className, String annotationName, boolean includeOwnPackage) {
        for (AnnotationMetadata metadata : configScanMetadata()) {
            if (metadata.getClassName().equals(className)) {
                return declaredScanPackagesFromMetadata(metadata);
            }
        }
        throw new IllegalStateException("未在 classpath 上找到配置类：" + className);
    }

    /**
     * 读取配置类声明的扫描包。
     * 同时取 {@code value} 与 {@code basePackages}：二者互为别名，ASM 读取不做别名解析，只取声明的那一个。
     */
    private Set<String> declaredScanPackagesFromMetadata(AnnotationMetadata metadata) {
        Set<String> packages = new LinkedHashSet<>();
        boolean declared = false;

        for (String annotationName : List.of(ComponentScan.class.getName(), SpringBootApplication.class.getName())) {
            Map<String, Object> attrs = metadata.getAnnotationAttributes(annotationName);
            if (attrs == null) {
                continue;
            }
            declared |= addAll(packages, attrs.get("value"));
            declared |= addAll(packages, attrs.get("basePackages"));
            declared |= addAll(packages, attrs.get("scanBasePackages"));
            declared |= addPackageOfClasses(packages, attrs.get("basePackageClasses"));
            declared |= addPackageOfClasses(packages, attrs.get("scanBasePackageClasses"));
        }

        // @SpringBootApplication 一个包都没声明时，默认扫描自身所在包；@ComponentScan 无此默认
        if (!declared && metadata.isAnnotated(SpringBootApplication.class.getName())) {
            packages.add(packageOf(metadata.getClassName()));
        }
        return packages;
    }

    private boolean addAll(Set<String> target, Object value) {
        if (!(value instanceof String[] array) || array.length == 0) {
            return false;
        }
        boolean added = false;
        for (String item : array) {
            if (item != null && !item.isBlank() && !"$__unresolved__".equals(item)) {
                added |= target.add(item.trim());
            }
        }
        return added;
    }

    /** basePackageClasses 拿到的是类名，取其所在包 */
    private boolean addPackageOfClasses(Set<String> target, Object value) {
        if (!(value instanceof String[] array) || array.length == 0) {
            return false;
        }
        boolean added = false;
        for (String className : array) {
            if (className != null && !className.isBlank()) {
                added |= target.add(packageOf(className));
            }
        }
        return added;
    }

    /** 读取基线清单（忽略 # 注释与空行） */
    private Set<String> baselinePackages() {
        Set<String> packages = new TreeSet<>();
        ClassPathResource resource = new ClassPathResource(BASELINE_RESOURCE);
        if (!resource.exists()) {
            return packages;
        }
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    packages.add(trimmed);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取基线清单失败：" + BASELINE_RESOURCE, e);
        }
        return packages;
    }

    private static ClassPathScanningCandidateComponentProvider scanner(Class<? extends Annotation>... stereotypes) {
        return new StaticAnnotationScanner(stereotypes);
    }

    /**
     * 只按注解做静态扫描，**不评估 {@code @Conditional*}**。
     *
     * <p>父类在扫描阶段就会调用 ConditionEvaluator，于是 {@code @ConditionalOnProperty}
     * 这类开关关闭的类会被直接跳过——表现为"该包里没有控制器"，从而让基线过期检查误报。
     * 但本门禁回答的是静态问题：这个包在不在扫描范围内、里面有没有控制器，与运行期开关无关。
     * 因此这里绕开条件评估（父类未提供公开的开关）。</p>
     */
    private static final class StaticAnnotationScanner extends ClassPathScanningCandidateComponentProvider {

        private final List<TypeFilter> includeFilters = new ArrayList<>();

        @SafeVarargs
        StaticAnnotationScanner(Class<? extends Annotation>... stereotypes) {
            // useDefaultFilters = false：只认我们关心的注解，避免把 @Component 也当成"控制器"
            super(false);
            for (Class<? extends Annotation> stereotype : stereotypes) {
                AnnotationTypeFilter filter = new AnnotationTypeFilter(stereotype);
                includeFilters.add(filter);
                addIncludeFilter(filter);
            }
        }

        @Override
        protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
            for (TypeFilter filter : includeFilters) {
                if (filter.match(metadataReader, getMetadataReaderFactory())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * ⚠️ 必须一并放开：父类默认要求"独立且**具体**"（{@code isConcrete()}），
         * 于是**接口会被跳过**。控制器都是具体类，所以过去没暴露这个问题；
         * 而 **mapper 全是接口** —— 不放开就会扫出 0 个，门禁变成"什么都没扫到所以全合规"。
         * 能走到这里说明已经过了注解过滤（{@code scanCandidateComponents} 先调上面那个重载）。
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isIndependent();
        }
    }

    /** 包是否落在扫描范围内（含子包） */
    private static boolean isCovered(String pkg, Set<String> scanPackages) {
        for (String scan : scanPackages) {
            if (pkg.equals(scan) || pkg.startsWith(scan + ".")) {
                return true;
            }
        }
        return false;
    }

    private static String packageOf(String className) {
        int idx = className.lastIndexOf('.');
        return idx < 0 ? "" : className.substring(0, idx);
    }
}
