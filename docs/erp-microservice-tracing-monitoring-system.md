# ERP系统微服务链路追踪与性能监控体系设计

## 1. 设计概述

### 1.1 设计目标
为ERP系统的微服务架构设计完整的链路追踪和性能监控体系，实现：
1. 分布式调用链路全链路追踪
2. 跨服务性能指标实时监控
3. 异常链路和慢调用自动识别
4. 可观测性数据统一可视化

### 1.2 技术栈选择
- **链路追踪**: OpenTelemetry + Jaeger
- **指标监控**: Prometheus + Grafana
- **日志收集**: Loki + Fluentd
- **告警管理**: Alertmanager

## 2. 链路追踪体系设计

### 2.1 OpenTelemetry架构设计

#### 2.1.1 数据采集层
```yaml
# OpenTelemetry Collector配置
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
  jaeger:
    protocols:
      grpc:
        endpoint: 0.0.0.0:14250
      thrift_http:
        endpoint: 0.0.0.0:14268

processors:
  batch:
    timeout: 5s
    send_batch_size: 100
  memory_limiter:
    check_interval: 1s
    limit_percentage: 75
    spike_limit_percentage: 15

exporters:
  jaeger:
    endpoint: jaeger:14250
    insecure: true
  prometheus:
    endpoint: "0.0.0.0:8889"
    const_labels:
      environment: "production"
      service: "erp-system"
  logging:
    loglevel: debug

service:
  pipelines:
    traces:
      receivers: [otlp, jaeger]
      processors: [memory_limiter, batch]
      exporters: [jaeger]
    metrics:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [prometheus]
```

### 2.2 Span/Trace设计规范

#### 2.2.1 Span命名规范
```
{service_name}.{resource}.{action}
示例：
- erp.order.create
- erp.inventory.query
- auth.user.login
```

#### 2.2.2 Trace上下文传播
```java
// Java示例 - 上下文传播
public class TracingInterceptor implements HandlerInterceptor {
    
    private final Tracer tracer;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        // 提取Trace上下文
        SpanContext parentContext = extract(request);
        
        // 创建新Span
        Span span = tracer.spanBuilder("http.request")
            .setParent(Context.current().with(parentContext))
            .setAttribute("http.method", request.getMethod())
            .setAttribute("http.path", request.getRequestURI())
            .startSpan();
            
        // 注入TraceID和SpanID到响应头
        response.setHeader("X-Trace-ID", span.getSpanContext().getTraceId());
        
        return true;
    }
}
```

### 2.3 微服务集成方案

#### 2.3.1 Spring Boot集成配置
```yaml
# application-tracing.yml
opentelemetry:
  exporter:
    jaeger:
      endpoint: http://opentelemetry-collector:14250
      service-name: ${spring.application.name}
  
  instrumentation:
    http:
      enabled: true
    jdbc:
      enabled: true
    kafka:
      enabled: true
    redis:
      enabled: true
      
management:
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 0.1  # 采样率10%
```

## 3. 性能监控体系设计

### 3.1 关键性能指标定义

#### 3.1.1 系统层指标
- CPU使用率、内存使用率、磁盘IO
- 网络带宽、TCP连接数
- 系统负载、线程数

#### 3.1.2 应用层指标
- HTTP请求量、响应时间、错误率
- 数据库查询性能、连接池状态
- 缓存命中率、消息队列积压

#### 3.1.3 业务层指标
- 订单处理成功率、平均处理时间
- 库存查询响应时间、准确率
- 支付成功率、支付超时率

### 3.2 Prometheus监控规则

#### 3.2.1 监控目标配置
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'erp-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'erp-order-service:8080'
        - 'erp-inventory-service:8080'
        - 'erp-payment-service:8080'
        - 'erp-auth-service:8080'
        - 'erp-report-service:8080'
    scrape_interval: 15s
    scrape_timeout: 10s
  
  - job_name: 'opentelemetry-collector'
    static_configs:
      - targets: ['opentelemetry-collector:8889']
  
  - job_name: 'jaeger'
    static_configs:
      - targets: ['jaeger:16686']
```

#### 3.2.2 告警规则设计
```yaml
# prometheus-rules.yml
groups:
  - name: erp-system-rules
    rules:
      # HTTP错误率告警
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status!~"2..", uri!~".*actuator.*"}[5m]))
          /
          sum(rate(http_server_requests_seconds_count{uri!~".*actuator.*"}[5m]))
          * 100 > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "高错误率告警"
          description: "服务 {{ $labels.service }} 的HTTP错误率超过5% (当前值: {{ $value }}%)"
      
      # 慢响应告警
      - alert: SlowResponse
        expr: |
          histogram_quantile(0.95, 
            rate(http_server_requests_seconds_bucket{uri!~".*actuator.*"}[5m])
          ) > 2
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "慢响应告警"
          description: "服务 {{ $labels.service }} 95%响应时间超过2秒 (当前值: {{ $value }}秒)"
      
      # 数据库连接池告警
      - alert: HighDbConnectionUsage
        expr: |
          hikaricp_connections_active / hikaricp_connections_max * 100 > 80
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池高使用率"
          description: "服务 {{ $labels.service }} 数据库连接池使用率超过80% (当前值: {{ $value }}%)"
      
      # 内存使用告警
      - alert: HighMemoryUsage
        expr: |
          (process_resident_memory_bytes / process_virtual_memory_bytes) * 100 > 85
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "高内存使用率告警"
          description: "服务 {{ $labels.service }} 内存使用率超过85% (当前值: {{ $value }}%)"
```

## 4. 部署架构设计

### 4.1 组件部署方案
```yaml
# docker-compose.tracing.yml
version: '3.8'

services:
  # OpenTelemetry Collector
  opentelemetry-collector:
    image: otel/opentelemetry-collector:latest
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "14250:14250" # Jaeger gRPC
      - "8889:8889"   # Prometheus metrics
    volumes:
      - ./config/otel-collector-config.yml:/etc/otelcol/config.yaml
    networks:
      - tracing-network

  # Jaeger UI
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686" # Web UI
      - "14250:14250" # gRPC
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    networks:
      - tracing-network
    depends_on:
      - opentelemetry-collector

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./config/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./config/prometheus-rules.yml:/etc/prometheus/rules.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
    networks:
      - tracing-network

  # Grafana
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - ./config/grafana-datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
      - ./config/grafana-dashboards.yml:/etc/grafana/provisioning/dashboards/dashboards.yml
      - ./dashboards:/etc/grafana/dashboards
      - grafana-data:/var/lib/grafana
    networks:
      - tracing-network
    depends_on:
      - prometheus
      - jaeger

  # Alertmanager
  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./config/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    networks:
      - tracing-network
    depends_on:
      - prometheus

networks:
  tracing-network:
    driver: bridge

volumes:
  prometheus-data:
  grafana-data:
```

## 5. 监控仪表板设计

### 5.1 Jaeger Trace搜索面板
- **搜索条件**: 服务名、操作名、标签、持续时间
- **可视化**: 调用链图、时序图、依赖图
- **分析功能**: 延迟分析、错误分析、依赖分析

### 5.2 Grafana综合监控仪表板

#### 5.2.1 系统概览面板
- 服务健康状态（红/黄/绿）
- 总体请求量、错误率、响应时间
- 资源使用情况（CPU、内存、网络）

#### 5.2.2 服务详情面板
- 单个服务的详细指标
- 调用拓扑图
- 性能瓶颈分析

#### 5.2.3 业务指标面板
- 订单处理指标
- 库存管理指标
- 支付处理指标

## 6. 异常链路识别机制

### 6.1 慢调用识别规则
```yaml
# jaeger-sampling-config.yml
sampling:
  default_strategy:
    type: probabilistic
    param: 0.1
  per_operation_strategies:
    - operation: "erp.order.*"
      type: probabilistic
      param: 1.0  # 对订单相关操作100%采样
    - operation: "*.create"
      type: rate_limiting
      param: 100  # 每秒最多100个

  # 慢调用自动采样
  adaptive_sampling:
    enabled: true
    target_rate: 10  # 目标采样率10%
    delta_tolerance: 0.3
    sampling_rate_cap: 0.5
```

### 6.2 异常检测算法
```python
# 异常检测脚本示例
class AnomalyDetector:
    def detect_slow_traces(self, traces, threshold_ms=1000):
        """检测慢调用链路"""
        slow_traces = []
        for trace in traces:
            if trace.duration > threshold_ms:
                slow_traces.append({
                    'trace_id': trace.trace_id,
                    'duration_ms': trace.duration,
                    'service_calls': trace.service_calls,
                    'bottleneck': self._find_bottleneck(trace)
                })
        return slow_traces
    
    def _find_bottleneck(self, trace):
        """找到性能瓶颈"""
        max_duration = 0
        bottleneck = None
        for span in trace.spans:
            if span.duration > max_duration:
                max_duration = span.duration
                bottleneck = span.operation_name
        return bottleneck
```

## 7. 实施路线图

### 7.1 第一阶段：基础设施搭建（2周）
1. 部署OpenTelemetry Collector、Jaeger、Prometheus
2. 配置微服务OpenTelemetry SDK集成
3. 设置基础告警规则

### 7.2 第二阶段：全面监控（3周）
1. 完善各服务指标收集
2. 设计并部署监控仪表板
3. 实现链路追踪上下文传播

### 7.3 第三阶段：智能分析（2周）
1. 部署异常检测算法
2. 实现智能告警和根因分析
3. 优化采样策略

## 8. 效益评估

### 8.1 技术效益
- 问题定位时间从小时级降低到分钟级
- 系统可观测性提升，故障预测能力增强
- 性能瓶颈可视化，优化效果可量化

### 8.2 业务效益
- 系统稳定性提升，用户体验改善
- 运维成本降低，自动化程度提高
- 业务决策支持，数据驱动优化

## 9. 维护和优化

### 9.1 定期维护任务
1. **数据清理**: 定期清理过期追踪数据和指标
2. **配置更新**: 根据业务变化调整告警阈值
3. **性能优化**: 优化采样策略，减少系统开销

### 9.2 监控体系优化
1. **指标精简**: 只收集关键业务指标
2. **告警收敛**: 减少告警风暴，提高告警准确性
3. **自动化运维**: 实现自动扩容和故障自愈

---

**设计完成时间**: 2026年5月4日 20:35  
**设计人**: devops-engineer  
**适用系统**: ERP微服务架构  
**技术栈**: OpenTelemetry, Jaeger, Prometheus, Grafana, Spring Boot