"""
安全配置验证器
验证测试环境的各项安全配置
"""

import os
import yaml
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime

class SecurityConfigValidator:
    """安全配置验证器"""
    
    def __init__(self, test_env_root: str):
        """
        初始化安全配置验证器
        
        Args:
            test_env_root: 测试环境根目录
        """
        self.test_env_root = test_env_root
        self.config_dir = os.path.join(test_env_root, "config")
        self.results = {}
        self.logger = logging.getLogger(__name__)
        
        # 安全配置要求
        self.security_requirements = {
            'jwt_secret': {
                'description': 'JWT密钥配置',
                'required': True,
                'min_length': 32,
                'not_default': True  # 不能使用默认值
            },
            'cors_config': {
                'description': 'CORS跨域配置',
                'required': True,
                'allowed_methods': ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
                'max_allowed_origins': 10
            },
            'rate_limiting': {
                'description': '速率限制配置',
                'required': True,
                'min_requests_per_minute': 10,
                'max_requests_per_minute': 1000
            }
        }
    
    def validate_all_configs(self) -> Dict[str, Any]:
        """
        验证所有安全配置
        
        Returns:
            包含所有验证结果的字典
        """
        self.logger.info("开始验证安全配置...")
        
        results = {
            'summary': {
                'total_checks': 0,
                'passed_checks': 0,
                'failed_checks': 0,
                'warnings': 0,
                'status': 'UNKNOWN'
            },
            'details': [],
            'timestamp': datetime.now().isoformat()
        }
        
        try:
            # 1. 验证安全配置文件存在性
            self.logger.info("检查安全配置文件...")
            config_check = self.validate_config_files_existence()
            results['details'].append(config_check)
            results['summary']['total_checks'] += 1
            if config_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif config_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif config_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 2. 验证JWT配置
            self.logger.info("验证JWT配置...")
            jwt_check = self.validate_jwt_config()
            results['details'].append(jwt_check)
            results['summary']['total_checks'] += 1
            if jwt_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif jwt_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif jwt_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 3. 验证CORS配置
            self.logger.info("验证CORS配置...")
            cors_check = self.validate_cors_config()
            results['details'].append(cors_check)
            results['summary']['total_checks'] += 1
            if cors_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif cors_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif cors_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 4. 验证速率限制配置
            self.logger.info("验证速率限制配置...")
            rate_limit_check = self.validate_rate_limit_config()
            results['details'].append(rate_limit_check)
            results['summary']['total_checks'] += 1
            if rate_limit_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif rate_limit_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif rate_limit_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 5. 验证数据库安全配置
            self.logger.info("验证数据库安全配置...")
            db_check = self.validate_database_security()
            results['details'].append(db_check)
            results['summary']['total_checks'] += 1
            if db_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif db_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif db_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 6. 验证文件权限
            self.logger.info("验证文件权限配置...")
            file_perm_check = self.validate_file_permissions()
            results['details'].append(file_perm_check)
            results['summary']['total_checks'] += 1
            if file_perm_check['status'] == 'PASSED':
                results['summary']['passed_checks'] += 1
            elif file_perm_check['status'] == 'FAILED':
                results['summary']['failed_checks'] += 1
            elif file_perm_check['status'] == 'WARNING':
                results['summary']['warnings'] += 1
            
            # 更新总体状态
            if results['summary']['failed_checks'] > 0:
                results['summary']['status'] = 'FAILED'
            elif results['summary']['warnings'] > 0:
                results['summary']['status'] = 'WARNING'
            elif results['summary']['passed_checks'] > 0:
                results['summary']['status'] = 'PASSED'
            else:
                results['summary']['status'] = 'NO_CHECKS'
            
            self.logger.info(f"安全配置验证完成，结果: {results['summary']}")
            
        except Exception as e:
            self.logger.error(f"安全配置验证过程中发生错误: {e}", exc_info=True)
            results['error'] = str(e)
            results['summary']['status'] = 'ERROR'
        
        self.results = results
        return results
    
    def validate_config_files_existence(self) -> Dict[str, Any]:
        """验证安全配置文件是否存在"""
        check_result = {
            'check_name': '安全配置文件存在性检查',
            'description': '检查必要的安全配置文件是否存在',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        required_files = [
            ('security.yml', '安全配置主文件'),
            ('application.yml', '应用配置主文件'),
            ('datasource.yml', '数据源配置')
        ]
        
        for file_name, description in required_files:
            file_path = os.path.join(self.config_dir, file_name)
            
            if os.path.exists(file_path):
                check_result['details'].append({
                    'file': file_name,
                    'path': file_path,
                    'exists': True,
                    'message': f"{description} 存在"
                })
            else:
                check_result['details'].append({
                    'file': file_name,
                    'path': file_path,
                    'exists': False,
                    'message': f"{description} 不存在"
                })
                check_result['status'] = 'FAILED'
                check_result['recommendations'].append(f"创建缺失的安全配置文件: {file_name}")
        
        return check_result
    
    def validate_jwt_config(self) -> Dict[str, Any]:
        """验证JWT配置"""
        check_result = {
            'check_name': 'JWT安全配置检查',
            'description': '检查JWT密钥配置的安全性',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        security_file = os.path.join(self.config_dir, "security.yml")
        
        if not os.path.exists(security_file):
            check_result['status'] = 'FAILED'
            check_result['details'].append({
                'message': 'security.yml 文件不存在',
                'severity': 'HIGH'
            })
            return check_result
        
        try:
            with open(security_file, 'r', encoding='utf-8') as f:
                security_config = yaml.safe_load(f)
            
            # 检查JWT配置
            if 'security' in security_config and 'jwt' in security_config['security']:
                jwt_config = security_config['security']['jwt']
                
                # 检查密钥
                if 'secret' in jwt_config:
                    secret_value = jwt_config['secret']
                    
                    # 检查是否为默认值
                    if 'your-secret-key-here' in secret_value:
                        check_result['status'] = 'FAILED'
                        check_result['details'].append({
                            'message': 'JWT密钥使用默认值，存在安全风险',
                            'severity': 'HIGH',
                            'actual_value': secret_value
                        })
                        check_result['recommendations'].append('请使用强随机字符串作为JWT密钥')
                    
                    # 检查密钥长度
                    if len(secret_value) < self.security_requirements['jwt_secret']['min_length']:
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': f'JWT密钥长度不足，建议至少{self.security_requirements["jwt_secret"]["min_length"]}个字符',
                            'severity': 'MEDIUM',
                            'actual_length': len(secret_value),
                            'recommended_length': self.security_requirements['jwt_secret']['min_length']
                        })
                    
                    else:
                        check_result['details'].append({
                            'message': 'JWT密钥配置符合安全要求',
                            'severity': 'LOW',
                            'actual_length': len(secret_value)
                        })
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': 'JWT密钥配置缺失',
                        'severity': 'HIGH'
                    })
                
                # 检查过期时间
                if 'expiration' in jwt_config:
                    expiration = jwt_config['expiration']
                    if expiration > 7 * 24 * 60 * 60 * 1000:  # 7天，单位毫秒
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': 'JWT过期时间过长，建议不超过7天',
                            'severity': 'MEDIUM',
                            'actual_value': expiration,
                            'recommended_max': 7 * 24 * 60 * 60 * 1000
                        })
                    else:
                        check_result['details'].append({
                            'message': 'JWT过期时间配置合理',
                            'severity': 'LOW',
                            'actual_value': expiration
                        })
            
            else:
                check_result['status'] = 'FAILED'
                check_result['details'].append({
                    'message': 'JWT配置缺失',
                    'severity': 'HIGH'
                })
        
        except Exception as e:
            check_result['status'] = 'ERROR'
            check_result['details'].append({
                'message': f'解析security.yml文件失败: {str(e)}',
                'severity': 'HIGH'
            })
        
        return check_result
    
    def validate_cors_config(self) -> Dict[str, Any]:
        """验证CORS配置"""
        check_result = {
            'check_name': 'CORS安全配置检查',
            'description': '检查CORS跨域配置的安全性',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        security_file = os.path.join(self.config_dir, "security.yml")
        
        if not os.path.exists(security_file):
            check_result['status'] = 'FAILED'
            check_result['details'].append({
                'message': 'security.yml 文件不存在',
                'severity': 'HIGH'
            })
            return check_result
        
        try:
            with open(security_file, 'r', encoding='utf-8') as f:
                security_config = yaml.safe_load(f)
            
            # 检查CORS配置
            if 'security' in security_config and 'cors' in security_config['security']:
                cors_config = security_config['security']['cors']
                
                # 检查允许的源
                if 'allowed-origins' in cors_config:
                    allowed_origins = cors_config['allowed-origins']
                    
                    if isinstance(allowed_origins, list):
                        if len(allowed_origins) == 0:
                            check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                            check_result['details'].append({
                                'message': 'CORS允许的源列表为空，可能限制过严',
                                'severity': 'MEDIUM'
                            })
                        
                        # 检查是否包含通配符
                        if '*' in allowed_origins:
                            check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                            check_result['details'].append({
                                'message': 'CORS配置包含通配符(*)，存在安全风险',
                                'severity': 'HIGH',
                                'actual_value': allowed_origins
                            })
                            check_result['recommendations'].append('在生产环境中应避免使用通配符，明确指定允许的域名')
                        
                        else:
                            check_result['details'].append({
                                'message': 'CORS允许的源配置合理',
                                'severity': 'LOW',
                                'count': len(allowed_origins),
                                'origins': allowed_origins[:5]  # 只显示前5个
                            })
                    
                    else:
                        check_result['status'] = 'FAILED'
                        check_result['details'].append({
                            'message': 'CORS allowed-origins 配置格式错误，应为列表',
                            'severity': 'HIGH'
                        })
                
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': 'CORS allowed-origins 配置缺失',
                        'severity': 'HIGH'
                    })
                
                # 检查允许的方法
                if 'allowed-methods' in cors_config:
                    allowed_methods = cors_config['allowed-methods']
                    
                    if isinstance(allowed_methods, list):
                        required_methods = self.security_requirements['cors_config']['allowed_methods']
                        missing_methods = [m for m in required_methods if m not in allowed_methods]
                        
                        if missing_methods:
                            check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                            check_result['details'].append({
                                'message': f'CORS允许的方法缺少必要的方法: {missing_methods}',
                                'severity': 'MEDIUM',
                                'actual_methods': allowed_methods,
                                'missing_methods': missing_methods
                            })
                        else:
                            check_result['details'].append({
                                'message': 'CORS允许的方法配置完整',
                                'severity': 'LOW',
                                'methods': allowed_methods
                            })
                    
                    else:
                        check_result['status'] = 'FAILED'
                        check_result['details'].append({
                            'message': 'CORS allowed-methods 配置格式错误，应为列表',
                            'severity': 'HIGH'
                        })
                
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': 'CORS allowed-methods 配置缺失',
                        'severity': 'HIGH'
                    })
            
            else:
                check_result['status'] = 'FAILED'
                check_result['details'].append({
                    'message': 'CORS配置缺失',
                    'severity': 'HIGH'
                })
        
        except Exception as e:
            check_result['status'] = 'ERROR'
            check_result['details'].append({
                'message': f'解析security.yml文件失败: {str(e)}',
                'severity': 'HIGH'
            })
        
        return check_result
    
    def validate_rate_limit_config(self) -> Dict[str, Any]:
        """验证速率限制配置"""
        check_result = {
            'check_name': '速率限制配置检查',
            'description': '检查速率限制配置的合理性',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        security_file = os.path.join(self.config_dir, "security.yml")
        
        if not os.path.exists(security_file):
            check_result['status'] = 'FAILED'
            check_result['details'].append({
                'message': 'security.yml 文件不存在',
                'severity': 'HIGH'
            })
            return check_result
        
        try:
            with open(security_file, 'r', encoding='utf-8') as f:
                security_config = yaml.safe_load(f)
            
            # 检查速率限制配置
            if 'security' in security_config and 'rate-limiting' in security_config['security']:
                rate_limit_config = security_config['security']['rate-limiting']
                
                # 检查是否启用
                if 'enabled' in rate_limit_config:
                    if not rate_limit_config['enabled']:
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': '速率限制未启用，存在DoS攻击风险',
                            'severity': 'MEDIUM'
                        })
                        check_result['recommendations'].append('建议启用速率限制功能')
                    else:
                        check_result['details'].append({
                            'message': '速率限制已启用',
                            'severity': 'LOW'
                        })
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': '速率限制启用状态配置缺失',
                        'severity': 'HIGH'
                    })
                
                # 检查请求限制
                if 'requests-per-minute' in rate_limit_config:
                    requests_per_min = rate_limit_config['requests-per-minute']
                    min_req = self.security_requirements['rate_limiting']['min_requests_per_minute']
                    max_req = self.security_requirements['rate_limiting']['max_requests_per_minute']
                    
                    if requests_per_min < min_req:
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': f'每分钟请求限制过低，建议至少{min_req}次',
                            'severity': 'MEDIUM',
                            'actual_value': requests_per_min,
                            'recommended_min': min_req
                        })
                    elif requests_per_min > max_req:
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': f'每分钟请求限制过高，建议不超过{max_req}次',
                            'severity': 'MEDIUM',
                            'actual_value': requests_per_min,
                            'recommended_max': max_req
                        })
                    else:
                        check_result['details'].append({
                            'message': '每分钟请求限制配置合理',
                            'severity': 'LOW',
                            'actual_value': requests_per_min
                        })
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': '每分钟请求限制配置缺失',
                        'severity': 'HIGH'
                    })
                
                # 检查突发容量
                if 'burst-capacity' in rate_limit_config:
                    burst_capacity = rate_limit_config['burst-capacity']
                    requests_per_min = rate_limit_config.get('requests-per-minute', 100)
                    
                    if burst_capacity <= requests_per_min:
                        check_result['status'] = 'WARNING' if check_result['status'] != 'FAILED' else 'FAILED'
                        check_result['details'].append({
                            'message': '突发容量设置不合理，应大于每分钟限制',
                            'severity': 'MEDIUM',
                            'actual_value': burst_capacity,
                            'requests_per_minute': requests_per_min
                        })
                    else:
                        check_result['details'].append({
                            'message': '突发容量配置合理',
                            'severity': 'LOW',
                            'actual_value': burst_capacity
                        })
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': '突发容量配置缺失',
                        'severity': 'HIGH'
                    })
            
            else:
                check_result['status'] = 'FAILED'
                check_result['details'].append({
                    'message': '速率限制配置缺失',
                    'severity': 'HIGH'
                })
        
        except Exception as e:
            check_result['status'] = 'ERROR'
            check_result['details'].append({
                'message': f'解析security.yml文件失败: {str(e)}',
                'severity': 'HIGH'
            })
        
        return check_result
    
    def validate_database_security(self) -> Dict[str, Any]:
        """验证数据库安全配置"""
        check_result = {
            'check_name': '数据库安全配置检查',
            'description': '检查数据库连接和安全配置',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        datasource_file = os.path.join(self.config_dir, "datasource.yml")
        
        if not os.path.exists(datasource_file):
            check_result['status'] = 'FAILED'
            check_result['details'].append({
                'message': 'datasource.yml 文件不存在',
                'severity': 'HIGH'
            })
            return check_result
        
        try:
            with open(datasource_file, 'r', encoding='utf-8') as f:
                datasource_config = yaml.safe_load(f)
            
            # 检查数据库配置
            if datasource_config:
                # 检查是否使用默认密码
                if 'password' in datasource_config:
                    password = datasource_config['password']
                    if password in ['test_password', 'password', '123456', '']:
                        check_result['status'] = 'FAILED'
                        check_result['details'].append({
                            'message': '数据库密码过于简单或使用默认值',
                            'severity': 'HIGH',
                            'actual_value': '***' if password else '空'
                        })
                        check_result['recommendations'].append('请使用强密码替换默认数据库密码')
                    else:
                        check_result['details'].append({
                            'message': '数据库密码配置符合安全要求',
                            'severity': 'LOW'
                        })
                else:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': '数据库密码配置缺失',
                        'severity': 'HIGH'
                    })
                
                # 检查数据库连接
                required_fields = ['host', 'port', 'name', 'user']
                missing_fields = []
                
                for field in required_fields:
                    if field not in datasource_config:
                        missing_fields.append(field)
                
                if missing_fields:
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'message': f'数据库配置缺少必要字段: {missing_fields}',
                        'severity': 'HIGH'
                    })
                else:
                    check_result['details'].append({
                        'message': '数据库连接配置完整',
                        'severity': 'LOW',
                        'host': datasource_config.get('host'),
                        'port': datasource_config.get('port'),
                        'database': datasource_config.get('name')
                    })
            
            else:
                check_result['status'] = 'FAILED'
                check_result['details'].append({
                    'message': 'datasource.yml 文件为空或格式错误',
                    'severity': 'HIGH'
                })
        
        except Exception as e:
            check_result['status'] = 'ERROR'
            check_result['details'].append({
                'message': f'解析datasource.yml文件失败: {str(e)}',
                'severity': 'HIGH'
            })
        
        return check_result
    
    def validate_file_permissions(self) -> Dict[str, Any]:
        """验证文件权限"""
        check_result = {
            'check_name': '文件权限检查',
            'description': '检查关键文件的权限设置',
            'status': 'PASSED',
            'details': [],
            'recommendations': []
        }
        
        # 检查的关键文件
        critical_files = [
            os.path.join(self.config_dir, "security.yml"),
            os.path.join(self.config_dir, "datasource.yml"),
            os.path.join(self.config_dir, "application.yml"),
            os.path.join(self.test_env_root, "etc", "important", "nginx.conf"),
            os.path.join(self.test_env_root, "etc", "important", "ssh_config")
        ]
        
        for file_path in critical_files:
            if os.path.exists(file_path):
                try:
                    # 检查文件是否存在
                    if os.path.isfile(file_path):
                        # 获取文件大小
                        file_size = os.path.getsize(file_path)
                        
                        # 检查文件是否可读
                        if os.access(file_path, os.R_OK):
                            # 检查文件是否可写（对于配置文件，应有适当限制）
                            if os.access(file_path, os.W_OK):
                                # 配置文件通常应该可写，但需要记录
                                check_result['details'].append({
                                    'file': os.path.basename(file_path),
                                    'path': file_path,
                                    'writable': True,
                                    'readable': True,
                                    'size': file_size,
                                    'message': '文件可读写',
                                    'severity': 'LOW'
                                })
                            else:
                                check_result['details'].append({
                                    'file': os.path.basename(file_path),
                                    'path': file_path,
                                    'writable': False,
                                    'readable': True,
                                    'size': file_size,
                                    'message': '文件只读',
                                    'severity': 'LOW'
                                })
                        else:
                            check_result['status'] = 'FAILED'
                            check_result['details'].append({
                                'file': os.path.basename(file_path),
                                'path': file_path,
                                'writable': False,
                                'readable': False,
                                'size': file_size,
                                'message': '文件不可读，可能导致应用无法启动',
                                'severity': 'HIGH'
                            })
                    else:
                        check_result['details'].append({
                            'file': os.path.basename(file_path),
                            'path': file_path,
                            'message': '不是文件（可能是目录）',
                            'severity': 'LOW'
                        })
                
                except Exception as e:
                    check_result['details'].append({
                        'file': os.path.basename(file_path),
                        'path': file_path,
                        'message': f'检查文件权限时发生错误: {str(e)}',
                        'severity': 'MEDIUM'
                    })
            else:
                # 文件不存在，但不是所有文件都是必须的
                if 'security.yml' in file_path or 'datasource.yml' in file_path:
                    # 这些是关键文件，必须存在
                    check_result['status'] = 'FAILED'
                    check_result['details'].append({
                        'file': os.path.basename(file_path),
                        'path': file_path,
                        'message': '关键配置文件不存在',
                        'severity': 'HIGH'
                    })
                else:
                    # 其他文件不是必须的
                    check_result['details'].append({
                        'file': os.path.basename(file_path),
                        'path': file_path,
                        'message': '文件不存在（非必需）',
                        'severity': 'LOW'
                    })
        
        return check_result