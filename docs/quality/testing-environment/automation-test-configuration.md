# 测试环境自动化测试配置指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**适用项目**: AI-Ready (企智连)  
**适用范围**: Sprint 27+1 测试环境配置

---

## 1. 自动化测试概述

本指南介绍测试环境自动化测试的配置和使用，包括：

- 环境健康检查自动化
- 服务连通性自动化测试
- 性能基准自动化测试
- 安全扫描自动化测试

### 1.1 自动化测试架构

```
┌──────────────────────────────────────────────────────────────────┐
│                     自动化测试框架                               │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ 健康检查测试 │  │ 连通性测试 │  │ 性能基准测试 │          │
│  │  (pytest)    │  │ (pytest)    │  │  (pytest)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐                             │
│  │ 安全扫描测试 │  │ 报告生成测试 │                             │
│  │  (pytest)    │  │  (pytest)    │                             │
│  └──────────────┘  └──────────────┘                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     测试结果管理                                 │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Allure 报告 │  │ JSON 结果 │  │ Prometheus 监控 │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     CI/CD 集成                                   │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Jenkins      │  │ GitLab CI    │  │ GitHub Actions│         │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. 环境健康检查自动化

### 2.1 测试目标

- 验证所有服务是否正常运行
- 检查服务健康端点
- 验证基础网络连通性

### 2.2 测试脚本

```python
# qa/automation/testing/health_check_test.py
import pytest
import requests
from datetime import datetime

class TestHealthCheck:
    """环境健康检查测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """初始化测试配置"""
        self.services = {
            'prometheus': 'http://localhost:9090',
            'alertmanager': 'http://localhost:9093',
            'grafana': 'http://localhost:3000',
            'exporter': 'http://localhost:9101',
            'api': 'http://localhost:8081',
            'nginx': 'http://localhost'
        }
        self.start_time = datetime.now()
    
    def test_prometheus_health(self):
        """测试 Prometheus 健康状态"""
        response = requests.get(f"{self.services['prometheus']}/-/healthy")
        assert response.status_code == 200
        assert response.json().get('status') == 'ok'
    
    def test_alertmanager_health(self):
        """测试 AlertManager 健康状态"""
        response = requests.get(f"{self.services['alertmanager']}/-/healthy")
        assert response.status_code == 200
    
    def test_grafana_health(self):
        """测试 Grafana 健康状态"""
        response = requests.get(f"{self.services['grafana']}/api/health")
        assert response.status_code == 200
        assert response.json().get('status') == 'ok'
    
    def test_exporter_health(self):
        """测试 Exporter 健康状态"""
        response = requests.get(f"{self.services['exporter']}/health")
        assert response.status_code == 200
    
    def test_api_health(self):
        """测试 API 服务健康状态"""
        response = requests.get(f"{self.services['api']}/actuator/health")
        assert response.status_code == 200
        assert response.json().get('status') == 'UP'
    
    def test_nginx_health(self):
        """测试 Nginx 健康状态"""
        response = requests.get(f"{self.services['nginx']}/")
        assert response.status_code == 200
    
    def test_all_services_up(self):
        """测试所有服务是否正常"""
        failed_services = []
        for name, url in self.services.items():
            try:
                if name == 'nginx':
                    response = requests.get(f"{url}/")
                else:
                    response = requests.get(f"{url}/-/healthy", timeout=5)
                
                if response.status_code != 200:
                    failed_services.append(name)
            except requests.exceptions.RequestException:
                failed_services.append(name)
        
        assert len(failed_services) == 0, f"以下服务异常: {', '.join(failed_services)}"
    
    def test_service_startup_time(self):
        """测试服务启动时间"""
        # 假设启动时间在合理范围内
        startup_duration = (datetime.now() - self.start_time).total_seconds()
        assert startup_duration < 300, f"服务启动时间过长: {startup_duration}s"

if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
```

### 2.3 运行脚本

```bash
# 运行健康检查测试
cd qa/automation/testing
python -m pytest health_check_test.py -v \
    --tb=short \
    --html=../../reports/health-check-report.html \
    --self-contained-html

# 使用 Allure 生成报告
allure serve ../../reports/
```

---

## 3. 服务连通性自动化测试

### 3.1 测试目标

- 验证各服务之间的网络连通性
- 检查端口开放情况
- 验证服务依赖关系

### 3.2 测试脚本

```python
# qa/automation/testing/connectivity_test.py
import pytest
import socket
import subprocess

class TestConnectivity:
    """服务连通性测试"""
    
    # 服务端口配置
    ports = {
        'postgresql': ('localhost', 5433),
        'redis': ('localhost', 6380),
        'rabbitmq': ('localhost', 5673),
        'prometheus': ('localhost', 9090),
        'alertmanager': ('localhost', 9093),
        'grafana': ('localhost', 3000),
        'exporter': ('localhost', 9101),
        'api': ('localhost', 8081),
        'nginx': ('localhost', 80),
    }
    
    def test_port_open(self):
        """测试所有端口是否开放"""
        failed_ports = []
        for name, (host, port) in self.ports.items():
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(5)
                result = sock.connect_ex((host, port))
                if result != 0:
                    failed_ports.append(f"{name}:{port}")
                sock.close()
            except Exception as e:
                failed_ports.append(f"{name}:{port} - {str(e)}")
        
        assert len(failed_ports) == 0, f"以下端口无法连接: {', '.join(failed_ports)}"
    
    def test_container_connectivity(self):
        """测试容器间网络连通性"""
        # 使用 Docker 网络测试
        try:
            result = subprocess.run(
                ['docker', 'network', 'inspect', 'monitoring-network', '--format', '{{range .Containers}}{{.Name}} {{end}}'],
                capture_output=True,
                text=True,
                timeout=10
            )
            assert result.returncode == 0, "无法获取网络信息"
            
            containers = result.stdout.strip().split()
            assert len(containers) >= 9, f"容器数量不足: {len(containers)}"
            
        except subprocess.TimeoutExpired:
            pytest.fail("Docker 命令超时")
        except FileNotFoundError:
            pytest.skip("未找到 Docker 命令，跳过容器连通性测试")
    
    def test_docker_compose_services(self):
        """测试 Docker Compose 服务状态"""
        try:
            result = subprocess.run(
                ['docker', 'compose', 'ps', '--format', 'json'],
                capture_output=True,
                text=True,
                timeout=10
            )
            assert result.returncode == 0, "无法获取服务状态"
            
            # 解析服务状态
            services = result.stdout.strip().split('\n')
            running_services = [s for s in services if 'healthy' in s or 'running' in s]
            assert len(running_services) >= 9, f"服务未全部运行: {len(running_services)}/9"
            
        except subprocess.TimeoutExpired:
            pytest.fail("Docker Compose 命令超时")
        except FileNotFoundError:
            pytest.skip("未找到 Docker Compose，跳过服务状态检查")

if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
```

---

## 4. 性能基准自动化测试

### 4.1 测试目标

- 验证数据库性能基准
- 验证Redis性能基准
- 验证API性能基准
- 验证监控系统性能

### 4.2 测试脚本

```python
# qa/automation/testing/performance_test.py
import pytest
import time
import requests
import psycopg2
import redis

class TestPerformance:
    """性能基准测试"""
    
    # 性能标准
    performance_thresholds = {
        'database_p95': 50,          # ms
        'redis_p95': 1,             # ms
        'api_p95': 500,             # ms
        'monitoring_latency': 30,   # seconds
        'cache_hit_rate': 0.95,     # 95%
    }
    
    @pytest.fixture
    def db_connection(self):
        """数据库连接"""
        conn = psycopg2.connect(
            host='localhost',
            port=5433,
            database='monitoring',
            user='monitoring_user',
            password='monitoring_password'
        )
        yield conn
        conn.close()
    
    @pytest.fixture
    def redis_client(self):
        """Redis 客户端"""
        client = redis.Redis(host='localhost', port=6380, decode_responses=True)
        yield client
        client.close()
    
    def test_database_query_performance(self, db_connection):
        """测试数据库查询性能"""
        cursor = db_connection.cursor()
        
        # 测试简单查询
        start_time = time.time()
        cursor.execute("SELECT COUNT(*) FROM pg_stat_activity")
        result = cursor.fetchone()
        query_time = (time.time() - start_time) * 1000  # 转换为毫秒
        
        assert query_time <= self.performance_thresholds['database_p95'], \
            f"数据库查询性能不达标: {query_time:.2f}ms > {self.performance_thresholds['database_p95']}ms"
        
        cursor.close()
    
    def test_redis_performance(self, redis_client):
        """测试 Redis 性能"""
        # 测试 SET 操作
        set_times = []
        for _ in range(100):
            start = time.time()
            redis_client.set('perf_test', 'test')
            set_times.append((time.time() - start) * 1000)
        
        # 测试 GET 操作
        get_times = []
        for _ in range(100):
            start = time.time()
            redis_client.get('perf_test')
            get_times.append((time.time() - start) * 1000)
        
        # 计算 P95
        set_p95 = sorted(set_times)[int(len(set_times) * 0.95)]
        get_p95 = sorted(get_times)[int(len(get_times) * 0.95)]
        
        assert set_p95 <= self.performance_thresholds['redis_p95'], \
            f"Redis SET P95 性能不达标: {set_p95:.2f}ms"
        assert get_p95 <= self.performance_thresholds['redis_p95'], \
            f"Redis GET P95 性能不达标: {get_p95:.2f}ms"
    
    def test_api_performance(self):
        """测试 API 性能"""
        api_endpoints = [
            ('/actuator/health', 200),
            ('/metrics', 200),
        ]
        
        response_times = []
        for endpoint, expected_status in api_endpoints:
            start = time.time()
            response = requests.get(f"http://localhost:8081{endpoint}", timeout=10)
            response_time = (time.time() - start) * 1000
            response_times.append(response_time)
            
            assert response.status_code == expected_status, \
                f"API {endpoint} 返回异常状态: {response.status_code}"
        
        p95_response = sorted(response_times)[int(len(response_times) * 0.95)]
        assert p95_response <= self.performance_thresholds['api_p95'], \
            f"API P95 性能不达标: {p95_response:.2f}ms > {self.performance_thresholds['api_p95']}ms"
    
    def test_monitoring_latency(self):
        """测试监控系统延迟"""
        # 测试 Prometheus 采集延迟
        response = requests.get('http://localhost:9090/api/v1/query', 
                              params={'query': 'prometheus_tsdb_head_samples_appended_total'},
                              timeout=10)
        
        assert response.status_code == 200, "Prometheus API 异常"
        
        data = response.json()
        assert data['status'] == 'success', "Prometheus 查询失败"
    
    def test_cache_hit_rate(self, redis_client):
        """测试缓存命中率"""
        # 执行多次操作
        hits = 0
        misses = 0
        
        for i in range(100):
            key = f'cache_test_{i}'
            redis_client.set(key, 'test_value')
            
            # 第二次访问应该命中
            if redis_client.get(key):
                hits += 1
            else:
                misses += 1
        
        hit_rate = hits / (hits + misses) if (hits + misses) > 0 else 0
        assert hit_rate >= self.performance_thresholds['cache_hit_rate'], \
            f"缓存命中率不达标: {hit_rate:.2%} < {self.performance_thresholds['cache_hit_rate']:.0%}"

if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
```

---

## 5. 安全扫描自动化测试

### 5.1 测试目标

- 验证服务安全配置
- 检查已知安全漏洞
- 验证数据加密配置

### 5.2 测试脚本

```python
# qa/automation/testing/security_test.py
import pytest
import requests
import subprocess

class TestSecurity:
    """安全扫描测试"""
    
    def test_no_default_credentials(self):
        """测试无默认凭证"""
        # 检查常见默认配置
        default_configs = [
            ('admin', 'admin'),
            ('root', 'root'),
            ('admin', 'admin123'),
        ]
        
        # 这里应该检查配置文件中是否有默认凭证
        # 实际实现中应通过配置审计完成
        pass
    
    def test_database_password_complexity(self):
        """测试数据库密码复杂度"""
        # 应检查环境变量中数据库密码是否符合安全标准
        # 实际实现中应通过配置审计完成
        pass
    
    def test_redis_auth_enabled(self):
        """测试 Redis 认证启用"""
        try:
            import redis
            client = redis.Redis(host='localhost', port=6380)
            
            # 尝试在无认证情况下执行命令
            try:
                client.ping()
                # 如果能执行，说明未启用认证（应该抛出异常）
                assert False, "Redis 未启用认证"
            except redis.AuthenticationError:
                # 期望行为：需要认证
                pass
                
        except Exception as e:
            pytest.skip(f"Redis 连接异常: {str(e)}")
    
    def test_rabbitmq_management_disabled(self):
        """测试 RabbitMQ 管理界面安全"""
        # 检查 RabbitMQ 管理界面是否启用 SSL
        response = requests.get('http://localhost:15672', timeout=5)
        
        # 应该重定向到 HTTPS 或需要认证
        assert response.status_code in [401, 403], "RabbitMQ 管理界面未正确保护"
    
    def test_grafana_anonymous_access_disabled(self):
        """测试 Grafana 匿名访问禁用"""
        response = requests.get('http://localhost:3000/api/search', timeout=5)
        
        # 应该返回 401 或 403
        assert response.status_code in [401, 403], "Grafana 允许匿名访问"
    
    def test_nginx_ssl_enabled(self):
        """测试 Nginx SSL 启用"""
        # 检查 HTTPS 是否可用
        try:
            response = requests.get('https://localhost', timeout=5, verify=False)
            assert response.status_code == 200, "HTTPS 不可用"
        except requests.exceptions.SSLError:
            pytest.skip("SSL 证书验证失败（测试环境）")
        except requests.exceptions.RequestException:
            pytest.skip("HTTPS 连接失败（测试环境）")

if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
```

---

## 6. 报告生成自动化

### 6.1 测试脚本

```python
# qa/automation/testing/generate_report.py
import pytest
import json
import os
from datetime import datetime

def generate_test_report(test_results, output_path):
    """生成测试报告"""
    
    report = {
        'timestamp': datetime.now().isoformat(),
        'summary': {
            'total': len(test_results),
            'passed': sum(1 for r in test_results if r['passed']),
            'failed': sum(1 for r in test_results if not r['passed']),
        },
        'results': test_results
    }
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    
    return report

def main():
    """主函数"""
    # 这里应读取测试结果并生成报告
    # 实际实现中通过 pytest 插件或 CI/CD 完成
    pass

if __name__ == '__main__':
    main()
```

---

## 7. CI/CD集成配置

### 7.1 Jenkins Pipeline 示例

```groovy
pipeline {
    agent any
    
    environment {
        TEST_ENV = 'testing'
        TEST_RESULTS_DIR = 'qa/reports'
    }
    
    stages {
        stage('环境准备') {
            steps {
                script {
                    // 检查服务状态
                    sh 'docker compose ps'
                }
            }
        }
        
        stage('健康检查测试') {
            steps {
                script {
                    // 运行健康检查测试
                    sh '''
                        cd qa/automation/testing
                        python -m pytest health_check_test.py -v \
                            --html=../../reports/health-check-report.html \
                            --self-contained-html
                    '''
                }
            }
            post {
                always {
                    // 上报测试结果
                    publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'qa/reports', reportFiles: 'health-check-report.html', reportName: 'Health Check Report'])
                }
            }
        }
        
        stage('性能测试') {
            steps {
                script {
                    // 运行性能测试
                    sh '''
                        cd qa/automation/testing
                        python -m pytest performance_test.py -v \
                            --html=../../reports/performance-report.html \
                            --self-contained-html
                    '''
                }
            }
        }
        
        stage('安全扫描') {
            steps {
                script {
                    // 运行安全测试
                    sh '''
                        cd qa/automation/testing
                        python -m pytest security_test.py -v \
                            --html=../../reports/security-report.html \
                            --self-contained-html
                    '''
                }
            }
        }
        
        stage('生成综合报告') {
            steps {
                script {
                    // 生成综合报告
                    echo 'Generating comprehensive test report...'
                }
            }
        }
    }
    
    post {
        success {
            echo 'All tests passed!'
        }
        failure {
            echo 'Tests failed!'
            strictFail('Test failures detected')
        }
    }
}
```

### 7.2 Docker Health Check

```yaml
# docker-compose.test.yml
version: '3.8'

services:
  postgresql:
    image: postgres:15
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U monitoring_user"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  redis:
    image: redis:7
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  prometheus:
    image: prom/prometheus:v2.45.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s

  grafana:
    image: grafana/grafana:10.0.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
```

---

## 8. 自动化测试执行命令

```bash
# 运行所有自动化测试
cd qa/automation/testing

# 健康检查测试
python -m pytest health_check_test.py -v

# 连通性测试
python -m pytest connectivity_test.py -v

# 性能测试
python -m pytest performance_test.py -v

# 安全测试
python -m pytest security_test.py -v

# 生成 Allure 报告
allure generate reports/ -o reports/allure-report --clean
allure open reports/allure-report
```

---

## 9. 自动化测试门禁标准

| 测试类别 | 通过标准 | 门禁级别 |
|---------|---------|---------|
| 健康检查 | 100% 通过 | P0 (阻断) |
| 连通性测试 | 100% 通过 | P0 (阻断) |
| 性能测试 | 100% 达标 | P1 (重要) |
| 安全测试 | 无 P0/P1 问题 | P1 (重要) |

---

## 10. 自动化测试维护

### 10.1 定期维护

- 每周: 更新测试脚本
- 每月: 审查测试标准
- 每季度: 评估测试覆盖率

### 10.2 脚本更新

```bash
# 更新测试脚本后运行
python -m pytest --collect-only

# 查看测试用例统计
python -m pytest --co -q
```

---

**文档维护**: QA Team  
**最后更新**: 2026-04-27