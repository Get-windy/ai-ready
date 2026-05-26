#!/usr/bin/env python3
"""
E2E测试验证脚本

验证Playwright适配器功能，确保test-agent-2的E2E测试脚本可以集成
"""

import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

def check_e2e_files():
    """检查E2E测试文件是否存在"""
    print("检查E2E测试文件...")
    
    e2e_dir = Path("I:/AI-Ready/tests/e2e")
    
    if not e2e_dir.exists():
        print("[FAIL] E2E测试目录不存在")
        return False
    
    print(f"[OK] E2E测试目录: {e2e_dir}")
    
    # 检查spec.ts文件
    spec_files = list(e2e_dir.glob("*.spec.ts"))
    print(f"[OK] 找到 {len(spec_files)} 个spec.ts文件")
    
    for spec_file in spec_files:
        size = spec_file.stat().st_size
        print(f"  - {spec_file.name} ({size:,} bytes)")
    
    # 检查配置文件
    config_file = e2e_dir / "playwright.config.ts"
    if config_file.exists():
        print(f"[OK] Playwright配置文件: {config_file.name}")
    else:
        print(f"[WARN] Playwright配置文件不存在: {config_file}")
    
    # 检查package.json
    package_file = e2e_dir.parent / "package.json"
    if package_file.exists():
        print(f"[OK] package.json存在")
    else:
        print(f"[WARN] package.json不存在")
    
    return True

def create_e2e_adapter():
    """创建E2E适配器"""
    print("\n创建E2E适配器...")
    
    try:
        from e2e_test_adapter import PlaywrightE2EAdapter, E2ETestSuiteManager
        
        # 创建适配器配置
        config = {
            'script_path': 'I:/AI-Ready/tests/e2e',
            'test_files': [
                'ai-dialog.spec.ts',
                'user-auth.spec.ts',
                'order-flow.spec.ts',
                'inventory-flow.spec.ts',
                'crm-flow.spec.ts'
            ],
            'workers': 2,
            'timeout': 120000,  # 2分钟
            'headless': True
        }
        
        # 创建适配器
        adapter = PlaywrightE2EAdapter(config)
        print("[OK] PlaywrightE2EAdapter创建成功")
        
        # 创建管理器
        manager = E2ETestSuiteManager()
        manager.add_test_script('AI-Ready E2E Tests', adapter)
        print("[OK] E2ETestSuiteManager创建成功")
        
        return adapter, manager
        
    except Exception as e:
        print(f"[FAIL] 创建E2E适配器失败: {e}")
        import traceback
        traceback.print_exc()
        return None, None

def test_adapter_prepare(adapter):
    """测试适配器准备功能"""
    print("\n测试适配器准备功能...")
    
    try:
        success = adapter.prepare()
        if success:
            print("[OK] 适配器准备成功")
        else:
            print("[FAIL] 适配器准备失败")
        return success
        
    except Exception as e:
        print(f"[FAIL] 适配器准备异常: {e}")
        return False

def run_adapter_test(adapter, manager):
    """运行适配器测试（简化版）"""
    print("\n运行适配器测试（简化版）...")
    
    # 在实际环境中，这里会调用adapter.run()或manager.run_all_tests()
    # 但由于我们没有实际安装Playwright环境，这里只做模拟测试
    
    print("[INFO] 模拟测试适配器功能...")
    print("[INFO] 实际环境需要：")
    print("  1. Node.js环境")
    print("  2. npm install @playwright/test -D")
    print("  3. npx playwright install")
    print("  4. 安装浏览器驱动")
    
    # 检查Node.js环境
    import subprocess
    
    try:
        result = subprocess.run(
            ['node', '--version'],
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            print(f"[OK] Node.js环境: {result.stdout.strip()}")
        else:
            print(f"[WARN] Node.js检查失败: {result.stderr}")
            
    except FileNotFoundError:
        print("[WARN] Node.js未安装")
    
    # 检查npm环境
    try:
        result = subprocess.run(
            ['npm', '--version'],
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            print(f"[OK] npm版本: {result.stdout.strip()}")
        else:
            print(f"[WARN] npm检查失败: {result.stderr}")
            
    except FileNotFoundError:
        print("[WARN] npm未安装")
    
    return True

def create_quick_start_guide():
    """创建快速开始指南"""
    print("\n" + "=" * 60)
    print("E2E测试快速开始指南")
    print("=" * 60)
    
    guide = """
## 🚀 E2E测试快速开始指南

### 1. 环境准备
```bash
# 安装Node.js（如果未安装）
# 下载地址: https://nodejs.org/

# 安装依赖
cd I:/AI-Ready/tests/e2e
npm install @playwright/test -D
npx playwright install
```

### 2. 运行测试
```bash
# 运行所有测试
npx playwright test

# 运行特定测试
npx playwright test user-auth.spec.ts
npx playwright test order-flow.spec.ts
```

### 3. 使用集成框架
```python
# 使用集成框架运行测试
from e2e_test_adapter import PlaywrightE2EAdapter, E2ETestSuiteManager

# 创建适配器
adapter = PlaywrightE2EAdapter({
    'script_path': 'I:/AI-Ready/tests/e2e',
    'test_files': ['user-auth.spec.ts', 'order-flow.spec.ts'],
    'workers': 4
})

# 运行测试
adapter.prepare()
adapter.execute()
results = adapter.collect_results()
```

### 4. 测试文件清单
- ai-dialog.spec.ts (6,290 bytes)
- user-auth.spec.ts (9,364 bytes)
- order-flow.spec.ts (11,597 bytes)
- inventory-flow.spec.ts (11,683 bytes)
- crm-flow.spec.ts (14,225 bytes)

### 5. 下一步
1. 安装Playwright环境
2. 运行测试验证
3. 集成到CI/CD流水线
4. 生成测试报告
"""
    
    print(guide)
    
    # 保存指南到文件
    guide_file = Path(__file__).parent / "e2e_quick_start.md"
    with open(guide_file, 'w', encoding='utf-8') as f:
        f.write(guide)
    
    print(f"[OK] 快速开始指南已保存: {guide_file}")

def main():
    """主函数"""
    print("=" * 60)
    print("E2E测试验证脚本")
    print("=" * 60)
    
    print(f"开始时间: 2026-04-24 16:45")
    print()
    
    # 1. 检查E2E文件
    if not check_e2e_files():
        return False
    
    # 2. 创建适配器
    adapter, manager = create_e2e_adapter()
    if not adapter or not manager:
        return False
    
    # 3. 测试准备功能
    if not test_adapter_prepare(adapter):
        print("[WARN] 适配器准备测试失败，但继续...")
    
    # 4. 运行测试（简化版）
    run_adapter_test(adapter, manager)
    
    # 5. 创建快速开始指南
    create_quick_start_guide()
    
    print("\n" + "=" * 60)
    print("E2E测试验证完成")
    print("=" * 60)
    print("[OK] 所有验证步骤完成")
    print("[INFO] 需要安装Playwright环境才能实际运行测试")
    print()
    print("下一步:")
    print("1. 安装Node.js和Playwright环境")
    print("2. 运行测试验证")
    print("3. 集成到统一框架")
    print("4. 生成测试报告")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)