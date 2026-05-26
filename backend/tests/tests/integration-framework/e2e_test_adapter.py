#!/usr/bin/env python3
"""
Playwright E2E测试适配器

将test-agent-2的Playwright测试脚本集成到统一测试框架中
"""

from pathlib import Path
from datetime import datetime
import json
import subprocess
import time
from test_script_integration import (
    TestScript, TestResult, TestExecution, TestScriptManager
)

class PlaywrightE2EAdapter(TestScript):
    """
    Playwright E2E测试脚本适配器
    
    适配test-agent-2的Playwright测试脚本：
    - user-auth.spec.ts
    - order-flow.spec.ts  
    - inventory-flow.spec.ts
    - crm-flow.spec.ts
    """
    
    def __init__(self, config):
        # 设置默认配置
        default_config = {
            'script_path': 'I:/AI-Ready/tests/e2e',
            'test_files': [
                'user-auth.spec.ts',
                'order-flow.spec.ts',
                'inventory-flow.spec.ts',
                'crm-flow.spec.ts'
            ],
            'playwright_config': 'playwright.config.ts',
            'timeout': 300000,  # 5分钟
            'workers': 4,  # 并行工作线程
            'headless': True,
            'screenshot': 'only-on-failure',
            'video': 'retain-on-failure',
            'trace': 'retain-on-failure'
        }
        
        # 合并配置
        default_config.update(config)
        super().__init__(default_config)
    
    def prepare(self) -> bool:
        """准备Playwright测试环境"""
        print("=" * 60)
        print("Playwright E2E测试环境准备")
        print("=" * 60)
        
        # 检查Node.js环境
        try:
            result = subprocess.run(
                ['node', '--version'],
                capture_output=True,
                text=True,
                timeout=10
            )
            if result.returncode == 0:
                print(f"[OK] Node.js环境: {result.stdout.strip()}")
            else:
                print("[FAIL] Node.js环境检查失败")
                return False
        except Exception as e:
            print(f"[FAIL] Node.js未安装: {e}")
            return False
        
        # 检查npm环境
        try:
            result = subprocess.run(
                ['npm', '--version'],
                capture_output=True,
                text=True,
                timeout=10
            )
            if result.returncode == 0:
                print(f"[OK] npm版本: {result.stdout.strip()}")
            else:
                print("[FAIL] npm检查失败")
                return False
        except Exception as e:
            print(f"[FAIL] npm未安装: {e}")
            return False
        
        # 检查Playwright安装
        script_dir = Path(self.config['script_path'])
        if not script_dir.exists():
            print(f"[FAIL] 测试目录不存在: {script_dir}")
            return False
        
        print(f"[OK] 测试目录: {script_dir}")
        
        # 检查测试文件
        test_files = self.config.get('test_files', [])
        found_files = []
        missing_files = []
        
        for test_file in test_files:
            file_path = script_dir / test_file
            if file_path.exists():
                size = file_path.stat().st_size
                print(f"[OK] 测试文件: {test_file} ({size:,} bytes)")
                found_files.append(test_file)
            else:
                print(f"[WARN]️ 缺失文件: {test_file}")
                missing_files.append(test_file)
        
        print(f"\n找到 {len(found_files)}/{len(test_files)} 个测试文件")
        
        if missing_files:
            print(f"[WARN]️ 缺失文件: {', '.join(missing_files)}")
        
        # 检查Playwright配置文件
        config_file = script_dir / self.config.get('playwright_config', 'playwright.config.ts')
        if config_file.exists():
            print(f"[OK] Playwright配置: {config_file.name}")
        else:
            print(f"[WARN]️ Playwright配置不存在: {config_file}")
        
        # 检查package.json
        package_file = script_dir.parent / 'package.json'
        if package_file.exists():
            print(f"[OK] package.json存在")
        else:
            print(f"[WARN]️ package.json不存在")
        
        print("\n[OK] Playwright E2E测试环境准备完成")
        return True
    
    def execute(self) -> TestExecution:
        """执行Playwright E2E测试"""
        print("\n" + "=" * 60)
        print("开始执行Playwright E2E测试")
        print("=" * 60)
        
        script_dir = Path(self.config['script_path'])
        test_files = self.config.get('test_files', [])
        
        # 构建npx playwright test命令
        args = [
            'npx', 'playwright', 'test',
            '--reporter=json',  # JSON格式输出
            f"--workers={self.config.get('workers', 4)}",
            f"--timeout={self.config.get('timeout', 300000)}"
        ]
        
        # 添加测试文件
        if test_files:
            args.extend(test_files)
        
        # 添加其他选项
        if self.config.get('headless', True):
            args.append('--headless')
        
        print(f"执行命令: {' '.join(args)}")
        print(f"工作目录: {script_dir}")
        
        try:
            # 设置环境变量
            env = {
                'PLAYWRIGHT_JSON_OUTPUT_NAME': 'playwright-results.json',
                **dict(subprocess.os.environ)
            }
            
            start_time = time.time()
            
            result = subprocess.run(
                args,
                capture_output=True,
                text=True,
                timeout=self.config.get('timeout', 300000) / 1000 + 60,  # 额外60秒
                cwd=script_dir,
                env=env
            )
            
            execution_time = time.time() - start_time
            
            print(f"\n执行完成:")
            print(f"  返回码: {result.returncode}")
            print(f"  执行时间: {execution_time:.2f}秒")
            
            if result.stdout:
                print(f"\n标准输出:")
                print(result.stdout[:2000])  # 限制输出长度
            
            if result.stderr:
                print(f"\n错误输出:")
                print(result.stderr[:1000])
            
            # 记录执行信息
            self.execution.stdout = result.stdout
            self.execution.stderr = result.stderr
            self.execution.return_code = result.returncode
            self.execution.duration = execution_time
            
            if result.returncode != 0:
                print(f"\n[WARN]️ 测试执行返回非零状态码: {result.returncode}")
                # 但Playwright可能返回非零状态码表示有测试失败，不一定是执行错误
            
        except subprocess.TimeoutExpired:
            print(f"\n[FAIL] 测试执行超时")
            raise RuntimeError("Playwright测试执行超时")
        except Exception as e:
            print(f"\n[FAIL] 测试执行失败: {e}")
            raise RuntimeError(f"Playwright测试执行失败: {e}")
        
        return self.execution
    
    def collect_results(self) -> list:
        """收集Playwright测试结果"""
        print("\n" + "=" * 60)
        print("收集Playwright测试结果")
        print("=" * 60)
        
        script_dir = Path(self.config['script_path'])
        results = []
        
        # 检查JSON结果文件
        json_result_file = script_dir / 'playwright-results.json'
        if json_result_file.exists():
            try:
                with open(json_result_file, 'r', encoding='utf-8') as f:
                    playwright_results = json.load(f)
                
                print("[OK] 找到Playwright JSON结果文件")
                
                # 解析结果
                if 'suites' in playwright_results:
                    for suite in playwright_results['suites']:
                        suite_title = suite.get('title', 'Unknown Suite')
                        
                        if 'specs' in suite:
                            for spec in suite['specs']:
                                spec_title = spec.get('title', 'Unknown Test')
                                
                                # 确定测试状态
                                status = 'passed'
                                duration = 0
                                error_message = None
                                
                                if 'tests' in spec and len(spec['tests']) > 0:
                                    test = spec['tests'][0]
                                    test_status = test.get('status', 'unknown')
                                    
                                    if test_status == 'expected':
                                        status = 'passed'
                                    elif test_status == 'unexpected':
                                        status = 'failed'
                                    elif test_status == 'skipped':
                                        status = 'skipped'
                                    elif test_status == 'flaky':
                                        status = 'flaky'
                                    
                                    # 获取执行时间
                                    if 'results' in test and len(test['results']) > 0:
                                        test_result = test['results'][0]
                                        duration = test_result.get('duration', 0)
                                        
                                        # 获取错误信息
                                        if 'error' in test_result:
                                            error_message = test_result['error'].get('message', '')
                                
                                # 创建TestResult
                                test_result = TestResult(
                                    test_id=f"e2e_{suite_title}_{spec_title}_{int(time.time())}",
                                    test_name=f"{suite_title} - {spec_title}",
                                    status=status,
                                    duration=duration / 1000,  # 转换为秒
                                    start_time=datetime.now(),
                                    end_time=datetime.now(),
                                    error_message=error_message,
                                    suite=suite_title,
                                    test_type='e2e'
                                )
                                results.append(test_result)
                
                # 统计结果
                passed = sum(1 for r in results if r.status == 'passed')
                failed = sum(1 for r in results if r.status == 'failed')
                skipped = sum(1 for r in results if r.status == 'skipped')
                
                print(f"\n测试结果统计:")
                print(f"  [OK] 通过: {passed}")
                print(f"  [FAIL] 失败: {failed}")
                print(f"  ⏭️ 跳过: {skipped}")
                print(f"  📊 总计: {len(results)}")
                
            except Exception as e:
                print(f"[FAIL] 解析结果文件失败: {e}")
                return []
        else:
            print(f"[WARN]️ 未找到结果文件: {json_result_file}")
            
            # 尝试从stdout解析结果
            if self.execution and self.execution.stdout:
                print("尝试从标准输出解析结果...")
                # 这里可以添加额外的解析逻辑
        
        return results
    
    def cleanup(self) -> bool:
        """清理测试环境"""
        print("\n" + "=" * 60)
        print("清理Playwright测试环境")
        print("=" * 60)
        
        script_dir = Path(self.config['script_path'])
        
        # 清理临时结果文件
        temp_files = [
            'playwright-results.json'
        ]
        
        for temp_file in temp_files:
            file_path = script_dir / temp_file
            if file_path.exists():
                try:
                    file_path.unlink()
                    print(f"[OK] 清理临时文件: {temp_file}")
                except Exception as e:
                    print(f"[WARN]️ 清理失败: {temp_file} - {e}")
        
        # 保留截图和视频（用于调试）
        print("[OK] 保留截图和视频用于调试")
        
        return True

class E2ETestSuiteManager:
    """
    E2E测试套件管理器
    
    管理所有E2E测试脚本的执行和结果收集
    """
    
    def __init__(self):
        self.scripts = {}
        self.results = []
    
    def add_test_script(self, name: str, adapter: PlaywrightE2EAdapter):
        """添加测试脚本"""
        self.scripts[name] = adapter
        print(f"[OK] 添加E2E测试脚本: {name}")
    
    def run_all_tests(self) -> list:
        """运行所有E2E测试"""
        print("=" * 60)
        print("执行所有E2E测试套件")
        print("=" * 60)
        
        all_results = []
        
        for name, adapter in self.scripts.items():
            print(f"\n{'='*60}")
            print(f"执行测试套件: {name}")
            print('='*60)
            
            try:
                # 准备环境
                if not adapter.prepare():
                    print(f"[FAIL] 测试套件准备失败: {name}")
                    continue
                
                # 执行测试
                adapter.execute()
                
                # 收集结果
                results = adapter.collect_results()
                all_results.extend(results)
                
                # 清理环境
                adapter.cleanup()
                
                print(f"[OK] 测试套件完成: {name}")
                
            except Exception as e:
                print(f"[FAIL] 测试套件执行失败: {name} - {e}")
                import traceback
                traceback.print_exc()
        
        # 汇总结果
        self._print_summary(all_results)
        
        return all_results
    
    def _print_summary(self, results: list):
        """打印测试摘要"""
        print("\n" + "=" * 60)
        print("E2E测试结果汇总")
        print("=" * 60)
        
        if not results:
            print("[WARN]️ 没有测试结果")
            return
        
        total = len(results)
        passed = sum(1 for r in results if r.status == 'passed')
        failed = sum(1 for r in results if r.status == 'failed')
        skipped = sum(1 for r in results if r.status == 'skipped')
        
        total_duration = sum(r.duration for r in results)
        
        print(f"\n总体统计:")
        print(f"  📊 总测试数: {total}")
        print(f"  [OK] 通过: {passed} ({passed/total*100:.1f}%)")
        print(f"  [FAIL] 失败: {failed} ({failed/total*100:.1f}%)")
        print(f"  ⏭️ 跳过: {skipped} ({skipped/total*100:.1f}%)")
        print(f"  ⏱️ 总耗时: {total_duration:.2f}秒")
        
        if failed > 0:
            print(f"\n[FAIL] 失败测试详情:")
            for result in results:
                if result.status == 'failed':
                    print(f"  - {result.test_name}")
                    if result.error_message:
                        print(f"    错误: {result.error_message[:200]}")

# 使用示例
if __name__ == "__main__":
    print("Playwright E2E测试适配器")
    print("=" * 60)
    
    # 创建测试套件管理器
    manager = E2ETestSuiteManager()
    
    # 配置E2E测试
    e2e_config = {
        'script_path': 'I:/AI-Ready/tests/e2e',
        'test_files': [
            'user-auth.spec.ts',
            'order-flow.spec.ts',
            'inventory-flow.spec.ts',
            'crm-flow.spec.ts'
        ],
        'workers': 4,
        'timeout': 300000,
        'headless': True
    }
    
    # 创建适配器
    e2e_adapter = PlaywrightE2EAdapter(e2e_config)
    
    # 添加到管理器
    manager.add_test_script('AI-Ready E2E Tests', e2e_adapter)
    
    print("\n[OK] Playwright E2E测试适配器已创建")
    print(f"[OK] 配置测试文件: {len(e2e_config['test_files'])} 个")
    print(f"[OK] 工作线程: {e2e_config['workers']}")
    
    print("\n下一步:")
    print("1. 确认test-agent-2的E2E测试文件路径正确")
    print("2. 安装Playwright依赖: npm install @playwright/test -D")
    print("3. 运行测试验证: npx playwright test")
    print("4. 集成到统一测试框架")
    
    print("\n提示: 此适配器已准备就绪，可以直接运行E2E测试！")