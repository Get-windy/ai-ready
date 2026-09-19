package cn.aiedge.datasource.controller;

import cn.aiedge.datasource.model.DataSource;
import cn.aiedge.datasource.service.DataSourceService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源控制器
 */
@RestController
@RequestMapping("/api/data-source")
@RequiredArgsConstructor
@Tag(name = "数据源管理", description = "数据源连接管理功能")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 本系统**自身运行时**的数据源（Spring Boot 自动配置的单数据源）。
     *
     * <p>刻意用全限定名 {@code javax.sql.DataSource}：本包内已有同名的业务实体
     * {@link cn.aiedge.datasource.model.DataSource}，简名会冲突。</p>
     */
    private final javax.sql.DataSource appDataSource;

    @GetMapping("/list")
    @SaCheckPermission("system:datasource:list")
    @Operation(summary = "分页查询数据源列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<DataSource> allDataSources = dataSourceService.list(keyword, tenantId);
        int total = allDataSources.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DataSource> records = fromIndex >= total ? List.of() : allDataSources.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", pageSize,
                "pages", (total + pageSize - 1) / pageSize
        ));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:datasource:list")
    @Operation(summary = "获取单个数据源")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getById(id);
        if (dataSource == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "数据源不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", dataSource));
    }

    @PostMapping("/")
    @SaCheckPermission("system:datasource:create")
    @Operation(summary = "创建数据源")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody DataSource dataSource,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        DataSource saved = dataSourceService.create(dataSource, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:datasource:update")
    @Operation(summary = "更新数据源")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody DataSource dataSource,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        DataSource updated = dataSourceService.update(id, dataSource, tenantId);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "数据源不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:datasource:delete")
    @Operation(summary = "删除数据源")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = dataSourceService.delete(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "数据源不存在"));
    }

    @PostMapping("/test")
    @SaCheckPermission("system:datasource:test")
    @Operation(summary = "测试数据源连接")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody Map<String, Object> request) {

        Object idObj = request.get("id");
        if (idObj == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供数据源ID"));
        }
        Long id = Long.valueOf(idObj.toString());
        boolean success = dataSourceService.testConnection(id);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败，请检查数据源配置"
        ));
    }

    /**
     * 当前生效的数据库连接（本系统自身，**不返回密码**）。
     *
     * <p><b>为什么需要它</b>：本页原先只展示 `sys_data_source` 表里**用户登记的外部数据源**
     * （增删改查 + 连通性测试）。该表为空时页面什么都不显示——管理员打开「连接管理」
     * 最想知道的「本系统此刻连的是哪个库」反而看不到。本端点补上这一视角：
     * 直接从运行时数据源读 JDBC 元数据，不依赖任何业务表。</p>
     *
     * <p>安全口径：只返回主机/端口/库名/驱动/版本与**脱敏后**的用户名，绝不返回密码。</p>
     */
    @GetMapping("/current")
    @SaCheckPermission("system:datasource:list")
    @Operation(summary = "当前生效的数据库连接（本系统自身）")
    public ResponseEntity<Map<String, Object>> currentConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = appDataSource.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String url = md.getURL();

            result.put("dbType", md.getDatabaseProductName());
            result.put("dbVersion", md.getDatabaseProductVersion());
            result.put("driver", md.getDriverName());
            result.put("url", url);
            result.put("username", maskUsername(md.getUserName()));
            result.put("readOnly", conn.isReadOnly());
            result.put("autoCommit", conn.getAutoCommit());

            // 解析 jdbc:<scheme>://<host>:<port>/<database>?<params>
            if (url != null && url.startsWith("jdbc:")) {
                String rest = url.substring("jdbc:".length());
                int schemeEnd = rest.indexOf("://");
                if (schemeEnd > 0) {
                    result.put("dbScheme", rest.substring(0, schemeEnd));
                    String hostPart = rest.substring(schemeEnd + 3);
                    int slash = hostPart.indexOf('/');
                    String authority = slash >= 0 ? hostPart.substring(0, slash) : hostPart;
                    String dbName = slash >= 0 ? hostPart.substring(slash + 1) : "";
                    int q = dbName.indexOf('?');
                    if (q >= 0) {
                        dbName = dbName.substring(0, q);
                    }
                    int colon = authority.lastIndexOf(':');
                    if (colon > 0) {
                        result.put("host", authority.substring(0, colon));
                        result.put("port", authority.substring(colon + 1));
                    } else {
                        result.put("host", authority);
                    }
                    result.put("databaseName", dbName);
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        // 连接池指标（HikariCP）；非 Hikari 实现时跳过，不影响主信息
        if (appDataSource instanceof HikariDataSource hikari) {
            try {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                result.put("poolName", hikari.getPoolName());
                result.put("poolMax", hikari.getMaximumPoolSize());
                if (pool != null) {
                    result.put("poolActive", pool.getActiveConnections());
                    result.put("poolIdle", pool.getIdleConnections());
                    result.put("poolTotal", pool.getTotalConnections());
                    result.put("poolWaiting", pool.getThreadsAwaitingConnection());
                }
            } catch (Exception ignored) {
                // 池指标拿不到不影响主信息
            }
        }
        return ResponseEntity.ok(result);
    }

    /** 用户名脱敏：保留首尾各 1 字符，中间打码（长度 ≤ 2 时全打码） */
    private String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        if (username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}
