#!/usr/bin/env python3
"""
集成验证脚本

验证测试脚本集成框架的功能
"""

import sys
import time
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

def verify_basic_framework():
    """验证基础框架功能"""
    print("=" * 60)
    print("验证基础集成框架")
    print("=" * 60)
    
    try:
        from test_script_integration import TestScriptManager, TestResult
        
        # 创建管理器
        manager = TestScriptManager()
        print("[OK] TestScriptManager创建成功")
        
        # 验证基本功能
        print(f"[OK] 脚本数量: {len(manager.scripts)}")
        
        # 检查可选属性
        if hasattr(manager, 'result_collector'):
            print(f"[OK] 结果收集器: {'已配置' if manager.result_collector else '未配置'}")
        else:
            print(f"[OK] 结果收集器: 未配置（可选）")
        
        if hasattr(manager, 'report_generator'):
            print(f"[OK] 报告生成器: {'已配置' if manager.report_generator else '未配置'}")
        else:
            print(f"[OK] 报告生成器: 未配置（可选）")
        
        return True
        
    except Exception as e:
        print(f"[FAIL] 基础框架验证失败: {e}")
        return False

def verify_e2e_adapter():
    """验证E2E适配器"""
    print("\n" + "=" * 60)
    print("验证Playwright E2E适配器")
    print("=" * 60)
    
    try:
        from e2e_test_adapter import PlaywrightE2EAdapter, E2ETestSuiteManager
        
        # 创建适配器
        config = {
            'script_path': 'I:/AI-Ready/tests/e2e',
            'test_files': [
                'user-auth.spec.ts',
                'order-flow.spec.ts',
                'inventory-flow.spec.ts',
                'crm-flow.spec.ts'
            ]
        }
        
        adapter = PlaywrightE2EAdapter(config)
        print("[OK] PlaywrightE2EAdapter创建成功")
        
        # 验证属性
        print(f"[OK] 脚本路径: {adapter.config['script_path']}")
        print(f"[OK] 测试文件数: {len(adapter.config['test_files'])}")
        
        # 创建管理器
        manager = E2ETestSuiteManager()
        manager.add_test_script('E2E Tests', adapter)
        print("[OK] E2ETestSuiteManager创建成功")
        
        return True
        
    except Exception as e:
        print(f"[FAIL] E2E适配器验证失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def verify_test_agent_2_adapter():
    """验证test-agent-2适配器"""
    print("\n" + "=" * 60)
    print("验证test-agent-2适配器")
    print("=" * 60)
    
    try:
        from test_agent_2_integration import (
            PerformanceTestRunnerAdapter,
            TestDataGeneratorAdapter,
            PerformanceMonitorAdapter
        )
        
        # 创建性能测试适配器
        perf_config = {
            'script_id': 'perf_test',
            'scenario': 'medium',
            'timeout': 600
        }
        perf_adapter = PerformanceTestRunnerAdapter(perf_config)
        print("[OK] PerformanceTestRunnerAdapter创建成功")
        
        # 创建数据生成适配器
        data_config = {
            'script_id': 'data_gen',
            'num_users': 1000,
            'num_products': 500
        }
        data_adapter = TestDataGeneratorAdapter(data_config)
        print("[OK] TestDataGeneratorAdapter创建成功")
        
        # 创建监控适配器
        monitor_config = {
            'script_id': 'monitor',
            'monitor_duration': 60
        }
        monitor_adapter = PerformanceMonitorAdapter(monitor_config)
        print("[OK] PerformanceMonitorAdapter创建成功")
        
        return True
        
    except Exception as e:
        print(f"[FAIL] test-agent-2适配器验证失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def verify_test_data_generator():
    """验证测试数据生成器"""
    print("\n" + "=" * 60)
    print("验证测试数据生成器")
    print("=" * 60)
    
    try:
        sys.path.insert(0, str(Path(__file__).parent.parent / 'data'))
        from test_data_generator import TestDataGenerator
        
        # 创建生成器
        generator = TestDataGenerator({
            'output_dir': './test_generated',
            'seed': 42
        })
        print("[OK] TestDataGenerator创建成功")
        
        # 生成少量数据验证
        print("\n生成测试数据...")
        users = generator.generate_users(10)
        print(f"[OK] 生成 {len(users)} 个用户")
        
        products = generator.generate_products(10)
        print(f"[OK] 生成 {len(products)} 个商品")
        
        orders = generator.generate_orders(5, users)
        print(f"[OK] 生成 {len(orders)} 个订单")
        
        return True
        
    except Exception as e:
        print(f"[FAIL] 测试数据生成器验证失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def run_all_verifications():
    """运行所有验证"""
    print("=" * 60)
    print("集成框架验证")
    print("=" * 60)
    print(f"验证时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    results = {}
    
    # 验证基础框架
    results['基础框架'] = verify_basic_framework()
    
    # 验证E2E适配器
    results['E2E适配器'] = verify_e2e_adapter()
    
    # 验证test-agent-2适配器
    results['test-agent-2适配器'] = verify_test_agent_2_adapter()
    
    # 验证测试数据生成器
    results['测试数据生成器'] = verify_test_data_generator()
    
    # 汇总结果
    print("\n" + "=" * 60)
    print("验证结果汇总")
    print("=" * 60)
    
    passed = 0
    failed = 0
    
    for name, result in results.items():
        status = "[PASS]" if result else "[FAIL]"
        print(f"{status}: {name}")
        
        if result:
            passed += 1
        else:
            failed += 1
    
    print(f"\n总计: {passed}/{len(results)} 通过")
    
    if failed == 0:
        print("\n[OK] 所有验证通过！集成框架已就绪！")
        return True
    else:
        print(f"\n[WARNING] {failed} 个验证失败，需要修复")
        return False

if __name__ == "__main__":
    success = run_all_verifications()
    sys.exit(0 if success else 1)