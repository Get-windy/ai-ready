# 【Sprint 27+1】性能测试工具设计

**文档编号**: PT-TL-20260427-001
**创建日期**: 2026-04-27
**任务ID**: task_1777242701394_5zhja559u

---

## 1. 概述

本文档定义了Sprint 27+1性能基准测试的所有测试工具，包括JMeter脚本设计、Gatling脚本设计、K6脚本设计、监控仪表盘设计以及工具使用指南。

---

## 2. JMeter性能测试脚本设计

### 2.1 JMeter测试脚本结构

```
qa/performance/benchmark/scripts/jmeter/
├── api/
│   ├── user-management/
│   │   ├── user-login.jmx
│   │   ├── user-query.jmx
│   │   ├── user-operations.jmx
│   │   └── user-batch.jmx
│   ├── order-management/
│   │   ├── order-create.jmx
│   │   ├── order-query.jmx
│   │   ├── order-update.jmx
│   │   └── order-batch.jmx
│   ├── stock-management/
│   │   ├── stock-query.jmx
│   │   ├── stock-update.jmx
│   │   ├── stock-lock.jmx
│   │   └── stock-batch.jmx
│   └── api-gateway/
│       ├── gateway-routing.jmx
│       ├── gateway-auth.jmx
│       ├── gateway-rate-limit.jmx
│       └── gateway-mixed.jmx
├── database/
│   ├── user-db-queries.jmx
│   ├── order-db-queries.jmx
│   ├── stock-db-queries.jmx
│   └── mixed-db-queries.jmx
└── common/
    ├── test-data-generator.jmx
    ├── result-analyzer.jmx
    └── report-generator.jmx
```

### 2.2 核心JMeter脚本设计

#### 用户登录性能测试脚本 (user-login.jmx)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="用户登录性能测试">
      <elementProp name="TestPlan.user_defined_variables">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementPropType="HTTPSamplerProxy">
            <stringProp name="HTTPSampler.domain">test-ai-ready.example.com</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.protocol">http</stringProp>
          </elementProp>
          <elementProp name="TEST_USERNAME" elementPropType="HTTPSamplerProxy">
            <stringProp name="Argument.value">admin</stringProp>
          </elementProp>
          <elementProp name="TEST_PASSWORD" elementPropType="HTTPSamplerProxy">
            <stringProp name="Argument.value">[REDACTED]</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="登录线程组">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <CookieManager guiclass="CookiePanel" testclass="CookieManager" testname="HTTP Cookie Manager"/>
        <hashTree/>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="用户登录请求">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <elementProp name="HTTPsampler.Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="username" elementPropType="HTTPArgument">
                <stringProp name="Argument.value">${TEST_USERNAME}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
              <elementProp name="password" elementPropType="HTTPArgument">
                <stringProp name="Argument.value">${TEST_PASSWORD}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="响应断言">
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">\"code\":200</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Response Data</stringProp>
            <boolProp name="Assertion.assume_success">false</boolProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

#### JMeter测试配置脚本 (config.properties)

```properties
# JMeter测试配置
jmeter.test.duration=300
jmeter.thread.ramp.up=10
jmeter.result.save.thread=false
jmeter.result.save.subtree=false
jmeter.result.save.assertion=false
jmeter.result.save.label=true
jmeter.result.save.response.data=false
jmeter.result.save.sampler=false

# 测试环境配置
api.base.url=http://test-ai-ready.example.com:8080
api.auth.login.path=/api/auth/login
api.user.query.path=/api/users
api.order.create.path=/api/orders
api.stock.query.path=/api/stock

# 测试数据配置
test.user.count=10000
test.order.count=5000
test.stock.sku.count=10000

# 性能指标配置
performance.threshold.p95=500
performance.threshold.p99=1000
performance.threshold.error=1
performance.threshold.availability=99.9
```

### 2.3 JMeter测试执行脚本

#### Windows批处理脚本 (run-jmeter-tests.bat)

```batch
@echo off
REM 性能测试执行脚本
SETLOCAL

REM 设置环境变量
SET JMETER_HOME=C:\apache-jmeter-5.6.3
SET TEST_DIR=I:\AI-Ready\qa\performance\benchmark\scripts\jmeter
SET RESULT_DIR=I:\AI-Ready\qa\test-reports\jmeter-results
SET DATE=%DATE:~0,4%-%DATE:~5,2%-%DATE:~8,2%

REM 创建结果目录
if not exist "%RESULT_DIR%" mkdir "%RESULT_DIR%"
if not exist "%RESULT_DIR%\%DATE%" mkdir "%RESULT_DIR%\%DATE%"

REM 1. 执行用户管理测试
echo [%TIME%] 开始执行用户管理性能测试...
"%JMETER_HOME%\bin\jmeter.bat" -n -t "%TEST_DIR%\api\user-management\user-login.jmx" -l "%RESULT_DIR%\%DATE%\user-login-%TIME:~0,2%%TIME:~3,2%.jtl" -e -o "%RESULT_DIR%\%DATE%\user-login-report"

REM 2. 执行订单管理测试
echo [%TIME%] 开始执行订单管理性能测试...
"%JMETER_HOME%\bin\jmeter.bat" -n -t "%TEST_DIR%\api\order-management\order-create.jmx" -l "%RESULT_DIR%\%DATE%\order-create-%TIME:~0,2%%TIME:~3,2%.jtl" -e -o "%RESULT_DIR%\%DATE%\order-create-report"

REM 3. 执行库存管理测试
echo [%TIME%] 开始执行库存管理性能测试...
"%JMETER_HOME%\bin\jmeter.bat" -n -t "%TEST_DIR%\api\stock-management\stock-query.jmx" -l "%RESULT_DIR%\%DATE%\stock-query-%TIME:~0,2%%TIME:~3,2%.jtl" -e -o "%RESULT_DIR%\%DATE%\stock-query-report"

REM 4. 执行API网关测试
echo [%TIME%] 开始执行API网关性能测试...
"%JMETER_HOME%\bin\jmeter.bat" -n -t "%TEST_DIR%\api\api-gateway\gateway-routing.jmx" -l "%RESULT_DIR%\%DATE%\gateway-routing-%TIME:~0,2%%TIME:~3,2%.jtl" -e -o "%RESULT_DIR%\%DATE%\gateway-routing-report"

REM 5. 生成综合报告
echo [%TIME%] 生成综合性能报告...
"%JMETER_HOME%\bin\jmeter.bat" -g "%RESULT_DIR%\%DATE%\*.jtl" -o "%RESULT_DIR%\%DATE%\combined-report"

echo [%TIME%] 所有性能测试完成！
pause
ENDLOCAL
```

---

## 3. Gatling性能测试脚本设计

### 3.1 Gatling测试脚本结构

```
qa/performance/benchmark/scripts/gatling/
├── src/
│   └── test/
│       └── scala/
│           └── com/
│               └── aiedge/
│                   └── performance/
│                       ├── UserManagementSimulation.scala
│                       ├── OrderManagementSimulation.scala
│                       ├── StockManagementSimulation.scala
│                       ├── ApiGatewaySimulation.scala
│                       ├── DatabaseSimulation.scala
│                       └── MixedWorkloadSimulation.scala
├── resources/
│   ├── data/
│   │   ├── users.csv
│   │   ├── orders.csv
│   │   └── stock.csv
│   └── conf/
│       ├── application.conf
│       └── gatling.conf
└── bin/
    ├── run-all-simulations.sh
    └── analyze-results.sh
```

### 3.2 核心Gatling脚本设计

#### 用户管理性能测试脚本 (UserManagementSimulation.scala)

```scala
package com.aiedge.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UserManagementSimulation extends Simulation {
  
  // HTTP配置
  val httpProtocol = http
    .baseUrl("http://test-ai-ready.example.com:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling/3.9.5")
  
  // 用户认证场景
  val userLoginScenario = scenario("用户登录场景")
    .exec(
      http("用户登录")
        .post("/api/auth/login")
        .body(StringBody(
          """{
            |  "username": "admin",
            |  "password": "[REDACTED]"
            |}""".stripMargin))
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
  
  // 用户查询场景
  val userQueryScenario = scenario("用户查询场景")
    .exec(
      http("查询用户列表")
        .get("/api/users")
        .queryParam("page", "1")
        .queryParam("size", "10")
        .check(status.is(200))
    )
    .pause(2.seconds)
  
  // 用户创建场景
  val userCreateScenario = scenario("用户创建场景")
    .exec(
      http("创建用户")
        .post("/api/users")
        .body(StringBody(
          """{
            |  "username": "testuser_#{randomInt(10000)}",
            |  "password": "Test@123456",
            |  "realName": "测试用户",
            |  "email": "test#{randomInt(10000)}@example.com",
            |  "phone": "138#{randomInt(10000000, 99999999)}",
            |  "status": 1,
            |  "gender": 1
            |}""".stripMargin))
        .check(status.is(200))
    )
    .pause(3.seconds)
  
  // 设置负载
  setUp(
    userLoginScenario.inject(
      rampUsers(100) during (10.seconds)
    ),
    userQueryScenario.inject(
      constantUsersPerSec(10) during (30.seconds)
    ),
    userCreateScenario.inject(
      rampUsers(20) during (10.seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(5.minutes)
}
```

#### Gatling配置 (gatling.conf)

```hocon
gatling {
  core {
    simulationClass = "com.aiedge.performance.UserManagementSimulation"
    runDescription = "用户管理服务性能测试"
    outputDirectoryBaseName = "user-management-performance"
    
    muteMode = false
    muteAudio = true
    muteVideo = true
  }
  
  http {
    fetchHtmlResources = false
    perUserCacheMaxSize = 0
    warmUpUrl = "http://test-ai-ready.example.com:8080/health"
    
    requestTimeout = 30000
    handshakeTimeout = 10000
    enableGA = false
  }
  
  data {
    writers = ["console", "file", "graphite"]
    console {
      light = false
    }
    file {
      bufferSize = 8192
    }
  }
  
  charting {
    noReports = false
    maxPlotPerSeries = 1000
    useGroupDurationMetric = false
  }
}
```

---

## 4. K6性能测试脚本设计

### 4.1 K6测试脚本结构

```
qa/performance/benchmark/scripts/k6/
├── scripts/
│   ├── api/
│   │   ├── user-management.js
│   │   ├── order-management.js
│   │   ├── stock-management.js
│   │   └── api-gateway.js
│   ├── database/
│   │   ├── user-db-test.js
│   │   ├── order-db-test.js
│   │   └── stock-db-test.js
│   ├── cache/
│   │   ├── redis-test.js
│   │   └── cache-performance.js
│   └── mixed/
│       ├── real-user-scenario.js
│       └── peak-traffic-scenario.js
├── data/
│   ├── users.json
│   ├── orders.json
│   └── stock.json
├── config/
│   ├── test-config.json
│   ├── environment.json
│   └── thresholds.json
└── bin/
    ├── run-k6-tests.sh
    └── analyze-k6-results.py
```

### 4.2 核心K6脚本设计

#### API性能测试脚本 (user-management.js)

```javascript
// k6性能测试脚本 - 用户管理服务
import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 测试配置
export const options = {
  stages: [
    { duration: '30s', target: 50 },  // 热身阶段
    { duration: '1m', target: 100 },  // 负载阶段
    { duration: '30s', target: 150 }, // 压力阶段
    { duration: '30s', target: 100 }, // 恢复阶段
    { duration: '30s', target: 0 },   // 清理阶段
  ],
  thresholds: {
    'http_req_duration': ['p(95) < 500', 'p(99) < 1000'],
    'http_req_failed': ['rate<0.01'],
    'http_reqs': ['rate>200'],
  },
  ext: {
    loadimpact: {
      name: '用户管理服务性能测试',
      projectID: 12345,
    },
  },
};

// 测试数据
const testData = {
  users: JSON.parse(open('./data/users.json')),
  baseUrl: 'http://test-ai-ready.example.com:8080',
};

// 测试场景
export default function () {
  const user = testData.users[Math.floor(Math.random() * testData.users.length)];
  
  // 场景1: 用户登录
  const loginResponse = http.post(`${testData.baseUrl}/api/auth/login`, JSON.stringify({
    username: user.username,
    password: user.password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(loginResponse, {
    '登录成功': (r) => r.status === 200,
    '登录响应时间': (r) => r.timings.duration < 500,
  });
  
  const authToken = loginResponse.json('data.token');
  
  // 场景2: 用户查询
  const queryResponse = http.get(`${testData.baseUrl}/api/users/${user.id}`, {
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json',
    },
  });
  
  check(queryResponse, {
    '查询成功': (r) => r.status === 200,
    '查询响应时间': (r) => r.timings.duration < 300,
  });
  
  // 场景3: 用户信息更新
  const updateResponse = http.put(`${testData.baseUrl}/api/users/${user.id}`, JSON.stringify({
    realName: `更新用户${Math.floor(Math.random() * 1000)}`,
    email: `updated${Math.floor(Math.random() * 1000)}@example.com`,
  }), {
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json',
    },
  });
  
  check(updateResponse, {
    '更新成功': (r) => r.status === 200,
    '更新响应时间': (r) => r.timings.duration < 400,
  });
  
  // 随机延迟，模拟用户思考时间
  sleep(Math.random() * 2);
}

// 生成HTML报告
export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data),
    'stdout': JSON.stringify(data, null, 2),
  };
}
```

#### K6测试配置 (test-config.json)

```json
{
  "test": {
    "name": "Sprint 27+1性能基准测试",
    "environment": "test",
    "version": "1.0.0"
  },
  "api": {
    "baseUrl": "http://test-ai-ready.example.com:8080",
    "endpoints": {
      "auth": "/api/auth",
      "users": "/api/users",
      "orders": "/api/orders",
      "stock": "/api/stock"
    }
  },
  "load": {
    "stages": [
      { "duration": "30s", "target": 50 },
      { "duration": "1m", "target": 100 },
      { "duration": "30s", "target": 150 },
      { "duration": "30s", "target": 100 },
      { "duration": "30s", "target": 0 }
    ],
    "maxVUs": 200,
    "iterations": 10000
  },
  "thresholds": {
    "responseTime": {
      "p95": 500,
      "p99": 1000
    },
    "errorRate": 0.01,
    "qps": 200
  },
  "monitoring": {
    "prometheus": "http://localhost:9090",
    "influxdb": "http://localhost:8086",
    "grafana": "http://localhost:3000"
  }
}
```

---

## 5. 性能监控仪表盘设计

### 5.1 Grafana监控仪表盘配置

#### API性能监控仪表盘配置

```json
{
  "dashboard": {
    "id": null,
    "title": "API性能监控仪表盘",
    "tags": ["performance", "api", "sprint27+1"],
    "style": "dark",
    "timezone": "browser",
    "panels": [
      {
        "title": "API响应时间",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[1m]))",
            "legendFormat": "P95响应时间",
            "refId": "A"
          },
          {
            "expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[1m]))",
            "legendFormat": "P99响应时间",
            "refId": "B"
          }
        ],
        "yaxes": [
          { "format": "s", "label": "响应时间(秒)" }
        ],
        "thresholds": [
          { "value": 0.5, "color": "green" },
          { "value": 1.0, "color": "yellow" },
          { "value": 2.0, "color": "red" }
        ]
      },
      {
        "title": "API QPS",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[1m])",
            "legendFormat": "QPS",
            "refId": "A"
          }
        ],
        "yaxes": [
          { "format": "ops", "label": "请求/秒" }
        ],
        "thresholds": [
          { "value": 100, "color": "red" },
          { "value": 200, "color": "yellow" },
          { "value": 500, "color": "green" }
        ]
      },
      {
        "title": "HTTP错误率",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[1m])) / sum(rate(http_requests_total[1m]))",
            "legendFormat": "5xx错误率",
            "refId": "A"
          },
          {
            "expr": "sum(rate(http_requests_total{status=~\"4..\"}[1m])) / sum(rate(http_requests_total[1m]))",
            "legendFormat": "4xx错误率",
            "refId": "B"
          }
        ],
        "yaxes": [
          { "format": "percentunit", "label": "错误率" }
        ],
        "thresholds": [
          { "value": 0.01, "color": "green" },
          { "value": 0.05, "color": "yellow" },
          { "value": 0.1, "color": "red" }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
```

#### 系统资源监控仪表盘配置

```json
{
  "dashboard": {
    "title": "系统资源监控仪表盘",
    "panels": [
      {
        "title": "CPU使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "CPU使用率",
            "refId": "A"
          }
        ],
        "yaxes": [
          { "format": "percent", "label": "使用率%" }
        ],
        "thresholds": [
          { "value": 70, "color": "green" },
          { "value": 85, "color": "yellow" },
          { "value": 90, "color": "red" }
        ]
      },
      {
        "title": "内存使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100",
            "legendFormat": "内存使用率",
            "refId": "A"
          }
        ],
        "yaxes": [
          { "format": "percent", "label": "使用率%" }
        ],
        "thresholds": [
          { "value": 80, "color": "green" },
          { "value": 90, "color": "yellow" },
          { "value": 95, "color": "red" }
        ]
      },
      {
        "title": "磁盘使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100",
            "legendFormat": "磁盘使用率",
            "refId": "A"
          }
        ],
        "yaxes": [
          { "format": "percent", "label": "使用率%" }
        ],
        "thresholds": [
          { "value": 70, "color": "green" },
          { "value": 85, "color": "yellow" },
          { "value": 90, "color": "red" }
        ]
      },
      {
        "title": "网络带宽使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(node_network_receive_bytes_total[5m]) / 1024 / 1024",
            "legendFormat": "接收带宽(MB/s)",
            "refId": "A"
          },
          {
            "expr": "rate(node_network_transmit_bytes_total[5m]) / 1024 / 1024",
            "legendFormat": "发送带宽(MB/s)",
            "refId": "B"
          }
        ],
        "yaxes": [
          { "format": "MBps", "label": "带宽(MB/s)" }
        ],
        "thresholds": [
          { "value": 100, "color": "green" },
          { "value": 200, "color": "yellow" },
          { "value": 300, "color": "red" }
        ]
      }
    ]
  }
}
```

### 5.2 Prometheus监控配置

#### Prometheus规则配置 (prometheus-rules.yml)

```yaml
groups:
  - name: api_performance_rules
    rules:
      - alert: APIResponseTimeTooHigh
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应时间过高"
          description: "API P95响应时间超过500ms (当前值: {{ $value }}s)"
      
      - alert: APIErrorRateTooHigh
        expr: sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "API错误率过高"
          description: "API 5xx错误率超过1% (当前值: {{ $value }})"
      
      - alert: APIQPSTooLow
        expr: rate(http_requests_total[5m]) < 100
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "API QPS过低"
          description: "API QPS低于100 (当前值: {{ $value }})"
  
  - name: system_resource_rules
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "CPU使用率超过85% (当前值: {{ $value }}%)"
      
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率过高"
          description: "内存使用率超过90% (当前值: {{ $value }}%)"
      
      - alert: HighDiskUsage
        expr: (node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100 > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘使用率过高"
          description: "磁盘使用率超过85% (当前值: {{ $value }}%)"
```

---

## 6. 测试工具集成与自动化

### 6.1 自动化测试执行脚本

#### 综合测试执行脚本 (run-all-performance-tests.sh)

```bash
#!/bin/bash
# 性能测试综合执行脚本

# 设置环境变量
export TEST_ENV="sprint27+1"
export TEST_DATE=$(date +%Y%m%d_%H%M%S)
export RESULTS_DIR="I:/AI-Ready/qa/test-reports/performance/$TEST_DATE"
export LOGS_DIR="$RESULTS_DIR/logs"

# 创建目录
mkdir -p "$RESULTS_DIR"
mkdir -p "$LOGS_DIR"

# 记录开始时间
echo "=== 性能测试开始时间: $(date) ===" > "$RESULTS_DIR/test-summary.txt"

# 1. 执行JMeter测试
echo "1. 执行JMeter性能测试..."
"$JMETER_HOME/bin/jmeter" -n -t "I:/AI-Ready/qa/performance/benchmark/scripts/jmeter/api/user-management/user-login.jmx" \
  -l "$RESULTS_DIR/jmeter-user-login.jtl" \
  -e -o "$RESULTS_DIR/jmeter-report" \
  >> "$LOGS_DIR/jmeter.log" 2>&1

# 2. 执行K6测试
echo "2. 执行K6性能测试..."
k6 run "I:/AI-Ready/qa/performance/benchmark/scripts/k6/scripts/api/user-management.js" \
  --out influxdb=http://localhost:8086/k6 \
  --out json="$RESULTS_DIR/k6-results.json" \
  >> "$LOGS_DIR/k6.log" 2>&1

# 3. 启动监控
echo "3. 启动性能监控..."
prometheus --config.file="I:/AI-Ready/qa/performance/config/prometheus.yml" \
  >> "$LOGS_DIR/prometheus.log" 2>&1 &
PROMETHEUS_PID=$!

# 4. 等待测试完成
sleep 300  # 等待5分钟测试完成

# 5. 停止监控并收集数据
echo "4. 收集性能数据..."
kill $PROMETHEUS_PID

# 6. 生成综合报告
echo "5. 生成综合性能报告..."
python "I:/AI-Ready/qa/performance/scripts/analyze-results.py" \
  --jmeter "$RESULTS_DIR/jmeter-report" \
  --k6 "$RESULTS_DIR/k6-results.json" \
  --prometheus "http://localhost:9090" \
  --output "$RESULTS_DIR/performance-report.md"

# 记录结束时间
echo "=== 性能测试结束时间: $(date) ===" >> "$RESULTS_DIR/test-summary.txt"

# 计算执行时间
START_TIME=$(date -d "$(head -1 test-summary.txt | cut -d: -f2-)" +%s)
END_TIME=$(date -d "$(tail -1 test-summary.txt | cut -d: -f2-)" +%s)
ELAPSED_TIME=$((END_TIME - START_TIME))

echo "=== 总执行时间: $ELAPSED_TIME 秒 ===" >> "$RESULTS_DIR/test-summary.txt"
echo "=== 测试结果保存在: $RESULTS_DIR ===" >> "$RESULTS_DIR/test-summary.txt"

echo ""
echo "性能测试执行完成！"
echo "测试结果目录: $RESULTS_DIR"
echo "日志目录: $LOGS_DIR"
```

---

## 7. 工具使用指南

### 7.1 JMeter使用指南

#### 安装步骤
1. 下载JMeter 5.6.3或更高版本
2. 解压到C:\apache-jmeter-5.6.3
3. 设置环境变量 JMETER_HOME
4. 运行jmeter.bat启动GUI界面

#### 执行命令
```bash
# 非GUI模式执行测试
jmeter -n -t test-plan.jmx -l results.jtl -e -o report

# 从properties文件加载配置
jmeter -n -t test-plan.jmx -p test.properties -l results.jtl

# 分布式测试
jmeter -n -t test-plan.jmx -r -l results.jtl
```

### 7.2 Gatling使用指南

#### 安装步骤
1. 下载Gatling Bundle
2. 解压到C:\gatling
3. 设置环境变量 GATLING_HOME
4. 运行bin\gatling.bat

#### 执行命令
```bash
# 执行测试
gatling.bat -s com.aiedge.performance.UserManagementSimulation

# 指定配置文件
gatling.bat -rf results -sf simulations -rsf resources

# 生成报告
gatling.bat -ro results/performance-test-20260427-120000
```

### 7.3 K6使用指南

#### 安装步骤
1. 下载K6安装包
2. 运行安装程序
3. 验证安装: k6 version

#### 执行命令
```bash
# 执行测试
k6 run script.js

# 指定VU和持续时间
k6 run --vus 100 --duration 5m script.js

# 输出到InfluxDB
k6 run --out influxdb=http://localhost:8086/k6 script.js

# 生成报告
k6 run --out json=results.json --out html=report.html script.js
```

---

## 8. 附录

### 附录A: 工具版本要求

| 工具名称 | 最低版本 | 推荐版本 | 下载地址 |
|---------|---------|---------|---------|
| **JMeter** | 5.0 | 5.6.3 | https://jmeter.apache.org |
| **Gatling** | 3.0 | 3.9.5 | https://gatling.io |
| **K6** | v0.40.0 | v0.45.0 | https://k6.io |
| **Prometheus** | 2.0 | 2.47.0 | https://prometheus.io |
| **Grafana** | 8.0 | 10.1.0 | https://grafana.com |
| **Node Exporter** | 1.0 | 1.6.1 | https://github.com/prometheus/node_exporter |

### 附录B: 常见问题解决

1. **JMeter内存不足**: 修改bin/jmeter.bat中的HEAP设置
2. **Gatling编译错误**: 检查Scala版本兼容性
3. **K6连接超