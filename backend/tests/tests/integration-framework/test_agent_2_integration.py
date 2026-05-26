#!/usr/bin/env python3
"""
test-agent-2脚本集成适配器模板

此模板展示如何将test-agent-2的具体脚本集成到统一框架中。
"""

from pathlib import Path
from datetime import datetime
import json
from test_script_integration import (
    TestScript, TestResult, TestExecution, TestScriptManager
)

class PerformanceTestRunnerAdapter(TestScript):
    """
    test-agent-2的performance_test_runner.py适配器
    
    此适配器将test-agent-2的性能测试脚本集成到统一框架中。
    """
    
    def __init__(self, config):
        # 设置默认脚本路径（test-agent-2的实际脚本）
        default_script_path = "I:/AI-Ready/tests/performance/python/performance_test_runner.py"
        
        # 如果配置中没有指定路径，使用默认路径
        if 'script_path' not in config:
            config['script_path'] = default_script_path
        
        super().__init__(config)
    
    def prepare(self) -> bool:
        """准备测试环境"""
        script_path = Path(self.config['script_path'])
        
        # 检查脚本文件是否存在
        if not script_path.exists():
            print(f"错误: 脚本文件不存在 {script_path}")
            return False
        
        print(f"[OK] 找到测试脚本: {script_path.name}")
        
        # 检查Python环境
        try:
            import subprocess
            result = subprocess.run(['python', '--version'], 
                                   capture_output=True, text=True, timeout=5)
            print(f"[OK] Python环境: {result.stdout.strip()}")
        except Exception as e:
            print(f"错误: Python环境检查失败 - {e}")
            return False
        
        # 检查依赖脚本
        dependencies = self.config.get('dependencies', [])
        for dep in dependencies:
            dep_path = Path(dep)
            if dep_path.exists():
                print(f"[OK] 依赖文件: {dep_path.name}")
            else:
                print(f"[WARN]️ 缺少依赖: {dep_path.name}")
        
        return True
    
    def execute(self) -> TestExecution:
        """执行性能测试"""
        import subprocess
        import time
        
        script_path = self.config['script_path']
        
        # 构建命令参数
        # test-agent-2的脚本支持: -s (scenario), -o (output)
        scenario = self.config.get('scenario', 'medium')  # low, medium, high
        output_file = self.config.get('output_file', 'performance_results.json')
        
        args = [
            'python', script_path,
            '-s', scenario,
            '-o', output_file
        ]
        
        # 添加其他参数（如果有）
        extra_args = self.config.get('extra_args', [])
        args.extend(extra_args)
        
        print(f"执行命令: {' '.join(args)}")
        
        try:
            result = subprocess.run(
                args,
                capture_output=True,
                text=True,
                timeout=self.config.get('timeout', 600),
                cwd=Path(script_path).parent
            )
            
            if result.returncode != 0:
                print(f"错误: 执行失败")
                print(f"stderr: {result.stderr}")
                raise RuntimeError(f"脚本执行失败: {result.stderr}")
            
            print(f"[OK] 执行成功")
            print(f"输出: {result.stdout[:500]}...")
            
        except subprocess.TimeoutExpired:
            print("错误: 执行超时")
            raise RuntimeError("脚本执行超时")
        
        return self.execution
    
    def collect_results(self) -> list:
        """收集测试结果"""
        import json
        
        output_file = self.config.get('output_file', 'performance_results.json')
        output_path = Path(output_file)
        
        if not output_path.exists():
            print(f"[WARN]️ 输出文件不存在: {output_file}")
            return []
        
        # 读取结果文件
        try:
            with open(output_path, 'r', encoding='utf-8') as f:
                results_data = json.load(f)
            
            print(f"[OK] 成功读取结果文件")
            
            # 转换为TestResult对象
            # 这里需要根据test-agent-2的实际输出格式调整
            test_results = []
            
            # 示例：假设每个测试场景都是一个结果
            if 'scenarios' in results_data:
                for scenario in results_data['scenarios']:
                    result = TestResult(
                        test_id=f"perf_{scenario['name']}_{int(time.time())}",
                        test_name=f"性能测试 - {scenario['name']}",
                        status='passed' if scenario.get('success', False) else 'failed',
                        duration=scenario.get('duration', 0),
                        start_time=datetime.now(),
                        end_time=datetime.now(),
                        metrics={
                            'throughput': scenario.get('throughput', 0),
                            'response_time_avg': scenario.get('avg_response_time', 0),
                            'error_rate': scenario.get('error_rate', 0)
                        }
                    )
                    test_results.append(result)
            
            # 如果是单个结果
            elif 'metrics' in results_data:
                result = TestResult(
                    test_id=f"perf_{int(time.time())}",
                    test_name="性能测试",
                    status='passed' if results_data.get('success', False) else 'failed',
                    duration=results_data.get('duration', 0),
                    start_time=datetime.now(),
                    end_time=datetime.now(),
                    metrics=results_data.get('metrics', {}),
                    details=results_data
                )
                test_results.append(result)
            
            print(f"[OK] 收集到 {len(test_results)} 个测试结果")
            return test_results
            
        except Exception as e:
            print(f"错误: 结果文件解析失败 - {e}")
            return []
    
    def cleanup(self) -> bool:
        """清理测试环境"""
        # 清理临时文件
        output_file = self.config.get('output_file', 'performance_results.json')
        output_path = Path(output_file)
        
        if output_path.exists():
            # 保留结果文件，不删除
            print(f"[OK] 保留结果文件: {output_file}")
        
        return True

class TestDataGeneratorAdapter(TestScript):
    """
    test-agent-2的test_data_generator.py适配器
    
    此适配器将测试数据生成脚本集成到统一框架中。
    """
    
    def __init__(self, config):
        default_script_path = "I:/AI-Ready/tests/performance/python/test_data_generator.py"
        if 'script_path' not in config:
            config['script_path'] = default_script_path
        super().__init__(config)
    
    def prepare(self) -> bool:
        """准备数据生成环境"""
        script_path = Path(self.config['script_path'])
        
        if not script_path.exists():
            print(f"错误: 脚本文件不存在 {script_path}")
            return False
        
        print(f"[OK] 找到数据生成脚本: {script_path.name}")
        
        # 检查输出目录
        output_dir = self.config.get('output_dir', './data/generated')
        output_path = Path(output_dir)
        
        if not output_path.exists():
            print(f"创建输出目录: {output_dir}")
            output_path.mkdir(parents=True, exist_ok=True)
        
        return True
    
    def execute(self) -> TestExecution:
        """执行数据生成"""
        import subprocess
        
        script_path = self.config['script_path']
        output_dir = self.config.get('output_dir', './data/generated')
        
        args = ['python', script_path]
        
        # 添加数据生成参数（根据test-agent-2的实际参数）
        num_users = self.config.get('num_users', 1000)
        num_products = self.config.get('num_products', 500)
        
        args.extend(['--users', str(num_users)])
        args.extend(['--products', str(num_products)])
        args.extend(['--output', output_dir])
        
        print(f"执行命令: {' '.join(args)}")
        
        try:
            result = subprocess.run(
                args,
                capture_output=True,
                text=True,
                timeout=300,
                cwd=Path(script_path).parent
            )
            
            if result.returncode != 0:
                raise RuntimeError(f"数据生成失败: {result.stderr}")
            
            print(f"[OK] 数据生成成功")
            
        except subprocess.TimeoutExpired:
            raise RuntimeError("数据生成超时")
        
        return self.execution
    
    def collect_results(self) -> list:
        """收集生成结果"""
        output_dir = self.config.get('output_dir', './data/generated')
        output_path = Path(output_dir)
        
        # 统计生成的文件
        generated_files = list(output_path.glob('*.json')) + list(output_path.glob('*.csv'))
        
        result = TestResult(
            test_id=f"data_gen_{int(time.time())}",
            test_name="测试数据生成",
            status='passed' if len(generated_files) > 0 else 'failed',
            duration=0,
            start_time=datetime.now(),
            end_time=datetime.now(),
            metrics={
                'files_generated': len(generated_files),
                'output_dir': str(output_dir)
            },
            details={'files': [f.name for f in generated_files]}
        )
        
        return [result]
    
    def cleanup(self) -> bool:
        """清理生成环境"""
        # 不删除生成的数据文件
        return True

class PerformanceMonitorAdapter(TestScript):
    """
    test-agent-2的performance_monitor.py适配器
    
    此适配器将性能监控脚本集成到统一框架中。
    """
    
    def __init__(self, config):
        default_script_path = "I:/AI-Ready/tests/performance/python/performance_monitor.py"
        if 'script_path' not in config:
            config['script_path'] = default_script_path
        super().__init__(config)
    
    def prepare(self) -> bool:
        """准备监控环境"""
        script_path = Path(self.config['script_path'])
        
        if not script_path.exists():
            print(f"错误: 脚本文件不存在 {script_path}")
            return False
        
        print(f"[OK] 找到监控脚本: {script_path.name}")
        
        # 检查监控目标（如果有）
        monitor_targets = self.config.get('monitor_targets', [])
        if monitor_targets:
            print(f"监控目标: {monitor_targets}")
        
        return True
    
    def execute(self) -> TestExecution:
        """执行性能监控"""
        import subprocess
        
        script_path = self.config['script_path']
        duration = self.config.get('monitor_duration', 60)  # 监控持续时间（秒）
        
        args = [
            'python', script_path,
            '--duration', str(duration)
        ]
        
        # 添加监控参数
        monitor_type = self.config.get('monitor_type', 'all')
        args.extend(['--type', monitor_type])
        
        print(f"执行命令: {' '.join(args)}")
        print(f"监控持续时间: {duration}秒")
        
        try:
            result = subprocess.run(
                args,
                capture_output=True,
                text=True,
                timeout=duration + 60,  # 给予额外时间
                cwd=Path(script_path).parent
            )
            
            if result.returncode != 0:
                raise RuntimeError(f"监控执行失败: {result.stderr}")
            
            print(f"[OK] 监控执行完成")
            
        except subprocess.TimeoutExpired:
            print("[WARN]️ 监控超时")
        
        return self.execution
    
    def collect_results(self) -> list:
        """收集监控结果"""
        # 监控结果通常保存在日志文件或数据库中
        # 这里简化处理
        
        result = TestResult(
            test_id=f"monitor_{int(time.time())}",
            test_name="性能监控",
            status='passed',
            duration=self.config.get('monitor_duration', 60),
            start_time=datetime.now(),
            end_time=datetime.now(),
            metrics={
                'monitor_duration': self.config.get('monitor_duration', 60),
                'monitor_type': self.config.get('monitor_type', 'all')
            }
        )
        
        return [result]
    
    def cleanup(self) -> bool:
        """清理监控环境"""
        return True

def integrate_test_agent_2_scripts():
    """
    集成test-agent-2的所有脚本到统一管理器
    
    返回: 配置好的TestScriptManager实例
    """
    manager = TestScriptManager()
    
    # 配置test-agent-2的脚本
    configs = {
        'performance_test': {
            'script_id': 'performance_test',
            'scenario': 'medium',
            'output_file': './results/performance_test.json',
            'timeout': 600,
            'dependencies': []
        },
        'data_generator': {
            'script_id': 'data_generator',
            'num_users': 1000,
            'num_products': 500,
            'output_dir': './data/generated'
        },
        'performance_monitor': {
            'script_id': 'performance_monitor',
            'monitor_duration': 60,
            'monitor_type': 'all'
        }
    }
    
    # 注册脚本
    print("注册test-agent-2脚本...")
    
    perf_test = PerformanceTestRunnerAdapter(configs['performance_test'])
    manager.register_script('test_agent_2_performance', perf_test)
    print("[OK] 性能测试脚本已注册")
    
    data_gen = TestDataGeneratorAdapter(configs['data_generator'])
    manager.register_script('test_agent_2_data_gen', data_gen)
    print("[OK] 数据生成脚本已注册")
    
    perf_monitor = PerformanceMonitorAdapter(configs['performance_monitor'])
    manager.register_script('test_agent_2_monitor', perf_monitor)
    print("[OK] 性能监控脚本已注册")
    
    print(f"\n总共注册了 {len(manager.scripts)} 个脚本")
    return manager

# 使用示例
if __name__ == "__main__":
    print("test-agent-2脚本集成适配器模板")
    print("=" * 60)
    
    # 创建管理器并注册脚本
    manager = integrate_test_agent_2_scripts()
    
    # 显示已注册的脚本
    print("\n已注册的脚本:")
    for script_id in manager.scripts:
        print(f"  • {script_id}")
    
    print("\n下一步:")
    print("1. 等待test-agent-2提供实际脚本清单")
    print("2. 根据实际脚本调整适配器参数")
    print("3. 测试集成后的脚本执行")
    print("4. 开始实际的测试工作")
    
    print("\n提示: 此模板已经准备好与test-agent-2的实际脚本集成")
    print("只需根据实际脚本的具体参数进行微调即可使用")