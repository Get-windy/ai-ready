# 测试环境监控栈配置

## 概述

监控栈使用 **Prometheus + Grafana** 组合，提供测试环境的实时监控和可视化。

## 组件

### Prometheus
- **端口**: 9091
- **功能**: 指标收集和存储
- **配置**: prometheus.yml

### Grafana
- **端口**: 3001
- **功能**: 可视化大盘
- **默认账号**: admin / admin123

## 访问地址

- Prometheus UI: http://localhost:9091
- Grafana UI: http://localhost:3001

## 监控指标

### 容器指标
- 容器状态 (up/down)
- CPU使用率
- 内存使用
- 网络IO
- 磁盘使用

### 数据库指标
- PostgreSQL连接数
- Redis内存使用
- 查询性能

### 测试执行指标
- 测试通过率
- 测试执行时间
- 失败率趋势

## 告警规则

### 容器停机告警
- 条件: 容器状态为down超过1分钟
- 级别: critical

### 资源使用告警
- 内存使用率 > 85%
- CPU使用率 > 80%
- 磁盘使用率 > 90%

### 测试质量告警
- 测试失败率 > 20%
- 测试执行时间 > 30秒 (95th percentile)

## 使用方法

### 启动监控栈

```bash
# 启动所有服务（包含监控）
./docker-manage.sh start

# 单独启动监控
 docker-compose -f docker-compose.test-env.yml up -d prometheus grafana
```

### 查看监控

1. 访问Grafana: http://localhost:3001
2. 登录: admin / admin123
3. 查看预配置的测试环境监控大盘

### 配置告警

1. 修改 alert_rules.yml 添加自定义规则
2. 重启Prometheus加载新配置

```bash
# 热重载配置
curl -X POST http://localhost:9091/-/reload
```

## 自定义监控

### 添加新指标

1. 在应用中暴露/metrics端点
2. 在prometheus.yml中添加抓取配置

```yaml
scrape_configs:
  - job_name: 'my-service'
    static_configs:
      - targets: ['my-service:8080']
```

### 添加新图表

1. 在Grafana中创建新图表
2. 导出JSON配置
3. 保存到grafana-dashboard.json

## 故障排除

### Prometheus无法启动

```bash
# 检查配置语法
promtool check config prometheus.yml

# 查看日志
docker logs ai-ready-prometheus
```

### Grafana无法连接Prometheus

1. 检查数据源配置
2. 确认网络连通性
3. 查看Grafana日志

```bash
docker logs ai-ready-grafana
```

## 参考文档

- [Prometheus文档](https://prometheus.io/docs/)
- [Grafana文档](https://grafana.com/docs/)
