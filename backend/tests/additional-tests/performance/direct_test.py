#!/usr/bin/env python3
"""
直接运行性能测试，避免编码和子进程问题
"""

import os
import sys
import time
import threading
import requests
from datetime import datetime

# 导入性能测试模块
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

def run_mock_service():
    """运行Mock服务"""
    print("Starting Mock API Gateway service...")
    mock_service_path = os.path.join(os.path.dirname(__file__), "mock_services", "mock_api_gateway.py")
    
    # 直接导入并运行mock服务
    import importlib.util
    spec = importlib.util.spec_from_file_location("mock_api_gateway", mock_service_path)
    mock_module = importlib.util.module_from_spec(spec)
    
    # 在单独的线程中运行mock服务
    def run_mock():
        spec.loader.exec_module(mock_module)
    
    mock_thread = threading.Thread(target=run_mock, daemon=True)
    mock_thread.start()
    
    # 等待服务启动
    time.sleep(5)
    
    # 检查服务是否启动
    try:
        response = requests.get("http://localhost:8080/actuator/health", timeout=5)
        if response.status_code == 200:
            print("Mock service started successfully on port 8080")
            return True
        else:
            print(f"Mock service returned status code: {response.status_code}")
            return False
    except Exception as e:
        print(f"Failed to connect to mock service: {e}")
        return False

def run_basic_performance_test():
    """运行基本的性能测试"""
    print("\nRunning basic performance test...")
    
    # 简单的性能测试逻辑
    test_results = []
    
    # 测试用户登录接口
    print("Testing user login endpoint...")
    login_endpoint = "/api/v1/users/login"
    test_data = {
        "username": "test_user_1",
        "password": "test_password"
    }
    
    # 运行10次请求
    response_times = []
    success_count = 0
    total_requests = 10
    
    for i in range(total_requests):
        start_time = time.time()
        try:
            response = requests.post(f"http://localhost:8080{login_endpoint}", 
                                    json=test_data, 
                                    timeout=10)
            response_time = (time.time() - start_time) * 1000  # ms
            response_times.append(response_time)
            
            if response.status_code == 200:
                success_count += 1
                print(f"Request {i+1}: Success - {response_time:.2f}ms")
            else:
                print(f"Request {i+1}: Failed - HTTP {response.status_code}")
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            response_times.append(response_time)
            print(f"Request {i+1}: Error - {str(e)}")
    
    # 计算统计信息
    if response_times:
        avg_response_time = sum(response_times) / len(response_times)
        min_response_time = min(response_times)
        max_response_time = max(response_times)
        success_rate = (success_count / total_requests) * 100
        
        test_result = {
            "endpoint": "user_login",
            "total_requests": total_requests,
            "successful_requests": success_count,
            "success_rate": success_rate,
            "min_response_time_ms": min_response_time,
            "max_response_time_ms": max_response_time,
            "avg_response_time_ms": avg_response_time,
            "test_timestamp": datetime.now().isoformat()
        }
        
        test_results.append(test_result)
        
        print(f"\nUser Login Performance Results:")
        print(f"  Total Requests: {total_requests}")
        print(f"  Successful: {success_count}")
        print(f"  Success Rate: {success_rate:.2f}%")
        print(f"  Min Response Time: {min_response_time:.2f}ms")
        print(f"  Max Response Time: {max_response_time:.2f}ms")
        print(f"  Avg Response Time: {avg_response_time:.2f}ms")
    
    # 测试订单创建接口
    print("\nTesting order creation endpoint...")
    order_endpoint = "/api/v1/orders"
    order_data = {
        "product_id": "prod_001",
        "quantity": 2,
        "customer_id": "cust_001"
    }
    
    response_times = []
    success_count = 0
    total_requests = 10
    
    for i in range(total_requests):
        start_time = time.time()
        try:
            response = requests.post(f"http://localhost:8080{order_endpoint}", 
                                    json=order_data, 
                                    timeout=10)
            response_time = (time.time() - start_time) * 1000  # ms
            response_times.append(response_time)
            
            if response.status_code == 200:
                success_count += 1
                print(f"Request {i+1}: Success - {response_time:.2f}ms")
            else:
                print(f"Request {i+1}: Failed - HTTP {response.status_code}")
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            response_times.append(response_time)
            print(f"Request {i+1}: Error - {str(e)}")
    
    # 计算统计信息
    if response_times:
        avg_response_time = sum(response_times) / len(response_times)
        min_response_time = min(response_times)
        max_response_time = max(response_times)
        success_rate = (success_count / total_requests) * 100
        
        test_result = {
            "endpoint": "order_create",
            "total_requests": total_requests,
            "successful_requests": success_count,
            "success_rate": success_rate,
            "min_response_time_ms": min_response_time,
            "max_response_time_ms": max_response_time,
            "avg_response_time_ms": avg_response_time,
            "test_timestamp": datetime.now().isoformat()
        }
        
        test_results.append(test_result)
        
        print(f"\nOrder Creation Performance Results:")
        print(f"  Total Requests: {total_requests}")
        print(f"  Successful: {success_count}")
        print(f"  Success Rate: {success_rate:.2f}%")
        print(f"  Min Response Time: {min_response_time:.2f}ms")
        print(f"  Max Response Time: {max_response_time:.2f}ms")
        print(f"  Avg Response Time: {avg_response_time:.2f}ms")
    
    return test_results

def generate_report(test_results):
    """生成测试报告"""
    print("\n" + "="*80)
    print("Performance Test Report")
    print("="*80)
    print(f"Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Test Environment: Mock API Gateway on localhost:8080")
    print(f"Total Endpoints Tested: {len(test_results)}")
    print("\n" + "="*80)
    
    for result in test_results:
        print(f"\nEndpoint: {result['endpoint']}")
        print(f"  Total Requests: {result['total_requests']}")
        print(f"  Successful Requests: {result['successful_requests']}")
        print(f"  Success Rate: {result['success_rate']:.2f}%")
        print(f"  Min Response Time: {result['min_response_time_ms']:.2f}ms")
        print(f"  Max Response Time: {result['max_response_time_ms']:.2f}ms")
        print(f"  Average Response Time: {result['avg_response_time_ms']:.2f}ms")
    
    # 保存报告到文件
    report_dir = os.path.join(os.path.dirname(__file__), "reports")
    os.makedirs(report_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_file = os.path.join(report_dir, f"performance_report_{timestamp}.json")
    
    import json
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump({
            "test_date": datetime.now().isoformat(),
            "test_environment": "mock_api_gateway",
            "results": test_results,
            "summary": {
                "total_endpoints_tested": len(test_results),
                "overall_success_rate": sum(r['success_rate'] for r in test_results) / len(test_results) if test_results else 0,
                "average_response_time": sum(r['avg_response_time_ms'] for r in test_results) / len(test_results) if test_results else 0
            }
        }, f, indent=2, ensure_ascii=False)
    
    print(f"\nReport saved to: {report_file}")
    print("="*80)

def main():
    """主函数"""
    print("AI-Ready Sprint 27+1 Performance Benchmark Test")
    print("Direct execution to avoid encoding issues")
    print("="*80)
    
    # 检查依赖
    print("\nChecking dependencies...")
    try:
        import requests
        print("✓ requests module available")
    except ImportError:
        print("✗ requests module not available, please install: pip install requests")
        return 1
    
    # 运行Mock服务
    if not run_mock_service():
        print("Failed to start mock service, exiting...")
        return 1
    
    try:
        # 运行性能测试
        test_results = run_basic_performance_test()
        
        if test_results:
            # 生成报告
            generate_report(test_results)
            
            print("\n" + "="*80)
            print("SUCCESS: Performance benchmark tests completed")
            print("="*80)
            return 0
        else:
            print("\n" + "="*80)
            print("ERROR: No test results generated")
            print("="*80)
            return 1
            
    except Exception as e:
        print(f"\nError during test execution: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        # 注意：Mock服务在守护线程中运行，主程序退出时会自动停止
        print("\nTest execution completed. Mock service will stop automatically.")

if __name__ == "__main__":
    sys.exit(main())