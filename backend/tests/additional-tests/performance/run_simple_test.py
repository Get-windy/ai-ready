#!/usr/bin/env python3
"""
简单的性能测试运行脚本，避免编码问题
"""

import os
import sys
import subprocess
import time

def run_mock_service():
    """运行Mock服务"""
    print("Starting Mock API Gateway service...")
    mock_service_path = os.path.join(os.path.dirname(__file__), "mock_services", "mock_api_gateway.py")
    
    try:
        process = subprocess.Popen(
            [sys.executable, mock_service_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding='utf-8',
            bufsize=1
        )
        
        # 等待服务启动
        time.sleep(5)
        
        # 检查服务是否启动
        try:
            import requests
            response = requests.get("http://localhost:8080/actuator/health", timeout=5)
            if response.status_code == 200:
                print(f"Mock service started successfully on port 8080")
                return process
            else:
                print(f"Mock service returned status code: {response.status_code}")
                process.terminate()
                return None
        except Exception as e:
            print(f"Failed to connect to mock service: {e}")
            process.terminate()
            return None
            
    except Exception as e:
        print(f"Failed to start mock service: {e}")
        return None

def run_performance_test():
    """运行性能测试"""
    print("\nRunning performance tests...")
    performance_test_path = os.path.join(os.path.dirname(__file__), "api", "performance_benchmark_test.py")
    
    try:
        result = subprocess.run(
            [sys.executable, performance_test_path],
            capture_output=True,
            text=True,
            encoding='utf-8',
            timeout=300
        )
        
        print("\n" + "="*80)
        print("Performance Test Output:")
        print("="*80)
        print(result.stdout)
        
        if result.stderr:
            print("\nError Output:")
            print("="*80)
            print(result.stderr)
        
        print("\n" + "="*80)
        print(f"Performance test completed, exit code: {result.returncode}")
        print("="*80)
        
        return result.returncode == 0
        
    except subprocess.TimeoutExpired:
        print("Performance test timed out")
        return False
    except Exception as e:
        print(f"Failed to run performance tests: {e}")
        return False

def main():
    """主函数"""
    print("AI-Ready Sprint 27+1 Performance Benchmark Test")
    print("Time: 2026-04-27")
    print("="*80)
    
    # 检查依赖
    print("\nChecking dependencies...")
    try:
        import requests
        import numpy
        import pandas
        import matplotlib
        print("All dependencies are available")
    except ImportError as e:
        print(f"Missing dependency: {e}")
        print("Please install: pip install requests numpy pandas matplotlib")
        return 1
    
    # 运行Mock服务
    mock_process = run_mock_service()
    if not mock_process:
        print("Failed to start mock service, exiting...")
        return 1
    
    try:
        # 运行性能测试
        success = run_performance_test()
        
        # 停止Mock服务
        print("\nStopping mock service...")
        mock_process.terminate()
        mock_process.wait(timeout=10)
        
        if success:
            print("\n" + "="*80)
            print("SUCCESS: Performance benchmark tests completed successfully")
            print("="*80)
            return 0
        else:
            print("\n" + "="*80)
            print("FAILURE: Performance benchmark tests failed")
            print("="*80)
            return 1
            
    except Exception as e:
        print(f"Error during test execution: {e}")
        if mock_process:
            mock_process.terminate()
        return 1

if __name__ == "__main__":
    sys.exit(main())