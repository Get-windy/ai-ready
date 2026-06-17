package cn.aiedge.report.service.impl;

import cn.aiedge.datasource.model.DataSource;
import cn.aiedge.datasource.service.DataSourceService;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.ReportDataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表数据源服务实现
 * <p>
 * 支持 SQL, API, mixed, custom 四种数据源类型：
 * <ul>
 *   <li><b>sql</b> — 通过 dataSourceConfig.dataSourceId 引用 sys_data_source 中的连接信息执行真实SQL</li>
 *   <li><b>api</b> — 通过 dataSourceConfig.url/method/headers 调用外部HTTP API</li>
 *   <li><b>mixed</b> — 先执行SQL再调API，合并结果</li>
 *   <li><b>custom</b> — 通过 dataSourceConfig.handler 指定自定义处理器</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDataSourceServiceImpl implements ReportDataSourceService {

    private final DataSourceService dataSourceService;
    private final RestTemplate restTemplate;

    // ==================== 核心入口 ====================

    @Override
    public List<Map<String, Object>> fetchData(ReportDefinition definition,
                                               Map<String, Object> parameters, Long tenantId) {
        String dataSourceType = definition.getDataSourceType();

        switch (dataSourceType.toLowerCase()) {
            case "sql":
                return fetchFromSql(definition, parameters, tenantId);
            case "api":
                return fetchFromApi(definition, parameters);
            case "mixed":
                Map<String, Object> config = definition.getDataSourceConfig();
                String sql = (String) config.get("sql");
                String apiUrl = (String) config.get("apiUrl");
                if (sql == null || apiUrl == null) {
                    throw new IllegalArgumentException("混合数据源必须同时提供 sql 和 apiUrl");
                }
                return fetchMixedData(sql, apiUrl, parameters, tenantId);
            case "custom":
                return fetchFromCustomHandler(definition, parameters, tenantId);
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + dataSourceType);
        }
    }

    @Override
    public boolean testConnection(Map<String, Object> dataSourceConfig) {
        String type = (String) dataSourceConfig.get("type");
        if (type == null) return false;

        try {
            switch (type) {
                case "sql":
                    return testSqlConnection(dataSourceConfig);
                case "api":
                    return testApiConnection(dataSourceConfig);
                default:
                    log.warn("不支持的数据源连接测试类型: {}", type);
                    return false;
            }
        } catch (Exception e) {
            log.error("数据源连接测试失败: type={}", type, e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedDataSourceTypes() {
        return List.of("sql", "api", "mixed", "custom");
    }

    @Override
    public List<Map<String, Object>> executeSql(String sql, Map<String, Object> parameters, Long tenantId) {
        // 默认使用 tenantId=1 的数据源作为默认数据源
        List<DataSource> dataSources = dataSourceService.list(null, tenantId != null ? tenantId : 1L);
        if (dataSources.isEmpty()) {
            log.warn("未找到可用数据源，返回模拟数据: tenantId={}", tenantId);
            return mockSqlResult(sql, parameters);
        }
        return executeSqlOnDataSource(sql, parameters, dataSources.get(0));
    }

    @Override
    public Map<String, Object> callApi(String apiUrl, String method,
                                       Map<String, String> headers,
                                       Map<String, Object> parameters) {
        return doHttpCall(apiUrl, method, headers, parameters);
    }

    @Override
    public List<Map<String, Object>> fetchMixedData(String sql, String apiUrl,
                                                    Map<String, Object> parameters, Long tenantId) {
        // 先执行SQL查询
        List<Map<String, Object>> sqlData = executeSql(sql, parameters, tenantId);

        // 再调用API获取补充数据
        Map<String, Object> apiData = callApi(apiUrl, "GET", Map.of(), parameters);

        // 合并数据
        return mergeSqlAndApiData(sqlData, apiData);
    }

    // ==================== SQL 数据源 ====================

    /**
     * 从SQL数据源获取数据
     */
    private List<Map<String, Object>> fetchFromSql(ReportDefinition definition,
                                                   Map<String, Object> parameters, Long tenantId) {
        Map<String, Object> config = definition.getDataSourceConfig();
        String sql = (String) config.get("sql");
        if (sql == null) {
            throw new IllegalArgumentException("SQL数据源必须提供SQL语句");
        }

        // 优先使用 dataSourceId 从 sys_data_source 查找数据源
        Object dsIdObj = config.get("dataSourceId");
        if (dsIdObj != null) {
            Long dataSourceId;
            if (dsIdObj instanceof Number) {
                dataSourceId = ((Number) dsIdObj).longValue();
            } else {
                dataSourceId = Long.parseLong(dsIdObj.toString());
            }

            DataSource ds = dataSourceService.getById(dataSourceId);
            if (ds != null) {
                return executeSqlOnDataSource(sql, parameters, ds);
            }
            log.warn("数据源不存在或已删除: dataSourceId={}, 回退到默认数据源", dataSourceId);
        }

        // 没有指定 dataSourceId 时，使用租户下的第一个数据源
        log.debug("未指定 dataSourceId，使用租户默认数据源: tenantId={}", tenantId);
        return executeSql(sql, parameters, tenantId);
    }

    /**
     * 在指定数据源上执行SQL查询
     */
    private List<Map<String, Object>> executeSqlOnDataSource(String sql, Map<String, Object> parameters,
                                                              DataSource ds) {
        String url = buildJdbcUrl(ds);
        Properties connProps = new Properties();
        connProps.setProperty("user", ds.getUsername());
        connProps.setProperty("password", ds.getPassword() != null ? ds.getPassword() : "");
        connProps.setProperty("connectTimeout", "10000");
        connProps.setProperty("socketTimeout", "30000");

        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, connProps);
             PreparedStatement stmt = buildPreparedStatement(conn, sql, parameters);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }

            log.debug("SQL查询完成: rows={}, dataSource={}", result.size(), ds.getName());
        } catch (SQLException e) {
            log.error("SQL查询执行失败: dataSource={}, sql={}", ds.getName(), sql, e);
            throw new RuntimeException("SQL查询执行失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 构建 PreparedStatement，支持参数绑定
     */
    private PreparedStatement buildPreparedStatement(Connection conn, String sql,
                                                     Map<String, Object> parameters) throws SQLException {
        // 如果SQL包含命名参数（如 :name），替换为 ?
        String resolvedSql = sql;
        List<Object> paramValues = new ArrayList<>();

        if (parameters != null && !parameters.isEmpty()) {
            // 支持 :paramName 和 ? 两种参数形式
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String placeholder = ":" + entry.getKey();
                if (resolvedSql.contains(placeholder)) {
                    resolvedSql = resolvedSql.replace(placeholder, "?");
                    paramValues.add(entry.getValue());
                }
            }

            // 替换报表预定义参数占位符 ${paramName}
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (resolvedSql.contains(placeholder)) {
                    resolvedSql = resolvedSql.replace(placeholder,
                            entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }
        }

        PreparedStatement stmt = conn.prepareStatement(resolvedSql);

        // 绑定 ? 参数
        for (int i = 0; i < paramValues.size(); i++) {
            stmt.setObject(i + 1, paramValues.get(i));
        }

        log.debug("构建PreparedStatement: sql={}, params={}", resolvedSql, paramValues);
        return stmt;
    }

    /**
     * 根据 DataSource 实体构建 JDBC URL
     */
    private String buildJdbcUrl(DataSource ds) {
        String dbType = ds.getDbType() != null ? ds.getDbType().toLowerCase() : "mysql";
        String host = ds.getHost() != null ? ds.getHost() : "localhost";
        int port = ds.getPort() != null ? ds.getPort() : 3306;
        String database = ds.getDatabaseName() != null ? ds.getDatabaseName() : "";

        switch (dbType) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8", host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false", host, port, database);
            default:
                log.warn("不支持的数据库类型: {}, 默认使用MySQL格式", dbType);
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai", host, port, database);
        }
    }

    // ==================== API 数据源 ====================

    /**
     * 从API数据源获取数据
     */
    private List<Map<String, Object>> fetchFromApi(ReportDefinition definition,
                                                   Map<String, Object> parameters) {
        Map<String, Object> config = definition.getDataSourceConfig();
        String apiUrl = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.get("headers");

        if (apiUrl == null) {
            throw new IllegalArgumentException("API数据源必须提供URL");
        }

        Map<String, Object> response = doHttpCall(apiUrl, method, headers, parameters);

        // 从响应中提取数据（response.data, response.items, response.rows 等）
        Object data = response.get("data");
        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) data;
            return result;
        }
        // 单对象响应包装为列表
        return List.of(response);
    }

    /**
     * 执行HTTP调用
     */
    private Map<String, Object> doHttpCall(String apiUrl, String method,
                                           Map<String, String> headers,
                                           Map<String, Object> parameters) {
        // 将参数拼接到URL（GET请求）或作为请求体（POST请求）
        String resolvedUrl = apiUrl;
        HttpMethod httpMethod = HttpMethod.GET;

        if ("POST".equalsIgnoreCase(method)) {
            httpMethod = HttpMethod.POST;
        } else {
            // GET请求：参数拼接到URL
            if (parameters != null && !parameters.isEmpty()) {
                String queryString = parameters.entrySet().stream()
                        .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue().toString() : ""))
                        .collect(Collectors.joining("&"));
                resolvedUrl = apiUrl + (apiUrl.contains("?") ? "&" : "?") + queryString;
            }
        }

        // 构建请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        // 构建请求体
        HttpEntity<?> requestEntity;
        if (httpMethod == HttpMethod.POST) {
            requestEntity = new HttpEntity<>(parameters, httpHeaders);
        } else {
            requestEntity = new HttpEntity<>(httpHeaders);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(resolvedUrl, httpMethod, requestEntity, Map.class);
            log.debug("API调用完成: url={}, status={}", resolvedUrl, response.getStatusCodeValue());

            if (response.getBody() != null) {
                return response.getBody();
            }
            return Map.of();
        } catch (Exception e) {
            log.error("API调用失败: url={}, method={}", resolvedUrl, method, e);
            throw new RuntimeException("API调用失败: " + e.getMessage(), e);
        }
    }

    // ==================== 自定义处理器 ====================

    /**
     * 从自定义处理器获取数据
     */
    private List<Map<String, Object>> fetchFromCustomHandler(ReportDefinition definition,
                                                             Map<String, Object> parameters, Long tenantId) {
        Map<String, Object> config = definition.getDataSourceConfig();
        String handlerName = (String) config.get("handler");

        if (handlerName == null) {
            throw new IllegalArgumentException("自定义数据源必须提供handler名称");
        }

        switch (handlerName) {
            case "CustomReportHandler":
                return handleCustomReport(definition, parameters, tenantId);
            default:
                log.warn("未知的自定义处理器: {}, 返回模拟数据", handlerName);
                return mockSqlResult("SELECT * FROM custom_data", parameters);
        }
    }

    // ==================== 连接测试 ====================

    /**
     * 测试SQL数据库连接
     */
    private boolean testSqlConnection(Map<String, Object> dataSourceConfig) {
        Object dsIdObj = dataSourceConfig.get("dataSourceId");
        if (dsIdObj == null) return false;

        Long dataSourceId;
        if (dsIdObj instanceof Number) {
            dataSourceId = ((Number) dsIdObj).longValue();
        } else {
            dataSourceId = Long.parseLong(dsIdObj.toString());
        }

        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) return false;

        String url = buildJdbcUrl(ds);
        Properties connProps = new Properties();
        connProps.setProperty("user", ds.getUsername());
        connProps.setProperty("password", ds.getPassword() != null ? ds.getPassword() : "");
        connProps.setProperty("connectTimeout", "5000");
        connProps.setProperty("socketTimeout", "5000");

        try (Connection conn = DriverManager.getConnection(url, connProps)) {
            log.info("SQL连接测试成功: dataSource={}", ds.getName());
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("SQL连接测试失败: dataSource={}, url={}", ds.getName(), url, e);
            return false;
        }
    }

    /**
     * 测试API连接
     */
    private boolean testApiConnection(Map<String, Object> dataSourceConfig) {
        String url = (String) dataSourceConfig.get("url");
        if (url == null) return false;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            log.info("API连接测试成功: url={}, status={}", url, response.getStatusCodeValue());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("API连接测试失败: url={}", url, e);
            return false;
        }
    }

    // ==================== 自定义报表处理 ====================

    /**
     * 处理自定义报表
     */
    private List<Map<String, Object>> handleCustomReport(ReportDefinition definition,
                                                         Map<String, Object> parameters, Long tenantId) {
        // 这里可以根据业务需求实现自定义逻辑
        // 例如：从多个数据源聚合、调用复杂计算等
        Map<String, Object> config = definition.getDataSourceConfig();

        // 检查是否指定了数据源
        Object dsIdObj = config.get("dataSourceId");
        if (dsIdObj != null) {
            Long dataSourceId;
            if (dsIdObj instanceof Number) {
                dataSourceId = ((Number) dsIdObj).longValue();
            } else {
                dataSourceId = Long.parseLong(dsIdObj.toString());
            }
            DataSource ds = dataSourceService.getById(dataSourceId);
            if (ds != null) {
                String sql = (String) config.getOrDefault("sql", "SELECT 1 AS result");
                return executeSqlOnDataSource(sql, parameters, ds);
            }
        }

        log.debug("自定义报表无有效数据源，返回模拟数据: reportId={}", definition.getReportId());
        return mockSqlResult("SELECT * FROM custom_data", parameters);
    }

    // ==================== 数据合并 ====================

    /**
     * 合并SQL和API数据
     */
    private List<Map<String, Object>> mergeSqlAndApiData(List<Map<String, Object>> sqlData,
                                                         Map<String, Object> apiData) {
        List<Map<String, Object>> merged = new ArrayList<>(sqlData);

        Object apiList = apiData.get("data");
        if (apiList instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) apiList;
            merged.addAll(list);
        } else {
            // 将API单对象作为一行附加
            Map<String, Object> flatRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : apiData.entrySet()) {
                if (!(entry.getValue() instanceof List) && !(entry.getValue() instanceof Map)) {
                    flatRow.put(entry.getKey(), entry.getValue());
                }
            }
            if (!flatRow.isEmpty()) {
                merged.add(flatRow);
            }
        }

        return merged;
    }

    // ==================== 模拟数据（向后兼容） ====================

    /**
     * 模拟SQL查询结果 — 当无可用数据源时作为降级方案
     */
    private List<Map<String, Object>> mockSqlResult(String sql, Map<String, Object> parameters) {
        String lowerSql = sql.toLowerCase();
        if (lowerSql.contains("orders") || lowerSql.contains("order")) {
            return List.of(
                    Map.of("orderId", 1, "customerName", "张三", "amount", 1000.0, "status", "已完成"),
                    Map.of("orderId", 2, "customerName", "李四", "amount", 2500.0, "status", "处理中"),
                    Map.of("orderId", 3, "customerName", "王五", "amount", 580.0, "status", "已发货"),
                    Map.of("orderId", 4, "customerName", "赵六", "amount", 3200.0, "status", "已完成")
            );
        } else if (lowerSql.contains("customers") || lowerSql.contains("customer")) {
            return List.of(
                    Map.of("customerId", 1, "name", "张三", "email", "zhangsan@example.com", "city", "北京"),
                    Map.of("customerId", 2, "name", "李四", "email", "lisi@example.com", "city", "上海"),
                    Map.of("customerId", 3, "name", "王五", "email", "wangwu@example.com", "city", "广州")
            );
        } else {
            return List.of(
                    Map.of("id", 1, "name", "测试数据1", "value", 100),
                    Map.of("id", 2, "name", "测试数据2", "value", 200),
                    Map.of("id", 3, "name", "测试数据3", "value", 300)
            );
        }
    }
}
