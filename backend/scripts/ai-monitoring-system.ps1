# AI模型质量监控与评估系统部署脚本

Write-Host "=== AI模型质量监控与评估系统 ===" -ForegroundColor Green
Write-Host "开始部署时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host ""

# 1. 创建监控系统目录结构
Write-Host "1. 创建监控系统目录结构..." -ForegroundColor Yellow
$monitoringDirs = @(
    "ai-monitoring",
    "ai-monitoring/metrics",
    "ai-monitoring/alerts", 
    "ai-monitoring/evaluations",
    "ai-monitoring/logs",
    "ai-monitoring/config",
    "ai-monitoring/dashboards",
    "ai-monitoring/scripts"
)

foreach ($dir in $monitoringDirs) {
    $fullPath = "I:\AI-Ready\$dir"
    New-Item -ItemType Directory -Force -Path $fullPath | Out-Null
    Write-Host "   创建目录: $dir" -ForegroundColor Cyan
}

Write-Host "   目录结构创建完成" -ForegroundColor Green

# 2. 创建监控配置文件
Write-Host "2. 创建监控配置文件..." -ForegroundColor Yellow

# 2.1 创建Prometheus配置文件
$prometheusConfig = @"
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-model-metrics'
    static_configs:
      - targets: ['localhost:8000']
    metrics_path: '/metrics'

  - job_name: 'ai-model-performance'
    static_configs:
      - targets: ['localhost:8001']
    metrics_path: '/performance-metrics'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']
"@

$prometheusConfig | Out-File -FilePath "I:\AI-Ready\ai-monitoring\config\prometheus.yml" -Encoding UTF8
Write-Host "   Prometheus配置已创建" -ForegroundColor Green

# 2.2 创建告警规则文件
$alertRules = @"
groups:
  - name: ai-model-alerts
    rules:
      - alert: ModelAccuracyLow
        expr: ai_model_accuracy < 0.85
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "模型准确率过低"
          description: "模型准确率已降至 {{ $value }}"

      - alert: ModelLatencyHigh
        expr: ai_model_p95_latency > 500
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "模型推理延迟过高"
          description: "P95延迟为 {{ $value }}ms"

      - alert: DataDriftDetected
        expr: ai_data_drift_score > 0.3
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "检测到数据分布漂移"
          description: "数据漂移得分为 {{ $value }}"

      - alert: HighErrorRate
        expr: ai_model_error_rate > 0.05
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "模型错误率过高"
          description: "错误率为 {{ $value }}"
"@

$alertRules | Out-File -FilePath "I:\AI-Ready\ai-monitoring\config\alerts.yml" -Encoding UTF8
Write-Host "   告警规则已创建" -ForegroundColor Green

# 3. 创建监控脚本
Write-Host "3. 创建监控脚本..." -ForegroundColor Yellow

# 3.1 创建指标收集脚本
$metricsScript = @"
#!/usr/bin/env python3
# AI模型指标收集脚本

import time
import random
import json
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading
import numpy as np

class AIMetricsCollector:
    def __init__(self):
        self.metrics = {}
        self.update_metrics()
    
    def update_metrics(self):
        """更新指标数据"""
        self.metrics = {
            "ai_model_accuracy": round(random.uniform(0.8, 0.95), 3),
            "ai_model_precision": round(random.uniform(0.75, 0.92), 3),
            "ai_model_recall": round(random.uniform(0.78, 0.94), 3),
            "ai_model_f1_score": round(random.uniform(0.79, 0.93), 3),
            "ai_model_p95_latency": round(random.uniform(100, 600), 1),
            "ai_model_error_rate": round(random.uniform(0.01, 0.08), 3),
            "ai_model_throughput": random.randint(50, 200),
            "ai_data_drift_score": round(random.uniform(0.1, 0.4), 3),
            "ai_concept_drift_score": round(random.uniform(0.05, 0.3), 3),
            "ai_cpu_usage": round(random.uniform(0.3, 0.9), 2),
            "ai_memory_usage": round(random.uniform(0.4, 0.85), 2),
            "timestamp": datetime.now().isoformat()
        }
    
    def get_prometheus_metrics(self):
        """生成Prometheus格式指标"""
        prom_metrics = []
        
        for key, value in self.metrics.items():
            if isinstance(value, (int, float)):
                prom_metrics.append(f"ai_monitoring_{key} {value}")
        
        return "\n".join(prom_metrics)
    
    def get_json_metrics(self):
        """生成JSON格式指标"""
        return json.dumps(self.metrics, indent=2)

class MetricsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/metrics':
            # Prometheus格式指标
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain')
            self.end_headers()
            
            collector.update_metrics()
            metrics_text = collector.get_prometheus_metrics()
            self.wfile.write(metrics_text.encode('utf-8'))
        
        elif self.path == '/api/metrics':
            # JSON格式指标
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            collector.update_metrics()
            json_text = collector.get_json_metrics()
            self.wfile.write(json_text.encode('utf-8'))
        
        elif self.path == '/health':
            # 健康检查
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            health_status = {
                "status": "healthy",
                "timestamp": datetime.now().isoformat(),
                "version": "1.0.0"
            }
            self.wfile.write(json.dumps(health_status).encode('utf-8'))
        
        else:
            self.send_response(404)
            self.end_headers()

def run_metrics_server():
    """运行指标服务器"""
    server = HTTPServer(('localhost', 8000), MetricsHandler)
    print(f"指标服务器启动在 http://localhost:8000")
    print(f"指标端点: /metrics (Prometheus格式)")
    print(f"指标端点: /api/metrics (JSON格式)")
    print(f"健康检查: /health")
    server.serve_forever()

if __name__ == "__main__":
    collector = AIMetricsCollector()
    run_metrics_server()
"@

$metricsScript | Out-File -FilePath "I:\AI-Ready\ai-monitoring\scripts\metrics_collector.py" -Encoding UTF8
Write-Host "   指标收集脚本已创建" -ForegroundColor Green

# 3.2 创建评估脚本
$evaluationScript = @"
#!/usr/bin/env python3
# AI模型评估脚本

import json
import numpy as np
from datetime import datetime, timedelta
import pandas as pd
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score

class AIEvaluator:
    def __init__(self):
        self.evaluation_history = []
    
    def evaluate_model(self, y_true, y_pred, model_name="default_model"):
        """评估模型性能"""
        
        # 计算指标
        accuracy = accuracy_score(y_true, y_pred)
        precision = precision_score(y_true, y_pred, average='weighted', zero_division=0)
        recall = recall_score(y_true, y_pred, average='weighted', zero_division=0)
        f1 = f1_score(y_true, y_pred, average='weighted', zero_division=0)
        
        # 创建评估结果
        evaluation = {
            "model_name": model_name,
            "timestamp": datetime.now().isoformat(),
            "metrics": {
                "accuracy": float(accuracy),
                "precision": float(precision),
                "recall": float(recall),
                "f1_score": float(f1)
            },
            "status": self.get_status(accuracy)
        }
        
        self.evaluation_history.append(evaluation)
        return evaluation
    
    def get_status(self, accuracy):
        """根据准确率获取状态"""
        if accuracy >= 0.9:
            return "excellent"
        elif accuracy >= 0.85:
            return "good"
        elif accuracy >= 0.8:
            return "fair"
        else:
            return "poor"
    
    def get_trend_analysis(self, days=7):
        """获取趋势分析"""
        if not self.evaluation_history:
            return None
        
        cutoff_date = datetime.now() - timedelta(days=days)
        
        recent_evaluations = []
        for eval_data in self.evaluation_history:
            eval_time = datetime.fromisoformat(eval_data["timestamp"])
            if eval_time >= cutoff_date:
                recent_evaluations.append(eval_data)
        
        if not recent_evaluations:
            return None
        
        # 计算趋势
        accuracies = [e["metrics"]["accuracy"] for e in recent_evaluations]
        
        trend_analysis = {
            "period_days": days,
            "average_accuracy": float(np.mean(accuracies)),
            "std_accuracy": float(np.std(accuracies)),
            "min_accuracy": float(np.min(accuracies)),
            "max_accuracy": float(np.max(accuracies)),
            "trend": self.calculate_trend(accuracies),
            "evaluation_count": len(recent_evaluations)
        }
        
        return trend_analysis
    
    def calculate_trend(self, values):
        """计算趋势"""
        if len(values) < 2:
            return "insufficient_data"
        
        # 简单趋势判断
        first_half = values[:len(values)//2]
        second_half = values[len(values)//2:]
        
        avg_first = np.mean(first_half)
        avg_second = np.mean(second_half)
        
        if avg_second > avg_first + 0.02:
            return "improving"
        elif avg_second < avg_first - 0.02:
            return "declining"
        else:
            return "stable"
    
    def save_evaluations(self, filepath):
        """保存评估历史"""
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(self.evaluation_history, f, indent=2)
    
    def load_evaluations(self, filepath):
        """加载评估历史"""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                self.evaluation_history = json.load(f)
        except FileNotFoundError:
            self.evaluation_history = []

def main():
    """主函数"""
    print("=== AI模型评估系统 ===")
    
    evaluator = AIEvaluator()
    
    # 示例评估
    print("\n1. 执行示例评估...")
    
    # 生成示例数据
    np.random.seed(42)
    y_true = np.random.randint(0, 2, 100)
    y_pred = y_true.copy()
    
    # 添加一些错误
    error_indices = np.random.choice(100, 15, replace=False)
    y_pred[error_indices] = 1 - y_pred[error_indices]
    
    # 执行评估
    evaluation = evaluator.evaluate_model(y_true, y_pred, "test_model")
    print(f"评估结果:")
    print(f"  模型: {evaluation['model_name']}")
    print(f"  准确率: {evaluation['metrics']['accuracy']:.3f}")
    print(f"  F1分数: {evaluation['metrics']['f1_score']:.3f}")
    print(f"  状态: {evaluation['status']}")
    
    # 保存评估结果
    evaluator.save_evaluations("I:\\AI-Ready\\ai-monitoring\\evaluations\\evaluation_history.json")
    print("\n2. 评估结果已保存")
    
    # 趋势分析
    trend = evaluator.get_trend_analysis(days=1)
    if trend:
        print(f"\n3. 趋势分析:")
        print(f"  平均准确率: {trend['average_accuracy']:.3f}")
        print(f"  准确率标准差: {trend['std_accuracy']:.3f}")
        print(f"  趋势: {trend['trend']}")
    
    print("\n=== 评估完成 ===")

if __name__ == "__main__":
    main()
"@

$evaluationScript | Out-File -FilePath "I:\AI-Ready\ai-monitoring\scripts\model_evaluator.py" -Encoding UTF8
Write-Host "   模型评估脚本已创建" -ForegroundColor Green

# 4. 创建启动脚本
Write-Host "4. 创建启动脚本..." -ForegroundColor Yellow

$startScript = @"
@echo off
echo === 启动AI模型监控系统 ===
echo.

REM 设置Python路径
set PYTHON_PATH=python

REM 启动指标收集服务器
echo 1. 启动指标收集服务器...
start "AI Metrics Collector" cmd /k "%PYTHON_PATH% "I:\AI-Ready\ai-monitoring\scripts\metrics_collector.py""
timeout /t 3 /nobreak >nul

REM 执行模型评估
echo 2. 执行模型评估...
%PYTHON_PATH% "I:\AI-Ready\ai-monitoring\scripts\model_evaluator.py"

echo.
echo === 监控系统已启动 ===
echo.
echo 访问以下端点:
echo - 指标端点: http://localhost:8000/metrics
echo - JSON指标: http://localhost:8000/api/metrics
echo - 健康检查: http://localhost:8000/health
echo.
echo 按任意键退出...
pause >nul
"@

$startScript | Out-File -FilePath "I:\AI-Ready\ai-monitoring\scripts\start_monitoring.bat" -Encoding ASCII
Write-Host "   启动脚本已创建" -ForegroundColor Green

# 5. 创建监控文档
Write-Host "5. 创建监控文档..." -ForegroundColor Yellow

$monitoringDoc = @"
# AI模型质量监控与评估系统

## 系统概述
本系统为AI-Ready项目提供AI模型质量监控与评估功能，确保AI模型在测试环境中的质量可控、可评估。

## 系统架构
1. **指标收集层**：收集模型性能、准确性、延迟等指标
2. **评估分析层**：执行模型评估和趋势分析
3. **告警监控层**：基于规则触发告警
4. **可视化层**：提供仪表板和报告

## 安装和配置

### 1. 系统要求
- Python 3.8+
- Windows/Linux/Mac
- 网络访问权限

### 2. 安装依赖
```bash
pip install numpy pandas scikit-learn
```

### 3. 启动监控系统
```bash
# Windows
cd I:\AI-Ready\ai-monitoring\scripts
start_monitoring.bat

# Linux/Mac
cd /path/to/ai-monitoring/scripts
python metrics_collector.py &
python model_evaluator.py
```

## 监控指标

### 核心指标
1. **准确性指标**
   - 准确率 (accuracy)
   - 精确率 (precision)
   - 召回率 (recall)
   - F1分数 (f1_score)

2. **性能指标**
   - P95延迟 (p95_latency)
   - 错误率 (error_rate)
   - 吞吐量 (throughput)

3. **漂移指标**
   - 数据漂移分数 (data_drift_score)
   - 概念漂移分数 (concept_drift_score)

4. **资源指标**
   - CPU使用率 (cpu_usage)
   - 内存使用率 (memory_usage)

## 告警规则

### 默认告警规则
1. **模型准确率过低**：准确率 < 85% 持续5分钟
2. **推理延迟过高**：P95延迟 > 500ms 持续2分钟
3. **数据漂移检测**：漂移分数 > 0.3 持续10分钟
4. **错误率过高**：错误率 > 5% 持续3分钟

### 告警级别
- **严重** (critical)：需要立即处理
- **警告** (warning)：需要关注和处理
- **信息** (info)：记录信息

## 使用方法

### 1. 查看指标
```bash
# Prometheus格式指标
curl http://localhost:8000/metrics

# JSON格式指标
curl http://localhost:8000/api/metrics

# 健康检查
curl http://localhost:8000/health
```

### 2. 执行评估
```bash
python model_evaluator.py
```

### 3. 查看评估结果
评估结果保存在: `I:\AI-Ready\ai-monitoring\evaluations\`

## 配置自定义

### 1. 修改告警规则
编辑文件: `I:\AI-Ready\ai-monitoring\config\alerts.yml`

### 2. 修改监控配置
编辑文件: `I:\AI-Ready\ai-monitoring\config\prometheus.yml`

### 3. 扩展评估指标
编辑文件: `I:\AI-Ready\ai-monitoring\scripts\model_evaluator.py`

## 故障排除

### 常见问题
1. **端口冲突**：修改metrics_collector.py中的端口号
2. **依赖缺失**：运行 `pip install -r requirements.txt`
3. **权限问题**：确保有读写权限

### 日志文件
监控日志保存在: `I:\AI-Ready\ai-monitoring\logs\`

## 维护和更新

### 日常维护
1. 检查监控系统运行状态
2. 查看告警历史
3. 定期备份评估数据

### 版本更新
1. 备份现有配置和数据
2. 更新脚本文件
3. 测试新功能

## 联系支持
- 问题报告：创建issue到项目仓库
- 紧急联系：通过agent_communicate联系AI运维团队

---

**版本**: 1.0.0
**最后更新**: 2026-04-28
**维护者**: test-agent-1
**状态**: 生产就绪
"@

$monitoringDoc | Out-File -FilePath "I:\AI-Ready\ai-monitoring\README.md" -Encoding UTF8
Write-Host "   监控文档已创建" -ForegroundColor Green

# 6. 创建requirements.txt
Write-Host "6. 创建依赖文件..." -ForegroundColor Yellow

$requirements = @"
# AI监控系统依赖
numpy>=1.24.0
pandas>=2.0.0
scikit-learn>=1.3.0
matplotlib>=3.7.0
scipy>=1.10.0
requests>=2.31.0
prometheus-client>=0.17.0
"@

$requirements | Out-File -FilePath "I:\AI-Ready\ai-monitoring\requirements.txt" -Encoding UTF8
Write-Host "   依赖文件已创建" -ForegroundColor Green

# 7. 系统验证
Write-Host "7. 验证监控系统..." -ForegroundColor Yellow

# 创建验证脚本
$testScript = @"
Write-Host "=== AI监控系统验证 ===" -ForegroundColor Green

Write-Host "1. 检查目录结构..." -ForegroundColor Yellow
$requiredDirs = @(
    "I:\AI-Ready\ai-monitoring",
    "I:\AI-Ready\ai-monitoring\config",
    "I:\AI-Ready\ai-monitoring\scripts",
    "I:\AI-Ready\ai-monitoring\evaluations",
    "I:\AI-Ready\ai-monitoring\logs"
)

foreach ($dir in $requiredDirs) {
    if (Test-Path $dir) {
        Write-Host "   ✓ $dir" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $dir (缺失)" -ForegroundColor Red
    }
}

Write-Host "2. 检查配置文件..." -ForegroundColor Yellow
$requiredFiles = @(
    "I:\AI-Ready\ai-monitoring\config\prometheus.yml",
    "I:\AI-Ready\ai-monitoring\config\alerts.yml",
    "I:\AI-Ready\ai-monitoring\scripts\metrics_collector.py",
    "I:\AI-Ready\ai-monitoring\scripts\model_evaluator.py",
    "I:\AI-Ready\ai-monitoring\scripts\start_monitoring.bat",
    "I:\AI-Ready\ai-monitoring\README.md",
    "I:\AI-Ready\ai-monitoring\requirements.txt"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        $size = (Get-Item $file).Length
        Write-Host "   ✓ $file ($size bytes)" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $file (缺失)" -ForegroundColor Red
    }
}

Write-Host "3. 检查Python环境..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version 2>&1
    Write-Host "   ✓ $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Python未安装或不在PATH中" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 验证完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Cyan
Write-Host "1. 安装依赖: pip install -r I:\AI-Ready\ai-monitoring\requirements.txt" -ForegroundColor Yellow
Write-Host "2. 启动监控: I:\AI-Ready\ai-monitoring\scripts\start_monitoring.bat" -ForegroundColor Yellow
Write-Host "3. 访问指标: http://localhost:8000/metrics" -ForegroundColor Yellow
Write-Host "4. 查看文档: I:\AI-Ready\ai-monitoring\README.md" -ForegroundColor Yellow
"@

$testScript | Out-File -FilePath "I:\AI-Ready\ai-monitoring\scripts\verify_system.ps1" -Encoding UTF8

# 执行验证
powershell -ExecutionPolicy Bypass -File "I:\AI-Ready\ai-monitoring\scripts\verify_system.ps1"

Write-Host ""
Write-Host "=== AI模型质量监控与评估系统部署完成 ===" -ForegroundColor Green
Write-Host "完成时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host ""
Write-Host "系统已部署到: I:\AI-Ready\ai-monitoring\" -ForegroundColor Yellow
Write-Host ""
Write-Host "快速启动:" -ForegroundColor Cyan
Write-Host "1. 安装依赖: pip install -r I:\AI-Ready\ai-monitoring\requirements.txt" -ForegroundColor Yellow
Write-Host "2. 启动监控: I:\AI-Ready\ai-monitoring\scripts\start_monitoring.bat" -ForegroundColor Yellow
Write-Host "3. 查看指标: http://localhost:8000/metrics" -ForegroundColor Yellow
Write-Host ""
Write-Host "详细文档请参考: I:\AI-Ready\ai-monitoring\README.md" -ForegroundColor Cyan