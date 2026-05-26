#!/usr/bin/env python3
"""
性能测试运行脚本
用于启动mock服务和运行性能测试
"""

import os
import sys
import subprocess
import time
import signal
import threading
from datetime import datetime

def start_mock_service():
    """启动Mock API Gateway服务"""
    print("启动Mock API Gateway服务...")
    
    mock_service_path = os.path.join(os.path.dirname(__file__), "mock_services", "mock_api_gateway.py")
    
    if not os.path.exists(mock_service_path):
        print(f"❌ 找不到Mock服务文件: {mock_service_path}")
        return None
    
    try:
        # 启动mock服务
        process = subprocess.Popen(
            [sys.executable, mock_service_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
            universal_newlines=True
        )
        
        # 等待服务启动
        print("等待Mock服务启动...")
        time.sleep(3)
        
        # 检查服务是否启动成功
        try:
            import requests
            response = requests.get("http://localhost:8080/actuator/health", timeout=5)
            if response.status_code == 200:
                print("✅ Mock API Gateway 服务启动成功")
                return process
            else:
                print(f"❌ Mock服务启动失败，状态码: {response.status_code}")
                process.terminate()
                return None
        except:
            print("❌ 无法连接到Mock服务")
            process.terminate()
            return None
            
    except Exception as e:
        print(f"❌ 启动Mock服务失败: {e}")
        return None

def run_performance_tests():
    """运行性能测试"""
    print("\n开始运行性能测试...")
    
    performance_test_path = os.path.join(os.path.dirname(__file__), "api", "performance_benchmark_test.py")
    
    if not os.path.exists(performance_test_path):
        print(f"❌ 找不到性能测试文件: {performance_test_path}")
        return False
    
    try:
        # 运行性能测试
        result = subprocess.run(
            [sys.executable, performance_test_path],
            capture_output=True,
            text=True,
            timeout=300  # 5分钟超时
        )
        
        print("\n" + "="*80)
        print("性能测试输出:")
        print("="*80)
        print(result.stdout)
        
        if result.stderr:
            print("\n错误输出:")
            print("="*80)
            print(result.stderr)
        
        print("\n" + "="*80)
        print(f"性能测试完成，退出码: {result.returncode}")
        print("="*80)
        
        return result.returncode == 0
        
    except subprocess.TimeoutExpired:
        print("❌ 性能测试超时")
        return False
    except Exception as e:
        print(f"❌ 运行性能测试失败: {e}")
        return False

def check_dependencies():
    """检查依赖包"""
    print("检查Python依赖包...")
    
    required_packages = [
        "requests",
        "numpy",
        "pandas",
        "matplotlib"
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
            print(f"✅ {package}")
        except ImportError:
            missing_packages.append(package)
            print(f"❌ {package}")
    
    if missing_packages:
        print(f"\n缺少以下依赖包: {missing_packages}")
        print("请使用以下命令安装:")
        print(f"pip install {' '.join(missing_packages)}")
        return False
    
    print("✅ 所有依赖包检查通过")
    return True

def main():
    """主函数"""
    print("="*80)
    print("AI-Ready Sprint 27+1 测试环境性能测试运行器")
    print(f"时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*80)
    
    # 检查依赖
    if not check_dependencies():
        print("\n❌ 依赖检查失败，请先安装缺失的包")
        return 1
    
    # 切换到脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    
    mock_process = None
    
    try:
        # 启动Mock服务
        mock_process = start_mock_service()
        if not mock_process:
            print("\n❌ Mock服务启动失败，无法继续")
            return 1
        
        # 运行性能测试
        success = run_performance_tests()
        
        if success:
            print("\n✅ 性能测试执行成功")
            print("\n测试报告已生成在:")
            print("  - I:\\AI-Ready\\tests\\performance\\reports\\")
            print("\nMock服务运行状态:")
            print("  - 健康检查: http://localhost:8080/actuator/health")
            print("  - 统计信息: http://localhost:8080/api/stats")
            print("\n要停止Mock服务，请按Ctrl+C")
            
            # 保持服务运行，等待用户中断
            try:
                while True:
                    time.sleep(1)
            except KeyboardInterrupt:
                print("\n用户中断，停止服务...")
                
        else:
            print("\n❌ 性能测试执行失败")
            return 1
            
    except KeyboardInterrupt:
        print("\n用户中断，停止服务...")
    except Exception as e:
        print(f"\n❌ 运行过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        # 停止Mock服务
        if mock_process:
            print("停止Mock服务...")
            mock_process.terminate()
            try:
                mock_process.wait(timeout=5)
                print("✅ Mock服务已停止")
            except subprocess.TimeoutExpired:
                print("⚠️  Mock服务终止超时，强制停止...")
                mock_process.kill()
    
    print("\n" + "="*80)
    print("性能测试运行完成")
    print("="*80)
    return 0

if __name__ == "__main__":
    sys.exit(main())