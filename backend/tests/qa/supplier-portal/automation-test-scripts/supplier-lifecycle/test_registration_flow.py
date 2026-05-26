#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
供应商注册流程端到端测试脚本
场景ID: SC-001: 新供应商自助注册完整流程
"""

import sys
import os
import time
import json
import pytest
from typing import Dict, List, Tuple

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from utils.api_client import APIClient
from utils.db_helper import DBHelper
from utils.assertion_helper import AssertionHelper
from data.test_data_generator import TestDataGenerator


class TestSupplierRegistrationFlow:
    """供应商注册流程测试类"""
    
    def setup_method(self):
        """测试方法前置设置"""
        # 初始化工具类
        self.api_client = APIClient()
        self.db_helper = DBHelper()
        self.assertion = AssertionHelper()
        self.data_generator = TestDataGenerator()
        
        # 生成测试数据
        self.test_supplier = self.data_generator.generate_supplier_data()
        
        # 测试执行记录
        self.execution_records = []
        
        print(f"测试开始: {self.__class__.__name__}")
    
    def teardown_method(self):
        """测试方法后置清理"""
        # 清理测试数据
        self._cleanup_test_data()
        
        # 生成测试报告
        self._generate_test_report()
        
        print(f"测试结束: {self.__class__.__name__}")
    
    def test_complete_registration_flow(self):
        """测试完整供应商注册流程"""
        # 步骤1: 供应商访问注册页面
        registration_form = self._get_registration_form()
        self._record_step("访问注册页面", "获取注册表单成功", True)
        
        # 步骤2: 填写注册信息
        registration_data = self._fill_registration_form(self.test_supplier)
        self._record_step("填写注册信息", f"生成注册数据: {registration_data['companyName']}", True)
        
        # 步骤3: 提交注册申请
        response = self.api_client.post("/api/supplier/register", registration_data)
        registration_result = response.json()
        
        # 验证注册提交结果
        self.assertion.assert_status_code(response, 200)
        self.assertion.assert_json_contains(registration_result, "registrationId")
        self.assertion.assert_json_contains(registration_result, "status")
        self._record_step("提交注册申请", f"注册ID: {registration_result['registrationId']}", True)
        
        # 步骤4: 验证注册记录保存
        registration_id = registration_result['registrationId']
        db_record = self.db_helper.query_supplier_registration(registration_id)
        self.assertion.assert_not_none(db_record, "注册记录应保存到数据库")
        self.assertion.assert_equal(db_record['company_name'], registration_data['companyName'], "公司名称应一致")
        self._record_step("验证注册记录", "数据库记录保存成功", True)
        
        # 步骤5: 模拟审核流程
        audit_result = self._simulate_audit_process(registration_id)
        self.assertion.assert_equal(audit_result['status'], 'APPROVED', "审核状态应为已批准")
        self._record_step("审核流程", f"审核结果: {audit_result['status']}", True)
        
        # 步骤6: 验证供应商账户激活
        activation_result = self.api_client.get(f"/api/supplier/{registration_id}/activation")
        self.assertion.assert_status_code(activation_result, 200)
        activation_data = activation_result.json()
        self.assertion.assert_equal(activation_data['status'], 'ACTIVE', "供应商账户应激活")
        self._record_step("账户激活", f"账户状态: {activation_data['status']}", True)
        
        # 步骤7: 验证登录功能
        login_result = self._test_supplier_login(registration_data)
        self.assertion.assert_status_code(login_result, 200)
        self.assertion.assert_json_contains(login_result.json(), "accessToken")
        self._record_step("登录验证", "登录成功，获取访问令牌", True)
        
        # 步骤8: 验证供应商信息展示
        supplier_info = self.api_client.get(f"/api/supplier/{registration_id}").json()
        expected_fields = ['id', 'companyName', 'status', 'contactInfo', 'createdAt']
        for field in expected_fields:
            self.assertion.assert_json_contains(supplier_info, field)
        self._record_step("信息展示", "供应商信息完整展示", True)
        
        # 步骤9: 生成详细测试报告
        test_result = {
            "test_scenario": "完整供应商注册流程",
            "registration_id": registration_id,
            "supplier_name": registration_data['companyName'],
            "total_steps": 9,
            "passed_steps": 9,
            "execution_time": time.time(),
            "status": "PASSED"
        }
        
        return test_result
    
    def test_registration_form_validation(self):
        """测试注册表单验证"""
        test_cases = [
            {
                "name": "空公司名称",
                "data": {"companyName": "", "contact": "张三"},
                "expected_error": "公司名称不能为空"
            },
            {
                "name": "非法邮箱格式",
                "data": {"companyName": "测试公司", "contact": "张三", "email": "invalid-email"},
                "expected_error": "邮箱格式不正确"
            },
            {
                "name": "密码太短",
                "data": {"companyName": "测试公司", "contact": "张三", "password": "123"},
                "expected_error": "密码至少6位"
            },
            {
                "name": "缺少必填字段",
                "data": {"companyName": "测试公司"},
                "expected_error": "缺少必填字段"
            }
        ]
        
        validation_results = []
        
        for test_case in test_cases:
            response = self.api_client.post("/api/supplier/register", test_case['data'])
            
            # 验证响应状态和错误信息
            self.assertion.assert_status_code(response, 400)
            error_response = response.json()
            self.assertion.assert_json_contains(error_response, "error")
            
            validation_result = {
                "test_case": test_case['name'],
                "status": "PASSED" if test_case['expected_error'] in error_response.get('error', '') else "FAILED",
                "expected": test_case['expected_error'],
                "actual": error_response.get('error', ''),
                "response_time": response.elapsed.total_seconds()
            }
            
            validation_results.append(validation_result)
        
        return validation_results
    
    def test_concurrent_registration(self):
        """测试并发注册场景"""
        concurrent_users = 10
        test_suppliers = [self.data_generator.generate_supplier_data() for _ in range(concurrent_users)]
        
        import threading
        import queue
        
        results_queue = queue.Queue()
        
        def worker(supplier_data, index):
            """并发注册工作线程"""
            try:
                response = self.api_client.post("/api/supplier/register", supplier_data)
                results_queue.put({
                    "worker": index,
                    "status_code": response.status_code,
                    "success": response.status_code == 200,
                    "response_time": response.elapsed.total_seconds()
                })
            except Exception as e:
                results_queue.put({
                    "worker": index,
                    "success": False,
                    "error": str(e)
                })
        
        # 启动并发线程
        threads = []
        for i, supplier_data in enumerate(test_suppliers):
            thread = threading.Thread(target=worker, args=(supplier_data, i))
            threads.append(thread)
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 收集结果
        results = []
        while not results_queue.empty():
            results.append(results_queue.get())
        
        # 分析并发测试结果
        success_count = sum(1 for r in results if r.get('success', False))
        total_count = len(results)
        
        concurrent_result = {
            "concurrent_users": concurrent_users,
            "total_requests": total_count,
            "successful_requests": success_count,
            "success_rate": (success_count / total_count) * 100 if total_count > 0 else 0,
            "average_response_time": sum(r.get('response_time', 0) for r in results) / total_count if total_count > 0 else 0,
            "max_response_time": max(r.get('response_time', 0) for r in results) if results else 0
        }
        
        return concurrent_result
    
    def _get_registration_form(self) -> Dict:
        """获取注册表单"""
        response = self.api_client.get("/api/supplier/registration-form")
        return response.json()
    
    def _fill_registration_form(self, supplier_data: Dict) -> Dict:
        """填写注册表单"""
        return {
            "companyName": supplier_data['company_name'],
            "contact": supplier_data['contact_person'],
            "email": supplier_data['email'],
            "phone": supplier_data['phone'],
            "address": supplier_data['address'],
            "businessType": supplier_data['business_type'],
            "taxNumber": supplier_data['tax_number'],
            "password": supplier_data['password'],
            "confirmPassword": supplier_data['password']
        }
    
    def _simulate_audit_process(self, registration_id: str) -> Dict:
        """模拟审核流程"""
        # 模拟审核员登录
        audit_login = self.api_client.post("/api/auditor/login", {
            "username": "auditor_admin",
            "password": "audit_password_123"
        })
        
        # 获取待审核列表
        pending_list = self.api_client.get("/api/auditor/pending-registrations").json()
        
        # 查找当前注册申请
        registration = next((r for r in pending_list if r['id'] == registration_id), None)
        self.assertion.assert_not_none(registration, "注册申请应在待审核列表中")
        
        # 执行审核
        audit_response = self.api_client.post(f"/api/auditor/registration/{registration_id}/review", {
            "status": "APPROVED",
            "comment": "资料完整，符合要求",
            "reviewer": "auditor_admin"
        })
        
        return audit_response.json()
    
    def _test_supplier_login(self, registration_data: Dict) -> Dict:
        """测试供应商登录"""
        login_data = {
            "username": registration_data['email'],
            "password": registration_data['password']
        }
        return self.api_client.post("/api/supplier/login", login_data)
    
    def _record_step(self, step_name: str, step_detail: str, success: bool):
        """记录测试步骤"""
        step_record = {
            "step": step_name,
            "detail": step_detail,
            "success": success,
            "timestamp": time.time()
        }
        self.execution_records.append(step_record)
    
    def _cleanup_test_data(self):
        """清理测试数据"""
        try:
            # 清理测试供应商数据
            if hasattr(self, 'test_supplier') and self.test_supplier:
                self.db_helper.cleanup_test_supplier(self.test_supplier.get('email', ''))
        except Exception as e:
            print(f"清理测试数据时出错: {e}")
    
    def _generate_test_report(self):
        """生成测试报告"""
        report = {
            "test_class": self.__class__.__name__,
            "execution_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_steps": len(self.execution_records),
            "passed_steps": sum(1 for step in self.execution_records if step['success']),
            "failed_steps": sum(1 for step in self.execution_records if not step['success']),
            "step_details": self.execution_records
        }
        
        # 保存报告到文件
        report_file = f"reports/{self.__class__.__name__}_{time.strftime('%Y%m%d_%H%M%S')}.json"
        os.makedirs(os.path.dirname(report_file), exist_ok=True)
        
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"测试报告已保存到: {report_file}")
        return report


if __name__ == "__main__":
    # 直接运行测试
    test_instance = TestSupplierRegistrationFlow()
    test_instance.setup_method()
    
    try:
        result = test_instance.test_complete_registration_flow()
        print(f"测试结果: {json.dumps(result, ensure_ascii=False, indent=2)}")
    except Exception as e:
        print(f"测试执行失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        test_instance.teardown_method()