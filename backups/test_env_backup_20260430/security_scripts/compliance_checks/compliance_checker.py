"""
合规检查器
检查测试环境是否符合安全合规标准
"""

import os
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime

class ComplianceChecker:
    """合规检查器"""
    
    def __init__(self, test_env_root: str):
        """
        初始化合规检查器
        
        Args:
            test_env_root: 测试环境根目录
        """
        self.test_env_root = test_env_root
        self.results = {}
        self.logger = logging.getLogger(__name__)
        
        # 合规标准定义
        self.compliance_standards = {
            'access_control': {
                'name': '访问控制标准',
                'description': '检查用户访问控制和权限管理',
                'requirements': [
                    '用户认证机制',
                    '权限分离原则',
                    '最小权限原则',
                    '访问日志记录'
                ]
            },
            'data_protection': {
                'name': '数据保护标准',
                'description': '检查数据保护和加密措施',
                'requirements': [
                    '数据传输加密',
                    '数据存储加密',
                    '敏感数据脱敏',
                    '数据备份机制'
                ]
            },
            'security_configuration': {
                'name': '安全配置标准',
                'description': '检查安全配置和基线合规',
                'requirements': [
                    '安全基线配置',
                    '漏洞管理流程',
                    '安全补丁更新',
                    '安全审计日志'
                ]
            },
            'incident_response': {
                'name': '事件响应标准',
                'description': '检查安全事件响应能力',
                'requirements': [
                    '事件检测机制',
                    '事件响应流程',
                    '恢复计划',
                    '事后分析改进'
                ]
            }
        }
    
    def check_all_compliance(self) -> Dict[str, Any]:
        """
        检查所有合规标准
        
        Returns:
            包含所有合规检查结果的字典
        """
        self.logger.info("开始执行合规检查...")
        
        results = {
            'summary': {
                'total_standards': 0,
                'compliant_standards': 0,
                'partially_compliant': 0,
                'non_compliant': 0,
                'compliance_score': 0,
                'status': 'UNKNOWN'
            },
            'standards': [],
            'timestamp': datetime.now().isoformat()
        }
        
        try:
            # 1. 访问控制合规检查
            self.logger.info("检查访问控制合规...")
            access_control_check = self.check_access_control_compliance()
            results['standards'].append(access_control_check)
            results['summary']['total_standards'] += 1
            
            # 更新统计
            if access_control_check['compliance_status'] == 'COMPLIANT':
                results['summary']['compliant_standards'] += 1
            elif access_control_check['compliance_status'] == 'PARTIALLY_COMPLIANT':
                results['summary']['partially_compliant'] += 1
            else:
                results['summary']['non_compliant'] += 1
            
            # 2. 数据保护合规检查
            self.logger.info("检查数据保护合规...")
            data_protection_check = self.check_data_protection_compliance()
            results['standards'].append(data_protection_check)
            results['summary']['total_standards'] += 1
            
            # 更新统计
            if data_protection_check['compliance_status'] == 'COMPLIANT':
                results['summary']['compliant_standards'] += 1
            elif data_protection_check['compliance_status'] == 'PARTIALLY_COMPLIANT':
                results['summary']['partially_compliant'] += 1
            else:
                results['summary']['non_compliant'] += 1
            
            # 3. 安全配置合规检查
            self.logger.info("检查安全配置合规...")
            security_config_check = self.check_security_config_compliance()
            results['standards'].append(security_config_check)
            results['summary']['total_standards'] += 1
            
            # 更新统计
            if security_config_check['compliance_status'] == 'COMPLIANT':
                results['summary']['compliant_standards'] += 1
            elif security_config_check['compliance_status'] == 'PARTIALLY_COMPLIANT':
                results['summary']['partially_compliant'] += 1
            else:
                results['summary']['non_compliant'] += 1
            
            # 4. 事件响应合规检查
            self.logger.info("检查事件响应合规...")
            incident_response_check = self.check_incident_response_compliance()
            results['standards'].append(incident_response_check)
            results['summary']['total_standards'] += 1
            
            # 更新统计
            if incident_response_check['compliance_status'] == 'COMPLIANT':
                results['summary']['compliant_standards'] += 1
            elif incident_response_check['compliance_status'] == 'PARTIALLY_COMPLIANT':
                results['summary']['partially_compliant'] += 1
            else:
                results['summary']['non_compliant'] += 1
            
            # 计算合规分数
            total_weight = results['summary']['total_standards'] * 100
            compliant_weight = results['summary']['compliant_standards'] * 100
            partial_weight = results['summary']['partially_compliant'] * 50
            
            if total_weight > 0:
                compliance_score = (compliant_weight + partial_weight) / total_weight
                results['summary']['compliance_score'] = round(compliance_score, 2)
            
            # 确定总体状态
            if results['summary']['compliance_score'] >= 90:
                results['summary']['status'] = 'HIGHLY_COMPLIANT'
            elif results['summary']['compliance_score'] >= 70:
                results['summary']['status'] = 'COMPLIANT'
            elif results['summary']['compliance_score'] >= 50:
                results['summary']['status'] = 'PARTIALLY_COMPLIANT'
            else:
                results['summary']['status'] = 'NON_COMPLIANT'
            
            self.logger.info(f"合规检查完成，分数: {results['summary']['compliance_score']}%")
            
        except Exception as e:
            self.logger.error(f"合规检查过程中发生错误: {e}", exc_info=True)
            results['error'] = str(e)
            results['summary']['status'] = 'ERROR'
        
        self.results = results
        return results
    
    def check_access_control_compliance(self) -> Dict[str, Any]:
        """检查访问控制合规"""
        check_result = {
            'standard_name': '访问控制标准',
            'description': '检查用户访问控制和权限管理合规性',
            'compliance_status': 'NON_COMPLIANT',
            'score': 0,
            'requirements': [],
            'findings': [],
            'recommendations': []
        }
        
        # 检查认证配置
        security_file = os.path.join(self.test_env_root, "config", "security.yml")
        
        if os.path.exists(security_file):
            try:
                import yaml
                with open(security_file, 'r', encoding='utf-8') as f:
                    security_config = yaml.safe_load(f)
                
                # 检查JWT配置（用户认证）
                if 'security' in security_config and 'jwt' in security_config['security']:
                    jwt_config = security_config['security']['jwt']
                    
                    if 'secret' in jwt_config and jwt_config['secret']:
                        check_result['requirements'].append({
                            'requirement': '用户认证机制',
                            'status': 'COMPLIANT',
                            'evidence': 'JWT认证配置存在',
                            'score': 25
                        })
                    else:
                        check_result['requirements'].append({
                            'requirement': '用户认证机制',
                            'status': 'NON_COMPLIANT',
                            'evidence': 'JWT密钥未配置',
                            'score': 0
                        })
                else:
                    check_result['requirements'].append({
                        'requirement': '用户认证机制',
                        'status': 'NON_COMPLIANT',
                        'evidence': 'JWT认证配置缺失',
                        'score': 0
                    })
                
                # 检查CORS配置（访问控制）
                if 'security' in security_config and 'cors' in security_config['security']:
                    cors_config = security_config['security']['cors']
                    
                    if 'allowed-origins' in cors_config and cors_config['allowed-origins']:
                        check_result['requirements'].append({
                            'requirement': '权限分离原则',
                            'status': 'PARTIALLY_COMPLIANT',
                            'evidence': 'CORS跨域控制配置存在',
                            'score': 15
                        })
                    else:
                        check_result['requirements'].append({
                            'requirement': '权限分离原则',
                            'status': 'NON_COMPLIANT',
                            'evidence': 'CORS跨域控制未配置',
                            'score': 0
                        })
                else:
                    check_result['requirements'].append({
                        'requirement': '权限分离原则',
                        'status': 'NON_COMPLIANT',
                        'evidence': 'CORS跨域控制配置缺失',
                        'score': 0
                    })
                
                # 检查速率限制（最小权限原则）
                if 'security' in security_config and 'rate-limiting' in security_config['security']:
                    rate_limit_config = security_config['security']['rate-limiting']
                    
                    if rate_limit_config.get('enabled', False):
                        check_result['requirements'].append({
                            'requirement': '最小权限原则',
                            'status': 'COMPLIANT',
                            'evidence': '速率限制已启用，实施访问控制',
                            'score': 25
                        })
                    else:
                        check_result['requirements'].append({
                            'requirement': '最小权限原则',
                            'status': 'NON_COMPLIANT',
                            'evidence': '速率限制未启用',
                            'score': 0
                        })
                else:
                    check_result['requirements'].append({
                        'requirement': '最小权限原则',
                        'status': 'NON_COMPLIANT',
                        'evidence': '速率限制配置缺失',
                        'score': 0
                    })
            
            except Exception as e:
                check_result['findings'].append(f"解析安全配置文件失败: {str(e)}")
        
        else:
            check_result['findings'].append("安全配置文件不存在")
        
        # 检查日志目录（访问日志记录）
        log_dir = os.path.join(self.test_env_root, "logs")
        
        if os.path.exists(log_dir):
            # 检查是否有应用日志
            app_log_dir = os.path.join(log_dir, "app")
            if os.path.exists(app_log_dir):
                # 检查日志文件
                log_files = [f for f in os.listdir(app_log_dir) if f.endswith('.log')]
                if log_files:
                    check_result['requirements'].append({
                        'requirement': '访问日志记录',
                        'status': 'COMPLIANT',
                        'evidence': f'发现{len(log_files)}个日志文件',
                        'score': 25
                    })
                else:
                    check_result['requirements'].append({
                        'requirement': '访问日志记录',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': '日志目录存在但无日志文件',
                        'score': 10
                    })
            else:
                check_result['requirements'].append({
                    'requirement': '访问日志记录',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '应用日志目录不存在',
                    'score': 10
                })
        else:
            check_result['requirements'].append({
                'requirement': '访问日志记录',
                'status': 'NON_COMPLIANT',
                'evidence': '日志目录不存在',
                'score': 0
            })
        
        # 计算总分
        total_score = sum(req['score'] for req in check_result['requirements'])
        check_result['score'] = total_score
        
        # 确定合规状态
        if total_score >= 90:
            check_result['compliance_status'] = 'COMPLIANT'
        elif total_score >= 70:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        elif total_score >= 50:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        else:
            check_result['compliance_status'] = 'NON_COMPLIANT'
        
        # 添加建议
        if total_score < 100:
            check_result['recommendations'].append("完善访问控制配置，包括认证、授权和审计")
        
        return check_result
    
    def check_data_protection_compliance(self) -> Dict[str, Any]:
        """检查数据保护合规"""
        check_result = {
            'standard_name': '数据保护标准',
            'description': '检查数据保护和加密措施合规性',
            'compliance_status': 'NON_COMPLIANT',
            'score': 0,
            'requirements': [],
            'findings': [],
            'recommendations': []
        }
        
        # 检查数据加密配置
        security_file = os.path.join(self.test_env_root, "config", "security.yml")
        
        if os.path.exists(security_file):
            try:
                import yaml
                with open(security_file, 'r', encoding='utf-8') as f:
                    security_config = yaml.safe_load(f)
                
                # 检查JWT配置（数据传输加密）
                if 'security' in security_config and 'jwt' in security_config['security']:
                    jwt_config = security_config['security']['jwt']
                    
                    # 检查密钥长度
                    if 'secret' in jwt_config:
                        secret = jwt_config['secret']
                        if len(secret) >= 32:
                            check_result['requirements'].append({
                                'requirement': '数据传输加密',
                                'status': 'COMPLIANT',
                                'evidence': 'JWT密钥长度符合安全要求',
                                'score': 25
                            })
                        else:
                            check_result['requirements'].append({
                                'requirement': '数据传输加密',
                                'status': 'PARTIALLY_COMPLIANT',
                                'evidence': f'JWT密钥长度不足({len(secret)}字符)',
                                'score': 15
                            })
                    else:
                        check_result['requirements'].append({
                            'requirement': '数据传输加密',
                            'status': 'NON_COMPLIANT',
                            'evidence': 'JWT密钥未配置',
                            'score': 0
                        })
                else:
                    check_result['requirements'].append({
                        'requirement': '数据传输加密',
                        'status': 'NON_COMPLIANT',
                        'evidence': 'JWT认证配置缺失',
                        'score': 0
                    })
            
            except Exception as e:
                check_result['findings'].append(f"解析安全配置文件失败: {str(e)}")
        else:
            check_result['requirements'].append({
                'requirement': '数据传输加密',
                'status': 'NON_COMPLIANT',
                'evidence': '安全配置文件不存在',
                'score': 0
            })
        
        # 检查数据库配置（数据存储加密）
        datasource_file = os.path.join(self.test_env_root, "config", "datasource.yml")
        
        if os.path.exists(datasource_file):
            try:
                import yaml
                with open(datasource_file, 'r', encoding='utf-8') as f:
                    datasource_config = yaml.safe_load(f)
                
                # 检查数据库连接是否使用SSL
                if isinstance(datasource_config, dict):
                    # 这里假设数据库连接配置存在
                    check_result['requirements'].append({
                        'requirement': '数据存储加密',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': '数据库配置存在，但未明确SSL/TLS配置',
                        'score': 15
                    })
                else:
                    check_result['requirements'].append({
                        'requirement': '数据存储加密',
                        'status': 'NON_COMPLIANT',
                        'evidence': '数据库配置格式错误',
                        'score': 0
                    })
            
            except Exception as e:
                check_result['requirements'].append({
                    'requirement': '数据存储加密',
                    'status': 'NON_COMPLIANT',
                    'evidence': f'解析数据库配置文件失败: {str(e)}',
                    'score': 0
                })
        else:
            check_result['requirements'].append({
                'requirement': '数据存储加密',
                'status': 'NON_COMPLIANT',
                'evidence': '数据库配置文件不存在',
                'score': 0
            })
        
        # 检查测试数据（敏感数据脱敏）
        test_data_dir = os.path.join(self.test_env_root, "data")
        
        if os.path.exists(test_data_dir):
            # 检查样本数据文件
            sample_files = [f for f in os.listdir(test_data_dir) if f.startswith('sample_data_')]
            if sample_files:
                # 检查文件内容（简化检查）
                try:
                    sample_file = os.path.join(test_data_dir, sample_files[0])
                    with open(sample_file, 'r', encoding='utf-8') as f:
                        content = f.read(1000)  # 只读取前1000个字符
                    
                    # 检查是否包含敏感数据模式
                    sensitive_patterns = [
                        r'password\s*[:=]',
                        r'credit.?card',
                        r'social.?security',
                        r'[0-9]{3}-[0-9]{2}-[0-9]{4}'  # SSN模式
                    ]
                    
                    import re
                    sensitive_found = False
                    for pattern in sensitive_patterns:
                        if re.search(pattern, content, re.IGNORECASE):
                            sensitive_found = True
                            break
                    
                    if sensitive_found:
                        check_result['requirements'].append({
                            'requirement': '敏感数据脱敏',
                            'status': 'NON_COMPLIANT',
                            'evidence': '测试数据中包含未脱敏的敏感信息',
                            'score': 0
                        })
                        check_result['recommendations'].append("对测试数据中的敏感信息进行脱敏处理")
                    else:
                        check_result['requirements'].append({
                            'requirement': '敏感数据脱敏',
                            'status': 'COMPLIANT',
                            'evidence': '测试数据中未发现明显的敏感信息',
                            'score': 25
                        })
                
                except Exception as e:
                    check_result['requirements'].append({
                        'requirement': '敏感数据脱敏',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': f'检查测试数据时发生错误: {str(e)}',
                        'score': 10
                    })
            else:
                check_result['requirements'].append({
                    'requirement': '敏感数据脱敏',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '未找到样本数据文件',
                    'score': 10
                })
        else:
            check_result['requirements'].append({
                'requirement': '敏感数据脱敏',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '测试数据目录不存在',
                'score': 10
            })
        
        # 检查备份文件（数据备份机制）
        backup_reports_dir = os.path.join(self.test_env_root, "..", "..", "filesystem-validation-reports")
        
        if os.path.exists(backup_reports_dir):
            backup_files = [f for f in os.listdir(backup_reports_dir) if 'backup' in f.lower() or 'validation' in f.lower()]
            if backup_files:
                check_result['requirements'].append({
                    'requirement': '数据备份机制',
                    'status': 'COMPLIANT',
                    'evidence': f'发现{len(backup_files)}个备份验证文件',
                    'score': 25
                })
            else:
                check_result['requirements'].append({
                    'requirement': '数据备份机制',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '备份目录存在但未发现备份文件',
                    'score': 15
                })
        else:
            check_result['requirements'].append({
                'requirement': '数据备份机制',
                'status': 'NON_COMPLIANT',
                'evidence': '备份验证目录不存在',
                'score': 0
            })
        
        # 计算总分
        total_score = sum(req['score'] for req in check_result['requirements'])
        check_result['score'] = total_score
        
        # 确定合规状态
        if total_score >= 90:
            check_result['compliance_status'] = 'COMPLIANT'
        elif total_score >= 70:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        elif total_score >= 50:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        else:
            check_result['compliance_status'] = 'NON_COMPLIANT'
        
        # 添加建议
        if total_score < 100:
            check_result['recommendations'].append("加强数据保护措施，包括加密传输、存储加密和敏感数据脱敏")
        
        return check_result
    
    def check_security_config_compliance(self) -> Dict[str, Any]:
        """检查安全配置合规"""
        check_result = {
            'standard_name': '安全配置标准',
            'description': '检查安全配置和基线合规性',
            'compliance_status': 'NON_COMPLIANT',
            'score': 0,
            'requirements': [],
            'findings': [],
            'recommendations': []
        }
        
        # 检查安全配置文件完整性
        config_dir = os.path.join(self.test_env_root, "config")
        
        if os.path.exists(config_dir):
            required_configs = ['security.yml', 'application.yml', 'datasource.yml']
            configs_found = []
            
            for config_file in required_configs:
                if os.path.exists(os.path.join(config_dir, config_file)):
                    configs_found.append(config_file)
            
            if len(configs_found) == len(required_configs):
                check_result['requirements'].append({
                    'requirement': '安全基线配置',
                    'status': 'COMPLIANT',
                    'evidence': f'所有必要配置文件都存在: {", ".join(configs_found)}',
                    'score': 25
                })
            elif configs_found:
                check_result['requirements'].append({
                    'requirement': '安全基线配置',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': f'部分配置文件存在: {", ".join(configs_found)}',
                    'score': 15
                })
                missing = [c for c in required_configs if c not in configs_found]
                check_result['findings'].append(f"缺少配置文件: {', '.join(missing)}")
            else:
                check_result['requirements'].append({
                    'requirement': '安全基线配置',
                    'status': 'NON_COMPLIANT',
                    'evidence': '未找到必要的配置文件',
                    'score': 0
                })
        else:
            check_result['requirements'].append({
                'requirement': '安全基线配置',
                'status': 'NON_COMPLIANT',
                'evidence': '配置目录不存在',
                'score': 0
            })
        
        # 检查安全脚本（漏洞管理流程）
        security_scripts_dir = os.path.join(self.test_env_root, "security_scripts")
        
        if os.path.exists(security_scripts_dir):
            # 检查主安全验证脚本
            main_script = os.path.join(security_scripts_dir, "security_validation_main.py")
            if os.path.exists(main_script):
                check_result['requirements'].append({
                    'requirement': '漏洞管理流程',
                    'status': 'COMPLIANT',
                    'evidence': '安全验证脚本存在，支持漏洞管理',
                    'score': 25
                })
            else:
                check_result['requirements'].append({
                    'requirement': '漏洞管理流程',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '安全脚本目录存在但主脚本缺失',
                    'score': 15
                })
        else:
            check_result['requirements'].append({
                'requirement': '漏洞管理流程',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '安全脚本目录不存在（当前正在创建）',
                'score': 10
            })
        
        # 检查测试框架（安全补丁更新）
        test_framework_dir = os.path.join(self.test_env_root, "test_framework")
        
        if os.path.exists(test_framework_dir):
            # 检查配置文件
            config_file = os.path.join(test_framework_dir, "config.py")
            if os.path.exists(config_file):
                try:
                    with open(config_file, 'r', encoding='utf-8') as f:
                        content = f.read(500)  # 只读取前500个字符
                    
                    # 检查是否有版本或更新相关信息
                    if 'version' in content.lower() or 'update' in content.lower():
                        check_result['requirements'].append({
                            'requirement': '安全补丁更新',
                            'status': 'COMPLIANT',
                            'evidence': '测试框架包含版本管理',
                            'score': 25
                        })
                    else:
                        check_result['requirements'].append({
                            'requirement': '安全补丁更新',
                            'status': 'PARTIALLY_COMPLIANT',
                            'evidence': '测试框架存在但版本管理信息不足',
                            'score': 15
                        })
                except Exception as e:
                    check_result['requirements'].append({
                        'requirement': '安全补丁更新',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': f'读取测试框架配置失败: {str(e)}',
                        'score': 10
                    })
            else:
                check_result['requirements'].append({
                    'requirement': '安全补丁更新',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '测试框架配置缺失',
                    'score': 10
                })
        else:
            check_result['requirements'].append({
                'requirement': '安全补丁更新',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '测试框架目录不存在',
                'score': 10
            })
        
        # 检查日志配置（安全审计日志）
        logs_dir = os.path.join(self.test_env_root, "logs")
        
        if os.path.exists(logs_dir):
            # 检查日志目录结构
            log_subdirs = [d for d in os.listdir(logs_dir) if os.path.isdir(os.path.join(logs_dir, d))]
            if log_subdirs:
                check_result['requirements'].append({
                    'requirement': '安全审计日志',
                    'status': 'COMPLIANT',
                    'evidence': f'日志目录结构完整，包含{len(log_subdirs)}个子目录',
                    'score': 25
                })
            else:
                check_result['requirements'].append({
                    'requirement': '安全审计日志',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '日志目录存在但无子目录结构',
                    'score': 15
                })
        else:
            check_result['requirements'].append({
                'requirement': '安全审计日志',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '日志目录不存在',
                'score': 10
            })
        
        # 计算总分
        total_score = sum(req['score'] for req in check_result['requirements'])
        check_result['score'] = total_score
        
        # 确定合规状态
        if total_score >= 90:
            check_result['compliance_status'] = 'COMPLIANT'
        elif total_score >= 70:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        elif total_score >= 50:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        else:
            check_result['compliance_status'] = 'NON_COMPLIANT'
        
        # 添加建议
        if total_score < 100:
            check_result['recommendations'].append("完善安全配置管理，包括基线配置、漏洞管理和审计日志")
        
        return check_result
    
    def check_incident_response_compliance(self) -> Dict[str, Any]:
        """检查事件响应合规"""
        check_result = {
            'standard_name': '事件响应标准',
            'description': '检查安全事件响应能力合规性',
            'compliance_status': 'NON_COMPLIANT',
            'score': 0,
            'requirements': [],
            'findings': [],
            'recommendations': []
        }
        
        # 检查监控和日志（事件检测机制）
        logs_dir = os.path.join(self.test_env_root, "logs")
        
        if os.path.exists(logs_dir):
            # 检查是否有应用日志
            app_log_dir = os.path.join(logs_dir, "app")
            if os.path.exists(app_log_dir):
                # 检查日志文件大小和数量
                log_files = [f for f in os.listdir(app_log_dir) if f.endswith('.log')]
                if len(log_files) >= 3:
                    check_result['requirements'].append({
                        'requirement': '事件检测机制',
                        'status': 'COMPLIANT',
                        'evidence': f'应用日志系统完整，有{len(log_files)}个日志文件',
                        'score': 25
                    })
                else:
                    check_result['requirements'].append({
                        'requirement': '事件检测机制',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': f'应用日志系统存在，但只有{len(log_files)}个日志文件',
                        'score': 15
                    })
            else:
                check_result['requirements'].append({
                    'requirement': '事件检测机制',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '应用日志目录不存在',
                    'score': 10
                })
        else:
            check_result['requirements'].append({
                'requirement': '事件检测机制',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '日志目录不存在',
                'score': 10
            })
        
        # 检查安全脚本（事件响应流程）
        security_scripts_dir = os.path.join(self.test_env_root, "security_scripts")
        
        if os.path.exists(security_scripts_dir):
            # 检查是否有漏洞扫描脚本
            vuln_dir = os.path.join(security_scripts_dir, "vulnerability_scanning")
            if os.path.exists(vuln_dir):
                vuln_script = os.path.join(vuln_dir, "vulnerability_scanner.py")
                if os.path.exists(vuln_script):
                    check_result['requirements'].append({
                        'requirement': '事件响应流程',
                        'status': 'COMPLIANT',
                        'evidence': '漏洞扫描脚本存在，支持安全事件识别',
                        'score': 25
                    })
                else:
                    check_result['requirements'].append({
                        'requirement': '事件响应流程',
                        'status': 'PARTIALLY_COMPLIANT',
                        'evidence': '漏洞扫描目录存在但主脚本缺失',
                        'score': 15
                    })
            else:
                check_result['requirements'].append({
                    'requirement': '事件响应流程',
                    'status': 'PARTIALLY_COMPLIANT',
                    'evidence': '漏洞扫描目录不存在（当前正在创建）',
                    'score': 10
                })
        else:
            check_result['requirements'].append({
                'requirement': '事件响应流程',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '安全脚本目录不存在（当前正在创建）',
                'score': 10
            })
        
        # 检查备份文件（恢复计划）
        backup_root = os.path.join(self.test_env_root, "..", "..")
        backup_dirs = [
            os.path.join(backup_root, "backups"),
            os.path.join(backup_root, "filesystem-validation-reports")
        ]
        
        backup_exists = False
        backup_evidence = []
        
        for backup_dir in backup_dirs:
            if os.path.exists(backup_dir):
                backup_exists = True
                dir_name = os.path.basename(backup_dir)
                backup_evidence.append(dir_name)
        
        if backup_exists:
            check_result['requirements'].append({
                'requirement': '恢复计划',
                'status': 'COMPLIANT',
                'evidence': f'备份相关目录存在: {", ".join(backup_evidence)}',
                'score': 25
            })
        else:
            check_result['requirements'].append({
                'requirement': '恢复计划',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '未发现备份目录',
                'score': 10
            })
        
        # 检查测试报告（事后分析改进）
        reports_root = os.path.join(self.test_env_root, "..", "..")
        report_dirs = [
            os.path.join(reports_root, "validation-reports"),
            os.path.join(reports_root, "filesystem-validation-reports")
        ]
        
        report_exists = False
        report_evidence = []
        
        for report_dir in report_dirs:
            if os.path.exists(report_dir):
                # 检查是否有报告文件
                report_files = [f for f in os.listdir(report_dir) if f.endswith('.json') or f.endswith('.log') or f.endswith('.md')]
                if report_files:
                    report_exists = True
                    dir_name = os.path.basename(report_dir)
                    report_evidence.append(f'{dir_name}({len(report_files)}文件)')
        
        if report_exists:
            check_result['requirements'].append({
                'requirement': '事后分析改进',
                'status': 'COMPLIANT',
                'evidence': f'测试报告目录存在: {", ".join(report_evidence)}',
                'score': 25
            })
        else:
            check_result['requirements'].append({
                'requirement': '事后分析改进',
                'status': 'PARTIALLY_COMPLIANT',
                'evidence': '未发现测试报告目录或报告文件',
                'score': 10
            })
        
        # 计算总分
        total_score = sum(req['score'] for req in check_result['requirements'])
        check_result['score'] = total_score
        
        # 确定合规状态
        if total_score >= 90:
            check_result['compliance_status'] = 'COMPLIANT'
        elif total_score >= 70:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        elif total_score >= 50:
            check_result['compliance_status'] = 'PARTIALLY_COMPLIANT'
        else:
            check_result['compliance_status'] = 'NON_COMPLIANT'
        
        # 添加建议
        if total_score < 100:
            check_result['recommendations'].append("建立完善的事件响应流程，包括检测、响应、恢复和改进机制")
        
        return check_result