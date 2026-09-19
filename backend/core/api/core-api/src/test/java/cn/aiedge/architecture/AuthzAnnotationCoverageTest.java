package cn.aiedge.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 鉴权覆盖率门禁。
 *
 * <p>背景：本项目的接口访问控制主要靠注解（{@code @SaCheckPermission} 等），
 * 而注解是「谁写谁有、不写不报错」——漏加既不会编译失败也不会启动失败，
 * 只在被越权调用时才暴露。2026-09-19 实测：396 个控制器中 242 个（61%）
 * 没有任何访问控制注解，覆盖 2162 个端点。
 *
 * <p>本测试把「新增控制器忘记加鉴权」变成 CI 红灯，并把存量缺口固化成一份**只允许缩小**的
 * 基线清单 {@code src/test/resources/known-unauthorized-controllers.txt}。
 *
 * <p>判定：含端点的控制器，类级或方法级必须出现任一访问控制注解，或在基线内。
 *
 * <p><b>为什么用字节码扫描而不是反射或 ArchUnit</b>
 * <ul>
 *   <li>反射（{@code getDeclaredMethods()}）会解析方法签名，遇到 classpath 缺失的可选依赖会抛
 *       {@code NoClassDefFoundError}，把一个与鉴权无关的类误判成"无法判定"；</li>
 *   <li>Spring 的 {@code MetadataReader}（ASM 模式）只暴露类级注解，读不到方法级注解，
 *       而本项目绝大多数鉴权注解标注在方法上；</li>
 *   <li>ArchUnit 能解，但需要给本模块新增 test 依赖，而本仓库以 {@code mvn -o}（离线）构建，
 *       新增依赖有失败风险。</li>
 * </ul>
 * 注解描述符（如 {@code Lcn/dev33/satoken/annotation/SaCheckPermission;}）会作为常量池的
 * UTF8 项写进 class 文件，**且类级与方法级都在同一个 class 文件里**，因此直接按字节搜索即可，
 * 无需解析、无需加载、零依赖。注解在 class 文件里不会因"未使用"被优化掉，
 * 而未使用的 import 根本不写入 class 文件，所以不存在这两类误判。
 */
public class AuthzAnnotationCoverageTest {

    /** 全部业务代码都在该包下 */
    private static final String SCAN_ROOT = "cn.aiedge";

    /**
     * 控制器数量下限：低于此值说明 classpath 不完整（例如测试被裁剪执行），
     * 门禁会退化成"什么都没扫到所以全都合规"。2026-09-19 实测 396 个。
     */
    private static final int MIN_EXPECTED_CONTROLLERS = 300;

    private static final String BASELINE_RESOURCE = "known-unauthorized-controllers.txt";

    /**
     * 算作「已鉴权」的注解（JVM 内部名形式，用于在 class 文件字节里搜索）。
     *
     * <p>自定义注解必须一并计入：{@code @RequirePermission}/{@code @RequireRole} 由
     * core-api 的 {@code PermissionAspect} 通过 AOP 真实执行，漏算会大幅高估缺口。
     *
     * <p>刻意**不计入**的：{@code @DataPermission}/{@code @DataPermissionCheck}/
     * {@code @BusinessPermissionCheck} —— 它们做的是数据范围过滤，不决定接口可达性。
     */
    private static final List<String> AUTH_ANNOTATION_DESCRIPTORS = List.of(
        "Lcn/dev33/satoken/annotation/SaCheckPermission;",
        "Lcn/dev33/satoken/annotation/SaCheckLogin;",
        "Lcn/dev33/satoken/annotation/SaCheckRole;",
        "Lcn/dev33/satoken/annotation/SaIgnore;",
        "Lcn/aiedge/permission/annotation/RequirePermission;",
        "Lcn/aiedge/permission/annotation/RequireRole;",
        "Lcn/aiedge/common/permission/RequiresPermission;",
        "Lcn/aiedge/storage/permission/RequireFilePermission;"
    );

    @Test
    @DisplayName("鉴权门禁：控制器必须带访问控制注解，或在基线清单内")
    void everyControllerIsAuthorizedOrBaselined() {
        Set<String> baseline = baselineClasses();

        List<String> violations = new ArrayList<>();
        for (String className : controllerClasses()) {
            if (hasAuthAnnotation(className)) {
                continue;
            }
            if (baseline.contains(className)) {
                continue;
            }
            violations.add(className);
        }

        if (!violations.isEmpty()) {
            fail("""
                以下控制器既没有任何访问控制注解，也不在基线清单内：

                %s

                说明：这些接口当前是「登录后即可访问」，即任何已登录用户（含最低权限角色）
                都能调用，包括增删改。

                处理方式二选一：
                  a) 要鉴权（推荐）→ 给类或方法加 @SaCheckPermission("模块:资源:动作")。
                     ⚠️ 补注解前**必须先确认该权限码已存在于 sys_permission 并关联到角色**，
                        否则会把当前能用的功能直接锁死（历史事故：90 个权限码缺失导致非超管全 403）。
                        查：python tools/audit-permission-codes.py
                  b) 确实要公开 → 在 src/test/resources/%s 里加一行，
                     并在 CLEANUP_SCOPE_20260919.md 写明理由。
                """.formatted(String.join("\n", violations), BASELINE_RESOURCE));
        }
    }

    @Test
    @DisplayName("鉴权门禁：基线清单不得过期（已补注解或已删除的必须从基线里去掉）")
    void baselineMustNotBeStale() {
        Set<String> live = new LinkedHashSet<>(controllerClasses());

        List<String> stale = new ArrayList<>();
        for (String className : baselineClasses()) {
            if (!live.contains(className)) {
                stale.add(className + "    （类已不存在）");
            } else if (hasAuthAnnotation(className)) {
                stale.add(className + "    （已补上鉴权注解）");
            }
        }

        if (!stale.isEmpty()) {
            fail("""
                基线清单已过期，请从 src/test/resources/%s 删除以下行：

                %s

                基线是「棘轮」：只允许缩小。已补注解的类若继续留在名单里，
                清单就会腐烂成一份"永远豁免"的名册，掩盖真实覆盖率。
                （目的只是补齐缺口，不是为了攒一份大名单。）
                """.formatted(BASELINE_RESOURCE, String.join("\n", stale)));
        }
    }

    @Test
    @DisplayName("鉴权门禁：扫描到的控制器数量不得异常偏低（防止门禁空跑成装饰）")
    void scanMustNotBeVacuous() {
        int found = controllerClasses().size();
        assertTrue(found >= MIN_EXPECTED_CONTROLLERS,
            "只扫描到 " + found + " 个控制器（期望 ≥ " + MIN_EXPECTED_CONTROLLERS + "）："
                + "说明测试 classpath 不完整，鉴权门禁已失去意义，请先修好扫描本身。");
    }

    // ────────────────────────── 内部实现 ──────────────────────────

    /** 扫描 classpath 上所有控制器类名（不加载类） */
    private Set<String> controllerClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        Set<String> names = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_ROOT)) {
            names.add(bd.getBeanClassName());
        }
        return names;
    }

    /**
     * 该控制器的 class 文件里是否出现任一访问控制注解。
     *
     * <p>读的是 class 文件原始字节，因此类级与方法级注解都会命中；
     * 读不到 class 文件时抛异常（宁可让门禁报错，也不要静默当成"无鉴权"而误报一堆违例）。
     */
    private boolean hasAuthAnnotation(String className) {
        byte[] bytes = readClassBytes(className);
        // ISO-8859-1 是字节到字符的一一映射，不会因编码问题丢失或改写字节
        String constantPoolText = new String(bytes, StandardCharsets.ISO_8859_1);
        for (String descriptor : AUTH_ANNOTATION_DESCRIPTORS) {
            if (constantPoolText.contains(descriptor)) {
                return true;
            }
        }
        return false;
    }

    private byte[] readClassBytes(String className) {
        String resource = className.replace('.', '/') + ".class";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AuthzAnnotationCoverageTest.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(
                    "在 classpath 上找不到 " + resource + " —— 扫描结果与 classpath 不一致，门禁不可信");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + resource + " 失败", e);
        }
    }

    /** 读取基线清单（忽略 # 注释与空行） */
    private Set<String> baselineClasses() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = AuthzAnnotationCoverageTest.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(BASELINE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("找不到基线清单 " + BASELINE_RESOURCE);
            }
            Set<String> result = new LinkedHashSet<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    result.add(trimmed);
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("读取基线清单 " + BASELINE_RESOURCE + " 失败", e);
        }
    }
}
