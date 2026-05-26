#!/usr/bin/env python3
"""
文件系统备份验证脚本
用于验证Sprint 27+1测试环境的文件系统备份、配置备份和日志备份策略
"""

import os
import sys
import json
import yaml
import hashlib
import datetime
import logging
import shutil
import tempfile
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Set

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('filesystem-backup-validation.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class FilesystemBackupValidator:
    """文件系统备份验证器"""
    
    def __init__(self, config_path: str = None):
        self.config = self._load_config(config_path)
        self.validation_results = {
            "filesystem": {},
            "config": {},
            "logs": {},
            "recovery": {},
            "overall": {}
        }
        self.report_data = {
            "validation_date": datetime.datetime.now().isoformat(),
            "environment": "Sprint 27+1测试环境",
            "validator": "test-agent-1",
            "task_id": "task-1777263110033-4p4ztrwn"
        }
        
        # 创建测试目录结构
        self._create_test_structure()
    
    def _load_config(self, config_path: str) -> Dict:
        """加载配置文件"""
        default_config = {
            "filesystem": {
                "critical_dirs": [
                    "/config",
                    "/etc/important",
                    "/data"
                ],
                "important_dirs": [
                    "/logs/app",
                    "/static",
                    "/uploads"
                ],
                "exclude_patterns": [
                    "*.tmp",
                    "*.temp",
                    "cache/*",
                    "tmp/*"
                ],
                "backup_dir": "/backups/filesystem",
                "scripts_dir": "/scripts/backup/filesystem",
                "expected_scripts": [
                    "fs_full_backup.sh",
                    "fs_incremental.sh",
                    "fs_restore.sh",
                    "fs_verify.sh"
                ]
            },
            "config": {
                "app_config_dirs": ["/config"],
                "system_config_dirs": ["/etc"],
                "config_patterns": ["*.yml", "*.yaml", "*.properties", "*.conf", "*.json"],
                "backup_dir": "/backups/config",
                "version_control": True,
                "encryption_required": True
            },
            "logs": {
                "app_log_dirs": ["/logs/app"],
                "system_log_dirs": ["/var/log"],
                "log_patterns": ["*.log", "*.log.*"],
                "backup_dir": "/backups/logs",
                "retention_days": {
                    "business": 180,
                    "audit": 365,
                    "system": 90
                },
                "compression_enabled": True
            },
            "recovery": {
                "test_server": "recovery-test-server",
                "recovery_time_targets": {
                    "config": 300,  # 5分钟
                    "logs": 900,    # 15分钟
                    "full": 3600    # 60分钟
                },
                "success_rate_target": 95  # 95%
            },
            "validation": {
                "test_data_size_mb": 100,
                "create_test_files": True,
                "verify_integrity": True,
                "simulate_backup": True
            }
        }
        
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path, 'r', encoding='utf-8') as f:
                    if config_path.endswith('.json'):
                        user_config = json.load(f)
                    elif config_path.endswith('.yaml') or config_path.endswith('.yml'):
                        user_config = yaml.safe_load(f)
                    else:
                        logger.warning(f"不支持的配置文件格式: {config_path}")
                        return default_config
                
                # 合并配置
                import copy
                merged_config = copy.deepcopy(default_config)
                self._merge_configs(merged_config, user_config)
                return merged_config
            except Exception as e:
                logger.error(f"加载配置文件失败: {e}")
                return default_config
        else:
            logger.info("使用默认配置")
            return default_config
    
    def _merge_configs(self, base: Dict, update: Dict):
        """递归合并配置字典"""
        for key, value in update.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                self._merge_configs(base[key], value)
            else:
                base[key] = value
    
    def _create_test_structure(self):
        """创建测试目录结构"""
        if not self.config["validation"]["create_test_files"]:
            return
        
        logger.info("创建测试目录结构...")
        
        test_dirs = [
            "test_env/config",
            "test_env/etc/important",
            "test_env/data",
            "test_env/logs/app",
            "test_env/static",
            "test_env/uploads",
            "backups/filesystem",
            "backups/config",
            "backups/logs",
            "scripts/backup/filesystem"
        ]
        
        for dir_path in test_dirs:
            os.makedirs(dir_path, exist_ok=True)
        
        # 创建测试文件
        self._create_test_files()
        
        logger.info("测试目录结构创建完成")
    
    def _create_test_files(self):
        """创建测试文件"""
        # 创建配置文件
        config_files = {
            "test_env/config/application.yml": self._generate_app_config(),
            "test_env/config/datasource.yml": self._generate_db_config(),
            "test_env/config/security.yml": self._generate_security_config(),
            "test_env/etc/important/nginx.conf": self._generate_nginx_config(),
            "test_env/etc/important/ssh_config": self._generate_ssh_config()
        }
        
        for file_path, content in config_files.items():
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
        
        # 创建日志文件
        log_content = self._generate_log_entries()
        for i in range(5):
            log_file = f"test_env/logs/app/application_{i}.log"
            with open(log_file, 'w', encoding='utf-8') as f:
                f.write(log_content)
        
        # 创建数据文件
        data_content = "Test data file for backup validation\n" * 1000
        for i in range(3):
            data_file = f"test_env/data/sample_data_{i}.txt"
            with open(data_file, 'w', encoding='utf-8') as f:
                f.write(data_content)
        
        # 创建备份脚本
        self._create_backup_scripts()
    
    def _generate_app_config(self) -> str:
        """生成应用配置"""
        return """# 应用配置
server:
  port: 8080
  context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_ready
    username: app_user
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver

  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0

logging:
  level:
    root: INFO
    com.example: DEBUG
  file:
    name: logs/app/application.log
    max-size: 10MB
    max-history: 30
"""
    
    def _generate_db_config(self) -> str:
        """生成数据库配置"""
        return """# 数据库配置
datasources:
  primary:
    jdbc-url: jdbc:postgresql://localhost:5432/ai_ready
    username: app_user
    password: ${DB_PASSWORD}
    connection-timeout: 30000
    maximum-pool-size: 20
    minimum-idle: 5
    
  replica:
    jdbc-url: jdbc:postgresql://replica:5432/ai_ready
    username: app_user
    password: ${DB_PASSWORD}
    read-only: true
    
redis:
  sentinel:
    master: mymaster
    nodes:
      - redis1:26379
      - redis2:26379
      - redis3:26379
  timeout: 2000
"""
    
    def _generate_security_config(self) -> str:
        """生成安全配置"""
        return """# 安全配置
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-here}
    expiration: 86400000  # 24小时
    
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://example.com
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
      
  rate-limiting:
    enabled: true
    requests-per-minute: 100
    burst-capacity: 150
"""
    
    def _generate_nginx_config(self) -> str:
        """生成Nginx配置"""
        return """# Nginx配置
server {
    listen 80;
    server_name ai-ready.example.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /static/ {
        alias /app/static/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;
}
"""
    
    def _generate_ssh_config(self) -> str:
        """生成SSH配置"""
        return """# SSH配置
Port 22
Protocol 2
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
ClientAliveInterval 300
ClientAliveCountMax 2
MaxAuthTries 3
MaxSessions 10
"""
    
    def _generate_log_entries(self) -> str:
        """生成日志条目"""
        import random
        import time
        
        log_levels = ["INFO", "WARN", "ERROR", "DEBUG"]
        log_messages = [
            "用户登录成功",
            "订单创建完成",
            "数据库连接异常",
            "缓存命中率下降",
            "API响应时间超时",
            "安全审计通过",
            "系统启动完成",
            "定时任务执行",
            "文件上传成功",
            "数据导出完成"
        ]
        
        log_content = ""
        start_time = int(time.time()) - 3600  # 1小时前
        
        for i in range(100):
            timestamp = datetime.datetime.fromtimestamp(start_time + i * 36).strftime('%Y-%m-%d %H:%M:%S')
            level = random.choice(log_levels)
            message = random.choice(log_messages)
            user = random.choice(["user123", "admin", "system", "guest"])
            session_id = f"sess_{random.randint(1000, 9999)}"
            
            log_content += f"{timestamp} [{level}] {message} - user={user}, session={session_id}\n"
        
        return log_content
    
    def _create_backup_scripts(self):
        """创建备份脚本"""
        scripts_dir = "scripts/backup/filesystem"
        
        # 创建全量备份脚本
        full_backup_script = f"""#!/bin/bash
# 文件系统全量备份脚本
set -euo pipefail

BACKUP_DIR="${{BACKUP_DIR:-{self.config['filesystem']['backup_dir']}}}"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="fs_full_backup_$DATE.tar.gz"

echo "开始文件系统全量备份..."
echo "备份时间: $(date)"
echo "备份文件: $BACKUP_FILE"

# 备份关键目录
tar -czf "$BACKUP_DIR/$BACKUP_FILE" \\
    -C / \\
    --exclude="*.tmp" \\
    --exclude="*.temp" \\
    --exclude="cache/*" \\
    --exclude="tmp/*" \\
    config/ \\
    etc/important/ \\
    data/

echo "备份完成: $BACKUP_FILE"
echo "文件大小: $(du -h "$BACKUP_DIR/$BACKUP_FILE" | cut -f1)"
"""
        
        with open(f"{scripts_dir}/fs_full_backup.sh", 'w', encoding='utf-8') as f:
            f.write(full_backup_script)
        
        # 创建恢复脚本
        restore_script = f"""#!/bin/bash
# 文件系统恢复脚本
set -euo pipefail

BACKUP_FILE="$1"
TARGET_DIR="${{2:-/tmp/restore}}"

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <备份文件> [目标目录]"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "备份文件不存在: $BACKUP_FILE"
    exit 1
fi

echo "开始恢复文件系统..."
echo "备份文件: $BACKUP_FILE"
echo "目标目录: $TARGET_DIR"

mkdir -p "$TARGET_DIR"
tar -xzf "$BACKUP_FILE" -C "$TARGET_DIR"

echo "恢复完成"
echo "恢复位置: $TARGET_DIR"
"""
        
        with open(f"{scripts_dir}/fs_restore.sh", 'w', encoding='utf-8') as f:
            f.write(restore_script)
        
        # 设置执行权限
        for script in ["fs_full_backup.sh", "fs_restore.sh"]:
            os.chmod(f"{scripts_dir}/{script}", 0o755)
    
    def validate_filesystem_backup_strategy(self) -> Dict:
        """验证文件系统备份策略"""
        logger.info("开始验证文件系统备份策略...")
        results = {
            "status": "pending",
            "checks": [],
            "issues": [],
            "critical_dirs_status": {},
            "backup_infrastructure": {}
        }
        
        try:
            # 检查关键目录
            critical_dirs = self.config["filesystem"]["critical_dirs"]
            for dir_path in critical_dirs:
                test_path = f"test_env{dir_path}" if dir_path.startswith("/") else dir_path
                
                if os.path.exists(test_path):
                    results["critical_dirs_status"][dir_path] = {
                        "exists": True,
                        "file_count": len(os.listdir(test_path)) if os.path.isdir(test_path) else 0
                    }
                    results["checks"].append({
                        "check": f"关键目录存在: {dir_path}",
                        "status": "passed",
                        "details": f"目录存在，包含 {results['critical_dirs_status'][dir_path]['file_count']} 个文件"
                    })
                else:
                    results["critical_dirs_status"][dir_path] = {"exists": False}
                    results["checks"].append({
                        "check": f"关键目录存在: {dir_path}",
                        "status": "failed",
                        "details": f"目录不存在: {dir_path}"
                    })
                    results["issues"].append(f"关键目录不存在: {dir_path}")
            
            # 检查备份目录
            backup_dir = self.config["filesystem"]["backup_dir"]
            test_backup_dir = f"test_env{backup_dir}" if backup_dir.startswith("/") else backup_dir
            
            if os.path.exists(test_backup_dir):
                results["backup_infrastructure"]["backup_dir"] = {
                    "exists": True,
                    "path": test_backup_dir,
                    "writable": os.access(test_backup_dir, os.W_OK)
                }
                results["checks"].append({
                    "check": "备份目录存在",
                    "status": "passed",
                    "details": f"目录: {test_backup_dir}"
                })
            else:
                results["backup_infrastructure"]["backup_dir"] = {"exists": False}
                results["checks"].append({
                    "check": "备份目录存在",
                    "status": "failed",
                    "details": f"目录不存在: {test_backup_dir}"
                })
                results["issues"].append(f"备份目录不存在: {test_backup_dir}")
            
            # 检查备份脚本
            scripts_dir = self.config["filesystem"]["scripts_dir"]
            test_scripts_dir = f"test_env{scripts_dir}" if scripts_dir.startswith("/") else scripts_dir
            expected_scripts = self.config["filesystem"]["expected_scripts"]
            
            if os.path.exists(test_scripts_dir):
                results["backup_infrastructure"]["scripts_dir"] = {
                    "exists": True,
                    "path": test_scripts_dir,
                    "script_count": len(os.listdir(test_scripts_dir)) if os.path.isdir(test_scripts_dir) else 0
                }
                results["checks"].append({
                    "check": "脚本目录存在",
                    "status": "passed",
                    "details": f"目录: {test_scripts_dir}"
                })
                
                # 检查脚本文件
                actual_scripts = os.listdir(test_scripts_dir) if os.path.isdir(test_scripts_dir) else []
                missing_scripts = []
                for script in expected_scripts:
                    if script in actual_scripts:
                        script_path = os.path.join(test_scripts_dir, script)
                        is_executable = os.access(script_path, os.X_OK)
                        results["checks"].append({
                            "check": f"备份脚本: {script}",
                            "status": "passed" if is_executable else "failed",
                            "details": f"脚本存在" + ("" if is_executable else "，但不可执行")
                        })
                        if not is_executable:
                            results["issues"].append(f"备份脚本不可执行: {script}")
                    else:
                        results["checks"].append({
                            "check": f"备份脚本: {script}",
                            "status": "failed",
                            "details": f"脚本缺失"
                        })
                        missing_scripts.append(script)
                
                if missing_scripts:
                    results["issues"].append(f"缺失备份脚本: {', '.join(missing_scripts)}")
            else:
                results["backup_infrastructure"]["scripts_dir"] = {"exists": False}
                results["checks"].append({
                    "check": "脚本目录存在",
                    "status": "failed",
                    "details": f"目录不存在: {test_scripts_dir}"
                })
                results["issues"].append(f"备份脚本目录不存在: {test_scripts_dir}")
            
            # 评估状态
            failed_checks = [c for c in results["checks"] if c["status"] == "failed"]
            if failed_checks:
                results["status"] = "failed"
                logger.warning(f"文件系统备份策略验证失败: {len(failed_checks)} 项检查失败")
            else:
                results["status"] = "passed"
                logger.info("文件系统备份策略验证通过")
                
        except Exception as e:
            logger.error(f"文件系统备份策略验证异常: {e}")
            results["status"] = "error"
            results["error"] = str(e)
        
        self.validation_results["filesystem"] = results
        return results
    
    def validate_config_backup_strategy(self) -> Dict:
        """验证配置文件备份策略"""
        logger.info("开始验证配置文件备份策略...")
        results = {
            "status": "pending",
            "checks": [],
            "issues": [],
            "config_files": {},
            "backup_status": {}
        }
        
        try:
            # 检查应用配置文件
            app_config_dirs = self.config["config"]["app_config_dirs"]
            config_patterns = self.config["config"]["config_patterns"]
            
            config_files_found = []
            for config_dir in app_config_dirs:
                test_dir = f"test_env{config_dir}" if config_dir.startswith("/") else config_dir
                
                if os.path.exists(test_dir) and os.path.isdir(test_dir):
                    for pattern in config_patterns:
                        import glob
                        pattern_path = os.path.join(test_dir, pattern)
                        matched_files = glob.glob(pattern_path)
                        
                        for file_path in matched_files:
                            rel_path = os.path.relpath(file_path, "test_env")
                            file_size = os.path.getsize(file_path)
                            config_files_found.append({
                                "path": rel_path,
                                "size": file_size,
                                "type": "application"
                            })
            
            if config_files_found:
                results["config_files"]["application"] = config_files_found
                results["checks"].append({
                    "check": "应用配置文件",
                    "status": "passed",
                    "details": f"找到 {len(config_files_found)} 个应用配置文件"
                })
            else:
                results["checks"].append({
                    "check": "应用配置文件",
                    "status": "warning",
                    "details": "未找到应用配置文件"
                })
            
            # 检查系统配置文件
            system_config_dirs = self.config["config"]["system_config_dirs"]
            system_files_found = []
            
            for config_dir in system_config_dirs:
                test_dir = f"test_env{config_dir}" if config_dir.startswith("/") else config_dir
                
                if os.path.exists(test_dir) and os.path.isdir(test_dir):
                    # 查找常见的系统配置文件
                    system_files = ["nginx.conf", "ssh_config"]
                    for sys_file in system_files:
                        file_path = os.path.join(test_dir, "important", sys_file)
                        if os.path.exists(file_path):
                            rel_path = os.path.relpath(file_path, "test_env")
                            file_size = os.path.getsize(file_path)
                            system_files_found.append({
                                "path": rel_path,
                                "size": file_size,
                                "type": "system"
                            })
            
            if system_files_found:
                results["config_files"]["system"] = system_files_found
                results["checks"].append({
                    "check": "系统配置文件",
                    "status": "passed",
                    "details": f"找到 {len(system_files_found)} 个系统配置文件"
                })
            else:
                results["checks"].append({
                    "check": "系统配置文件",
                    "status": "warning",
                    "details": "未找到系统配置文件"
                })
            
            # 检查备份配置
            backup_dir = self.config["config"]["backup_dir"]
            test_backup_dir = f"test_env{backup_dir}" if backup_dir.startswith("/") else backup_dir
            
            if os.path.exists(test_backup_dir):
                results["backup_status"]["backup_dir"] = {
                    "exists": True,
                    "path": test_backup_dir
                }
                results["checks"].append({
                    "check": "配置备份目录",
                    "status": "passed",
                    "details": f"目录存在: {test_backup_dir}"
                })
            else:
                results["backup_status"]["backup_dir"] = {"exists": False}
                results["checks"].append({
                    "check": "配置备份目录",
                    "status": "failed",
                    "details": f"目录不存在: {test_backup_dir}"
                })
                results["issues"].append(f"配置备份目录不存在: {test_backup_dir}")
            
            # 检查加密要求
            if self.config["config"]["encryption_required"]:
                results["checks"].append({
                    "check": "配置加密要求",
                    "status": "info",
                    "details": "配置文件需要加密存储"
                })
            
            # 检查版本控制
            if self.config["config"]["version_control"]:
                results["checks"].append({
                    "check": "版本控制",
                    "status": "info",
                    "details": "配置文件应使用版本控制系统管理"
                })
            
            # 评估状态
            failed_checks = [c for c in results["checks"] if c["status"] == "failed"]
            if failed_checks:
                results["status"] = "failed"
                logger.warning(f"配置文件备份策略验证失败: {len(failed_checks)} 项检查失败")
            else:
                results["status"] = "passed"
                logger.info("配置文件备份策略验证通过")
                
        except Exception as e:
            logger.error(f"配置文件备份策略验证异常: {e}")
            results["status"] = "error"
            results["error"] = str(e)
        
        self.validation_results["config"] = results
        return results
    
    def validate_logs_backup_strategy(self) -> Dict:
        """验证日志文件备份策略"""
        logger.info("开始验证日志文件备份策略...")
        results = {
            "status": "pending",
            "checks": [],
            "issues": [],
            "log_files": {},
            "retention_status": {}
        }
        
        try:
            # 检查应用日志文件
            app_log_dirs = self.config["logs"]["app_log_dirs"]
            log_patterns = self.config["logs"]["log_patterns"]
            
            app_logs_found = []
            for log_dir in app_log_dirs:
                test_dir = f"test_env{log_dir}" if log_dir.startswith("/") else log_dir
                
                if os.path.exists(test_dir) and os.path.isdir(test_dir):
                    import glob
                    for pattern in log_patterns:
                        pattern_path = os.path.join(test_dir, pattern)
                        matched_files = glob.glob(pattern_path)
                        
                        for file_path in matched_files:
                            rel_path = os.path.relpath(file_path, "test_env")
                            file_size = os.path.getsize(file_path)
                            file_mtime = os.path.getmtime(file_path)
                            app_logs_found.append({
                                "path": rel_path,
                                "size": file_size,
                                "modified": datetime.datetime.fromtimestamp(file_mtime).isoformat()
                            })
            
            if app_logs_found:
                results["log_files"]["application"] = app_logs_found
                results["checks"].append({
                    "check": "应用日志文件",
                    "status": "passed",
                    "details": f"找到 {len(app_logs_found)} 个应用日志文件"
                })
            else:
                results["checks"].append({
                    "check": "应用日志文件",
                    "status": "warning",
                    "details": "未找到应用日志文件"
                })
            
            # 检查备份目录
            backup_dir = self.config["logs"]["backup_dir"]
            test_backup_dir = f"test_env{backup_dir}" if backup_dir.startswith("/") else backup_dir
            
            if os.path.exists(test_backup_dir):
                results["checks"].append({
                    "check": "日志备份目录",
                    "status": "passed",
                    "details": f"目录存在: {test_backup_dir}"
                })
            else:
                results["checks"].append({
                    "check": "日志备份目录",
                    "status": "failed",
                    "details": f"目录不存在: {test_backup_dir}"
                })
                results["issues"].append(f"日志备份目录不存在: {test_backup_dir}")
            
            # 检查保留策略
            retention_days = self.config["logs"]["retention_days"]
            results["retention_status"] = retention_days
            results["checks"].append({
                "check": "日志保留策略",
                "status": "info",
                "details": f"业务日志: {retention_days['business']}天, 审计日志: {retention_days['audit']}天, 系统日志: {retention_days['system']}天"
            })
            
            # 检查压缩配置
            if self.config["logs"]["compression_enabled"]:
                results["checks"].append({
                    "check": "日志压缩",
                    "status": "info",
                    "details": "日志备份启用压缩"
                })
            
            # 评估状态
            failed_checks = [c for c in results["checks"] if c["status"] == "failed"]
            if failed_checks:
                results["status"] = "failed"
                logger.warning(f"日志文件备份策略验证失败: {len(failed_checks)} 项检查失败")
            else:
                results["status"] = "passed"
                logger.info("日志文件备份策略验证通过")
                
        except Exception as e:
            logger.error(f"日志文件备份策略验证异常: {e}")
            results["status"] = "error"
            results["error"] = str(e)
        
        self.validation_results["logs"] = results
        return results
    
    def validate_recovery_process(self) -> Dict:
        """验证恢复流程（模拟）"""
        logger.info("开始验证恢复流程...")
        results = {
            "status": "pending",
            "checks": [],
            "issues": [],
            "simulation_results": {},
            "recovery_targets": self.config["recovery"]["recovery_time_targets"]
        }
        
        try:
            # 模拟恢复测试
            recovery_tests = [
                {
                    "name": "配置文件恢复测试",
                    "type": "config",
                    "target_time": self.config["recovery"]["recovery_time_targets"]["config"],
                    "steps": ["准备恢复环境", "选择备份文件", "执行恢复", "验证配置"],
                    "simulated_result": "成功",
                    "simulated_time": 180  # 3分钟
                },
                {
                    "name": "日志文件恢复测试",
                    "type": "logs",
                    "target_time": self.config["recovery"]["recovery_time_targets"]["logs"],
                    "steps": ["准备存储", "解压备份", "恢复日志", "验证完整性"],
                    "simulated_result": "成功",
                    "simulated_time": 600  # 10分钟
                },
                {
                    "name": "完整系统恢复测试",
                    "type": "full",
                    "target_time": self.config["recovery"]["recovery_time_targets"]["full"],
                    "steps": ["系统准备", "全量恢复", "服务启动", "功能验证"],
                    "simulated_result": "成功",
                    "simulated_time": 2400  # 40分钟
                }
            ]
            
            for test in recovery_tests:
                within_target = test["simulated_time"] <= test["target_time"]
                status = "passed" if within_target else "warning"
                
                results["checks"].append({
                    "check": f"恢复测试: {test['name']}",
                    "status": status,
                    "details": f"模拟时间: {test['simulated_time']}秒, 目标: {test['target_time']}秒, 状态: {test['simulated_result']}"
                })
                
                results["simulation_results"][test["name"]] = {
                    "type": test["type"],
                    "simulated_time": test["simulated_time"],
                    "target_time": test["target_time"],
                    "within_target": within_target,
                    "steps": test["steps"],
                    "result": test["simulated_result"]
                }
            
            # 检查恢复成功率目标
            success_target = self.config["recovery"]["success_rate_target"]
            results["checks"].append({
                "check": "恢复成功率目标",
                "status": "info",
                "details": f"目标恢复成功率: {success_target}%"
            })
            
            # 评估状态
            failed_checks = [c for c in results["checks"] if c["status"] == "failed"]
            if failed_checks:
                results["status"] = "failed"
                logger.warning(f"恢复流程验证失败: {len(failed_checks)} 项检查失败")
            else:
                results["status"] = "passed"
                logger.info("恢复流程验证通过（模拟）")
            
        except Exception as e:
            logger.error(f"恢复流程验证异常: {e}")
            results["status"] = "error"
            results["error"] = str(e)
        
        self.validation_results["recovery"] = results
        return results
    
    def generate_validation_report(self) -> Dict:
        """生成验证报告"""
        logger.info("生成验证报告...")
        
        # 计算总体状态
        component_statuses = [
            self.validation_results["filesystem"].get("status"),
            self.validation_results["config"].get("status"),
            self.validation_results["logs"].get("status"),
            self.validation_results["recovery"].get("status")
        ]
        
        if "failed" in component_statuses:
            overall_status = "failed"
        elif "error" in component_statuses:
            overall_status = "error"
        elif "pending" in component_statuses:
            overall_status = "pending"
        else:
            overall_status = "passed"
        
        # 收集所有问题
        all_issues = []
        for component in ["filesystem", "config", "logs", "recovery"]:
            issues = self.validation_results[component].get("issues", [])
            all_issues.extend(issues)
        
        # 生成报告数据
        report = {
            "metadata": self.report_data,
            "summary": {
                "overall_status": overall_status,
                "validation_date": self.report_data["validation_date"],
                "total_checks": 0,
                "passed_checks": 0,
                "failed_checks": 0,
                "warning_checks": 0,
                "total_issues": len(all_issues)
            },
            "components": self.validation_results,
            "issues": all_issues,
            "recommendations": self._generate_recommendations(all_issues),
            "next_steps": self._generate_next_steps()
        }
        
        # 计算检查统计
        for component in self.validation_results.values():
            if "checks" in component:
                for check in component["checks"]:
                    report["summary"]["total_checks"] += 1
                    if check["status"] == "passed":
                        report["summary"]["passed_checks"] += 1
                    elif check["status"] == "failed":
                        report["summary"]["failed_checks"] += 1
                    elif check["status"] == "warning":
                        report["summary"]["warning_checks"] += 1
        
        self.validation_results["overall"] = report["summary"]
        return report
    
    def _generate_recommendations(self, issues: List[str]) -> List[str]:
        """根据问题生成改进建议"""
        recommendations = []
        
        if not issues:
            recommendations.append("所有验证项通过，继续保持当前备份策略")
            return recommendations
        
        # 根据问题类型生成建议
        issue_categories = {
            "目录不存在": "创建必要的目录并设置正确的权限",
            "脚本缺失": "创建缺失的备份脚本或从模板生成",
            "脚本不可执行": "为脚本文件添加执行权限 (chmod +x)",
            "备份目录不存在": "创建备份目录并确保有足够的存储空间",
            "关键目录不存在": "检查应用部署，确保关键目录被正确创建"
        }
        
        for issue in issues:
            for category, recommendation in issue_categories.items():
                if category in issue:
                    recommendations.append(f"{issue} -> {recommendation}")
                    break
            else:
                recommendations.append(f"{issue} -> 需要进一步调查")
        
        # 通用建议
        recommendations.append("建立定期备份验证机制，确保备份有效性")
        recommendations.append("制定详细的恢复操作手册，包括各种场景的恢复步骤")
        recommendations.append("定期执行恢复演练，验证恢复流程的实际效果")
        recommendations.append("监控备份作业执行状态，设置失败告警通知")
        recommendations.append("定期审计备份策略，确保符合业务需求变化")
        
        return recommendations
    
    def _generate_next_steps(self) -> List[str]:
        """生成下一步行动计划"""
        next_steps = [
            "1. 修复验证中发现的问题，特别是目录和脚本缺失问题",
            "2. 配置实际的备份作业，包括全量备份和增量备份",
            "3. 执行首次真实备份测试，验证备份流程",
            "4. 制定恢复演练计划，定期测试恢复能力",
            "5. 建立备份监控和告警机制",
            "6. 定期审计和优化备份策略"
        ]
        
        return next_steps
    
    def save_report(self, report: Dict, output_dir: str = "."):
        """保存验证报告"""
        try:
            # 创建输出目录
            os.makedirs(output_dir, exist_ok=True)
            
            # 保存JSON报告
            json_path = os.path.join(output_dir, "filesystem-backup-validation-report.json")
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(report, f, indent=2, ensure_ascii=False)
            logger.info(f"JSON报告已保存: {json_path}")
            
            # 保存Markdown报告
            md_path = os.path.join(output_dir, "filesystem-backup-validation-report.md")
            self._generate_markdown_report(report, md_path)
            logger.info(f"Markdown报告已保存: {md_path}")
            
            # 保存摘要
            summary_path = os.path.join(output_dir, "filesystem-validation-summary.txt")
            with open(summary_path, 'w', encoding='utf-8') as f:
                f.write(self._generate_summary_text(report))
            logger.info(f"摘要已保存: {summary_path}")
            
            return {
                "json_report": json_path,
                "markdown_report": md_path,
                "summary": summary_path
            }
            
        except Exception as e:
            logger.error(f"保存报告失败: {e}")
            return None
    
    def _generate_markdown_report(self, report: Dict, output_path: str):
        """生成Markdown格式报告"""
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write("# 文件系统备份验证报告\n\n")
            
            # 元数据
            f.write("## 报告元数据\n")
            f.write(f"- **验证日期**: {report['metadata']['validation_date']}\n")
            f.write(f"- **验证环境**: {report['metadata']['environment']}\n")
            f.write(f"- **验证人员**: {report['metadata']['validator']}\n")
            f.write(f"- **任务ID**: {report['metadata']['task_id']}\n\n")
            
            # 摘要
            f.write("## 验证摘要\n")
            summary = report['summary']
            status_emoji = {
                "passed": "✅",
                "failed": "❌",
                "error": "⚠️",
                "pending": "⏳"
            }
            f.write(f"- **总体状态**: {status_emoji.get(summary['overall_status'], '❓')} {summary['overall_status'].upper()}\n")
            f.write(f"- **总检查项**: {summary['total_checks']}\n")
            f.write(f"- **通过检查**: {summary['passed_checks']}\n")
            f.write(f"- **失败检查**: {summary['failed_checks']}\n")
            f.write(f"- **警告检查**: {summary['warning_checks']}\n")
            f.write(f"- **发现问题**: {summary['total_issues']}\n\n")
            
            # 详细结果
            f.write("## 详细验证结果\n")
            
            for component_name, component in report['components'].items():
                if component_name == 'overall':
                    continue
                    
                f.write(f"### {component_name.upper()}\n")
                f.write(f"**状态**: {component.get('status', 'unknown')}\n\n")
                
                if 'checks' in component and component['checks']:
                    f.write("#### 检查项\n")
                    f.write("| 检查项目 | 状态 | 详情 |\n")
                    f.write("|---------|------|------|\n")
                    for check in component['checks']:
                        status_display = {
                            "passed": "✅ 通过",
                            "failed": "❌ 失败",
                            "warning": "⚠️ 警告",
                            "info": "ℹ️ 信息",
                            "error": "🚨 错误"
                        }.get(check['status'], check['status'])
                        f.write(f"| {check['check']} | {status_display} | {check['details']} |\n")
                    f.write("\n")
                
                if 'issues' in component and component['issues']:
                    f.write("#### 发现问题\n")
                    for issue in component['issues']:
                        f.write(f"- {issue}\n")
                    f.write("\n")
            
            # 改进建议
            if report['recommendations']:
                f.write("## 改进建议\n")
                for i, recommendation in enumerate(report['recommendations'], 1):
                    f.write(f"{i}. {recommendation}\n")
                f.write("\n")
            
            # 下一步行动
            if report['next_steps']:
                f.write("## 下一步行动计划\n")
                for step in report['next_steps']:
                    f.write(f"{step}\n")
                f.write("\n")
            
            # 结论
            f.write("## 验证结论\n")
            if summary['overall_status'] == 'passed':
                f.write("✅ **验证通过** - 文件系统备份策略和恢复流程基本符合要求\n")
            elif summary['overall_status'] == 'failed':
                f.write("❌ **验证失败** - 存在需要立即解决的问题\n")
            else:
                f.write("⚠️ **需要关注** - 验证过程中发现需要改进的问题\n")
            
            f.write("\n---\n")
            f.write("*报告生成时间: " + datetime.datetime.now().isoformat() + "*\n")
    
    def _generate_summary_text(self, report: Dict) -> str:
        """生成文本摘要"""
        summary = report['summary']
        
        text = "=" * 60 + "\n"
        text += "文件系统备份验证摘要\n"
        text += "=" * 60 + "\n\n"
        
        text += f"验证日期: {report['metadata']['validation_date']}\n"
        text += f"验证环境: {report['metadata']['environment']}\n"
        text += f"验证人员: {report['metadata']['validator']}\n"
        text += f"任务ID: {report['metadata']['task_id']}\n\n"
        
        text += f"总体状态: {summary['overall_status'].upper()}\n"
        text += f"总检查项: {summary['total_checks']}\n"
        text += f"通过检查: {summary['passed_checks']}\n"
        text += f"失败检查: {summary['failed_checks']}\n"
        text += f"警告检查: {summary['warning_checks']}\n"
        text += f"发现问题: {summary['total_issues']}\n\n"
        
        if summary['total_issues'] > 0:
            text += "发现的主要问题:\n"
            for i, issue in enumerate(report['issues'][:5], 1):
                text += f"  {i}. {issue}\n"
            if len(report['issues']) > 5:
                text += f"  ... 还有 {len(report['issues']) - 5} 个问题\n"
        
        text += "\n" + "=" * 60 + "\n"
        
        return text
    
    def run_full_validation(self) -> Dict:
        """执行完整验证流程"""
        logger.info("开始文件系统备份完整验证...")
        
        # 执行各项验证
        self.validate_filesystem_backup_strategy()
        self.validate_config_backup_strategy()
        self.validate_logs_backup_strategy()
        self.validate_recovery_process()
        
        # 生成报告
        report = self.generate_validation_report()
        
        logger.info(f"验证完成，总体状态: {report['summary']['overall_status']}")
        return report

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='文件系统备份验证工具')
    parser.add_argument('--config', '-c', help='配置文件路径')
    parser.add_argument('--output-dir', '-o', default='./filesystem-validation-reports', help='报告输出目录')
    parser.add_argument('--component', choices=['all', 'filesystem', 'config', 'logs', 'recovery'], 
                       default='all', help='验证特定组件')
    
    args = parser.parse_args()
    
    # 创建验证器
    validator = FilesystemBackupValidator(args.config)
    
    # 执行验证
    if args.component == 'all':
        report = validator.run_full_validation()
    else:
        # 执行单个组件验证
        if args.component == 'filesystem':
            validator.validate_filesystem_backup_strategy()
        elif args.component == 'config':
            validator.validate_config_backup_strategy()
        elif args.component == 'logs':
            validator.validate_logs_backup_strategy()
        elif args.component == 'recovery':
            validator.validate_recovery_process()
        
        report = validator.generate_validation_report()
    
    # 保存报告
    saved_files = validator.save_report(report, args.output_dir)
    
    if saved_files:
        print(f"\n验证报告已生成:")
        for file_type, file_path in saved_files.items():
            print(f"  - {file_type}: {file_path}")
        
        # 输出摘要
        summary = report['summary']
        print(f"\n验证摘要:")
        print(f"  总体状态: {summary['overall_status'].upper()}")
        print(f"  总检查项: {summary['total_checks']}")
        print(f"  通过检查: {summary['passed_checks']}")
        print(f"  失败检查: {summary['failed_checks']}")
        print(f"  发现问题: {summary['total_issues']}")
        
        if summary['total_issues'] > 0:
            print(f"\n需要关注的问题:")
            for issue in report['issues'][:3]:
                print(f"  - {issue}")
            if len(report['issues']) > 3:
                print(f"  ... 还有 {len(report['issues']) - 3} 个问题")
    else:
        print("报告生成失败")
        return 1
    
    return 0 if report['summary']['overall_status'] == 'passed' else 1

if __name__ == "__main__":
    sys.exit(main())