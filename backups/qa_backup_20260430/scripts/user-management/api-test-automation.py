#!/usr/bin/env python3
"""
用户管理模块API自动化测试脚本
Sprint 27+1 测试环境
"""

import requests
import json
import time
import sys
import os
from datetime import datetime
from typing import Dict, List, Optional, Tuple

class UserManagementAPITester:
    """用户管理API测试器"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        """
        初始化测试器
        
        Args:
            base_url: API基础URL
        """
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.test_results = []
        self.test_data = self._generate_test_data()
        
    def _generate_test_data(self) -> Dict:
        """生成测试数据"""
        timestamp = int(time.time())
        return {
            "users": {
                "normal": {
                    "username": f"testuser_{timestamp}",
                    "password": "Test@12345",
                    "email": f"test{timestamp}@example.com",
                    "fullName": f"测试用户_{timestamp}",
                    "phone": f"138{timestamp % 100000000:08d}"
                },
                "duplicate_username": {
                    "username": "duplicate_user",
                    "password": "Test@12345",
                    "email": f"duplicate{timestamp}@example.com",
                    "fullName": "重复用户名用户"
                },
                "duplicate_email": {
                    "username": f"dupemail_{timestamp}",
                    "password": "Test@12345",
                    "email": "duplicate@example.com",
                    "fullName": "重复邮箱用户"
                },
                "weak_password": {
                    "username": f"weakpass_{timestamp}",
                    "password": "123",
                    "email": f"weak{timestamp}@example.com",
                    "fullName": "弱密码用户"
                },
                "invalid_email": {
                    "username": f"invalidemail_{timestamp}",
                    "password": "Test@12345",
                    "email": "invalid-email",
                    "fullName": "无效邮箱用户"
                },
                "empty_fields": {
                    "username": "",
                    "password": "Test@12345",
                    "email": f"empty{timestamp}@example.com",
                    "fullName": "空字段用户"
                }
            },
            "login": {
                "normal": {
                    "username": "",  # 将在测试中设置
                    "password": "Test@12345"
                },
                "wrong_password": {
                    "username": "",  # 将在测试中设置
                    "password": "WrongPassword123"
                }
            }
        }
    
    def _record_result(self, test_name: str, passed: bool, 
                      details: str = "", response=None):
        """记录测试结果"""
        result = {
            "test_name": test_name,
            "timestamp": datetime.now().isoformat(),
            "passed": passed,
            "details": details,
            "response_status": response.status_code if response else None,
            "response_body": response.json() if response and response.text else None
        }
        self.test_results.append(result)
        
        status = "✓ PASS" if passed else "✗ FAIL"
        print(f"{status} {test_name}")
        if details:
            print(f"  Details: {details}")
        if response and not passed:
            print(f"  Status: {response.status_code}")
            if response.text:
                try:
                    print(f"  Response: {response.json()}")
                except:
                    print(f"  Response: {response.text[:200]}")
        print()
        
        return passed
    
    def test_health_check(self) -> bool:
        """测试健康检查接口"""
        test_name = "健康检查"
        try:
            response = self.session.get(f"{self.base_url}/actuator/health", timeout=5)
            if response.status_code == 200:
                return self._record_result(test_name, True, "服务健康状态正常", response)
            else:
                return self._record_result(test_name, False, f"服务不健康: {response.status_code}", response)
        except Exception as e:
            return self._record_result(test_name, False, f"连接失败: {str(e)}")
    
    def test_create_user_normal(self) -> bool:
        """测试正常创建用户"""
        test_name = "创建用户-正常流程"
        user_data = self.test_data["users"]["normal"]
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/users",
                json=user_data,
                timeout=10
            )
            
            if response.status_code == 201:
                data = response.json()
                if data.get("success") and data.get("message") == "用户创建成功":
                    # 保存用户ID供后续测试使用
                    if "data" in data:
                        self.created_user = data["data"]
                        # 更新登录测试数据中的用户名
                        self.test_data["login"]["normal"]["username"] = user_data["username"]
                        self.test_data["login"]["wrong_password"]["username"] = user_data["username"]
                    return self._record_result(test_name, True, "用户创建成功", response)
                else:
                    return self._record_result(test_name, False, "响应格式不正确", response)
            else:
                return self._record_result(test_name, False, f"状态码不正确: {response.status_code}", response)
                
        except Exception as e:
            return self._record_result(test_name, False, f"请求失败: {str(e)}")
    
    def test_create_user_duplicate_username(self) -> bool:
        """测试重复用户名创建"""
        test_name = "创建用户-重复用户名"
        user_data = self.test_data["users"]["duplicate_username"]
        
        # 先创建一个用户
        self.session.post(f"{self.base_url}/api/v1/users", json=user_data, timeout=5)
        
        # 再尝试用相同用户名创建
        response = self.session.post(
            f"{self.base_url}/api/v1/users",
            json=user_data,
            timeout=5
        )
        
        if response.status_code == 400:
            data = response.json()
            if data.get("success") == False and "用户名" in data.get("message", ""):
                return self._record_result(test_name, True, "重复用户名被正确拒绝", response)
            else:
                return self._record_result(test_name, False, "错误消息不匹配", response)
        else:
            return self._record_result(test_name, False, f"应该返回400但返回了{response.status_code}", response)
    
    def test_create_user_weak_password(self) -> bool:
        """测试弱密码创建"""
        test_name = "创建用户-弱密码"
        user_data = self.test_data["users"]["weak_password"]
        
        response = self.session.post(
            f"{self.base_url}/api/v1/users",
            json=user_data,
            timeout=5
        )
        
        if response.status_code == 400:
            data = response.json()
            if data.get("success") == False and "密码" in data.get("message", ""):
                return self._record_result(test_name, True, "弱密码被正确拒绝", response)
            else:
                return self._record_result(test_name, False, "错误消息不匹配", response)
        else:
            return self._record_result(test_name, False, f"应该返回400但返回了{response.status_code}", response)
    
    def test_user_login_normal(self) -> bool:
        """测试正常用户登录"""
        test_name = "用户登录-正常流程"
        
        # 确保有已创建的用户
        if not hasattr(self, 'created_user'):
            self._record_result(test_name, False, "没有可用的测试用户")
            return False
        
        login_data = self.test_data["login"]["normal"]
        login_data["username"] = self.created_user.get("username", "")
        
        response = self.session.post(
            f"{self.base_url}/api/v1/auth/login",
            json=login_data,
            timeout=5
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("message") == "登录成功":
                # 保存token供后续测试使用
                if "data" in data and "token" in data["data"]:
                    self.auth_token = data["data"]["token"]
                return self._record_result(test_name, True, "登录成功", response)
            else:
                return self._record_result(test_name, False, "登录响应格式不正确", response)
        else:
            return self._record_result(test_name, False, f"登录失败: {response.status_code}", response)
    
    def test_user_login_wrong_password(self) -> bool:
        """测试错误密码登录"""
        test_name = "用户登录-错误密码"
        
        if not hasattr(self, 'created_user'):
            self._record_result(test_name, False, "没有可用的测试用户")
            return False
        
        login_data = self.test_data["login"]["wrong_password"]
        login_data["username"] = self.created_user.get("username", "")
        
        response = self.session.post(
            f"{self.base_url}/api/v1/auth/login",
            json=login_data,
            timeout=5
        )
        
        if response.status_code == 401:
            data = response.json()
            if data.get("success") == False and "密码" in data.get("message", ""):
                return self._record_result(test_name, True, "错误密码被正确拒绝", response)
            else:
                return self._record_result(test_name, False, "错误消息不匹配", response)
        else:
            return self._record_result(test_name, False, f"应该返回401但返回了{response.status_code}", response)
    
    def test_get_user_by_id(self) -> bool:
        """测试根据ID获取用户"""
        test_name = "获取用户-根据ID"
        
        if not hasattr(self, 'created_user'):
            self._record_result(test_name, False, "没有可用的测试用户")
            return False
        
        user_id = self.created_user.get("id")
        if not user_id:
            self._record_result(test_name, False, "用户ID不存在")
            return False
        
        response = self.session.get(
            f"{self.base_url}/api/v1/users/{user_id}",
            timeout=5
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and data.get("data"):
                return self._record_result(test_name, True, "用户信息获取成功", response)
            else:
                return self._record_result(test_name, False, "响应格式不正确", response)
        else:
            return self._record_result(test_name, False, f"获取用户失败: {response.status_code}", response)
    
    def test_get_user_list(self) -> bool:
        """测试获取用户列表"""
        test_name = "获取用户-列表"
        
        response = self.session.get(
            f"{self.base_url}/api/v1/users",
            params={"page": 0, "size": 10},
            timeout=5
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success") and "data" in data:
                return self._record_result(test_name, True, "用户列表获取成功", response)
            else:
                return self._record_result(test_name, False, "响应格式不正确", response)
        else:
            return self._record_result(test_name, False, f"获取用户列表失败: {response.status_code}", response)
    
    def run_all_tests(self) -> Dict:
        """运行所有测试"""
        print("=" * 60)
        print("用户管理模块API自动化测试")
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"测试环境: {self.base_url}")
        print("=" * 60)
        print()
        
        # 运行测试
        tests = [
            self.test_health_check,
            self.test_create_user_normal,
            self.test_create_user_duplicate_username,
            self.test_create_user_weak_password,
            self.test_user_login_normal,
            self.test_user_login_wrong_password,
            self.test_get_user_by_id,
            self.test_get_user_list,
        ]
        
        passed_count = 0
        for test_func in tests:
            if test_func():
                passed_count += 1
        
        # 生成测试报告
        total_tests = len(tests)
        failed_count = total_tests - passed_count
        success_rate = (passed_count / total_tests * 100) if total_tests > 0 else 0
        
        print("=" * 60)
        print("测试总结")
        print("=" * 60)
        print(f"总测试数: {total_tests}")
        print(f"通过数: {passed_count}")
        print(f"失败数: {failed_count}")
        print(f"成功率: {success_rate:.2f}%")
        print()
        
        # 生成详细报告
        report = {
            "summary": {
                "total_tests": total_tests,
                "passed": passed_count,
                "failed": failed_count,
                "success_rate": success_rate,
                "test_time": datetime.now().isoformat(),
                "environment": self.base_url
            },
            "details": self.test_results
        }
        
        return report
    
    def generate_html_report(self, report: Dict, output_file: str = "test-report.html"):
        """生成HTML测试报告"""
        html_template = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>用户管理模块测试报告</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
                .summary { background: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
                .test-result { margin: 10px 0; padding: 10px; border-left: 4px solid #ccc; }
                .passed { border-left-color: #4CAF50; background: #f1f8e9; }
                .failed { border-left-color: #f44336; background: #ffebee; }
                .test-name { font-weight: bold; }
                .test-details { color: #666; font-size: 0.9em; }
                .status { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 0.8em; }
                .status-passed { background: #4CAF50; color: white; }
                .status-failed { background: #f44336; color: white; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>用户管理模块测试报告</h1>
                <p>测试时间: {test_time}</p>
                <p>测试环境: {environment}</p>
            </div>
            
            <div class="summary">
                <h2>测试概览</h2>
                <p>总测试数: {total_tests}</p>
                <p>通过数: {passed} (<span style="color: #4CAF50; font-weight: bold;">{success_rate}%</span>)</p>
                <p>失败数: {failed}</p>
            </div>
            
            <h2>详细测试结果</h2>
            {test_details}
        </body>
        </html>
        """
        
        # 生成测试详情HTML
        test_details_html = ""
        for result in report["details"]:
            status_class = "passed" if result["passed"] else "failed"
            status_text = "PASS" if result["passed"] else "FAIL"
            status_span_class = "status-passed" if result["passed"] else "status-failed"
            
            details = f"<div class='test-details'>{result['details']}</div>"
            if result["response_status"]:
                details += f"<div class='test-details'>状态码: {result['response_status']}</div>"
            
            test_details_html += f"""
            <div class="test-result {status_class}">
                <div class="test-name">
                    <span class="status {status_span_class}">{status_text}</span>
                    {result['test_name']}
                </div>
                {details}
            </div>
            """
        
        # 填充模板
        html_content = html_template.format(
            test_time=report["summary"]["test_time"],
            environment=report["summary"]["environment"],
            total_tests=report["summary"]["total_tests"],
            passed=report["summary"]["passed"],
            failed=report["summary"]["failed"],
            success_rate=f"{report['summary']['success_rate']:.2f}",
            test_details=test_details_html
        )
        
        # 写入文件
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        print(f"HTML报告已生成: {output_file}")

def main():
    """主函数"""
    # 解析命令行参数
    import argparse
    parser = argparse.ArgumentParser(description='用户管理模块API自动化测试')
    parser.add_argument('--url', default='http://localhost:8080', help='API基础URL')
    parser.add_argument('--output', default='test-report.html', help='输出报告文件')
    args = parser.parse_args()
    
    # 运行测试
    tester = UserManagementAPITester(base_url=args.url)
    report = tester.run_all_tests()
    
    # 生成报告
    tester.generate_html_report(report, args.output)
    
    # 保存JSON报告
    json_file = args.output.replace('.html', '.json')
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"JSON报告已生成: {json_file}")
    
    # 根据测试结果退出
    if report["summary"]["success_rate"] >= 90:
        print("测试通过!")
        sys.exit(0)
    else:
        print("测试失败!")
        sys.exit(1)

if __name__ == "__main__":
    main()