#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1 测试环境配置验证脚本
验证所有配置文件的完整性和正确性
"""

import os
import sys
import yaml
import json
import re
from pathlib import Path
from typing import Dict, List, Any, Tuple

# 颜色定义
class Colors:
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    MAGENTA = '\033[95m'
    CYAN = '\033[96m'
    WHITE = '\033[97m'
    RESET = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

def print_color(text: str, color: str = Colors.RESET, end: str = '\n'):
    """打印带颜色的文本"""
    print(f"{color}{text}{Colors.RESET}", end=end)

def print_success(message: str):
    """打印成功信息"""
    print_color(f"✅ {message}", Colors.GREEN)

def print_warning(message: str):
    """打印警告信息"""
    print_color(f"⚠️  {message}", Colors.YELLOW)

def print_error(message: str):
    """打印错误信息"""
    print_color(f"❌ {message}", Colors.RED)

def print_info(message: str):
    """打印信息"""
    print_color(f"ℹ️  {message}", Colors.CYAN)

def print_header(message: str):
    """打印标题"""
    print_color(f"\n{'='*60}", Colors.BLUE)
    print_color(f"{message}", Colors.BLUE + Colors.BOLD)
    print_color(f"{'='*60}", Colors.BLUE)

def validate_yaml_file(file_path: Path) -> Tuple[bool, Dict[str, Any], str]:
    """验证YAML文件"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = yaml.safe_load(f)
        return True, content, "YAML文件格式正确"
    except yaml.YAMLError as e:
        return False, {}, f"YAML解析错误: {str(e)}"
    except Exception as e:
        return False, {}, f"文件读取错误: {str(e)}"

def validate_json_file(file_path: Path) -> Tuple[bool, Dict[str, Any], str]:
    """验证JSON文件"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = json.load(f)
        return True, content, "JSON文件格式正确"
    except json.JSONDecodeError as e:
        return False, {}, f"JSON解析错误: {str(e)}"
    except Exception as e:
        return False, {}, f"文件读取错误: {str(e)}"

def validate_service_cluster_config(config: Dict[str, Any]) -> List[str]:
    """验证服务集群配置"""
    issues = []
    
    # 检查必要字段
    required_fields = ['service_registry', 'microservices', 'load_balancer']
    for field in required_fields:
        if field not in config:
            issues.append(f"缺少必要字段: {field}")
    
    # 检查微服务配置
    if 'microservices' in config:
        microservices = config['microservices']
        required_services = ['user-service', 'order-service', 'inventory-service']
        
        for service in required_services:
            if service not in microservices:
                issues.append(f"缺少必要微服务: {service}")
            else:
                service_config = microservices[service]
                if 'enabled' not in service_config:
                    issues.append(f"{service} 缺少enabled字段")
                elif not service_config['enabled']:
                    issues.append(f"{service} 未启用")
                
                if 'instances' not in service_config:
                    issues.append(f"{service} 缺少instances配置")
                elif not service_config['instances']:
                    issues.append(f"{service} 没有配置实例")
                
                if 'endpoints' not in service_config:
                    issues.append(f"{service} 缺少endpoints配置")
    
    # 检查负载均衡配置
    if 'load_balancer' in config:
        lb_config = config['load_balancer']
        if 'type' not in lb_config:
            issues.append("负载均衡器缺少type配置")
        elif lb_config['type'] not in ['round-robin', 'random', 'weighted', 'least-connections']:
            issues.append(f"不支持的负载均衡类型: {lb_config['type']}")
    
    return issues

def validate_database_cluster_config(config: Dict[str, Any]) -> List[str]:
    """验证数据库集群配置"""
    issues = []
    
    # 检查必要字段
    required_fields = ['primary_database', 'replica_databases', 'read_write_splitting']
    for field in required_fields:
        if field not in config:
            issues.append(f"缺少必要字段: {field}")
    
    # 检查主数据库配置
    if 'primary_database' in config:
        primary = config['primary_database']
        required_db_fields = ['type', 'host', 'port', 'name', 'username']
        for field in required_db_fields:
            if field not in primary:
                issues.append(f"主数据库缺少字段: {field}")
        
        # 检查连接池配置
        if 'connection_pool' in primary:
            pool = primary['connection_pool']
            if 'max_connections' not in pool:
                issues.append("主数据库连接池缺少max_connections")
            elif pool['max_connections'] <= 0:
                issues.append("主数据库max_connections必须大于0")
    
    # 检查从数据库配置
    if 'replica_databases' in config:
        replicas = config['replica_databases']
        if not isinstance(replicas, list):
            issues.append("replica_databases必须是列表")
        elif len(replicas) == 0:
            issues.append("至少需要配置一个从数据库")
        else:
            for i, replica in enumerate(replicas):
                if 'name' not in replica:
                    issues.append(f"从数据库 {i} 缺少name字段")
                if 'host' not in replica:
                    issues.append(f"从数据库 {i} 缺少host字段")
    
    # 检查读写分离配置
    if 'read_write_splitting' in config:
        rw_config = config['read_write_splitting']
        if 'enabled' not in rw_config:
            issues.append("读写分离缺少enabled字段")
        
        if 'strategy' in rw_config:
            strategy = rw_config['strategy']
            valid_strategies = ['round_robin', 'random', 'weighted', 'least_connections']
            if strategy not in valid_strategies:
                issues.append(f"不支持的读写分离策略: {strategy}")
    
    return issues

def validate_message_queue_config(config: Dict[str, Any]) -> List[str]:
    """验证消息队列配置"""
    issues = []
    
    # 检查必要字段
    required_fields = ['message_queue', 'topics', 'consumer_groups', 'producers']
    for field in required_fields:
        if field not in config:
            issues.append(f"缺少必要字段: {field}")
    
    # 检查消息队列类型
    if 'message_queue' in config:
        mq_config = config['message_queue']
        if 'type' not in mq_config:
            issues.append("消息队列缺少type字段")
        elif mq_config['type'] not in ['kafka', 'rabbitmq', 'pulsar']:
            issues.append(f"不支持的消息队列类型: {mq_config['type']}")
    
    # 检查Topic配置
    if 'topics' in config:
        topics = config['topics']
        if not isinstance(topics, list):
            issues.append("topics必须是列表")
        elif len(topics) == 0:
            issues.append("至少需要配置一个Topic")
        else:
            required_topic_fields = ['name', 'partitions', 'replication_factor']
            for i, topic in enumerate(topics):
                for field in required_topic_fields:
                    if field not in topic:
                        issues.append(f"Topic {i} 缺少字段: {field}")
                
                # 检查分区数
                if 'partitions' in topic and topic['partitions'] <= 0:
                    issues.append(f"Topic {topic.get('name', f'#{i}')} 分区数必须大于0")
                
                # 检查复制因子
                if 'replication_factor' in topic and topic['replication_factor'] <= 0:
                    issues.append(f"Topic {topic.get('name', f'#{i}')} 复制因子必须大于0")
    
    # 检查消费者组配置
    if 'consumer_groups' in config:
        groups = config['consumer_groups']
        if not isinstance(groups, list):
            issues.append("consumer_groups必须是列表")
        else:
            for i, group in enumerate(groups):
                if 'name' not in group:
                    issues.append(f"消费者组 {i} 缺少name字段")
                if 'topics' not in group:
                    issues.append(f"消费者组 {i} 缺少topics字段")
                elif not isinstance(group['topics'], list) or len(group['topics']) == 0:
                    issues.append(f"消费者组 {i} 的topics必须是非空列表")
    
    return issues

def validate_integration_test_config(config: Dict[str, Any]) -> List[str]:
    """验证集成测试配置"""
    issues = []
    
    # 检查必要字段
    required_fields = ['environment', 'integration_scenarios', 'service_dependencies']
    for field in required_fields:
        if field not in config:
            issues.append(f"缺少必要字段: {field}")
    
    # 检查环境配置
    if 'environment' in config:
        env_config = config['environment']
        if 'name' not in env_config:
            issues.append("环境配置缺少name字段")
        
        if 'services' in env_config:
            services = env_config['services']
            if not isinstance(services, list):
                issues.append("services必须是列表")
            elif len(services) == 0:
                issues.append("至少需要配置一个服务")
    
    # 检查集成场景
    if 'integration_scenarios' in config:
        scenarios = config['integration_scenarios']
        if not isinstance(scenarios, list):
            issues.append("integration_scenarios必须是列表")
        elif len(scenarios) == 0:
            issues.append("至少需要配置一个集成场景")
        else:
            required_scenario_fields = ['id', 'name', 'description', 'services_involved', 'test_steps']
            for i, scenario in enumerate(scenarios):
                for field in required_scenario_fields:
                    if field not in scenario:
                        issues.append(f"场景 {i} 缺少字段: {field}")
                
                # 检查测试步骤
                if 'test_steps' in scenario:
                    steps = scenario['test_steps']
                    if not isinstance(steps, list):
                        issues.append(f"场景 {scenario.get('name', f'#{i}')} 的test_steps必须是列表")
                    elif len(steps) == 0:
                        issues.append(f"场景 {scenario.get('name', f'#{i}')} 至少需要一个测试步骤")
    
    # 检查服务依赖
    if 'service_dependencies' in config:
        dependencies = config['service_dependencies']
        required_services = ['user-service', 'order-service', 'inventory-service']
        for service in required_services:
            if service not in dependencies:
                issues.append(f"缺少服务依赖配置: {service}")
    
    return issues

def check_directory_structure(base_dir: Path) -> List[str]:
    """检查目录结构"""
    issues = []
    
    required_dirs = [
        'config',
        'scripts',
        'data',
        'logs',
        'reports'
    ]
    
    for dir_name in required_dirs:
        dir_path = base_dir / dir_name
        if not dir_path.exists():
            issues.append(f"缺少目录: {dir_name}")
        elif not dir_path.is_dir():
            issues.append(f"{dir_name} 不是目录")
    
    return issues

def check_required_files(base_dir: Path) -> List[str]:
    """检查必要文件"""
    issues = []
    
    required_files = [
        'config/service_cluster_config.yaml',
        'config/database_cluster_config.yaml',
        'config/message_queue_cluster_config.yaml',
        'config/integration_test_config.yaml',
        'scripts/deploy_cluster.sh',
        'README.md'
    ]
    
    for file_path in required_files:
        full_path = base_dir / file_path
        if not full_path.exists():
            issues.append(f"缺少文件: {file_path}")
        elif not full_path.is_file():
            issues.append(f"{file_path} 不是文件")
    
    return issues

def validate_port_configurations(configs: List[Tuple[str, Dict[str, Any]]]) -> List[str]:
    """验证端口配置，检查冲突"""
    issues = []
    port_usage = {}
    
    for config_name, config in configs:
        # 检查服务集群配置中的端口
        if config_name == 'service_cluster_config':
            if 'microservices' in config:
                for service_name, service_config in config['microservices'].items():
                    if 'instances' in service_config:
                        for instance in service_config['instances']:
                            if 'port' in instance:
                                port = instance['port']
                                service_info = f"{service_name}.{instance.get('id', 'unknown')}"
                                if port in port_usage:
                                    issues.append(f"端口冲突: 端口 {port} 被 {port_usage[port]} 和 {service_info} 使用")
                                else:
                                    port_usage[port] = service_info
                                
                                # 检查端口范围
                                if port < 1024 or port > 65535:
                                    issues.append(f"端口超出范围: {port} (应在1024-65535之间)")
    
    return issues

def validate_configuration_consistency(configs: Dict[str, Dict[str, Any]]) -> List[str]:
    """验证配置一致性"""
    issues = []
    
    # 获取所有配置中的服务名称
    service_cluster_config = configs.get('service_cluster_config', {})
    integration_test_config = configs.get('integration_test_config', {})
    
    # 检查服务集群和集成测试中的服务一致性
    if 'microservices' in service_cluster_config and 'environment' in integration_test_config:
        cluster_services = set(service_cluster_config['microservices'].keys())
        if 'services' in integration_test_config['environment']:
            test_services = set([s.get('name', '') for s in integration_test_config['environment']['services']])
            
            # 检查测试环境中有但集群中没有的服务
            missing_in_cluster = test_services - cluster_services
            if missing_in_cluster:
                issues.append(f"测试环境中有但服务集群中缺少的服务: {', '.join(missing_in_cluster)}")
            
            # 检查集群中有但测试环境中没有的服务（不是错误，只是警告）
            missing_in_test = cluster_services - test_services
            if missing_in_test:
                print_warning(f"服务集群中有但测试环境中缺少的服务: {', '.join(missing_in_test)}")
    
    return issues

def main():
    """主函数"""
    print_header("Sprint 27+1 测试环境配置验证")
    print_info("开始验证测试环境配置...")
    
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    base_dir = script_dir.parent
    
    print_info(f"基础目录: {base_dir}")
    
    all_issues = []
    config_files = {}
    
    # 1. 检查目录结构
    print_header("1. 检查目录结构")
    dir_issues = check_directory_structure(base_dir)
    if dir_issues:
        for issue in dir_issues:
            print_error(issue)
        all_issues.extend(dir_issues)
    else:
        print_success("目录结构检查通过")
    
    # 2. 检查必要文件
    print_header("2. 检查必要文件")
    file_issues = check_required_files(base_dir)
    if file_issues:
        for issue in file_issues:
            print_error(issue)
        all_issues.extend(file_issues)
    else:
        print_success("必要文件检查通过")
    
    # 3. 验证配置文件
    print_header("3. 验证配置文件")
    
    config_files_to_check = [
        ('service_cluster_config', base_dir / 'config' / 'service_cluster_config.yaml', validate_service_cluster_config),
        ('database_cluster_config', base_dir / 'config' / 'database_cluster_config.yaml', validate_database_cluster_config),
        ('message_queue_config', base_dir / 'config' / 'message_queue_cluster_config.yaml', validate_message_queue_config),
        ('integration_test_config', base_dir / 'config' / 'integration_test_config.yaml', validate_integration_test_config),
    ]
    
    for config_name, file_path, validator in config_files_to_check:
        print_info(f"验证 {config_name}...")
        
        if not file_path.exists():
            print_error(f"文件不存在: {file_path}")
            all_issues.append(f"{config_name}: 文件不存在")
            continue
        
        # 验证YAML格式
        is_valid, config, message = validate_yaml_file(file_path)
        if not is_valid:
            print_error(f"{config_name}: {message}")
            all_issues.append(f"{config_name}: {message}")
            continue
        
        print_success(f"{config_name}: YAML格式正确")
        config_files[config_name] = config
        
        # 验证配置内容
        issues = validator(config)
        if issues:
            for issue in issues:
                print_error(f"{config_name}: {issue}")
                all_issues.append(f"{config_name}: {issue}")
        else:
            print_success(f"{config_name}: 配置内容检查通过")
    
    # 4. 验证端口配置
    print_header("4. 验证端口配置")
    if config_files:
        config_list = [(name, config) for name, config in config_files.items()]
        port_issues = validate_port_configurations(config_list)
        if port_issues:
            for issue in port_issues:
                print_error(issue)
                all_issues.append(f"端口配置: {issue}")
        else:
            print_success("端口配置检查通过")
    
    # 5. 验证配置一致性
    print_header("5. 验证配置一致性")
    if len(config_files) >= 2:
        consistency_issues = validate_configuration_consistency(config_files)
        if consistency_issues:
            for issue in consistency_issues:
                print_error(issue)
                all_issues.append(f"配置一致性: {issue}")
        else:
            print_success("配置一致性检查通过")
    else:
        print_warning("配置不足，跳过配置一致性检查")
    
    # 6. 总结
    print_header("验证结果")
    
    if all_issues:
        print_error(f"发现 {len(all_issues)} 个问题:")
        for i, issue in enumerate(all_issues, 1):
            print_color(f"{i:3d}. {issue}", Colors.RED)
        
        print_color("\n❌ 配置验证失败，请修复上述问题后再试。", Colors.RED + Colors.BOLD)
        return 1
    else:
        print_success("✅ 所有配置验证通过！")
        print_info("\n配置验证摘要:")
        print_success(f"  目录结构: 通过")
        print_success(f"  必要文件: 通过")
        print_success(f"  配置文件: {len(config_files_to_check)}/{len(config_files_to_check)} 通过")
        print_success(f"  端口配置: 通过")
        print_success(f"  配置一致性: 通过")
        
        print_info("\n🎉 测试环境配置准备就绪，可以开始部署！")
        print_info("运行以下命令开始部署:")
        print_color("  ./scripts/deploy_cluster.sh --all --validate", Colors.GREEN + Colors.BOLD)
        
        return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print_color("\n\n⚠️  用户中断操作", Colors.YELLOW)
        sys.exit(130)
    except Exception as e:
        print_color(f"\n❌ 发生未预期的错误: {str(e)}", Colors.RED)
        import traceback
        traceback.print_exc()
        sys.exit(1)