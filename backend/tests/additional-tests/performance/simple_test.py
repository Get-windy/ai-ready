#!/usr/bin/env python3
"""
简单的性能测试，无Unicode字符
"""

import os
import sys
import time
import threading
import requests
from datetime import datetime

def start_mock_service():
    """启动Mock服务"""
    print("[INFO] Starting Mock API Gateway service...")
    
    # 尝试导入和运行mock服务
    mock_service_path = os.path.join(os.path.dirname(__file__), "mock_services", "mock_api_gateway.py")
    
    try:
        # 在单独线程中运行mock服务
        import importlib.util
        spec = importlib.util.spec_from_file_location("mock_api_gateway", mock_service_path)
        mock_module = importlib.util.module_from_spec(spec)
        
        # 设置sys.argv以模拟命令行执行
        sys.argv = [mock_service_path]
        
        def run_mock():
            try:
                spec.loader.exec_module(mock_module)
            except Exception as e:
                print(f"[ERROR] Mock service error: {e}")
        
        mock_thread = threading.Thread(target=run_mock, daemon=True)
        mock_thread.start()
        
        # 等待服务启动
        time.sleep(5)
        
        # 检查服务是否运行
        try:
            response = requests.get("http://localhost:8080/actuator/health", timeout=5)
            if response.status_code == 200:
                print("[INFO] Mock service started successfully on port 8080")
                return True
            else:
                print(f"[ERROR] Mock service returned status: {response.status_code}")
                return False
        except Exception as e:
            print(f"[ERROR] Cannot connect to mock service: {e}")
            return False
            
    except Exception as e:
        print(f"[ERROR] Failed to start mock service: {e}")
        return False

def run_performance_tests():
    """运行性能测试"""
    print("\n[INFO] Running performance tests...")
    
    results = []
    
    # 测试端点
    test_cases = [
        {
            "name": "user_login",
            "endpoint": "/api/v1/users/login",
            "method": "POST",
            "data": {"username": "test_user", "password": "test_password"}
        },
        {
            "name": "order_create", 
            "endpoint": "/api/v1/orders",
            "method": "POST",
            "data": {"product_id": "prod_001", "quantity": 2, "customer_id": "cust_001"}
        },
        {
            "name": "inventory_check",
            "endpoint": "/api/v1/inventory/check",
            "method": "POST", 
            "data": {"product_id": "prod_001", "quantity": 1}
        }
    ]
    
    for test in test_cases:
        print(f"\n[TEST] Testing {test['name']}...")
        
        response_times = []
        successes = 0
        failures = 0
        total_requests = 5  # 减少请求数以加快测试
        
        for i in range(total_requests):
            start_time = time.time()
            url = f"http://localhost:8080{test['endpoint']}"
            
            try:
                if test['method'] == 'POST':
                    response = requests.post(url, json=test['data'], timeout=10)
                else:
                    response = requests.get(url, timeout=10)
                
                response_time = (time.time() - start_time) * 1000
                response_times.append(response_time)
                
                if response.status_code == 200:
                    successes += 1
                    print(f"  Request {i+1}: OK - {response_time:.1f}ms")
                else:
                    failures += 1
                    print(f"  Request {i+1}: FAIL - HTTP {response.status_code}")
                    
            except Exception as e:
                response_time = (time.time() - start_time) * 1000
                response_times.append(response_time)
                failures += 1
                print(f"  Request {i+1}: ERROR - {str(e)[:50]}")
        
        # 计算结果
        if response_times:
            result = {
                "endpoint": test['name'],
                "total_requests": total_requests,
                "successful_requests": successes,
                "failed_requests": failures,
                "success_rate": (successes / total_requests) * 100 if total_requests > 0 else 0,
                "min_response_time_ms": min(response_times) if response_times else 0,
                "max_response_time_ms": max(response_times) if response_times else 0,
                "avg_response_time_ms": sum(response_times) / len(response_times) if response_times else 0,
                "test_timestamp": datetime.now().isoformat()
            }
            results.append(result)
    
    return results

def generate_report(results):
    """生成测试报告"""
    print("\n" + "="*80)
    print("PERFORMANCE TEST REPORT")
    print("="*80)
    print(f"Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Test Environment: Mock API Gateway")
    print(f"Total Endpoints Tested: {len(results)}")
    print("="*80)
    
    overall_success_rate = 0
    overall_avg_response = 0
    
    for result in results:
        print(f"\nEndpoint: {result['endpoint']}")
        print(f"  Requests: {result['total_requests']} (Success: {result['successful_requests']}, Failed: {result['failed_requests']})")
        print(f"  Success Rate: {result['success_rate']:.1f}%")
        print(f"  Response Time: {result['min_response_time_ms']:.1f}ms min, {result['avg_response_time_ms']:.1f}ms avg, {result['max_response_time_ms']:.1f}ms max")
        
        overall_success_rate += result['success_rate']
        overall_avg_response += result['avg_response_time_ms']
    
    if results:
        overall_success_rate /= len(results)
        overall_avg_response /= len(results)
        
        print("\n" + "="*80)
        print(f"OVERALL SUMMARY:")
        print(f"  Average Success Rate: {overall_success_rate:.1f}%")
        print(f"  Average Response Time: {overall_avg_response:.1f}ms")
        print("="*80)
    
    # 保存报告
    report_dir = os.path.join(os.path.dirname(__file__), "reports")
    os.makedirs(report_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_file = os.path.join(report_dir, f"performance_report_{timestamp}.txt")
    
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("PERFORMANCE TEST REPORT\n")
        f.write("="*80 + "\n")
        f.write(f"Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"Test Environment: Mock API Gateway on localhost:8080\n")
        f.write(f"Total Endpoints Tested: {len(results)}\n")
        f.write("="*80 + "\n\n")
        
        for result in results:
            f.write(f"Endpoint: {result['endpoint']}\n")
            f.write(f"  Total Requests: {result['total_requests']}\n")
            f.write(f"  Successful: {result['successful_requests']}\n")
            f.write(f"  Failed: {result['failed_requests']}\n")
            f.write(f"  Success Rate: {result['success_rate']:.1f}%\n")
            f.write(f"  Min Response Time: {result['min_response_time_ms']:.1f}ms\n")
            f.write(f"  Avg Response Time: {result['avg_response_time_ms']:.1f}ms\n")
            f.write(f"  Max Response Time: {result['max_response_time_ms']:.1f}ms\n")
            f.write(f"  Timestamp: {result['test_timestamp']}\n\n")
        
        if results:
            f.write("="*80 + "\n")
            f.write(f"OVERALL SUMMARY:\n")
            f.write(f"  Average Success Rate: {overall_success_rate:.1f}%\n")
            f.write(f"  Average Response Time: {overall_avg_response:.1f}ms\n")
            f.write("="*80 + "\n")
    
    print(f"\n[INFO] Report saved to: {report_file}")
    
    return report_file

def main():
    """主函数"""
    print("AI-Ready Sprint 27+1 Performance Benchmark Test")
    print("Simple Test Execution")
    print("="*80)
    
    # 检查requests模块
    try:
        import requests
        print("[OK] requests module available")
    except ImportError:
        print("[ERROR] requests module not found")
        print("Please install: pip install requests")
        return 1
    
    # 启动Mock服务
    if not start_mock_service():
        print("[ERROR] Failed to start mock service")
        return 1
    
    try:
        # 运行性能测试
        results = run_performance_tests()
        
        if results:
            # 生成报告
            report_file = generate_report(results)
            
            print("\n" + "="*80)
            print("[SUCCESS] Performance benchmark test completed")
            print(f"[INFO] Report generated: {report_file}")
            print("="*80)
            
            # 返回成功
            return 0
        else:
            print("\n" + "="*80)
            print("[ERROR] No test results generated")
            print("="*80)
            return 1
            
    except Exception as e:
        print(f"\n[ERROR] Test execution failed: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        print("\n[INFO] Test execution completed")

if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)