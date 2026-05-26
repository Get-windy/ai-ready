# 数据同步性能测试方案

**版本**: 1.0  
**创建日期**: 2026-04-27  
**作者**: test-agent-1  
**项目**: AI-Ready企业级ERP系统  
**Sprint**: Sprint 27+1  
**任务ID**: task_1777265563112_gbc25du74  

---

## 一、测试概述

### 1.1 测试目的
验证Sprint 27+1测试环境数据同步功能的性能和稳定性，确保数据同步功能能够满足业务需求，包括批量同步、增量同步、实时同步和高并发场景下的性能表现。

### 1.2 测试范围
| 测试类型 | 测试场景 | 测试重点 | 测试数据量 |
|---------|---------|---------|-----------|
| 批量同步性能测试 | 全量数据同步 | 同步时间、吞吐量、资源占用 | 10万-100万条 |
| 增量同步性能测试 | 增量数据同步 | 同步延迟、数据一致性 | 1万-10万条 |
| 实时同步性能测试 | 实时数据变更同步 | 实时性、数据完整性 | 持续流式数据 |
| 高并发同步测试 | 多用户并发同步 | 并发处理能力、系统稳定性 | 100-1000并发 |

### 1.3 测试环境

#### 1.3.1 硬件环境
| 组件 | 配置 | 数量 | 用途 |
|------|------|------|------|
| 应用服务器 | 8核CPU, 32GB内存 | 2台 | 数据同步服务 |
| 数据库服务器 | 16核CPU, 64GB内存, 1TB SSD | 1台 | PostgreSQL主库 |
| Redis服务器 | 4核CPU, 16GB内存 | 1台 | 缓存和队列 |
| 测试客户端 | 4核CPU, 16GB内存 | 3台 | 压力测试工具 |

#### 1.3.2 软件环境
| 组件 | 版本 | 配置 |
|------|------|------|
| 操作系统 | Ubuntu 22.04 LTS | 最新安全补丁 |
| PostgreSQL | v14.2 | 主从复制配置 |
| Redis | v7.0 | 哨兵模式 |
| Java | OpenJDK 17 | G1垃圾回收器 |
| Python | 3.9+ | 测试脚本环境 |
| 监控系统 | Prometheus + Grafana | 全链路监控 |

### 1.4 测试指标

#### 1.4.1 性能指标
1. **吞吐量 (Throughput)**
   - 批量同步：记录/秒
   - 增量同步：事务/秒
   - 实时同步：消息/秒

2. **响应时间 (Response Time)**
   - 平均响应时间
   - P95响应时间
   - P99响应时间
   - 最大响应时间

3. **资源利用率 (Resource Utilization)**
   - CPU使用率
   - 内存使用率
   - 磁盘I/O
   - 网络带宽

4. **可靠性指标 (Reliability)**
   - 同步成功率
   - 数据一致性率
   - 错误率
   - 恢复时间

#### 1.4.2 验收标准
| 指标 | 目标值 | 优先级 |
|------|-------|-------|
| 批量同步吞吐量 | ≥1000记录/秒 | P0 |
| 增量同步延迟 | ≤5秒 | P0 |
| 实时同步延迟 | ≤1秒 | P0 |
| 高并发处理能力 | ≥500并发 | P0 |
| 同步成功率 | ≥99.9% | P1 |
| 数据一致性 | 100% | P1 |
| 系统稳定性 | 7×24小时运行 | P2 |

---

## 二、测试方案设计

### 2.1 测试场景设计

#### 2.1.1 场景1：批量数据同步测试
**场景描述**: 测试全量数据同步的性能，模拟数据迁移或初始化场景

**测试步骤**:
1. 准备测试数据：生成100万条测试数据
2. 执行批量同步：调用批量同步API
3. 监控同步过程：记录开始时间、结束时间、资源占用
4. 验证数据一致性：对比源数据和目标数据
5. 分析性能指标：计算吞吐量、响应时间

**测试数据**:
```sql
-- 测试数据表结构
CREATE TABLE sync_test_data (
    id BIGSERIAL PRIMARY KEY,
    business_id VARCHAR(50) NOT NULL,
    data_content JSONB NOT NULL,
    sync_status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 数据量配置
- 小批量测试：10,000条记录
- 中批量测试：100,000条记录  
- 大批量测试：1,000,000条记录
```

#### 2.1.2 场景2：增量数据同步测试
**场景描述**: 测试增量数据同步的性能，模拟日常数据变更场景

**测试步骤**:
1. 准备基准数据：100万条基础数据
2. 生成增量数据：10,000条变更数据
3. 执行增量同步：触发增量同步机制
4. 监控同步延迟：记录数据变更到同步完成的时间
5. 验证数据一致性：确保增量数据正确同步

**测试数据**:
- 插入操作：30%比例
- 更新操作：50%比例
- 删除操作：20%比例

#### 2.1.3 场景3：实时数据同步测试
**场景描述**: 测试实时数据同步的性能，模拟实时业务场景

**测试步骤**:
1. 建立实时数据流：模拟业务系统实时数据产生
2. 启动实时同步：开启实时同步服务
3. 持续产生数据：以固定频率产生测试数据
4. 监控实时延迟：记录数据产生到同步完成的时间差
5. 验证数据完整性：确保所有数据都被正确同步

**测试数据频率**:
- 低频：10条/秒
- 中频：100条/秒
- 高频：1000条/秒

#### 2.1.4 场景4：高并发数据同步测试
**场景描述**: 测试高并发场景下的数据同步性能，模拟峰值业务场景

**测试步骤**:
1. 准备并发测试环境：配置负载均衡和连接池
2. 设计并发测试场景：模拟多用户同时操作
3. 执行并发测试：使用压力测试工具模拟并发请求
4. 监控系统表现：观察系统在高并发下的表现
5. 分析瓶颈点：识别性能瓶颈和优化点

**并发级别**:
- 低并发：100并发用户
- 中并发：500并发用户
- 高并发：1000并发用户

### 2.2 测试数据设计

#### 2.2.1 数据模型设计
```python
# 测试数据模型
class TestDataModel:
    def __init__(self):
        self.data_types = {
            "user_data": self._generate_user_data,
            "order_data": self._generate_order_data,
            "product_data": self._generate_product_data,
            "log_data": self._generate_log_data
        }
    
    def _generate_user_data(self, count):
        """生成用户测试数据"""
        pass
    
    def _generate_order_data(self, count):
        """生成订单测试数据"""
        pass
    
    def _generate_product_data(self, count):
        """生成产品测试数据"""
        pass
    
    def _generate_log_data(self, count):
        """生成日志测试数据"""
        pass
```

#### 2.2.2 数据生成策略
1. **基础数据生成**: 使用Faker库生成模拟数据
2. **关系数据生成**: 确保数据之间的关联关系
3. **时序数据生成**: 按照时间序列生成数据
4. **异常数据生成**: 包含边界情况和异常情况

#### 2.2.3 数据验证规则
1. **完整性验证**: 验证所有数据都被同步
2. **一致性验证**: 验证源数据和目标数据一致
3. **时序验证**: 验证数据的时间顺序正确
4. **关系验证**: 验证数据之间的关系正确

### 2.3 测试工具设计

#### 2.3.1 压力测试工具
```python
# 压力测试工具框架
class PerformanceTestFramework:
    def __init__(self):
        self.test_scenarios = []
        self.metrics_collector = MetricsCollector()
        self.report_generator = ReportGenerator()
    
    def add_scenario(self, scenario):
        """添加测试场景"""
        self.test_scenarios.append(scenario)
    
    def run_tests(self):
        """执行所有测试场景"""
        for scenario in self.test_scenarios:
            self._run_scenario(scenario)
    
    def generate_report(self):
        """生成测试报告"""
        return self.report_generator.generate(self.metrics_collector.metrics)
```

#### 2.3.2 监控工具集成
1. **Prometheus监控**: 收集系统指标
2. **Grafana仪表盘**: 可视化监控数据
3. **ELK Stack**: 日志收集和分析
4. **自定义监控**: 业务指标监控

#### 2.3.3 自动化测试脚本
1. **环境准备脚本**: 自动化环境准备
2. **数据生成脚本**: 自动化测试数据生成
3. **测试执行脚本**: 自动化测试执行
4. **结果分析脚本**: 自动化结果分析

---

## 三、测试脚本编写

### 3.1 批量同步性能测试脚本

#### 3.1.1 Python测试脚本
```python
#!/usr/bin/env python3
"""
批量同步性能测试脚本
"""

import asyncio
import aiohttp
import time
import statistics
from typing import List, Dict
import logging
from dataclasses import dataclass
from concurrent.futures import ThreadPoolExecutor

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class TestConfig:
    """测试配置"""
    base_url: str = "http://localhost:8080"
    batch_size: int = 1000
    total_records: int = 100000
    concurrency_level: int = 10
    timeout_seconds: int = 300

@dataclass
class TestResult:
    """测试结果"""
    total_time: float
    records_per_second: float
    success_count: int
    failure_count: int
    error_rate: float
    response_times: List[float]
    
    @property
    def avg_response_time(self) -> float:
        return statistics.mean(self.response_times) if self.response_times else 0
    
    @property
    def p95_response_time(self) -> float:
        if not self.response_times:
            return 0
        sorted_times = sorted(self.response_times)
        index = int(len(sorted_times) * 0.95)
        return sorted_times[index]
    
    @property
    def p99_response_time(self) -> float:
        if not self.response_times:
            return 0
        sorted_times = sorted(self.response_times)
        index = int(len(sorted_times) * 0.99)
        return sorted_times[index]

class BatchSyncPerformanceTest:
    """批量同步性能测试"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.results = TestResult(
            total_time=0,
            records_per_second=0,
            success_count=0,
            failure_count=0,
            error_rate=0,
            response_times=[]
        )
    
    async def generate_test_data(self) -> List[Dict]:
        """生成测试数据"""
        logger.info(f"生成 {self.config.total_records} 条测试数据...")
        test_data = []
        
        for i in range(self.config.total_records):
            record = {
                "id": f"test_{i:08d}",
                "business_id": f"biz_{i % 1000:04d}",
                "data_type": "user" if i % 3 == 0 else "order" if i % 3 == 1 else "product",
                "content": {
                    "field1": f"value_{i}",
                    "field2": i * 10,
                    "field3": i % 2 == 0,
                    "timestamp": time.time()
                },
                "metadata": {
                    "source": "test_generator",
                    "batch_id": f"batch_{i // self.config.batch_size}",
                    "sequence": i
                }
            }
            test_data.append(record)
        
        logger.info(f"测试数据生成完成，共 {len(test_data)} 条记录")
        return test_data
    
    async def send_batch_request(self, session: aiohttp.ClientSession, 
                                batch_data: List[Dict], batch_num: int) -> bool:
        """发送批量请求"""
        url = f"{self.config.base_url}/api/v1/sync/batch"
        
        payload = {
            "batch_id": f"test_batch_{batch_num}",
            "records": batch_data,
            "sync_type": "full",
            "priority": "high"
        }
        
        start_time = time.time()
        try:
            async with session.post(url, json=payload, 
                                   timeout=aiohttp.ClientTimeout(total=self.config.timeout_seconds)) as response:
                end_time = time.time()
                response_time = end_time - start_time
                
                if response.status == 200:
                    self.results.success_count += len(batch_data)
                    self.results.response_times.append(response_time)
                    logger.debug(f"批次 {batch_num} 同步成功，响应时间: {response_time:.3f}秒")
                    return True
                else:
                    self.results.failure_count += len(batch_data)
                    logger.warning(f"批次 {batch_num} 同步失败，状态码: {response.status}")
                    return False
                    
        except Exception as e:
            self.results.failure_count += len(batch_data)
            logger.error(f"批次 {batch_num} 请求异常: {str(e)}")
            return False
    
    async def run_test(self):
        """执行性能测试"""
        logger.info("开始批量同步性能测试...")
        
        # 生成测试数据
        test_data = await self.generate_test_data()
        
        # 分批处理
        batches = []
        for i in range(0, len(test_data), self.config.batch_size):
            batch = test_data[i:i + self.config.batch_size]
            batches.append((batch, i // self.config.batch_size))
        
        logger.info(f"数据分成了 {len(batches)} 个批次，每批 {self.config.batch_size} 条记录")
        
        # 创建HTTP会话
        connector = aiohttp.TCPConnector(limit=self.config.concurrency_level)
        
        start_time = time.time()
        
        async with aiohttp.ClientSession(connector=connector) as session:
            # 并发执行批量请求
            tasks = []
            for batch_data, batch_num in batches:
                task = self.send_batch_request(session, batch_data, batch_num)
                tasks.append(task)
            
            # 等待所有任务完成
            results = await asyncio.gather(*tasks, return_exceptions=True)
            
        end_time = time.time()
        
        # 计算测试结果
        self.results.total_time = end_time - start_time
        self.results.records_per_second = self.results.success_count / self.results.total_time
        
        total_attempts = self.results.success_count + self.results.failure_count
        if total_attempts > 0:
            self.results.error_rate = self.results.failure_count / total_attempts * 100
        
        return self.results
    
    def print_results(self):
        """打印测试结果"""
        print("\n" + "="*60)
        print("批量同步性能测试结果")
        print("="*60)
        print(f"总记录数: {self.results.success_count + self.results.failure_count:,}")
        print(f"成功记录: {self.results.success_count:,}")
        print(f"失败记录: {self.results.failure_count:,}")
        print(f"错误率: {self.results.error_rate:.2f}%")
        print(f"总耗时: {self.results.total_time:.2f}秒")
        print(f"吞吐量: {self.results.records_per_second:.2f} 记录/秒")
        print(f"平均响应时间: {self.results.avg_response_time:.3f}秒")
        print(f"P95响应时间: {self.results.p95_response_time:.3f}秒")
        print(f"P99响应时间: {self.results.p99_response_time:.3f}秒")
        print("="*60)

async def main():
    """主函数"""
    # 测试配置
    config = TestConfig(
        base_url="http://localhost:8080",
        batch_size=1000,
        total_records=100000,
        concurrency_level=20,
        timeout_seconds=300
    )
    
    # 执行测试
    test = BatchSyncPerformanceTest(config)
    results = await test.run_test()
    test.print_results()
    
    # 判断测试是否通过
    if results.records_per_second >= 1000 and results.error_rate < 1:
        print("✅ 批量同步性能测试通过")
        return 0
    else:
        print("❌ 批量同步性能测试失败")
        return 1

if __name__ == "__main__":
    import sys
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
```

#### 3.1.2 Shell包装脚本
```bash
#!/bin/bash
# 批量同步性能测试包装脚本

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="${SCRIPT_DIR}/batch_sync_performance_test.py"
LOG_DIR="${SCRIPT_DIR}/logs"
REPORT_DIR="${SCRIPT_DIR}/reports"
CONFIG_FILE="${SCRIPT_DIR}/test_config.yaml"

# 创建目录
mkdir -p "${LOG_DIR}" "${REPORT_DIR}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查环境
check_environment() {
    log_info "检查测试环境..."
    
    # 检查Python
    if ! command -v python3 &> /dev/null; then
        log_error "Python3未安装"
        return 1
    fi
    
    # 检查Python版本
    python_version=$(python3 --version | cut -d' ' -f2)
    log_info "Python版本: ${python_version}"
    
    # 检查依赖
    if ! python3 -c "import aiohttp" 2>/dev/null; then
        log_warning "aiohttp未安装，尝试安装..."
        pip install aiohttp || {
            log_error "安装aiohttp失败"
            return 1
        }
    fi
    
    # 检查API端点
    local base_url="${1:-http://localhost:8080}"
    if ! curl -s "${base_url}/health" | grep -q "healthy"; then
        log_warning "API服务可能未运行或健康检查失败"
    fi
    
    log_success "环境检查完成"
    return 0
}

# 执行测试
run_test() {
    local test_type="$1"
    local config_file="$2"
    local log_file="${LOG_DIR}/batch_sync_$(date +%Y%m%d_%H%M%S).log"
    local report_file="${REPORT_DIR}/batch_sync_report_$(date +%Y%m%d_%H%M%S).json"
    
    log_info "开始执行 ${test_type} 测试..."
    log_info "日志文件: ${log_file}"
    log_info "报告文件: ${report_file}"
    
    # 执行Python测试脚本
    if python3 "${PYTHON_SCRIPT}" --config "${config_file}" --output "${report_file}" 2>&1 | tee "${log_file}"; then
        log_success "${test_type} 测试执行完成"
        
        # 检查测试结果
        if grep -q "批量同步性能测试通过" "${log_file}"; then
            log_success "${test_type} 测试通过"
            return 0
        else
            log_error "${test_type} 测试失败"
            return 1
        fi
    else
        log_error "${test_type} 测试执行失败"
        return 1
    fi
}

# 生成测试报告
generate_report() {
    local report_file="$1"
    
    log_info "生成测试报告..."
    
    if [ ! -f "${report_file}" ]; then
        log_error "测试结果文件不存在: ${report_file}"
        return 1
    fi
    
    # 读取JSON报告并生成Markdown
    local markdown_file="${report_file%.json}.md"
    
    python3 -c "
import json
import sys

try:
    with open('${report_file}', 'r') as f:
        data = json.load(f)
    
    with open('${markdown_file}', 'w') as f:
        f.write('# 批量同步性能测试报告\\n\\n')
        f.write('## 测试摘要\\n')
        f.write(f'- **测试时间**: {data.get(\"timestamp\", \"N/A\")}\\n')
        f.write(f'- **总记录数**: {data.get(\"total_records\", 0):,}\\n')
        f.write(f'- **成功记录**: {data.get(\"success_count\", 0):,}\\n')
        f.write(f'- **失败记录**: {data.get(\"failure_count\", 0):,}\\n')
        f.write(f'- **错误率**: {data.get(\"error_rate\", 0):.2f}%\\n')
        f.write(f'- **总耗时**: {data.get(\"total_time\", 0):.2f}秒\\n')
        f.write(f'- **吞吐量**: {data.get(\"records_per_second\", 0):.2f} 记录/秒\\n')
        f.write(f'- **测试结果**: {\"✅ 通过\" if data.get(\"passed\", False) else \"❌ 失败\"}\\n')
        
        if data.get(\"passed\", False):
            f.write('\\n## 结论\\n')
            f.write('批量同步性能测试通过，所有指标达到预期目标。\\n')
        else:
            f.write('\\n## 问题分析\\n')
            f.write('批量同步性能测试失败，需要检查以下问题：\\n')
            for issue in data.get(\"issues\", []):
                f.write(f'- {issue}\\n')
    
    print(f'Markdown报告已生成: ${markdown_file}')
    
except Exception as e:
    print(f'生成报告失败: {str(e)}')
    sys.exit(1)
"
    
    if [ $? -eq 0 ]; then
        log_success "测试报告生成完成: ${markdown_file}"
        return 0
    else
        log_error "测试报告生成失败"
        return 1
    fi
}

# 主函数
main() {
    local test_type="${1:-standard}"
    local base_url="${2:-http://localhost:8080}"
    
    log_info "批量同步性能测试工具启动"
    log_info "测试类型: ${test_type}"
    log_info "API地址: ${base_url}"
    
    # 检查环境
    if ! check_environment "${base_url}"; then
        log_error "环境检查失败，退出测试"
        return 1
    fi
    
    # 根据测试类型选择配置
    local config_file
    case "${test_type}" in
        "small")
            config_file="${SCRIPT_DIR}/config_small.yaml"
            ;;
        "medium")
            config_file="${SCRIPT_DIR}/config_medium.yaml"
            ;;
        "large")
            config_file="${SCRIPT_DIR}/config_large.yaml"
            ;;
        "standard"|*)
            config_file="${SCRIPT_DIR}/config_standard.yaml"
            ;;
    esac
    
    # 检查配置文件
    if [ ! -f "${config_file}" ]; then
        log_warning "配置文件不存在: ${config_file}，使用默认配置"
        config_file="${CONFIG_FILE}"
    fi
    
    # 执行测试
    local test_result=0
    if run_test "${test_type}" "${config_file}"; then
        log_success "测试执行成功"
        
        # 查找最新的报告文件
        local latest_report=$(ls -t "${REPORT_DIR}"/*.json 2>/dev/null | head -1)
        if [ -n "${latest_report}" ]; then
            generate_report "${latest_report}"
        fi
        
        test_result=0
    else
        log_error "测试执行失败"
        test_result=1
    fi
    
    # 汇总结果
    if [ ${test_result} -eq 0 ]; then
        log_success "批量同步性能测试全部完成 ✅"
    else
        log_error "批量同步性能测试存在失败 ❌"
    fi
    
    return ${test_result}
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
```

### 3.2 增量同步性能测试脚本

#### 3.2.1 增量同步测试框架
```python
#!/usr/bin/env python3
"""
增量同步性能测试脚本
"""

import asyncio
import aiohttp
import time
import random
import statistics
from typing import List, Dict, Optional
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta
import json

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class IncrementalTestConfig:
    """增量同步测试配置"""
    base_url: str = "http://localhost:8080"
    initial_records: int = 100000  # 初始数据量
    incremental_records: int = 10000  # 增量数据量
    change_types: Dict[str, float] = None  # 变更类型分布
    test_duration: int = 300  # 测试持续时间(秒)
    check_interval: int = 5  # 检查间隔(秒)
    
    def __post_init__(self):
        if self.change_types is None:
            self.change_types = {
                "insert": 0.3,  # 30%插入
                "update": 0.5,  # 50%更新
                "delete": 0.2   # 20%删除
            }

@dataclass
class ChangeRecord:
    """变更记录"""
    change_id: str
    change_type: str  # insert, update, delete
    record_id: Optional[str]
    data: Optional[Dict]
    timestamp: float
    sync_start_time: Optional[float] = None
    sync_end_time: Optional[float] = None
    
    @property
    def sync_latency(self) -> Optional[float]:
        if self.sync_start_time and self.sync_end_time:
            return self.sync_end_time - self.sync_start_time
        return None

@dataclass  
class IncrementalTestResult:
    """增量同步测试结果"""
    total_changes: int
    synced_changes: int
    failed_changes: int
    sync_success_rate: float
    avg_sync_latency: float
    p95_sync_latency: float
    p99_sync_latency: float
    max_sync_latency: float
    data_consistency_rate: float
    test_duration: float
    throughput: float  # 变更/秒
    
    def to_dict(self) -> Dict:
        return {
            "total_changes": self.total_changes,
            "synced_changes": self.synced_changes,
            "failed_changes": self.failed_changes,
            "sync_success_rate": self.sync_success_rate,
            "avg_sync_latency": self.avg_sync_latency,
            "p95_sync_latency": self.p95_sync_latency,
            "p99_sync_latency": self.p99_sync_latency,
            "max_sync_latency": self.max_sync_latency,
            "data_consistency_rate": self.data_consistency_rate,
            "test_duration": self.test_duration,
            "throughput": self.throughput
        }

class IncrementalSyncPerformanceTest:
    """增量同步性能测试"""
    
    def __init__(self, config: IncrementalTestConfig):
        self.config = config
        self.change_records: List[ChangeRecord] = []
        self.synced_ids = set()
        self.results = IncrementalTestResult(
            total_changes=0,
            synced_changes=0,
            failed_changes=0,
            sync_success_rate=0,
            avg_sync_latency=0,
            p95_sync_latency=0,
            p99_sync_latency=0,
            max_sync_latency=0,
            data_consistency_rate=0,
            test_duration=0,
            throughput=0
        )
    
    def generate_initial_data(self) -> List[Dict]:
        """生成初始数据"""
        logger.info(f"生成 {self.config.initial_records} 条初始数据...")
        
        initial_data = []
        for i in range(self.config.initial_records):
            record = {
                "id": f"init_{i:08d}",
                "business_id": f"biz_{i % 1000:04d}",
                "data_type": "user" if i % 3 == 0 else "order" if i % 3 == 1 else "product",
                "content": {
                    "name": f"Record_{i}",
                    "value": i * 100,
                    "status": "active",
                    "created_at": datetime.now().isoformat()
                },
                "version": 1,
                "is_deleted": False
            }
            initial_data.append(record)
        
        logger.info(f"初始数据生成完成，共 {len(initial_data)} 条记录")
        return initial_data
    
    def generate_incremental_changes(self) -> List[ChangeRecord]:
        """生成增量变更"""
        logger.info(f"生成 {self.config.incremental_records} 条增量变更...")
        
        changes = []
        
        # 生成初始ID池
        initial_ids = [f"init_{i:08d}" for i in range(self.config.initial_records)]
        
        for i in range(self.config.incremental_records):
            # 选择变更类型
            change_type = random.choices(
                list(self.config.change_types.keys()),
                weights=list(self.config.change_types.values())
            )[0]
            
            change_id = f"change_{i:08d}"
            record_id = None
            data = None
            
            if change_type == "insert":
                # 插入新记录
                record_id = f"new_{i:08d}"
                data = {
                    "id": record_id,
                    "business_id": f"biz_{(self.config.initial_records + i) % 1000:04d}",
                    "data_type": random.choice(["user", "order", "product"]),
                    "content": {
                        "name": f"New_Record_{i}",
                        "value": random.randint(1, 10000),
                        "status": "active",
                        "created_at": datetime.now().isoformat()
                    },
                    "version": 1,
                    "is_deleted": False
                }
            elif change_type == "update":
                # 更新现有记录
                if initial_ids:
                    record_id = random.choice(initial_ids)
                    data = {
                        "content": {
                            "name": f"Updated_Record_{record_id}",
                            "value": random.randint(1, 10000),
                            "status": random.choice(["active", "inactive", "pending"]),
                            "updated_at": datetime.now().isoformat()
                        },
                        "version": random.randint(2, 10)
                    }
            elif change_type == "delete":
                # 删除现有记录
                if initial_ids:
                    record_id = random.choice(initial_ids)
                    initial_ids.remove(record_id)  # 从ID池中移除
                    data = {
                        "is_deleted": True,
                        "deleted_at": datetime.now().isoformat()
                    }
            
            change_record = ChangeRecord(
                change_id=change_id,
                change_type=change_type,
                record_id=record_id,
                data=data,
                timestamp=time.time() + random.uniform(0, self.config.test_duration)
            )
            
            changes.append(change_record)
        
        # 按时间戳排序
        changes.sort(key=lambda x: x.timestamp)
        
        logger.info(f"增量变更生成完成，共 {len(changes)} 条变更")
        return changes
    
    async def apply_change(self, session: aiohttp.ClientSession, 
                          change: ChangeRecord) -> bool:
        """应用单个变更"""
        change.sync_start_time = time.time()
        
        try:
            if change.change_type == "insert":
                url = f"{self.config.base_url}/api/v1/sync/incremental/insert"
                payload = change.data
            elif change.change_type == "update":
                url = f"{self.config.base_url}/api/v1/sync/incremental/update"
                payload = {
                    "record_id": change.record_id,
                    "data": change.data
                }
            elif change.change_type == "delete":
                url = f"{self.config.base_url}/api/v1/sync/incremental/delete"
                payload = {
                    "record_id": change.record_id,
                    "data": change.data
                }
            else:
                logger.warning(f"未知变更类型: {change.change_type}")
                return False
            
            async with session.post(url, json=payload, timeout=30) as response:
                change.sync_end_time = time.time()
                
                if response.status == 200:
                    self.synced_ids.add(change.change_id)
                    return True
                else:
                    logger.warning(f"变更 {change.change_id} 失败，状态码: {response.status}")
                    return False
                    
        except Exception as e:
            logger.error(f"变更 {change.change_id} 异常: {str(e)}")
            return False
    
    async def monitor_sync_progress(self):
        """监控同步进度"""
        logger.info("开始监控同步进度...")
        
        start_time = time.time()
        last_check_time = start_time
        last_synced_count = 0
        
        while time.time() - start_time < self.config.test_duration:
            current_time = time.time()
            
            if current_time - last_check_time >= self.config.check_interval:
                # 检查需要同步的变更
                pending_changes = [
                    c for c in self.change_records 
                    if c.timestamp <= current_time and c.sync_start_time is None
                ]
                
                # 应用待同步的变更
                if pending_changes:
                    connector = aiohttp.TCPConnector(limit=50)
                    async with aiohttp.ClientSession(connector=connector) as session:
                        tasks = [self.apply_change(session, change) for change in pending_changes]
                        results = await asyncio.gather(*tasks, return_exceptions