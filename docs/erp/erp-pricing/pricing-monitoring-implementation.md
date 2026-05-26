# 价格策略模块性能监控实施指南

## 1. 快速开始

### 1.1 环境要求
- Java 17+
- Docker & Docker Compose
- 至少4GB内存
- 至少20GB磁盘空间

### 1.2 部署步骤
```bash
# 1. 克隆监控配置仓库
git clone https://github.com/your-org/pricing-monitoring.git
cd pricing-monitoring

# 2. 启动监控栈
docker-compose up -d

# 3. 验证部署
docker-compose ps
```

## 2. 监控配置

### 2.1 Prometheus配置
创建 `prometheus.yml`:

```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "alert_rules.yml"

scrape_configs:
  - job_name: 'pricing-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['pricing-service:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '(.+):\d+'
        replacement: '${1}'
```

### 2.2 Grafana仪表板
导入预配置的仪表板:

1. 访问 `http://localhost:3000`
2. 登录 (admin/admin)
3. 导入仪表板ID: `12345`
4. 选择Prometheus数据源

## 3. 告警配置

### 3.1 告警规则文件
创建 `alert_rules.yml`:

```yaml
groups:
  - name: pricing-performance
    rules:
      # 查询性能告警
      - alert: HighQueryLatency
        expr: pricing_query_latency_seconds{quantile="0.95"} > 0.2
        for: 5m
        labels:
          severity: warning
          service: pricing
        annotations:
          summary: "价格查询延迟过高"
          description: "{{ $labels.instance }} 的95分位查询延迟为{{ $value }}s"
          
      # 计算性能告警
      - alert: HighCalculationTime
        expr: pricing_calculation_duration_seconds > 0.1
        for: 3m
        labels:
          severity: warning
          service: pricing
        annotations:
          summary: "价格计算时间过长"
          description: "{{ $labels.instance }} 的价格计算时间为{{ $value }}s"

      # 系统资源告警
      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes / process_virtual_memory_bytes > 0.85
        for: 5m
        labels:
          severity: critical
          service: pricing
        annotations:
          summary: "内存使用率过高"
          description: "{{ $labels.instance }} 内存使用率达到{{ $value | humanizePercentage }}"
```

### 3.2 Alertmanager配置
创建 `alertmanager.yml`:

```yaml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@example.com'
  smtp_auth_username: 'user'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default-receiver'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
      continue: false
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'team@example.com'
  
  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@example.com'
    webhook_configs:
      - url: 'https://chat.example.com/hooks/alert'
        send_resolved: true
  
  - name: 'warning-alerts'
    email_configs:
      - to: 'devs@example.com'
```

## 4. 应用集成

### 4.1 Spring Boot应用集成
在 `pom.xml` 中添加依赖:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 4.2 应用配置
在 `application.yml` 中配置:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: "pricing-service"
      environment: "${spring.profiles.active}"
    
  endpoint:
    prometheus:
      access: "read_only"
```

### 4.3 自定义指标
创建自定义指标:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PricingMetrics {
    
    private final Counter queryCounter;
    private final Timer queryTimer;
    private final Counter calculationCounter;
    private final Timer calculationTimer;
    
    public PricingMetrics(MeterRegistry registry) {
        this.queryCounter = Counter.builder("pricing.query.count")
            .description("价格查询次数")
            .tag("type", "query")
            .register(registry);
            
        this.queryTimer = Timer.builder("pricing.query.duration")
            .description("价格查询耗时")
            .tag("type", "query")
            .register(registry);
            
        this.calculationCounter = Counter.builder("pricing.calculation.count")
            .description("价格计算次数")
            .tag("type", "calculation")
            .register(registry);
            
        this.calculationTimer = Timer.builder("pricing.calculation.duration")
            .description("价格计算耗时")
            .tag("type", "calculation")
            .register(registry);
    }
    
    public void recordQuery(long duration) {
        queryCounter.increment();
        queryTimer.record(duration, TimeUnit.MILLISECONDS);
    }
    
    public void recordCalculation(long duration) {
        calculationCounter.increment();
        calculationTimer.record(duration, TimeUnit.MILLISECONDS);
    }
}
```

## 5. 监控脚本

### 5.1 健康检查脚本
创建 `health_check.sh`:

```bash
#!/bin/bash

# 检查服务状态
check_service() {
    local service=$1
    local port=$2
    
    if curl -s "http://localhost:${port}/actuator/health" | grep -q "UP"; then
        echo "✅ ${service} is healthy"
        return 0
    else
        echo "❌ ${service} is down"
        return 1
    fi
}

# 检查监控组件
check_service "Pricing Service" 8080
check_service "Prometheus" 9090
check_service "Grafana" 3000
check_service "Alertmanager" 9093

# 检查指标收集
if curl -s "http://localhost:9090/api/v1/targets" | grep -q "UP"; then
    echo "✅ Prometheus targets are healthy"
else
    echo "❌ Prometheus targets have issues"
fi
```

### 5.2 性能测试脚本
创建 `performance_test.py`:

```python
import requests
import time
import statistics
import json
from concurrent.futures import ThreadPoolExecutor

class PricingPerformanceTest:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.results = []
        
    def test_query(self, product_id="test-001", quantity=1):
        """测试价格查询性能"""
        start_time = time.time()
        try:
            response = requests.get(
                f"{self.base_url}/api/pricing/query",
                params={"productId": product_id, "quantity": quantity},
                timeout=5
            )
            duration = (time.time() - start_time) * 1000  # 转为毫秒
            return {
                "success": response.status_code == 200,
                "duration": duration,
                "status_code": response.status_code
            }
        except Exception as e:
            return {
                "success": False,
                "duration": (time.time() - start_time) * 1000,
                "error": str(e)
            }
    
    def test_calculation(self, product_id="test-001", quantity=10):
        """测试价格计算性能"""
        start_time = time.time()
        try:
            payload = {
                "items": [{"productId": product_id, "quantity": quantity}],
                "customerId": "test-customer"
            }
            response = requests.post(
                f"{self.base_url}/api/pricing/calculate",
                json=payload,
                timeout=10
            )
            duration = (time.time() - start_time) * 1000
            return {
                "success": response.status_code == 200,
                "duration": duration,
                "status_code": response.status_code
            }
        except Exception as e:
            return {
                "success": False,
                "duration": (time.time() - start_time) * 1000,
                "error": str(e)
            }
    
    def run_concurrent_test(self, test_func, concurrent_users=10, requests_per_user=10):
        """并发测试"""
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = []
            for _ in range(concurrent_users * requests_per_user):
                futures.append(executor.submit(test_func))
            
            results = []
            for future in futures:
                results.append(future.result())
        
        # 分析结果
        successful = [r for r in results if r["success"]]
        failed = [r for r in results if not r["success"]]
        durations = [r["duration"] for r in successful]
        
        return {
            "total_requests": len(results),
            "successful": len(successful),
            "failed": len(failed),
            "success_rate": len(successful) / len(results) * 100,
            "avg_duration": statistics.mean(durations) if durations else 0,
            "p95_duration": statistics.quantiles(durations, n=20)[18] if len(durations) >= 20 else 0,
            "max_duration": max(durations) if durations else 0
        }
    
    def generate_report(self):
        """生成性能测试报告"""
        print("=" * 60)
        print("价格策略模块性能测试报告")
        print("=" * 60)
        
        # 测试查询性能
        print("\n1. 价格查询性能测试")
        query_results = self.run_concurrent_test(
            lambda: self.test_query(),
            concurrent_users=5,
            requests_per_user=20
        )
        print(f"  总请求数: {query_results['total_requests']}")
        print(f"  成功率: {query_results['success_rate']:.2f}%")
        print(f"  平均响应时间: {query_results['avg_duration']:.2f}ms")
        print(f"  P95响应时间: {query_results['p95_duration']:.2f}ms")
        
        # 测试计算性能
        print("\n2. 价格计算性能测试")
        calc_results = self.run_concurrent_test(
            lambda: self.test_calculation(),
            concurrent_users=3,
            requests_per_user=10
        )
        print(f"  总请求数: {calc_results['total_requests']}")
        print(f"  成功率: {calc_results['success_rate']:.2f}%")
        print(f"  平均响应时间: {calc_results['avg_duration']:.2f}ms")
        print(f"  P95响应时间: {calc_results['p95_duration']:.2f}ms")
        
        # 保存结果
        report = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "query_performance": query_results,
            "calculation_performance": calc_results
        }
        
        with open("performance_report.json", "w") as f:
            json.dump(report, f, indent=2)
        
        print(f"\n报告已保存到: performance_report.json")

if __name__ == "__main__":
    test = PricingPerformanceTest()
    test.generate_report()
```

## 6. 运维手册

### 6.1 日常检查清单
```bash
# 1. 检查服务状态
./health_check.sh

# 2. 检查监控数据
curl "http://localhost:9090/api/v1/query?query=up"

# 3. 检查告警状态
curl "http://localhost:9093/api/v2/alerts"

# 4. 查看日志
docker-compose logs --tail=100 pricing-service
```

### 6.2 故障排查指南

#### 6.2.1 服务不可用
1. 检查服务健康状态
2. 查看应用日志
3. 检查依赖服务（数据库、缓存）
4. 检查网络连接

#### 6.2.2 性能下降
1. 查看监控图表，定位问题时间点
2. 分析资源使用情况（CPU、内存、磁盘IO）
3. 检查慢查询日志
4. 分析应用线程状态

#### 6.2.3 监控数据缺失
1. 检查Prometheus目标状态
2. 验证应用指标端点
3. 检查网络连通性
4. 查看Prometheus日志

### 6.3 维护计划
| 任务 | 频率 | 负责人 | 预计时长 |
|-----|-----|-------|---------|
| 监控系统健康检查 | 每天 | 运维工程师 | 15分钟 |
| 告警规则评审 | 每周 | 开发团队 | 1小时 |
| 性能报告分析 | 每周 | 性能工程师 | 2小时 |
| 监控配置备份 | 每月 | 运维工程师 | 30分钟 |
| 系统升级 | 每季度 | 运维团队 | 4小时 |

## 7. 性能优化检查清单

### 7.1 数据库优化
- [ ] 为高频查询字段建立索引
- [ ] 定期分析并优化慢查询
- [ ] 配置合理的连接池参数
- [ ] 实现读写分离

### 7.2 缓存优化
- [ ] 配置多级缓存策略
- [ ] 设置合理的缓存过期时间
- [ ] 实现缓存预热机制
- [ ] 监控缓存命中率

### 7.3 代码优化
- [ ] 使用异步处理耗时操作
- [ ] 实现批量操作减少IO次数
- [ ] 优化算法复杂度
- [ ] 减少不必要的对象创建

### 7.4 JVM优化
- [ ] 配置合适的堆内存大小
- [ ] 选择合适的GC算法
- [ ] 监控GC频率和耗时
- [ ] 优化JVM参数

## 8. 附录

### 8.1 常用命令
```bash
# 重启监控服务
docker-compose restart prometheus grafana alertmanager

# 查看监控配置
docker exec -it prometheus cat /etc/prometheus/prometheus.yml

# 导入新的告警规则
docker cp alert_rules.yml prometheus:/etc/prometheus/

# 备份监控数据
docker exec prometheus tar -czf /tmp/prometheus-backup.tar.gz /prometheus/data/
```

### 8.2 故障恢复流程
1. **识别问题**: 通过告警和监控数据确认问题
2. **分析原因**: 查看日志，分析监控图表
3. **制定方案**: 根据问题原因制定恢复方案
4. **执行恢复**: 执行恢复操作
5. **验证效果**: 验证服务是否恢复正常
6. **记录总结**: 记录故障原因和恢复过程

### 8.3 联系方式
- **技术支持**: tech-support@example.com
- **监控团队**: monitoring-team@example.com
- **紧急联系人**: +86-138-XXXX-XXXX

---

**文档版本**: v1.0  
**创建日期**: 2026年5月1日  
**最后更新**: 2026年5月1日  
**维护团队**: 运维团队 & 开发团队