# 订单管理模块接口测试计划

**版本**: 1.0  
**创建日期**: 2026-04-27  
**作者**: qa-lead  
**项目**: AI-Ready企业级ERP系统  
**Sprint**: Sprint 27+1  

---

## 一、测试概述

### 1.1 测试目标

验证订单管理模块的API接口功能正确性，确保订单创建、查询、修改、删除等核心功能的实现符合业务需求。

### 1.2 测试范围

**API端点**:
- 订单创建：POST /api/v1/orders
- 订单查询：GET /api/v1/orders/{id}
- 订单修改：PUT /api/v1/orders/{id}
- 订单删除：DELETE /api/v1/orders/{id}
- 订单列表：GET /api/v1/orders
- 健康检查：GET /actuator/health

**预期端口**: 8082

**依赖服务**:
- MySQL数据库 (3307端口)
- RabbitMQ消息队列 (5672端口)
- Redis缓存

### 1.3 测试策略

采用自动化测试优先策略：
1. 首先验证服务健康状态
2. 测试核心CRUD功能
3. 测试业务流程（订单状态流转）
4. 测试与其他模块的集成（库存管理）

---

## 二、测试用例设计

### 2.1 健康检查测试 (TC-OM-001)

**测试目标**: 验证订单管理服务健康状态

**测试步骤**:
1. 访问健康检查端点 `/actuator/health`
2. 检查响应状态码 (200)
3. 验证响应内容包含状态字段 "UP"
4. 检查依赖服务状态（MySQL、RabbitMQ、Redis）

**验收标准**:
- HTTP状态码：200
- 响应状态："UP"
- 所有依赖服务状态："UP"

**优先级**: P0 (最高)

### 2.2 订单创建测试 (TC-OM-002)

**测试目标**: 验证订单创建接口功能

**测试数据**:
```json
{
  "userId": "user_001",
  "productId": "product_001",
  "quantity": 2,
  "totalPrice": 199.99,
  "status": "pending"
}
```

**测试步骤**:
1. 准备测试订单数据
2. POST请求到 `/api/v1/orders`
3. 检查响应状态码 (201)
4. 验证响应包含订单ID
5. 验证订单数据正确保存

**验收标准**:
- HTTP状态码：201
- 响应包含订单ID
- 订单数据正确保存

**优先级**: P0 (最高)

### 2.3 订单查询测试 (TC-OM-003)

**测试目标**: 验证订单查询接口功能

**测试步骤**:
1. 使用已知订单ID
2. GET请求到 `/api/v1/orders/{id}`
3. 检查响应状态码 (200)
4. 验证响应订单数据正确

**验收标准**:
- HTTP状态码：200
- 响应订单数据与创建数据一致

**优先级**: P0 (最高)

### 2.4 订单修改测试 (TC-OM-004)

**测试目标**: 验证订单修改接口功能

**测试数据**:
```json
{
  "quantity": 3,
  "totalPrice": 299.99,
  "status": "confirmed"
}
```

**测试步骤**:
1. 准备修改数据
2. PUT请求到 `/api/v1/orders/{id}`
3. 检查响应状态码 (200)
4. 验证订单数据已更新

**验收标准**:
- HTTP状态码：200
- 订单数据已正确更新

**优先级**: P1 (高)

### 2.5 订单删除测试 (TC-OM-005)

**测试目标**: 验证订单删除接口功能

**测试步骤**:
1. 使用已知订单ID
2. DELETE请求到 `/api/v1/orders/{id}`
3. 检查响应状态码 (200/204)
4. 验证订单已被删除
5. 再次查询订单，验证返回404

**验收标准**:
- DELETE响应状态码：200或204
- 再次查询返回404

**优先级**: P1 (高)

### 2.6 订单状态流转测试 (TC-OM-006)

**测试目标**: 验证订单状态流转业务逻辑

**测试步骤**:
1. 创建订单，初始状态："pending"
2. 更新状态为："confirmed"
3. 更新状态为："shipped"
4. 更新状态为："delivered"
5. 验证每个状态转换正确

**验收标准**:
- 状态转换流程正确
- 每个状态转换响应正确

**优先级**: P1 (高)

### 2.7 订单列表查询测试 (TC-OM-007)

**测试目标**: 验证订单列表查询接口功能

**测试步骤**:
1. GET请求到 `/api/v1/orders`
2. 检查响应状态码 (200)
3. 验证响应包含订单列表
4. 验证分页参数正确处理

**验收标准**:
- HTTP状态码：200
- 响应包含订单列表
- 分页功能正常

**优先级**: P2 (中)

### 2.8 订单与库存集成测试 (TC-OM-008)

**测试目标**: 验证订单创建与库存管理的集成

**测试步骤**:
1. 查询当前库存数量
2. 创建订单，指定商品数量
3. 验证库存数量减少
4. 删除订单
5. 验证库存数量恢复

**验收标准**:
- 创建订单后库存正确减少
- 删除订单后库存正确恢复

**优先级**: P2 (中)

---

## 三、测试执行脚本

### 3.1 Python自动化测试脚本

```python
#!/usr/bin/env python3
"""
订单管理模块API接口自动化测试脚本
"""

import requests
import json
import sys
import time
from datetime import datetime

class OrderManagementAPITest:
    """订单管理API测试类"""
    
    def __init__(self, base_url="http://localhost:8082"):
        self.base_url = base_url
        self.created_order_id = None
        self.test_results = []
        self.passed_count = 0
        self.failed_count = 0
        
    def log_result(self, test_id, test_name, passed, details=""):
        """记录测试结果"""
        result = {
            'test_id': test_id,
            'test_name': test_name,
            'passed': passed,
            'details': details,
            'timestamp': datetime.now().isoformat()
        }
        self.test_results.append(result)
        
        if passed:
            self.passed_count += 1
            print(f"✅ [{test_id}] {test_name} - 通过")
        else:
            self.failed_count += 1
            print(f"❌ [{test_id}] {test_name} - 失败: {details}")
    
    def test_health_check(self):
        """TC-OM-001: 健康检查测试"""
        test_id = "TC-OM-001"
        test_name = "健康检查测试"
        
        try:
            response = requests.get(
                f"{self.base_url}/actuator/health",
                timeout=10
            )
            
            if response.status_code == 200:
                health_data = response.json()
                
                if health_data.get('status') == 'UP':
                    self.log_result(test_id, test_name, True,
                                   f"服务健康状态: {health_data['status']}")
                    return True
                else:
                    self.log_result(test_id, test_name, False,
                                   f"服务状态异常: {health_data.get('status')}")
                    return False
            else:
                self.log_result(test_id, test_name, False,
                               f"HTTP状态码错误: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_result(test_id, test_name, False,
                           f"请求失败: {str(e)}")
            return False
    
    def test_order_create(self):
        """TC-OM-002: 订单创建测试"""
        test_id = "TC-OM-002"
        test_name = "订单创建测试"
        
        order_data = {
            "userId": "test_user_001",
            "productId": "test_product_001",
            "quantity": 2,
            "totalPrice": 199.99,
            "status": "pending"
        }
        
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/orders",
                json=order_data,
                timeout=10
            )
            
            if response.status_code == 201:
                order = response.json()
                
                if 'id' in order or 'orderId' in order:
                    self.created_order_id = order.get('id') or order.get('orderId')
                    self.log_result(test_id, test_name, True,
                                   f"订单创建成功，ID: {self.created_order_id}")
                    return True
                else:
                    self.log_result(test_id, test_name, False,
                                   "响应缺少订单ID字段")
                    return False
            else:
                self.log_result(test_id, test_name, False,
                               f"HTTP状态码错误: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_result(test_id, test_name, False,
                           f"请求失败: {str(e)}")
            return False
    
    def test_order_get(self):
        """TC-OM-003: 订单查询测试"""
        test_id = "TC-OM-003"
        test_name = "订单查询测试"
        
        if not self.created_order_id:
            self.log_result(test_id, test_name, False,
                           "没有可用的订单ID")
            return False
        
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                timeout=10
            )
            
            if response.status_code == 200:
                order = response.json()
                
                # 验证订单数据
                if order.get('userId') == 'test_user_001':
                    self.log_result(test_id, test_name, True,
                                   f"订单查询成功，数据正确")
                    return True
                else:
                    self.log_result(test_id, test_name, False,
                                   f"订单数据不一致")
                    return False
            else:
                self.log_result(test_id, test_name, False,
                               f"HTTP状态码错误: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_result(test_id, test_name, False,
                           f"请求失败: {str(e)}")
            return False
    
    def test_order_update(self):
        """TC-OM-004: 订单修改测试"""
        test_id = "TC-OM-004"
        test_name = "订单修改测试"
        
        if not self.created_order_id:
            self.log_result(test_id, test_name, False,
                           "没有可用的订单ID")
            return False
        
        update_data = {
            "quantity": 3,
            "totalPrice": 299.99,
            "status": "confirmed"
        }
        
        try:
            response = requests.put(
                f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                json=update_data,
                timeout=10
            )
            
            if response.status_code == 200:
                order = response.json()
                
                if order.get('quantity') == 3 and order.get('status') == 'confirmed':
                    self.log_result(test_id, test_name, True,
                                   f"订单修改成功")
                    return True
                else:
                    self.log_result(test_id, test_name, False,
                                   f"订单数据未正确更新")
                    return False
            else:
                self.log_result(test_id, test_name, False,
                               f"HTTP状态码错误: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_result(test_id, test_name, False,
                           f"请求失败: {str(e)}")
            return False
    
    def test_order_delete(self):
        """TC-OM-005: 订单删除测试"""
        test_id = "TC-OM-005"
        test_name = "订单删除测试"
        
        if not self.created_order_id:
            self.log_result(test_id, test_name, False,
                           "没有可用的订单ID")
            return False
        
        try:
            # 删除订单
            response = requests.delete(
                f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                timeout=10
            )
            
            if response.status_code in [200, 204]:
                # 验证订单已删除
                verify_response = requests.get(
                    f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                    timeout=10
                )
                
                if verify_response.status_code == 404:
                    self.log_result(test_id, test_name, True,
                                   f"订单删除成功，查询返回404")
                    return True
                else:
                    self.log_result(test_id, test_name, False,
                                   f"订单删除后仍可查询: {verify_response.status_code}")
                    return False
            else:
                self.log_result(test_id, test_name, False,
                               f"删除HTTP状态码错误: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_result(test_id, test_name, False,
                           f"请求失败: {str(e)}")
            return False
    
    def run_all_tests(self):
        """执行所有测试"""
        print(f"===== 开始执行订单管理API测试 =====")
        print(f"服务地址: {self.base_url}")
        
        # 1. 健康检查
        self.test_health_check()
        
        # 2. 订单创建
        self.test_order_create()
        
        # 3. 订单查询
        self.test_order_get()
        
        # 4. 订单修改
        self.test_order_update()
        
        # 5. 订单删除
        self.test_order_delete()
        
        print(f"===== 测试执行完成 =====")
        print(f"总测试数: {len(self.test_results)}")
        print(f"通过数: {self.passed_count}")
        print(f"失败数: {self.failed_count}")
        print(f"通过率: {(self.passed_count/len(self.test_results)*100):.2f}%")
        
        return self.generate_report()
    
    def generate_report(self):
        """生成测试报告"""
        report = {
            'test_date': datetime.now().isoformat(),
            'service_url': self.base_url,
            'total_tests': len(self.test_results),
            'passed': self.passed_count,
            'failed': self.failed_count,
            'pass_rate': (self.passed_count/len(self.test_results)*100) if self.test_results else 0,
            'results': self.test_results
        }
        
        # 保存报告
        with open('order_api_test_report.json', 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        return report


def main():
    """主函数"""
    # 检查服务端口
    base_url = "http://localhost:8082"
    
    # 可从命令行参数获取服务地址
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    tester = OrderManagementAPITest(base_url)
    report = tester.run_all_tests()
    
    print(f"\n测试报告已生成: order_api_test_report.json")


if __name__ == '__main__':
    main()
```

### 3.2 PowerShell快速测试脚本

```powershell
# PowerShell订单管理API快速测试脚本

$baseUrl = "http://localhost:8082"

# 1. 健康检查测试
Write-Host "===== TC-OM-001: 健康检查测试 =====" -ForegroundColor Green
try {
    $healthResponse = Invoke-WebRequest -Uri "$baseUrl/actuator/health" -Method GET -TimeoutSec 10
    $healthData = $healthResponse.Content | ConvertFrom-Json
    
    if ($healthData.status -eq "UP") {
        Write-Host "✅ 服务健康状态: UP" -ForegroundColor Green
    } else {
        Write-Host "❌ 服务状态异常: $($healthData.status)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 健康检查失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. 订单创建测试
Write-Host "===== TC-OM-002: 订单创建测试 =====" -ForegroundColor Green
$orderData = @{
    userId = "test_user_powershell"
    productId = "test_product_001"
    quantity = 2
    totalPrice = 199.99
    status = "pending"
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/orders" -Method POST -Body $orderData -ContentType "application/json" -TimeoutSec 10
    $createdOrder = $createResponse.Content | ConvertFrom-Json
    $orderId = $createdOrder.id
    
    if ($orderId) {
        Write-Host "✅ 订单创建成功，ID: $orderId" -ForegroundColor Green
    } else {
        Write-Host "❌ 响应缺少订单ID" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 订单创建失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. 订单查询测试
Write-Host "===== TC-OM-003: 订单查询测试 =====" -ForegroundColor Green
if ($orderId) {
    try {
        $getResponse = Invoke-WebRequest -Uri "$baseUrl/api/v1/orders/$orderId" -Method GET -TimeoutSec 10
        $order = $getResponse.Content | ConvertFrom-Json
        
        if ($order.userId -eq "test_user_powershell") {
            Write-Host "✅ 订单查询成功，数据正确" -ForegroundColor Green
        } else {
            Write-Host "❌ 订单数据不一致" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ 订单查询失败: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "===== 测试执行完成 =====" -ForegroundColor Green
```

---

## 四、测试数据准备

### 4.1 测试订单数据模板

```json
{
  "userId": "test_user_001",
  "productId": "test_product_001",
  "quantity": 2,
  "totalPrice": 199.99,
  "status": "pending",
  "shippingAddress": {
    "street": "测试街道1号",
    "city": "测试城市",
    "zipCode": "100001"
  },
  "paymentMethod": "credit_card"
}
```

### 4.2 测试用户数据

- 用户ID: test_user_001, test_user_002, test_user_003
- 用户类型: 普通用户, VIP用户, 企业用户

### 4.3 测试商品数据

- 商品ID: test_product_001, test_product_002, test_product_003
- 商品类型: 普通商品, 限时商品, 虚拟商品

---

## 五、测试报告模板

### 5.1 JSON测试报告格式

```json
{
  "test_date": "2026-04-27T13:00:00",
  "service_url": "http://localhost:8082",
  "total_tests": 8,
  "passed": 7,
  "failed": 1,
  "pass_rate": 87.5,
  "results": [
    {
      "test_id": "TC-OM-001",
      "test_name": "健康检查测试",
      "passed": true,
      "details": "服务健康状态: UP",
      "timestamp": "2026-04-27T13:00:05"
    },
    {
      "test_id": "TC-OM-002",
      "test_name": "订单创建测试",
      "passed": true,
      "details": "订单创建成功，ID: order_12345",
      "timestamp": "2026-04-27T13:00:10"
    }
  ],
  "issues": [
    {
      "issue_id": "ISSUE-OM-001",
      "test_id": "TC-OM-005",
      "description": "订单删除接口返回500错误",
      "severity": "medium",
      "suggested_fix": "检查数据库事务处理逻辑"
    }
  ]
}
```

### 5.2 Markdown测试报告格式

```markdown
# 订单管理模块API接口测试报告

**测试日期**: 2026-04-27
**测试执行人**: qa-lead
**服务地址**: http://localhost:8082

## 一、测试概况

- 测试用例总数: 8个
- 测试通过数: 7个
- 测试失败数: 1个
- 测试通过率: 87.5%

## 二、测试结果

| 测试ID | 测试名称 | 结果 | 备注 |
|-------|---------|------|------|
| TC-OM-001 | 健康检查测试 | ✅ 通过 | 服务健康状态: UP |
| TC-OM-002 | 订单创建测试 | ✅ 通过 | 订单ID: order_12345 |
| TC-OM-003 | 订单查询测试 | ✅ 通过 | 数据正确 |
| TC-OM-004 | 订单修改测试 | ✅ 通过 | 数据更新成功 |
| TC-OM-005 | 订单删除测试 | ❌ 失败 | 返回500错误 |

## 三、发现的问题

### 问题1: 订单删除接口错误

**测试ID**: TC-OM-005
**问题描述**: 订单删除接口返回500内部错误
**严重程度**: 中等
**建议修复**: 检查数据库事务处理逻辑
```

---

## 六、验收标准

### 6.1 功能验收标准

- [ ] 健康检查接口返回状态 "UP"
- [ ] 订单创建接口成功创建订单并返回订单ID
- [ ] 订单查询接口正确返回订单数据
- [ ] 订单修改接口成功修改订单数据
- [ ] 订单删除接口成功删除订单
- [ ] 订单状态流转逻辑正确
- [ ] 订单列表查询接口正确返回订单列表
- [ ] 订单与库存集成功能正确

### 6.2 性能验收标准

- [ ] API响应时间 P99 ≤ 500ms
- [ ] 错误率 ≤ 1%
- [ ] 并发处理能力 ≥ 100 QPS

---

**文档版本**: v1.0
**最后更新**: 2026-04-27
**维护者**: qa-lead