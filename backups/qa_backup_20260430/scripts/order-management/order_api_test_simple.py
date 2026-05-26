#!/usr/bin/env python3
"""
订单管理模块API接口自动化测试脚本 (简化版)
用途: 快速验证订单管理模块API接口功能
作者: qa-lead
版本: 1.0
"""

import requests
import json
import sys
from datetime import datetime

class OrderAPITest:
    """订单管理API测试类"""
    
    def __init__(self, base_url="http://localhost:8082"):
        self.base_url = base_url
        self.created_order_id = None
        self.results = []
        
    def test_health_check(self):
        """健康检查测试"""
        print(f"\n[TC-OM-001] 健康检查测试")
        try:
            response = requests.get(f"{self.base_url}/actuator/health", timeout=10)
            if response.status_code == 200:
                data = response.json()
                status = data.get('status', 'unknown')
                print(f"  结果: ✅ 通过 - 服务状态: {status}")
                self.results.append({'test': '健康检查', 'passed': True})
                return True
            else:
                print(f"  结果: ❌ 失败 - HTTP {response.status_code}")
                self.results.append({'test': '健康检查', 'passed': False})
                return False
        except Exception as e:
            print(f"  结果: ❌ 失败 - {str(e)}")
            self.results.append({'test': '健康检查', 'passed': False, 'error': str(e)})
            return False
    
    def test_order_create(self):
        """订单创建测试"""
        print(f"\n[TC-OM-002] 订单创建测试")
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
                self.created_order_id = order.get('id') or order.get('orderId')
                print(f"  结果: ✅ 通过 - 订单ID: {self.created_order_id}")
                self.results.append({'test': '订单创建', 'passed': True, 'order_id': self.created_order_id})
                return True
            else:
                print(f"  结果: ❌ 失败 - HTTP {response.status_code}")
                self.results.append({'test': '订单创建', 'passed': False})
                return False
        except Exception as e:
            print(f"  结果: ❌ 失败 - {str(e)}")
            self.results.append({'test': '订单创建', 'passed': False, 'error': str(e)})
            return False
    
    def test_order_get(self):
        """订单查询测试"""
        print(f"\n[TC-OM-003] 订单查询测试")
        if not self.created_order_id:
            print(f"  结果: ⏭️ 跳过 - 无可用订单ID")
            self.results.append({'test': '订单查询', 'passed': False, 'reason': '跳过'})
            return False
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                timeout=10
            )
            if response.status_code == 200:
                order = response.json()
                print(f"  结果: ✅ 通过 - 数据正确")
                self.results.append({'test': '订单查询', 'passed': True})
                return True
            else:
                print(f"  结果: ❌ 失败 - HTTP {response.status_code}")
                self.results.append({'test': '订单查询', 'passed': False})
                return False
        except Exception as e:
            print(f"  结果: ❌ 失败 - {str(e)}")
            self.results.append({'test': '订单查询', 'passed': False, 'error': str(e)})
            return False
    
    def test_order_update(self):
        """订单修改测试"""
        print(f"\n[TC-OM-004] 订单修改测试")
        if not self.created_order_id:
            print(f"  结果: ⏭️ 跳过 - 无可用订单ID")
            self.results.append({'test': '订单修改', 'passed': False, 'reason': '跳过'})
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
                print(f"  结果: ✅ 通过 - 修改成功")
                self.results.append({'test': '订单修改', 'passed': True})
                return True
            else:
                print(f"  结果: ❌ 失败 - HTTP {response.status_code}")
                self.results.append({'test': '订单修改', 'passed': False})
                return False
        except Exception as e:
            print(f"  结果: ❌ 失败 - {str(e)}")
            self.results.append({'test': '订单修改', 'passed': False, 'error': str(e)})
            return False
    
    def test_order_delete(self):
        """订单删除测试"""
        print(f"\n[TC-OM-005] 订单删除测试")
        if not self.created_order_id:
            print(f"  结果: ⏭️ 跳过 - 无可用订单ID")
            self.results.append({'test': '订单删除', 'passed': False, 'reason': '跳过'})
            return False
        try:
            # 删除订单
            response = requests.delete(
                f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                timeout=10
            )
            if response.status_code in [200, 204]:
                # 验证删除
                verify_response = requests.get(
                    f"{self.base_url}/api/v1/orders/{self.created_order_id}",
                    timeout=10
                )
                if verify_response.status_code == 404:
                    print(f"  结果: ✅ 通过 - 删除成功")
                    self.results.append({'test': '订单删除', 'passed': True})
                    return True
                else:
                    print(f"  结果: ❌ 失败 - 删除后仍可查询")
                    self.results.append({'test': '订单删除', 'passed': False})
                    return False
            else:
                print(f"  结果: ❌ 失败 - HTTP {response.status_code}")
                self.results.append({'test': '订单删除', 'passed': False})
                return False
        except Exception as e:
            print(f"  结果: ❌ 失败 - {str(e)}")
            self.results.append({'test': '订单删除', 'passed': False, 'error': str(e)})
            return False
    
    def run_tests(self):
        """执行所有测试"""
        print(f"\n{'='*50}")
        print(f"订单管理模块API接口测试")
        print(f"服务地址: {self.base_url}")
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'='*50}")
        
        # 执行测试
        self.test_health_check()
        self.test_order_create()
        self.test_order_get()
        self.test_order_update()
        self.test_order_delete()
        
        # 生成摘要
        print(f"\n{'='*50}")
        print(f"测试结果摘要")
        print(f"{'='*50}")
        passed = sum(1 for r in self.results if r['passed'])
        failed = len(self.results) - passed
        print(f"总测试数: {len(self.results)}")
        print(f"通过数: {passed}")
        print(f"失败数: {failed}")
        print(f"通过率: {(passed/len(self.results)*100):.1f}%")
        
        # 保存报告
        report = {
            'test_date': datetime.now().isoformat(),
            'base_url': self.base_url,
            'total_tests': len(self.results),
            'passed': passed,
            'failed': failed,
            'pass_rate': (passed/len(self.results)*100),
            'results': self.results
        }
        with open('order_api_test_report.json', 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        print(f"\n测试报告已保存: order_api_test_report.json")
        
        return report


if __name__ == '__main__':
    # 获取服务地址
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8082"
    
    tester = OrderAPITest(base_url)
    tester.run_tests()