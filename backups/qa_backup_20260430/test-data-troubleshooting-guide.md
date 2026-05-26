# 测试数据问题排查指南

## 1. 概述

本文档提供测试数据相关问题的排查步骤和解决方案，帮助用户快速定位和解决测试数据相关问题。

## 2. 问题分类

### 2.1 数据生成问题

#### 症状：
- 数据生成失败
- 生成的数据不正确
- 生成速度过慢
- 内存或资源耗尽

#### 常见原因：
1. 配置参数错误
2. 模板文件损坏
3. 资源限制
4. 算法问题

### 2.2 数据质量问题

#### 症状：
- 数据完整性检查失败
- 数据格式不正确
- 业务规则违反
- 数据关联错误

#### 常见原因：
1. 数据生成规则错误
2. 数据验证规则不完整
3. 业务逻辑变更未同步
4. 数据源问题

### 2.3 数据管理问题

#### 症状：
- 数据版本冲突
- 数据清理失败
- 备份恢复问题
- 权限访问问题

#### 常见原因：
1. 版本控制问题
2. 文件系统权限
3. 网络连接问题
4. 配置错误

## 3. 排查工具

### 3.1 日志工具

```python
# 数据生成日志查看工具
def view_generation_logs(log_file="test-data-generation.log", lines=100):
    """查看数据生成日志"""
    import subprocess
    
    try:
        result = subprocess.run(
            f"tail -n {lines} {log_file}",
            shell=True,
            capture_output=True,
            text=True
        )
        print("=== 数据生成日志 ===")
        print(result.stdout)
        
        if result.stderr:
            print("=== 错误信息 ===")
            print(result.stderr)
    except Exception as e:
        print(f"查看日志失败: {e}")

# 质量检查日志查看工具
def view_quality_logs(log_file="data-quality-check.log", filter_level="ERROR"):
    """查看质量检查日志"""
    import subprocess
    
    try:
        if filter_level:
            cmd = f"grep -i {filter_level} {log_file} | head -50"
        else:
            cmd = f"tail -100 {log_file}"
        
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        print(f"=== 质量检查日志 ({filter_level}) ===")
        print(result.stdout)
    except Exception as e:
        print(f"查看日志失败: {e}")
```

### 3.2 诊断工具

```python
# 数据生成诊断工具
def diagnose_generation_issue(config_file, data_type):
    """诊断数据生成问题"""
    print(f"=== 诊断数据生成问题: {data_type} ===")
    
    # 1. 检查配置文件
    print("1. 检查配置文件...")
    if not os.path.exists(config_file):
        print(f"   ❌ 配置文件不存在: {config_file}")
        return False
    
    # 2. 验证配置格式
    print("2. 验证配置格式...")
    try:
        with open(config_file, 'r') as f:
            config = json.load(f)
        print("   ✅ 配置文件格式正确")
    except json.JSONDecodeError as e:
        print(f"   ❌ 配置文件JSON格式错误: {e}")
        return False
    
    # 3. 检查必要参数
    print("3. 检查必要参数...")
    required_params = ["template", "count", "output"]
    missing_params = []
    
    for param in required_params:
        if param not in config:
            missing_params.append(param)
    
    if missing_params:
        print(f"   ❌ 缺少必要参数: {missing_params}")
        return False
    else:
        print("   ✅ 所有必要参数都存在")
    
    # 4. 检查模板文件
    print("4. 检查模板文件...")
    template_file = config.get("template")
    if template_file and os.path.exists(template_file):
        print(f"   ✅ 模板文件存在: {template_file}")
        
        # 检查模板文件内容
        try:
            with open(template_file, 'r') as f:
                template_content = f.read()
            if not template_content.strip():
                print("   ⚠️ 模板文件为空")
            else:
                print("   ✅ 模板文件有内容")
        except Exception as e:
            print(f"   ❌ 读取模板文件失败: {e}")
    else:
        print(f"   ❌ 模板文件不存在: {template_file}")
        return False
    
    # 5. 检查输出目录
    print("5. 检查输出目录...")
    output_path = config.get("output")
    if output_path:
        output_dir = os.path.dirname(output_path)
        if output_dir and not os.path.exists(output_dir):
            print(f"   ⚠️ 输出目录不存在，尝试创建: {output_dir}")
            try:
                os.makedirs(output_dir, exist_ok=True)
                print("   ✅ 输出目录创建成功")
            except Exception as e:
                print(f"   ❌ 创建输出目录失败: {e}")
                return False
        else:
            print("   ✅ 输出目录存在")
    else:
        print("   ⚠️ 未指定输出路径")
    
    print("=== 诊断完成 ===")
    return True

# 数据质量诊断工具
def diagnose_quality_issue(data_file, quality_rules_file):
    """诊断数据质量问题"""
    print(f"=== 诊断数据质量问题: {data_file} ===")
    
    # 1. 检查数据文件
    print("1. 检查数据文件...")
    if not os.path.exists(data_file):
        print(f"   ❌ 数据文件不存在: {data_file}")
        return False
    
    # 检查文件大小
    file_size = os.path.getsize(data_file)
    if file_size == 0:
        print(f"   ❌ 数据文件为空: {file_size} bytes")
        return False
    else:
        print(f"   ✅ 数据文件大小: {file_size} bytes")
    
    # 2. 检查数据格式
    print("2. 检查数据格式...")
    try:
        with open(data_file, 'r') as f:
            # 尝试读取第一行判断格式
            first_line = f.readline().strip()
            
            if data_file.endswith('.json'):
                # JSON格式检查
                f.seek(0)
                data = json.load(f)
                print(f"   ✅ JSON格式正确，包含 {len(data)} 条记录")
            elif data_file.endswith('.csv'):
                # CSV格式检查
                import csv
                f.seek(0)
                reader = csv.reader(f)
                headers = next(reader)
                row_count = sum(1 for _ in reader) + 1  # 包括标题行
                print(f"   ✅ CSV格式正确，标题: {headers}, 行数: {row_count}")
            else:
                print(f"   ⚠️ 未知文件格式: {data_file}")
    except Exception as e:
        print(f"   ❌ 数据格式检查失败: {e}")
        return False
    
    # 3. 检查质量规则文件
    print("3. 检查质量规则文件...")
    if not os.path.exists(quality_rules_file):
        print(f"   ❌ 质量规则文件不存在: {quality_rules_file}")
        return False
    
    try:
        with open(quality_rules_file, 'r') as f:
            rules = json.load(f)
        print(f"   ✅ 质量规则文件正确，包含 {len(rules)} 条规则")
    except Exception as e:
        print(f"   ❌ 质量规则文件格式错误: {e}")
        return False
    
    # 4. 抽样检查数据
    print("4. 抽样检查数据...")
    try:
        sample_size = min(10, len(data) if isinstance(data, list) else 10)
        
        if isinstance(data, list):
            print(f"   抽样检查前 {sample_size} 条记录:")
            for i in range(sample_size):
                record = data[i]
                print(f"   记录 {i+1}: {list(record.keys())[:3]}...")
        else:
            print(f"   数据结构类型: {type(data).__name__}")
    except Exception as e:
        print(f"   ⚠️ 抽样检查失败: {e}")
    
    print("=== 诊断完成 ===")
    return True
```

### 3.3 修复工具

```python
# 数据修复工具
class DataRepairTool:
    """数据修复工具"""
    
    def fix_data_format(self, data_file, target_format="json"):
        """修复数据格式"""
        print(f"修复数据格式: {data_file} -> {target_format}")
        
        try:
            # 读取原始数据
            if data_file.endswith('.json'):
                with open(data_file, 'r') as f:
                    data = json.load(f)
            elif data_file.endswith('.csv'):
                import pandas as pd
                data = pd.read_csv(data_file).to_dict('records')
            else:
                print(f"不支持的文件格式: {data_file}")
                return False
            
            # 转换格式
            if target_format == "json":
                output_file = data_file.replace('.csv', '.json').replace('.txt', '.json')
                with open(output_file, 'w') as f:
                    json.dump(data, f, indent=2, ensure_ascii=False)
                print(f"已转换为JSON格式: {output_file}")
                
            elif target_format == "csv":
                output_file = data_file.replace('.json', '.csv').replace('.txt', '.csv')
                import pandas as pd
                df = pd.DataFrame(data)
                df.to_csv(output_file, index=False, encoding='utf-8')
                print(f"已转换为CSV格式: {output_file}")
            
            return True
            
        except Exception as e:
            print(f"格式转换失败: {e}")
            return False
    
    def fix_missing_fields(self, data, field_rules):
        """修复缺失字段"""
        print("修复缺失字段...")
        
        fixed_data = []
        fixed_count = 0
        
        for record in data:
            fixed_record = record.copy()
            
            for field, rule in field_rules.items():
                if field not in fixed_record or fixed_record[field] is None:
                    # 根据规则生成默认值
                    if "default" in rule:
                        fixed_record[field] = rule["default"]
                        fixed_count += 1
                    elif "generator" in rule:
                        # 使用生成器生成值
                        generator = rule["generator"]
                        if callable(generator):
                            fixed_record[field] = generator()
                        elif isinstance(generator, str):
                            if generator == "auto_increment":
                                fixed_record[field] = len(fixed_data) + 1
                            elif generator == "timestamp":
                                fixed_record[field] = datetime.now().isoformat()
                        fixed_count += 1
            
            fixed_data.append(fixed_record)
        
        print(f"修复了 {fixed_count} 个缺失字段")
        return fixed_data
    
    def fix_data_validation(self, data, validation_rules):
        """修复数据验证问题"""
        print("修复数据验证问题...")
        
        fixed_data = []
        fixed_count = 0
        
        for record in data:
            fixed_record = record.copy()
            
            for field, rules in validation_rules.items():
                if field not in fixed_record:
                    continue
                
                value = fixed_record[field]
                if value is None:
                    continue
                
                # 检查格式
                if "format" in rules:
                    format_validator = getattr(self, f"validate_{rules['format']}", None)
                    if format_validator and not format_validator(value):
                        # 尝试修复格式
                        fixed_value = self.fix_format(value, rules["format"])
                        if fixed_value != value:
                            fixed_record[field] = fixed_value
                            fixed_count += 1
                
                # 检查范围
                if "min" in rules or "max" in rules:
                    min_val = rules.get("min")
                    max_val = rules.get("max")
                    
                    try:
                        num_value = float(value)
                        needs_fix = False
                        
                        if min_val is not None and num_value < min_val:
                            fixed_record[field] = min_val
                            needs_fix = True
                        elif max_val is not None and num_value > max_val:
                            fixed_record[field] = max_val
                            needs_fix = True
                        
                        if needs_fix:
                            fixed_count += 1
                    except ValueError:
                        pass
            
            fixed_data.append(fixed_record)
        
        print(f"修复了 {fixed_count} 个验证问题")
        return fixed_data
    
    def fix_format(self, value, format_type):
        """修复格式"""
        if format_type == "email":
            # 修复邮箱格式
            if "@" not in value:
                return f"{value}@example.com"
        
        elif format_type == "phone":
            # 修复手机号格式
            import re
            digits = re.sub(r'\D', '', str(value))
            if len(digits) == 11:
                return digits
            elif len(digits) > 11:
                return digits[:11]
        
        elif format_type == "date":
            # 修复日期格式
            try:
                from datetime import datetime
                # 尝试多种日期格式
                formats = [
                    "%Y-%m-%d",
                    "%Y/%m/%d",
                    "%d-%m-%Y",
                    "%d/%m/%Y",
                    "%Y%m%d"
                ]
                
                for fmt in formats:
                    try:
                        dt = datetime.strptime(str(value), fmt)
                        return dt.strftime("%Y-%m-%d")
                    except ValueError:
                        continue
            except:
                pass
        
        return value
```

## 4. 常见问题解决方案

### 4.1 数据生成失败

#### 问题：内存不足
```
症状: 数据生成过程中内存使用持续增长，最终进程被杀死
错误信息: MemoryError, Killed, OutOfMemoryError
```

**解决方案：**
```python
# 方案1: 分批生成
def generate_data_in_batches(total_count, batch_size=1000):
    """分批生成数据"""
    all_data = []
    
    for batch_num in range(0, total_count, batch_size):
        current_batch_size = min(batch_size, total_count - batch_num)
        print(f"生成批次 {batch_num//batch_size + 1}, 数量: {current_batch_size}")
        
        batch_data = generate_batch_data(current_batch_size)
        all_data.extend(batch_data)
        
        # 每批生成后可以保存到文件，释放内存
        if (batch_num + batch_size) % (batch_size * 10) == 0:
            save_to_file(all_data, f"data_batch_{batch_num}.json")
            all_data = []  # 清空内存
    
    return all_data

# 方案2: 使用生成器
def generate_data_stream(total_count):
    """使用生成器流式生成数据"""
    for i in range(total_count):
        yield generate_single_record(i)
        
        # 每生成一定数量记录一次进度
        if i % 1000 == 0:
            print(f"已生成 {i}/{total_count} 条记录")

# 方案3: 优化数据结构
def optimize_data_structure(record):
    """优化数据结构减少内存使用"""
    optimized = {}
    
    # 使用更小的数据类型
    for key, value in record.items():
        if isinstance(value, str):
            # 字符串优化
            if len(value) > 100:
                optimized[key] = value[:100] + "..."
            else:
                optimized[key] = value
        elif isinstance(value, (int, float)):
            # 数值优化
            if isinstance(value, float):
                optimized[key] = round(value, 2)  # 减少精度
            else:
                optimized[key] = value
        elif isinstance(value, list):
            # 列表优化
            if len(value) > 10:
                optimized[key] = value[:10]  # 只保留前10个元素
            else:
                optimized[key] = value
        else:
            optimized[key] = value
    
    return optimized
```

#### 问题：生成速度过慢
```
症状: 数据生成过程耗时过长，无法满足测试需求
性能指标: 生成速度 < 100条/秒
```

**解决方案：**
```python
# 方案1: 使用多线程/多进程
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor

def generate_data_parallel(total_count, workers=4):
    """并行生成数据"""
    import math
    
    # 计算每个worker的任务量
    batch_size = math.ceil(total_count / workers)
    
    def worker(worker_id, count):
        """worker函数"""
        worker_data = []
        start_idx = worker_id * batch_size
        
        for i in range(count):
            record = generate_single_record(start_idx + i)
            worker_data.append(record)
        
        return worker_data
    
    # 使用线程池
    all_data = []
    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = []
        
        for worker_id in range(workers):
            actual_count = min(batch_size, total_count - worker_id * batch_size)
            if actual_count > 0:
                future = executor.submit(worker, worker_id, actual_count)
                futures.append(future)
        
        # 收集结果
        for future in futures:
            worker_data = future.result()
            all_data.extend(worker_data)
    
    return all_data

# 方案2: 优化生成算法
def optimize_generation_algorithm():
    """优化生成算法"""
    optimizations = {
        "缓存重复计算": "缓存常用计算结果",
        "减少随机数生成": "批量生成随机数",
        "预计算模板": "预先生成数据模板",
        "使用高效数据结构": "使用数组代替列表，使用集合代替列表查找"
    }
    
    return optimizations

# 方案3: 使用高性能库
def use_high_performance_libraries():
    """使用高性能库"""
    libraries = {
        "numpy": "数值计算和数组操作",
        "pandas": "数据处理和分析",
        "polars": "更快的DataFrame库",
        "numba": "JIT编译加速Python代码"
    }
    
    return libraries
```

### 4.2 数据质量问题

#### 问题：数据完整性不足
```
症状: 必填字段缺失，数据记录不完整
质量指标: 完整性率 < 90%
```

**解决方案：**
```python
# 方案1: 完善数据生成规则
def improve_data_generation_rules():
    """完善数据生成规则"""
    improvements = {
        "必填字段检查": "在生成时检查必填字段",
        "默认值设置": "为可选字段设置合理的默认值",
        "依赖字段处理": "处理字段间的依赖关系",
        "业务规则验证": "生成后验证业务规则"
    }
    
    return improvements

# 方案2: 实施数据验证
def implement_data_validation(data, validation_rules):
    """实施数据验证"""
    validation_results = {
        "passed": [],
        "failed": [],
        "fixed": []
    }
    
    for record in data:
        is_valid = True
        validation_errors = []
        
        for field, rules in validation_rules.items():
            if field not in record or record[field] is None:
                if rules.get("required", False):
                    is_valid = False
                    validation_errors.append(f"缺失必填字段: {field}")
        
        if is_valid:
            validation_results["passed"].append(record)
        else:
            # 尝试自动修复
            fixed_record = auto_fix_record(record, validation_errors)
            if fixed_record:
                validation_results["fixed"].append(fixed_record)
            else:
                validation_results["failed"].append({
                    "record": record,
                    "errors": validation_errors
                })
    
    return validation_results

# 方案3: 建立数据质量监控
def setup_data_quality_monitoring():
    """建立数据质量监控"""
    monitoring_config = {
        "实时监控": "生成过程中实时检查数据质量",
        "定期检查": "定期对现有数据进行检查",
        "阈值告警": "设置质量阈值，触发告警",
        "自动修复": "对可自动修复的问题进行自动修复"
    }
    
    return monitoring_config
```

#### 问题：数据准确性不高
```
症状: 数据格式错误，数值范围不合理，业务逻辑违反
质量指标: 准确率 < 95%
```

**解决方案：**
```python
# 方案1: 加强格式验证
def enhance_format_validation(data, format_rules):
    """加强格式验证"""
    validation_results = []
    
    for record in data:
        record_errors = []
        
        for field, expected_format in format_rules.items():
            if field in record:
                value = record[field]
                
                if expected_format == "email":
                    if not re.match(r'^[^@]+@[^@]+\.[^@]+$', str(value)):
                        record_errors.append(f"邮箱格式错误: {value}")
                
                elif expected_format == "phone":
                    if not re.match(r'^1[3-9]\d{9}$', str(value)):
                        record_errors.append(f"手机号格式错误: {value}")
                
                elif expected_format == "date":
                    try:
                        datetime.strptime(str(value), "%Y-%m-%d")
                    except ValueError:
                        record_errors.append(f"日期格式错误: {value}")
        
        if record_errors:
            validation_results.append({
                "record_id": record.get("id"),
                "errors": record_errors
            })
    
    return validation_results

# 方案2: 实施业务规则检查
def implement_business_rules_check(data, business_rules):
    """实施业务规则检查"""
    check_results = []
    
    for rule_name, rule_check in business_rules.items():
        rule_violations = []
        
        for record in data:
            if not rule_check(record):
                rule_violations.append(record.get("id"))
        
        if rule_violations:
            check_results.append({
                "rule": rule_name,
                "violation_count": len(rule_violations),
                "sample_violations": rule_violations[:5]  # 只显示前5个
            })
    
    return check_results

# 方案3: 建立数据溯源
def setup_data_lineage():
    """建立数据溯源"""
    lineage_system = {
        "数据来源记录": "记录数据的生成来源和过程",
        "变更追踪": "追踪数据的变更历史",
        "质量历史": "记录数据的质量变化历史",
        "影响分析": "分析数据问题的影响范围"
    }
    
    return lineage_system
```

### 4.3 数据管理问题

#### 问题：版本冲突
```
症状: 多人协作时数据版本冲突，合并困难
具体表现: Git合并冲突，数据不一致
```

**解决方案：**
```python
# 方案1: 实施版本控制策略
def implement_version_control_strategy():
    """实施版本控制策略"""
    strategy = {
        "分支策略": {
            "main": "稳定版本",
            "develop": "开发版本",
            "feature/*": "特性分支",
            "release/*": "发布分支"
        },
        "合并策略": {
            "定期合并": "每天合并develop到feature分支",
            "代码审查": "合并前进行代码审查",
            "自动化测试": "合并前运行自动化测试"
        },
        "冲突解决": {
            "冲突检测": "使用工具检测冲突",
            "冲突解决指南": "提供冲突解决指南",
            "人工干预": "复杂冲突人工解决"
        }
    }
    
    return strategy

# 方案2: 使用数据版本管理工具
def use_data_version_tool():
    """使用数据版本管理工具"""
    tools = {
        "DVC (Data Version Control)": "专门用于数据版本控制",
        "Git LFS": "Git大文件存储",
        "自定义工具": "根据需求自定义工具"
    }
    
    return tools

# 方案3: 建立协作规范
def setup_collaboration_guidelines():
    """建立协作规范"""
    guidelines = {
        "工作流程": "明确的数据修改和提交流程",
        "责任分工": "明确各成员的责任分工",
        "沟通机制": "定期的沟通和同步机制",
        "文档要求": "完善的文档记录要求"
    }
    
    return guidelines
```

#### 问题：数据清理失败
```
症状: 数据清理过程出错，残留测试数据
具体表现: 清理脚本失败，数据库残留数据
```

**解决方案：**
```python
# 方案1: 完善清理脚本
def improve_cleanup_script():
    """完善清理脚本"""
    improvements = {
        "错误处理": "添加完善的错误处理机制",
        "回滚机制": "清理失败时能够回滚",
        "日志记录": "详细的清理过程日志",
        "状态检查": "清理前后的状态检查"
    }
    
    return improvements

# 方案2: 实施清理策略
def implement_cleanup_strategy():
    """实施清理策略"""
    strategy = {
        "分层清理": {
            "临时数据": "每次测试后清理",
            "测试数据": "每天清理",
            "归档数据": "每月清理"
        },
        "备份机制": {
            "清理前备份": "重要数据清理前备份",
            "备份验证": "验证备份完整性",
            "备份清理": "定期清理旧备份"
        },
        "监控告警": {
            "清理监控": "监控清理过程",
            "异常告警": "清理异常时告警",
            "效果评估": "评估清理效果"
        }
    }
    
    return strategy

# 方案3: 使用容器化技术
def use_containerization():
    """使用容器化技术"""
    benefits = {
        "环境隔离": "每个测试在独立容器中运行",
        "快速重置": "测试完成后快速重置环境",
        "资源控制": "精确控制资源使用",
        "版本管理": "容器镜像版本管理"
    }
    
    return benefits
```

## 5. 故障排查流程

### 5.1 标准排查流程

```python
# 标准故障排查流程
def standard_troubleshooting_process(problem_description):
    """标准故障排查流程"""
    steps = [
        {
            "step": 1,
            "action": "问题确认",
            "description": "确认问题的具体表现和影响范围",
            "checkpoints": [
                "问题发生时间",
                "影响的功能模块",
                "错误信息",
                "复现步骤"
            ]
        },
        {
            "step": 2,
            "action": "信息收集",
            "description": "收集相关日志和监控数据",
            "checkpoints": [
                "应用程序日志",
                "系统日志",
                "监控指标",
                "相关配置"
            ]
        },
        {
            "step": 3,
            "action": "问题分析",
            "description": "分析问题原因和根本原因",
            "checkpoints": [
                "日志分析",
                "代码审查",
                "数据检查",
                "环境检查"
            ]
        },
        {
            "step": 4,
            "action": "解决方案",
            "description": "制定和实施解决方案",
            "checkpoints": [
                "临时解决方案",
                "根本解决方案",
                "实施计划",
                "回滚计划"
            ]
        },
        {
            "step": 5,
            "action": "验证测试",
            "description": "验证解决方案的有效性",
            "checkpoints": [
                "功能测试",
                "性能测试",
                "回归测试",
                "监控验证"
            ]
        },
        {
            "step": 6,
            "action": "总结改进",
            "description": "总结经验和改进措施",
            "checkpoints": [
                "问题总结",
                "经验教训",
                "改进措施",
                "文档更新"
            ]
        }
    ]
    
    return steps
```

### 5.2 紧急故障处理

```python
# 紧急故障处理流程
def emergency_troubleshooting(severity="high"):
    """紧急故障处理流程"""
    emergency_process = {
        "high": [
            "1. 立即停止受影响的服务",
            "2. 通知相关干系人",
            "3. 收集故障信息",
            "4. 实施临时解决方案",
            "5. 恢复服务运行",
            "6. 分析根本原因",
            "7. 实施永久解决方案"
        ],
        "medium": [
            "1. 评估影响范围",
            "2. 记录故障信息",
            "3. 分析问题原因",
            "4. 制定解决方案",
            "5. 实施解决方案",
            "6. 验证解决效果",
            "7. 更新相关文档"
        ],
        "low": [
            "1. 记录问题现象",
            "2. 分析可能原因",
            "3. 安排修复计划",
            "4. 实施修复",
            "5. 验证修复效果"
        ]
    }
    
    return emergency_process.get(severity, emergency_process["medium"])
```

### 5.3 预防性维护

```python
# 预防性维护计划
def preventive_maintenance_plan():
    """预防性维护计划"""
    plan = {
        "daily": [
            "检查系统日志是否有异常",
            "监控数据生成任务状态",
            "检查数据质量报告",
            "验证备份是否成功"
        ],
        "weekly": [
            "清理过期测试数据",
            "优化数据库索引",
            "更新数据生成模板",
            "审查数据质量规则"
        ],
        "monthly": [
            "全面数据质量检查",
            "性能优化和调优",
            "安全审计和检查",
            "工具版本升级"
        ],
        "quarterly": [
            "架构审查和优化",
            "容量规划和扩展",
            "灾难恢复演练",
            "团队技能培训"
        ]
    }
    
    return plan
```

## 6. 工具和资源

### 6.1 内置工具

```python
# 内置故障排查工具
class BuiltinTroubleshootingTools:
    """内置故障排查工具"""
    
    def __init__(self):
        self.tools = {
            "log_analyzer": self.analyze_logs,
            "config_checker": self.check_config,
            "performance_monitor": self.monitor_performance,
            "data_validator": self.validate_data
        }
    
    def analyze_logs(self, log_file, pattern=None):
        """分析日志文件"""
        import re
        
        results = {
            "errors": [],
            "warnings": [],
            "info": [],
            "pattern_matches": []
        }
        
        try:
            with open(log_file, 'r') as f:
                for line_num, line in enumerate(f, 1):
                    line = line.strip()
                    
                    # 按级别分类
                    if "ERROR" in line or "错误" in line:
                        results["errors"].append(f"第{line_num}行: {line}")
                    elif "WARN" in line or "警告" in line:
                        results["warnings"].append(f"第{line_num}行: {line}")
                    elif "INFO" in line or "信息" in line:
                        results["info"].append(f"第{line_num}行: {line}")
                    
                    # 模式匹配
                    if pattern and re.search(pattern, line):
                        results["pattern_matches"].append(f"第{line_num}行: {line}")
            
            return results
            
        except Exception as e:
            return {"error": f"分析日志失败: {e}"}
    
    def check_config(self, config_file):
        """检查配置文件"""
        import yaml
        import json
        
        check_results = {
            "file_exists": False,
            "file_format": "unknown",
            "parsing_success": False,
            "validation_errors": [],
            "warnings": []
        }
        
        # 检查文件是否存在
        if not os.path.exists(config_file):
            check_results["validation_errors"].append("配置文件不存在")
            return check_results
        
        check_results["file_exists"] = True
        
        # 确定文件格式
        if config_file.endswith('.yaml') or config_file.endswith('.yml'):
            check_results["file_format"] = "yaml"
            parser = yaml.safe_load
        elif config_file.endswith('.json'):
            check_results["file_format"] = "json"
            parser = json.load
        else:
            check_results["validation_errors"].append("不支持的配置文件格式")
            return check_results
        
        # 解析配置文件
        try:
            with open(config_file, 'r') as f:
                config = parser(f)
            check_results["parsing_success"] = True
        except Exception as e:
            check_results["validation_errors"].append(f"解析配置文件失败: {e}")
            return check_results
        
        # 验证配置内容
        required_fields = ["version", "environment", "settings"]
        for field in required_fields:
            if field not in config:
                check_results["validation_errors"].append(f"缺少必要字段: {field}")
        
        # 检查环境配置
        if "environment" in config:
            env = config["environment"]
            if env not in ["dev", "test", "prod"]:
                check_results["warnings"].append(f"未知环境: {env}")
        
        return check_results
    
    def monitor_performance(self, duration=60):
        """监控性能"""
        import psutil
        import time
        
        performance_data = {
            "cpu_usage": [],
            "memory_usage": [],
            "disk_io": [],
            "network_io": []
        }
        
        start_time = time.time()
        
        while time.time() - start_time < duration:
            # CPU使用率
            cpu_percent = psutil.cpu_percent(interval=1)
            performance_data["cpu_usage"].append(cpu_percent)
            
            # 内存使用
            memory = psutil.virtual_memory()
            performance_data["memory_usage"].append(memory.percent)
            
            # 磁盘IO
            disk_io = psutil.disk_io_counters()
            performance_data["disk_io"].append({
                "read_bytes": disk_io.read_bytes,
                "write_bytes": disk_io.write_bytes
            })
            
            # 网络IO
            net_io = psutil.net_io_counters()
            performance_data["network_io"].append({
                "bytes_sent": net_io.bytes_sent,
                "bytes_recv": net_io.bytes_recv
            })
            
            time.sleep(1)
        
        # 计算统计信息
        stats = {
            "cpu_avg": sum(performance_data["cpu_usage"]) / len(performance_data["cpu_usage"]),
            "cpu_max": max(performance_data["cpu_usage"]),
            "memory_avg": sum(performance_data["memory_usage"]) / len(performance_data["memory_usage"]),
            "memory_max": max(performance_data["memory_usage"])
        }
        
        return stats
    
    def validate_data(self, data_file, schema_file):
        """验证数据"""
        import jsonschema
        
        validation_results = {
            "valid": False,
            "errors": [],
            "warnings": [],
            "checked_count": 0
        }
        
        try:
            # 加载数据
            if data_file.endswith('.json'):
                with open(data_file, 'r') as f:
                    data = json.load(f)
            else:
                validation_results["errors"].append("只支持JSON格式数据")
                return validation_results
            
            # 加载schema
            with open(schema_file, 'r') as f:
                schema = json.load(f)
            
            # 验证数据
            if isinstance(data, list):
                validation_results["checked_count"] = len(data)
                for i, item in enumerate(data):
                    try:
                        jsonschema.validate(item, schema)
                    except jsonschema.ValidationError as e:
                        validation_results["errors"].append(f"第{i+1}条数据: {e.message}")
            else:
                validation_results["checked_count"] = 1
                try:
                    jsonschema.validate(data, schema)
                except jsonschema.ValidationError as e:
                    validation_results["errors"].append(f"数据验证失败: {e.message}")
            
            # 检查结果
            if not validation_results["errors"]:
                validation_results["valid"] = True
            
            return validation_results
            
        except Exception as e:
            validation_results["errors"].append(f"验证过程出错: {e}")
            return validation_results
```

### 6.2 外部工具推荐

```python
# 推荐