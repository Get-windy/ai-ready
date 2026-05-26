"""
RESTful API自动化测试脚本
为Sprint 27+1测试环境配置RESTful API接口测试
"""

import sys
import os
import time
import json
from typing import Dict, List, Any, Optional

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config, generate_test_data
from test_utils import logger, api_client, data_generator, validator


class RESTfulAPITests:
    """RESTful API测试类"""
    
    def __init__(self):
        self.test_results = []
        self.created_resources = []  # 跟踪创建的测试资源，用于清理
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有RESTful API测试"""
        logger.log_test_start("RESTful API 完整测试套件")
        
        test_suite = {
            "start_time": time.time(),
            "tests": [],
            "summary": {
                "total": 0,
                "passed": 0,
                "failed": 0,
                "warnings": 0
            }
        }
        
        # 运行身份认证测试
        auth_result = self.test_authentication()
        test_suite["tests"].append(auth_result)
        
        if auth_result["status"] == "passed":
            # 只有认证通过后才运行其他测试
            tests = [
                self.test_health_endpoint,
                self.test_users_endpoint,
                self.test_products_endpoint,
                self.test_orders_endpoint,
                self.test_error_handling,
                self.test_performance,
                self.test_pagination,
                self.test_search_filtering,
                self.test_validation,
                self.test_concurrent_requests,
            ]
            
            for test_func in tests:
                result = test_func()
                test_suite["tests"].append(result)
        
        # 清理测试数据
        self.cleanup_test_data()
        
        # 计算统计信息
        for test in test_suite["tests"]:
            test_suite["summary"]["total"] += 1
            if test["status"] == "passed":
                test_suite["summary"]["passed"] += 1
            elif test["status"] == "failed":
                test_suite["summary"]["failed"] += 1
            elif test["status"] == "warning":
                test_suite["summary"]["warnings"] += 1
        
        test_suite["end_time"] = time.time()
        test_suite["duration"] = test_suite["end_time"] - test_suite["start_time"]
        
        logger.log_test_pass("RESTful API 测试套件完成", 
                           f"通过: {test_suite['summary']['passed']}, "
                           f"失败: {test_suite['summary']['failed']}, "
                           f"警告: {test_suite['summary']['warnings']}")
        
        return test_suite
    
    def test_authentication(self) -> Dict[str, Any]:
        """测试身份认证"""
        test_name = "身份认证测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            start_time = time.time()
            
            # 测试认证
            if api_client.authenticate():
                result["status"] = "passed"
                result["details"] = "成功获取认证令牌"
            else:
                result["details"] = "认证失败，检查用户名密码配置"
                logger.log_test_fail(test_name, "认证失败")
            
            result["response_time"] = (time.time() - start_time) * 1000
            
        except Exception as e:
            result["details"] = f"认证异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_health_endpoint(self) -> Dict[str, Any]:
        """测试健康检查端点"""
        test_name = "健康检查端点测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_endpoint("health")
            start_time = time.time()
            
            response = api_client.get(endpoint)
            response_time = (time.time() - start_time) * 1000
            
            # 验证响应
            if validator.validate_response_status(response, 200):
                health_data = response.json()
                if health_data.get("status") == "UP":
                    result["status"] = "passed"
                    result["details"] = f"服务健康状态: {health_data.get('status')}"
                    logger.log_test_pass(test_name, f"响应时间: {response_time:.2f}ms")
                else:
                    result["details"] = f"服务状态异常: {health_data}"
                    logger.log_test_warning(test_name, f"服务状态: {health_data.get('status')}")
            else:
                result["details"] = f"HTTP状态码异常: {response.status_code}"
                logger.log_test_fail(test_name, f"状态码: {response.status_code}")
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"健康检查异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_users_endpoint(self) -> Dict[str, Any]:
        """测试用户相关端点"""
        test_name = "用户端点测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0,
            "subtests": []
        }
        
        try:
            users_endpoint = config.get_endpoint("users")
            
            # 子测试1: 获取用户列表
            subtests = []
            
            # 获取现有用户
            start_time = time.time()
            response = api_client.get(users_endpoint)
            get_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                subtests.append({
                    "name": "获取用户列表",
                    "status": "passed",
                    "response_time": get_time
                })
            else:
                subtests.append({
                    "name": "获取用户列表",
                    "status": "failed",
                    "response_time": get_time,
                    "details": f"状态码: {response.status_code}"
                })
            
            # 子测试2: 创建新用户
            user_data = data_generator.generate_user_data()
            start_time = time.time()
            response = api_client.post(users_endpoint, user_data)
            create_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 201):
                created_user = response.json()
                user_id = created_user.get("id")
                if user_id:
                    self.created_resources.append(("user", user_id))
                    subtests.append({
                        "name": "创建用户",
                        "status": "passed",
                        "response_time": create_time,
                        "user_id": user_id
                    })
                    
                    # 子测试3: 获取特定用户
                    user_endpoint = f"{users_endpoint}/{user_id}"
                    start_time = time.time()
                    response = api_client.get(user_endpoint)
                    get_single_time = (time.time() - start_time) * 1000
                    
                    if validator.validate_response_status(response, 200):
                        subtests.append({
                            "name": "获取特定用户",
                            "status": "passed",
                            "response_time": get_single_time
                        })
                    else:
                        subtests.append({
                            "name": "获取特定用户",
                            "status": "failed",
                            "response_time": get_single_time,
                            "details": f"状态码: {response.status_code}"
                        })
                    
                    # 子测试4: 更新用户
                    update_data = {"first_name": "UpdatedName"}
                    start_time = time.time()
                    response = api_client.put(user_endpoint, update_data)
                    update_time = (time.time() - start_time) * 1000
                    
                    if validator.validate_response_status(response, 200):
                        subtests.append({
                            "name": "更新用户",
                            "status": "passed",
                            "response_time": update_time
                        })
                    else:
                        subtests.append({
                            "name": "更新用户",
                            "status": "failed",
                            "response_time": update_time,
                            "details": f"状态码: {response.status_code}"
                        })
                else:
                    subtests.append({
                        "name": "创建用户",
                        "status": "warning",
                        "response_time": create_time,
                        "details": "响应中未找到用户ID"
                    })
            else:
                subtests.append({
                    "name": "创建用户",
                    "status": "failed",
                    "response_time": create_time,
                    "details": f"状态码: {response.status_code}"
                })
            
            # 评估整体测试结果
            passed_count = sum(1 for st in subtests if st["status"] == "passed")
            failed_count = sum(1 for st in subtests if st["status"] == "failed")
            
            if failed_count == 0:
                result["status"] = "passed"
            elif passed_count > 0:
                result["status"] = "warning"
            
            result["details"] = f"子测试: {passed_count}通过, {failed_count}失败"
            result["subtests"] = subtests
            result["response_time"] = sum(st["response_time"] for st in subtests)
            
            logger.log_test_pass(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"用户端点测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_products_endpoint(self) -> Dict[str, Any]:
        """测试产品相关端点"""
        test_name = "产品端点测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",  # 默认警告，因为可能依赖产品服务
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_endpoint("products")
            
            # 测试获取产品列表
            start_time = time.time()
            response = api_client.get(endpoint)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                products = response.json()
                if isinstance(products, list):
                    result["status"] = "passed"
                    result["details"] = f"成功获取{len(products)}个产品"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "响应格式不正确"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"产品端点异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_orders_endpoint(self) -> Dict[str, Any]:
        """测试订单相关端点"""
        test_name = "订单端点测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_endpoint("orders")
            
            # 测试获取订单列表
            start_time = time.time()
            response = api_client.get(endpoint)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                orders = response.json()
                if isinstance(orders, list):
                    result["status"] = "passed"
                    result["details"] = f"成功获取{len(orders)}个订单"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "响应格式不正确"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"订单端点异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_error_handling(self) -> Dict[str, Any]:
        """测试错误处理"""
        test_name = "错误处理测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 测试不存在的资源
            nonexistent_endpoint = f"{config.get_endpoint('users')}/999999"
            response = api_client.get(nonexistent_endpoint)
            
            if validator.validate_response_status(response, 404):
                result["status"] = "passed"
                result["details"] = "404错误处理正常"
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"期望404，实际得到: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"错误处理测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_performance(self) -> Dict[str, Any]:
        """测试性能"""
        test_name = "性能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "passed",
            "details": "",
            "response_time": 0,
            "measurements": []
        }
        
        try:
            endpoint = config.get_endpoint("health")
            threshold = config.performance_thresholds["response_time_p95"]
            
            # 进行多次请求测量性能
            measurements = []
            for i in range(5):
                start_time = time.time()
                response = api_client.get(endpoint)
                response_time = (time.time() - start_time) * 1000
                
                measurements.append({
                    "iteration": i + 1,
                    "response_time_ms": round(response_time, 2),
                    "status_code": response.status_code
                })
                
                if response_time > threshold:
                    result["status"] = "warning"
                    logger.log_test_warning(test_name, f"第{i+1}次响应时间: {response_time:.2f}ms > {threshold}ms")
            
            # 计算统计信息
            response_times = [m["response_time_ms"] for m in measurements]
            avg_time = sum(response_times) / len(response_times)
            max_time = max(response_times)
            
            result["details"] = f"平均响应时间: {avg_time:.2f}ms, 最大: {max_time:.2f}ms"
            result["measurements"] = measurements
            result["response_time"] = avg_time
            
            if result["status"] == "passed":
                logger.log_test_pass(test_name, result["details"])
            else:
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["status"] = "failed"
            result["details"] = f"性能测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_pagination(self) -> Dict[str, Any]:
        """测试分页功能"""
        test_name = "分页功能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = f"{config.get_endpoint('users')}?page=1&per_page=5"
            response = api_client.get(endpoint)
            
            if validator.validate_pagination(response):
                result["status"] = "passed"
                result["details"] = "分页功能正常"
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = "分页响应格式不正确"
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"分页测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_search_filtering(self) -> Dict[str, Any]:
        """测试搜索过滤功能"""
        test_name = "搜索过滤测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 测试带查询参数的请求
            endpoint = f"{config.get_endpoint('users')}?active=true"
            response = api_client.get(endpoint)
            
            if validator.validate_response_status(response, 200):
                result["status"] = "passed"
                result["details"] = "搜索过滤功能正常"
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"搜索过滤失败，状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"搜索过滤测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_validation(self) -> Dict[str, Any]:
        """测试数据验证"""
        test_name = "数据验证测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 测试无效数据
            endpoint = config.get_endpoint("users")
            invalid_data = {"email": "invalid-email"}  # 无效的邮箱格式
            
            response = api_client.post(endpoint, invalid_data)
            
            if validator.validate_response_status(response, 400):
                result["status"] = "passed"
                result["details"] = "数据验证正常"
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"期望400验证错误，实际得到: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"数据验证测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_concurrent_requests(self) -> Dict[str, Any]:
        """测试并发请求"""
        test_name = "并发请求测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 简单并发测试（实际项目中应使用线程池）
            import threading
            
            endpoint = config.get_endpoint("health")
            results = []
            errors = []
            
            def make_request():
                try:
                    start_time = time.time()
                    response = api_client.get(endpoint)
                    response_time = (time.time() - start_time) * 1000
                    results.append({
                        "status_code": response.status_code,
                        "response_time_ms": response_time
                    })
                except Exception as e:
                    errors.append(str(e))
            
            # 创建多个线程同时请求
            threads = []
            for i in range(3):  # 小规模并发测试
                thread = threading.Thread(target=make_request)
                threads.append(thread)
                thread.start()
            
            # 等待所有线程完成
            for thread in threads:
                thread.join()
            
            if errors:
                result["details"] = f"并发测试出现{len(errors)}个错误"
                logger.log_test_warning(test_name, result["details"])
            else:
                result["status"] = "passed"
                avg_time = sum(r["response_time_ms"] for r in results) / len(results)
                result["details"] = f"并发测试完成，平均响应时间: {avg_time:.2f}ms"
                logger.log_test_pass(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"并发测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def cleanup_test_data(self):
        """清理测试创建的资源"""
        logger.log_test_start("清理测试数据")
        
        for resource_type, resource_id in self.created_resources:
            try:
                if resource_type == "user":
                    endpoint = f"{config.get_endpoint('users')}/{resource_id}"
                    response = api_client.delete(endpoint)
                    
                    if validator.validate_response_status(response, 200) or \
                       validator.validate_response_status(response, 204):
                        logger.log_test_pass("清理用户数据", f"用户ID: {resource_id}")
                    else:
                        logger.log_test_warning("清理用户数据", 
                                              f"用户ID: {resource_id}, 状态码: {response.status_code}")
            except Exception as e:
                logger.log_test_warning("清理测试数据", f"资源{resource_type} ID {resource_id}: {str(e)}")
        
        logger.log_test_pass("清理测试数据", f"清理了{len(self.created_resources)}个测试资源")


def save_test_report(results: Dict[str, Any], filename: str = None):
    """保存测试报告"""
    if filename is None:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        filename = f"restful_api_test_report_{timestamp}.json"
    
    report_path = os.path.join(config.reports_dir, filename)
    
    try:
        os.makedirs(config.reports_dir, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.log_test_pass("保存测试报告", f"报告已保存到: {report_path}")
        return report_path
    except Exception as e:
        logger.log_test_fail("保存测试报告", str(e))
        return None


def generate_html_report(results: Dict[str, Any]) -> str:
    """生成HTML格式的测试报告"""
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    
    html = f"""
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sprint 27+1 - RESTful API测试报告</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
        .header {{ text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #eee; }}
        .summary {{ display: flex; justify-content: space-around; margin-bottom: 30px; }}
        .summary-item {{ text-align: center; padding: 20px; border-radius: 8px; }}
        .passed {{ background-color: #d4edda; color: #155724; }}
        .failed {{ background-color: #f8d7da; color: #721c24; }}
        .warning {{ background-color: #fff3cd; color: #856404; }}
        .total {{ background-color: #d1ecf1; color: #0c5460; }}
        .test-result {{ margin-bottom: 15px; padding: 15px; border-radius: 5px; }}
        .test-name {{ font-weight: bold; margin-bottom: 5px; }}
        .test-details {{ font-size: 0.9em; color: #666; }}
        .timestamp {{ text-align: right; color: #888; font-size: 0.9em; margin-top: 30px; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 20px; }}
        th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #f2f2f2; }}
        tr:hover {{ background-color: #f5f5f5; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔧 Sprint 27+1 - RESTful API测试报告</h1>
            <p>测试环境: {config.base_url} | API版本: {config.api_version}</p>
        </div>
        
        <div class="summary">
            <div class="summary-item total">
                <h3>总测试数</h3>
                <h2>{results['summary']['total']}</h2>
            </div>
            <div class="summary-item passed">
                <h3>通过</h3>
                <h2>{results['summary']['passed']}</h2>
            </div>
            <div class="summary-item failed">
                <h3>失败</h3>
                <h2>{results['summary']['failed']}</h2>
            </div>
            <div class="summary-item warning">
                <h3>警告</h3>
                <h2>{results['summary']['warnings']}</h2>
            </div>
        </div>
        
        <h2>测试详情</h2>
        <table>
            <thead>
                <tr>
                    <th>测试名称</th>
                    <th>状态</th>
                    <th>响应时间</th>
                    <th>详情</th>
                </tr>
            </thead>
            <tbody>
"""
    
    for test in results['tests']:
        status_class = test['status']
        status_icon = {
            'passed': '✅',
            'failed': '❌',
            'warning': '⚠️'
        }.get(status_class, '❓')
        
        html += f"""
                <tr>
                    <td><strong>{test['name']}</strong></td>
                    <td><span class="{status_class}">{status_icon} {status_class.upper()}</span></td>
                    <td>{test.get('response_time', 0):.2f} ms</td>
                    <td>{test.get('details', '')}</td>
                </tr>
"""
    
    html += f"""
            </tbody>
        </table>
        
        <div class="timestamp">
            <p>测试执行时间: {results['duration']:.2f} 秒</p>
            <p>报告生成时间: {timestamp}</p>
        </div>
    </div>
</body>
</html>
"""
    
    return html


def main():
    """主函数"""
    print("=" * 60)
    print("🔧 Sprint 27+1 - RESTful API自动化测试")
    print("=" * 60)
    print(f"测试环境: {config.base_url}")
    print(f"开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 创建测试实例并运行测试
    tester = RESTfulAPITests()
    results = tester.run_all_tests()
    
    # 保存JSON报告
    json_report_path = save_test_report(results)
    
    # 生成并保存HTML报告
    html_content = generate_html_report(results)
    if html_content:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        html_report_path = os.path.join(config.reports_dir, f"restful_api_test_report_{timestamp}.html")
        
        try:
            with open(html_report_path, 'w', encoding='utf-8') as f:
                f.write(html_content)
            print(f"📊 HTML报告已保存: {html_report_path}")
        except Exception as e:
            print(f"❌ 保存HTML报告失败: {str(e)}")
    
    # 输出总结
    print()
    print("=" * 60)
    print("📋 测试总结")
    print("=" * 60)
    print(f"总测试数: {results['summary']['total']}")
    print(f"通过: {results['summary']['passed']} ✅")
    print(f"失败: {results['summary']['failed']} ❌")
    print(f"警告: {results['summary']['warnings']} ⚠️")
    print(f"总耗时: {results['duration']:.2f} 秒")
    
    if results['summary']['failed'] == 0:
        print("🎉 所有关键测试通过！")
        return 0
    else:
        print("⚠️  存在失败的测试，需要检查")
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)