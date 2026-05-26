#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试自动化框架验证脚本
"""

import sys
import os

# 添加项目根目录
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

def test_basic():
    """基本功能测试"""
    print("Running basic test...")
    
    # 导入测试框架
    import pytest
    print("✓ pytest imported")
    
    # 导入API客户端
    try:
        from utils.api_client import APIClient
        print("✓ APIClient imported")
    except ImportError as e:
        print(f"✗ APIClient import failed: {e}")
    
    # 导入数据生成器
    try:
        from utils.data_generator import DataGenerator
        print("✓ DataGenerator imported")
    except ImportError as e:
        print(f"✗ DataGenerator import failed: {e}")
    
    print("Basic test completed!")

def test_api_client():
    """API客户端测试"""
    print("\nRunning API client test...")
    
    try:
        from utils.api_client import APIClient
        
        # 创建API客户端
        api_client = APIClient(base_url='http://localhost:8080/api/v1')
        print("✓ APIClient instantiated")
        
        # 检查属性
        assert api_client.base_url == 'http://localhost:8080/api/v1'
        assert api_client.timeout == 30
        print("✓ APIClient properties correct")
        
        print("API client test completed!")
    except Exception as e:
        print(f"✗ API client test failed: {e}")

def test_data_generator():
    """数据生成器测试"""
    print("\nRunning data generator test...")
    
    try:
        from utils.data_generator import DataGenerator
        
        # 创建数据生成器
        generator = DataGenerator()
        print("✓ DataGenerator instantiated")
        
        # 测试用户数据生成
        user_data = generator.generate_user()
        assert 'username' in user_data
        assert 'email' in user_data
        assert 'phone' in user_data
        print(f"✓ User data generated: {user_data}")
        
        # 测试订单数据生成
        order_data = generator.generate_order()
        assert 'productIds' in order_data
        assert 'quantity' in order_data
        print(f"✓ Order data generated: {order_data}")
        
        print("Data generator test completed!")
    except Exception as e:
        print(f"✗ Data generator test failed: {e}")

def main():
    """主函数"""
    print("=" * 60)
    print("AI-Ready Test Automation Framework Verification")
    print("=" * 60)
    
    # 运行所有测试
    test_basic()
    test_api_client()
    test_data_generator()
    
    print("\n" + "=" * 60)
    print("Verification completed!")
    print("=" * 60)

if __name__ == '__main__':
    main()