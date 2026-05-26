#!/usr/bin/env python3
"""
测试监控服务 - 为AI-Ready测试环境提供测试执行监控、质量分析和告警功能
"""

import os
import sys
import time
import json
import logging
from datetime import datetime
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from enum import Enum

import uvicorn
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import psutil
import requests
from prometheus_client import Counter, Gauge, Histogram, Summary, generate_latest
from prometheus_client.registry import CollectorRegistry

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test_monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Prometheus指标定义
class TestMetrics:
    """测试监控指标"""
    
    def __init__(self):
        # 创建单独的注册表
        self.registry = CollectorRegistry()
        
        # 测试执行指标
        self.tests_total = Counter(
            'test_execution_total',
            '总测试执行次数',
            ['status', 'module', 'test_type'],
            registry=self.registry
        )
        
        self.tests_duration = Histogram(
            'test_execution_duration_seconds',
            '测试执行时间分布',
            ['module', 'test_type'],
            buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0],
            registry=self.registry
        )
        
        # 测试质量指标
        self.test_success_rate = Gauge(
            'test_success_rate',
            '测试成功率',
            ['module'],
            registry=self.registry
        )
        
        self.test_failure_rate = Gauge(
            'test_failure_rate',
            '测试失败率',
            ['module'],
            registry=self.registry
        )
        
        # 测试性能指标
        self.test_execution_time = Gauge(
            'test_execution_time_seconds',
            '测试执行总时间',
            registry=self.registry
        )
        
        self.tests_per_second = Gauge(
            'tests_per_second',
            '每秒测试执行数',
            registry=self.registry
        )
        
        # 测试环境指标
        self.test_cpu_usage = Gauge(
            'test_cpu_usage_percent',
            '测试执行CPU使用率',
            registry=self.registry
        )
        
        self.test_memory_usage = Gauge(
            'test_memory_usage_mb',
            '测试执行内存使用(MB)',
            registry=self.registry
        )
        
        # 测试覆盖率指标
        self.test_coverage = Gauge(
            'test_coverage_percent',
            '测试覆盖率百分比',
            ['coverage_type'],
            registry=self.registry
        )
        
        # 测试缺陷指标
        self.defects_found = Counter(
            'defects_found_total',
            '发现的缺陷总数',
            ['severity', 'module'],
            registry=self.registry
        )
        
        self.defects_resolved = Counter(
            'defects_resolved_total',
            '已解决的缺陷总数',
            ['severity', 'module'],
            registry=self.registry
        )

# 数据模型
class TestStatus(str, Enum):
    """测试状态枚举"""
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"

class TestType(str, Enum):
    """测试类型枚举"""
    UNIT = "unit"
    INTEGRATION = "integration"
    E2E = "e2e"
    API = "api"
    UI = "ui"
    PERFORMANCE = "performance"
    SECURITY = "security"

class TestExecution(BaseModel):
    """测试执行记录"""
    test_id: str
    test_name: str
    module: str
    test_type: TestType
    status: TestStatus
    duration_seconds: float
    timestamp: float = time.time()
    metadata: Dict[str, Any] = {}

class TestCoverage(BaseModel):
    """测试覆盖率数据"""
    timestamp: float = time.time()
    module: str
    line_coverage: float
    branch_coverage: float
    function_coverage: float
    total_lines: int
    covered_lines: int

class TestDefect(BaseModel):
    """测试缺陷数据"""
    defect_id: str
    module: str
    severity: str  # critical, high, medium, low
    description: str
    discovered_at: float = time.time()
    resolved_at: Optional[float] = None
    status: str = "open"  # open, in_progress, resolved, closed

class TestEnvironment(BaseModel):
    """测试环境数据"""
    timestamp: float = time.time()
    cpu_usage_percent: float
    memory_usage_mb: float
    disk_usage_percent: float
    network_active: bool
    services: Dict[str, bool]  # 服务名称: 是否可用

class AlertNotification(BaseModel):
    """告警通知"""
    alert_id: str
    severity: str  # emergency, critical, warning, info
    title: str
    description: str
    timestamp: float = time.time()
    acknowledged: bool = False
    resolved: bool = False

# 测试监控服务
class TestMonitorService:
    """测试监控服务核心类"""
    
    def __init__(self):
        self.metrics = TestMetrics()
        self.test_history: List[TestExecution] = []
        self.coverage_history: List[TestCoverage] = []
        self.defects: List[TestDefect] = []
        self.alerts: List[AlertNotification] = []
        self.start_time = time.time()
        
        # 统计数据
        self.stats = {
            'total_tests': 0,
            'passed_tests': 0,
            'failed_tests': 0,
            'skipped_tests': 0,
            'total_duration': 0.0,
            'last_update': time.time()
        }
        
        # 模块统计数据
        self.module_stats: Dict[str, Dict] = {}
        
        logger.info("测试监控服务初始化完成")
    
    def record_test_execution(self, test_exec: TestExecution) -> Dict[str, Any]:
        """记录测试执行数据"""
        try:
            # 更新总体统计
            self.stats['total_tests'] += 1
            self.stats['total_duration'] += test_exec.duration_seconds
            
            if test_exec.status == TestStatus.PASSED:
                self.stats['passed_tests'] += 1
            elif test_exec.status == TestStatus.FAILED:
                self.stats['failed_tests'] += 1
            elif test_exec.status == TestStatus.SKIPPED:
                self.stats['skipped_tests'] += 1
            
            # 更新模块统计
            if test_exec.module not in self.module_stats:
                self.module_stats[test_exec.module] = {
                    'total': 0, 'passed': 0, 'failed': 0, 'skipped': 0,
                    'duration': 0.0, 'success_rate': 0.0
                }
            
            module_stat = self.module_stats[test_exec.module]
            module_stat['total'] += 1
            module_stat['duration'] += test_exec.duration_seconds
            
            if test_exec.status == TestStatus.PASSED:
                module_stat['passed'] += 1
            elif test_exec.status == TestStatus.FAILED:
                module_stat['failed'] += 1
            elif test_exec.status == TestStatus.SKIPPED:
                module_stat['skipped'] += 1
            
            # 计算成功率
            if module_stat['total'] > 0:
                module_stat['success_rate'] = (module_stat['passed'] / module_stat['total']) * 100
            
            # 更新指标
            self.metrics.tests_total.labels(
                status=test_exec.status.value,
                module=test_exec.module,
                test_type=test_exec.test_type.value
            ).inc()
            
            self.metrics.tests_duration.labels(
                module=test_exec.module,
                test_type=test_exec.test_type.value
            ).observe(test_exec.duration_seconds)
            
            # 更新模块成功率指标
            self.metrics.test_success_rate.labels(
                module=test_exec.module
            ).set(module_stat['success_rate'])
            
            self.metrics.test_failure_rate.labels(
                module=test_exec.module
            ).set((module_stat['failed'] / module_stat['total']) * 100)
            
            # 更新总体执行时间
            self.metrics.test_execution_time.set(self.stats['total_duration'])
            
            # 计算TPS
            elapsed_time = time.time() - self.start_time
            if elapsed_time > 0:
                tps = self.stats['total_tests'] / elapsed_time
                self.metrics.tests_per_second.set(tps)
            
            # 保存历史记录
            self.test_history.append(test_exec)
            
            # 检查是否需要触发告警
            self._check_alerts(test_exec)
            
            logger.info(f"记录测试执行: {test_exec.test_name} - {test_exec.status} ({test_exec.duration_seconds:.2f}s)")
            
            return {
                'success': True,
                'test_id': test_exec.test_id,
                'recorded_at': time.time(),
                'stats': self.stats.copy()
            }
            
        except Exception as e:
            logger.error(f"记录测试执行失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"记录测试执行失败: {str(e)}")
    
    def update_coverage(self, coverage: TestCoverage) -> Dict[str, Any]:
        """更新测试覆盖率数据"""
        try:
            self.coverage_history.append(coverage)
            
            # 更新覆盖率指标
            self.metrics.test_coverage.labels(
                coverage_type='line'
            ).set(coverage.line_coverage)
            
            self.metrics.test_coverage.labels(
                coverage_type='branch'
            ).set(coverage.branch_coverage)
            
            self.metrics.test_coverage.labels(
                coverage_type='function'
            ).set(coverage.function_coverage)
            
            logger.info(f"更新测试覆盖率: {coverage.module} - 行覆盖率: {coverage.line_coverage:.1f}%")
            
            return {
                'success': True,
                'coverage_id': f"coverage_{int(time.time())}",
                'recorded_at': time.time()
            }
            
        except Exception as e:
            logger.error(f"更新测试覆盖率失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"更新测试覆盖率失败: {str(e)}")
    
    def record_defect(self, defect: TestDefect) -> Dict[str, Any]:
        """记录测试发现的缺陷"""
        try:
            self.defects.append(defect)
            
            # 更新缺陷指标
            self.metrics.defects_found.labels(
                severity=defect.severity,
                module=defect.module
            ).inc()
            
            logger.info(f"记录缺陷: {defect.defect_id} - {defect.severity} - {defect.module}")
            
            # 触发缺陷告警
            self._create_defect_alert(defect)
            
            return {
                'success': True,
                'defect_id': defect.defect_id,
                'recorded_at': time.time()
            }
            
        except Exception as e:
            logger.error(f"记录缺陷失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"记录缺陷失败: {str(e)}")
    
    def resolve_defect(self, defect_id: str) -> Dict[str, Any]:
        """标记缺陷已解决"""
        try:
            for defect in self.defects:
                if defect.defect_id == defect_id:
                    defect.resolved_at = time.time()
                    defect.status = "resolved"
                    
                    # 更新已解决缺陷指标
                    self.metrics.defects_resolved.labels(
                        severity=defect.severity,
                        module=defect.module
                    ).inc()
                    
                    logger.info(f"缺陷已解决: {defect_id}")
                    
                    return {
                        'success': True,
                        'defect_id': defect_id,
                        'resolved_at': defect.resolved_at
                    }
            
            raise HTTPException(status_code=404, detail=f"未找到缺陷: {defect_id}")
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"标记缺陷解决失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"标记缺陷解决失败: {str(e)}")
    
    def update_environment_status(self) -> Dict[str, Any]:
        """更新测试环境状态"""
        try:
            # 收集环境指标
            cpu_percent = psutil.cpu_percent(interval=0.1)
            memory = psutil.virtual_memory()
            disk = psutil.disk_usage('/')
            
            env_status = TestEnvironment(
                cpu_usage_percent=cpu_percent,
                memory_usage_mb=memory.used / (1024 * 1024),
                disk_usage_percent=disk.percent,
                network_active=self._check_network(),
                services=self._check_services()
            )
            
            # 更新环境指标
            self.metrics.test_cpu_usage.set(cpu_percent)
            self.metrics.test_memory_usage.set(memory.used / (1024 * 1024))
            
            # 检查环境告警
            self._check_environment_alerts(env_status)
            
            logger.info(f"环境状态更新: CPU={cpu_percent:.1f}%, 内存={memory.used / (1024 * 1024):.1f}MB")
            
            return {
                'success': True,
                'environment': env_status.dict(),
                'timestamp': time.time()
            }
            
        except Exception as e:
            logger.error(f"更新环境状态失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"更新环境状态失败: {str(e)}")
    
    def get_test_report(self, time_range: Optional[str] = None) -> Dict[str, Any]:
        """获取测试报告"""
        try:
            report = {
                'summary': self.stats.copy(),
                'modules': self.module_stats,
                'coverage': self._get_coverage_summary(),
                'defects': self._get_defects_summary(),
                'performance': self._get_performance_summary(),
                'alerts': len(self.alerts),
                'generated_at': time.time()
            }
            
            return report
            
        except Exception as e:
            logger.error(f"生成测试报告失败: {str(e)}")
            raise HTTPException(status_code=500, detail=f"生成测试报告失败: {str(e)}")
    
    def _check_alerts(self, test_exec: TestExecution):
        """检查是否需要触发告警"""
        try:
            # 检查失败率告警
            if test_exec.status == TestStatus.FAILED:
                module_stat = self.module_stats.get(test_exec.module, {})
                if module_stat.get('total', 0) > 10:
                    failure_rate = (module_stat.get('failed', 0) / module_stat.get('total', 0)) * 100
                    
                    if failure_rate > 20:  # 失败率超过20%
                        self._create_alert(
                            severity="critical",
                            title=f"测试模块 {test_exec.module} 失败率过高",
                            description=f"模块 {test_exec.module} 测试失败率达到 {failure_rate:.1f}%，请立即检查"
                        )
                    elif failure_rate > 10:  # 失败率超过10%
                        self._create_alert(
                            severity="warning",
                            title=f"测试模块 {test_exec.module} 失败率偏高",
                            description=f"模块 {test_exec.module} 测试失败率达到 {failure_rate:.1f}%，建议检查"
                        )
            
            # 检查执行时间告警
            if test_exec.duration_seconds > 30:  # 测试执行超过30秒
                self._create_alert(
                    severity="warning",
                    title=f"测试执行时间过长",
                    description=f"测试 {test_exec.test_name} 执行时间 {test_exec.duration_seconds:.1f}秒，建议优化"
                )
                
        except Exception as e:
            logger.error(f"检查告警失败: {str(e)}")
    
    def _create_defect_alert(self, defect: TestDefect):
        """创建缺陷告警"""
        severity_map = {
            'critical': 'emergency',
            'high': 'critical',
            'medium': 'warning',
            'low': 'info'
        }
        
        alert_severity = severity_map.get(defect.severity, 'info')
        
        self._create_alert(
            severity=alert_severity,
            title=f"发现 {defect.severity} 级别缺陷",
            description=f"在模块 {defect.module} 中发现缺陷: {defect.description}"
        )
    
    def _check_environment_alerts(self, env_status: TestEnvironment):
        """检查环境告警"""
        # CPU使用率告警
        if env_status.cpu_usage_percent > 90:
            self._create_alert(
                severity="critical",
                title="测试环境CPU使用率过高",
                description=f"CPU使用率 {env_status.cpu_usage_percent:.1f}%，可能影响测试执行"
            )
        elif env_status.cpu_usage_percent > 80:
            self._create_alert(
                severity="warning",
                title="测试环境CPU使用率偏高",
                description=f"CPU使用率 {env_status.cpu_usage_percent:.1f}%，建议监控"
            )
        
        # 内存使用告警
        if env_status.memory_usage_mb > 8 * 1024:  # 超过8GB
            self._create_alert(
                severity="warning",
                title="测试环境内存使用较高",
                description=f"内存使用 {env_status.memory_usage_mb / 1024:.1f}GB，建议优化"
            )
        
        # 磁盘空间告警
        if env_status.disk_usage_percent > 90:
            self._create_alert(
                severity="critical",
                title="测试环境磁盘空间不足",
                description=f"磁盘使用率 {env_status.disk_usage_percent:.1f}%，建议清理"
            )
    
    def _create_alert(self, severity: str, title: str, description: str):
        """创建告警"""
        alert = AlertNotification(
            alert_id=f"alert_{int(time.time())}",
            severity=severity,
            title=title,
            description=description
        )
        
        self.alerts.append(alert)
        logger.warning(f"创建告警: [{severity}] {title}")
        
        # 这里可以添加通知发送逻辑
        # self._send_notification(alert)
    
    def _check_network(self) -> bool:
        """检查网络连通性"""
        try:
            # 尝试连接公共DNS
            import socket
            socket.create_connection(("8.8.8.8", 53), timeout=2)
            return True
        except:
            return False
    
    def _check_services(self) -> Dict[str, bool]:
        """检查依赖服务状态"""
        services = {
            'database': False,
            'api_server': False,
            'cache': False,
            'message_queue': False
        }
        
        # 这里应该实现实际的健康检查逻辑
        # 暂时返回模拟数据
        return services
    
    def _get_coverage_summary(self) -> Dict[str, Any]:
        """获取覆盖率汇总"""
        if not self.coverage_history:
            return {}
        
        latest = self.coverage_history[-1]
        return {
            'line_coverage': latest.line_coverage,
            'branch_coverage': latest.branch_coverage,
            'function_coverage': latest.function_coverage,
            'total_lines': latest.total_lines,
            'covered_lines': latest.covered_lines,
            'updated_at': latest.timestamp
        }
    
    def _get_defects_summary(self) -> Dict[str, Any]:
        """获取缺陷汇总"""
        if not self.defects:
            return {'total': 0, 'by_severity': {}, 'open': 0, 'resolved': 0}
        
        by_severity = {}
        open_count = 0
        resolved_count = 0
        
        for defect in self.defects:
            by_severity[defect.severity] = by_severity.get(defect.severity, 0) + 1
            if defect.status == 'open':
                open_count += 1
            elif defect.status == 'resolved':
                resolved_count += 1
        
        return {
            'total': len(self.defects),
            'by_severity': by_severity,
            'open': open_count,
            'resolved': resolved_count
        }
    
    def _get_performance_summary(self) -> Dict[str, Any]:
        """获取性能汇总"""
        if not self.test_history:
            return {}
        
        durations = [t.duration_seconds for t in self.test_history]
        
        return {
            'avg_duration': sum(durations) / len(durations),
            'max_duration': max(durations),
            'min_duration': min(durations),
            'total_duration': sum(durations),
            'tests_per_second': len(self.test_history) / (time.time() - self.start_time)
        }

# FastAPI应用
app = FastAPI(
    title="AI-Ready测试监控服务",
    description="为AI-Ready测试环境提供测试执行监控、质量分析和告警功能",
    version="1.0.0"
)

# 添加CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 创建监控服务实例
monitor_service = TestMonitorService()

@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "AI-Ready测试监控服务",
        "version": "1.0.0",
        "status": "running",
        "uptime": time.time() - monitor_service.start_time
    }

@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "timestamp": time.time(),
        "metrics": {
            "test_count": monitor_service.stats['total_tests'],
            "uptime": time.time() - monitor_service.start_time
        }
    }

@app.get("/metrics")
async def metrics():
    """Prometheus指标端点"""
    return generate_latest(monitor_service.metrics.registry)

@app.post("/record-test")
async def record_test(test_exec: TestExecution):
    """记录测试执行"""
    return monitor_service.record_test_execution(test_exec)

@app.post("/update-coverage")
async def update_coverage(coverage: TestCoverage):
    """更新测试覆盖率"""
    return monitor_service.update_coverage(coverage)

@app.post("/record-defect")
async def record_defect(defect: TestDefect):
    """记录测试发现的缺陷"""
    return monitor_service.record_defect(defect)

@app.post("/resolve-defect/{defect_id}")
async def resolve_defect(defect_id: str):
    """标记缺陷已解决"""
    return monitor_service.resolve_defect(defect_id)

@app.get("/environment-status")
async def environment_status():
    """获取测试环境状态"""
    return monitor_service.update_environment_status()

@app.get("/report")
async def get_report(time_range: Optional[str] = None):
    """获取测试报告"""
    return monitor_service.get_test_report(time_range)

@app.get("/alerts")
async def get_alerts(acknowledged: Optional[bool] = None, resolved: Optional[bool] = None):
    """获取告警列表"""
    alerts = monitor_service.alerts
    
    if acknowledged is not None:
        alerts = [a for a in alerts if a.acknowledged == acknowledged]
    
    if resolved is not None:
        alerts = [a for a in alerts if a.resolved == resolved]
    
    return {
        "total": len(alerts),
        "alerts": [a.dict() for a in alerts]
    }

@app.post("/acknowledge-alert/{alert_id}")
async def acknowledge_alert(alert_id: str):
    """确认告警"""
    for alert in monitor_service.alerts:
        if alert.alert_id == alert_id:
            alert.acknowledged = True
            return {"success": True, "alert_id": alert_id, "acknowledged": True}
    
    raise HTTPException(status_code=404, detail=f"未找到告警: {alert_id}")

@app.post("/resolve-alert/{alert_id}")
async def resolve_alert(alert_id: str):
    """标记告警已解决"""
    for alert in monitor_service.alerts:
        if alert.alert_id == alert_id:
            alert.resolved = True
            return {"success": True, "alert_id": alert_id, "resolved": True}
    
    raise HTTPException(status_code=404, detail=f"未找到告警: {alert_id}")

if __name__ == "__main__":
    # 启动服务
    logger.info("启动测试监控服务...")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8100,
        log_level="info"
    )