#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 数据库连接池性能测试执行器
"""

import os
import sys
import subprocess
import argparse
from datetime import datetime

def run_connection_pool_test():
    """运行连接池性能测试"""
    print("=" * 60)
    print("AI-Ready 数据库连接池性能测试")
    print("=" * 60)
    
    # 获取脚本路径
    script_path = os.path.join(os.path.dirname(__file__), "connection_pool_test.py")
    
    # 运行测试
    try:
        result = subprocess.run([sys.executable, script_path], 
                              capture_output=True, text=True, cwd=os.path.dirname(__file__))
        
        if result.returncode == 0:
            print("✅ 连接池性能测试执行成功")
            print(result.stdout)
            
            # 检查是否有报告生成
            report_dir = os.path.join(os.path.dirname(__file__), "..", "docs")
            if os.path.exists(report_dir):
                reports = [f for f in os.listdir(report_dir) if f.startswith("CONNECTION_POOL_PERFORMANCE_REPORT")]
                if reports:
                    latest_report = max(reports, key=lambda x: os.path.getctime(os.path.join(report_dir, x)))
                    print(f"📊 最新报告: {os.path.join(report_dir, latest_report)}")
            
            return True
        else:
            print("❌ 连接池性能测试执行失败")
            print(result.stderr)
            return False
            
    except Exception as e:
        print(f"❌ 执行测试时发生错误: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description='AI-Ready 数据库连接池性能测试')
    parser.add_argument('--config', '-c', help='测试配置文件路径', default='test_config.yml')
    parser.add_argument('--verbose', '-v', action='store_true', help='详细输出')
    
    args = parser.parse_args()
    
    if args.verbose:
        print(f"使用配置文件: {args.config}")
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    success = run_connection_pool_test()
    
    if success:
        print("\n🎉 数据库连接池性能测试完成！")
        print("请查看生成的报告以获取详细结果和优化建议。")
        sys.exit(0)
    else:
        print("\n💥 数据库连接池性能测试失败！")
        sys.exit(1)

if __name__ == '__main__':
    main()