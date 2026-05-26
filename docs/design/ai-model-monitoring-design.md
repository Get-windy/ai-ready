# AI模型质量监控与评估系统设计

**任务ID**: task_1777313502160_i15wun6oj
**任务标题**: 【Sprint 27+1】测试环境AI模型质量监控与评估系统
**创建时间**: 2026-04-28

## 1. 系统概述

### 1.1 目标
为Sprint 27+1测试环境配置AI模型质量监控与评估系统，确保测试环境AI模型质量可控、可评估。

### 1.2 范围
- 智能推荐模型质量监控
- 对话聊天模型质量监控  
- NLQ搜索模型质量监控
- 异常检测模型质量监控（如果存在）

### 1.3 系统架构
```
┌─────────────────────────────────────────────────────┐
│                 AI模型质量监控系统                   │
├─────────────────────────────────────────────────────┤
│  数据采集层    │  监控指标层    │  评估分析层       │
│  - 模型预测    │  - 准确性      │  - 漂移检测       │
│  - 真实反馈    │  - 延迟        │  - 性能退化       │
│  - 系统指标    │  - 吞吐量      │  - 异常检测       │
│  - 业务指标    │  - 资源使用    │  - 根因分析       │
└─────────────────────────────────────────────────────┘
           │               │               │
           ▼               ▼               ▼
┌─────────────────────────────────────────────────────┐
│                 可视化与告警层                       │
│  - Grafana仪表板  - 告警规则  - 报告生成            │
└─────────────────────────────────────────────────────┘
```

## 2. 质量监控指标体系设计

### 2.1 准确性指标
| 指标 | 描述 | 计算公式 | 监控频率 | 告警阈值 |
|------|------|----------|----------|----------|
| 准确率 | 分类正确的比例 | (TP+TN)/(TP+TN+FP+FN) | 实时 | < 85% |
| 精确率 | 正例预测的准确率 | TP/(TP+FP) | 实时 | < 80% |
| 召回率 | 正例被正确识别的比例 | TP/(TP+FN) | 实时 | < 75% |
| F1分数 | 精确率和召回率的调和平均 | 2*(精确率*召回率)/(精确率+召回率) | 实时 | < 0.8 |
| AUC-ROC | 模型整体性能 | ROC曲线下面积 | 每日 | < 0.85 |
| MAE/RMSE | 回归模型误差 | 平均绝对误差/均方根误差 | 实时 | > 阈值 |

### 2.2 性能指标
| 指标 | 描述 | 计算公式 | 监控频率 | 告警阈值 |
|------|------|----------|----------|----------|
| 推理延迟 | 单次推理耗时 | 处理时间 | 实时 | P95 > 500ms |
| 吞吐量 | 每秒处理请求数 | QPS | 实时 | < 50 QPS |
| 错误率 | 失败请求比例 | 失败请求数/总请求数 | 实时 | > 5% |
| 可用性 | 服务可用时间比例 | 正常运行时间/总时间 | 实时 | < 99% |

### 2.3 资源指标
| 指标 | 描述 | 监控频率 | 告警阈值 |
|------|------|----------|----------|
| CPU使用率 | 模型服务CPU占用 | 实时 | > 80% |
| 内存使用率 | 模型服务内存占用 | 实时 | > 85% |
| GPU使用率 | GPU显存和计算使用 | 实时 | > 90% |
| 磁盘IO | 模型加载和存储IO | 实时 | > 100MB/s |
| 网络IO | 模型数据传输 | 实时 | > 1Gbps |

### 2.4 业务指标
| 指标 | 描述 | 计算公式 | 监控频率 |
|------|------|----------|----------|
| 用户满意度 | 用户反馈评分 | 正面反馈数/总反馈数 | 每日 |
| 转化率 | 推荐点击转化率 | 点击次数/展示次数 | 实时 |
| 留存率 | 用户持续使用率 | 活跃用户数/总用户数 | 每周 |
| 业务价值 | 模型产生的业务价值 | 具体业务指标 | 每月 |

## 3. 模型漂移检测机制

### 3.1 数据漂移检测
```python
# I:\AI-Ready\ai\scripts\drift_detection.py
import numpy as np
from scipy import stats
from sklearn.metrics import pairwise_distances
import warnings
warnings.filterwarnings('ignore')

class DataDriftDetector:
    def __init__(self, reference_data, window_size=1000):
        self.reference_data = reference_data
        self.window_size = window_size
        self.drift_history = []
    
    def detect_ks_test(self, current_data, feature_name, alpha=0.05):
        """KS检验检测数据分布变化"""
        ref_feature = self.reference_data[feature_name]
        cur_feature = current_data[feature_name]
        
        statistic, p_value = stats.ks_2samp(ref_feature, cur_feature)
        
        return {
            "feature": feature_name,
            "statistic": statistic,
            "p_value": p_value,
            "is_drift": p_value < alpha,
            "test": "KS_test"
        }
    
    def detect_psi(self, reference_data, current_data, feature_name, bins=10):
        """PSI（群体稳定性指标）检测"""
        # 计算分箱
        ref_min = reference_data.min()
        ref_max = reference_data.max()
        bin_edges = np.linspace(ref_min, ref_max, bins + 1)
        
        # 计算分布
        ref_dist, _ = np.histogram(reference_data, bins=bin_edges)
        cur_dist, _ = np.histogram(current_data, bins=bin_edges)
        
        # 添加平滑
        ref_dist = ref_dist + 0.0001
        cur_dist = cur_dist + 0.0001
        
        # 计算PSI
        ref_prop = ref_dist / ref_dist.sum()
        cur_prop = cur_dist / cur_dist.sum()
        
        psi = np.sum((cur_prop - ref_prop) * np.log(cur_prop / ref_prop))
        
        # PSI阈值
        psi_thresholds = {
            "no_drift": 0.1,
            "moderate_drift": 0.25,
            "high_drift": 0.5
        }
        
        drift_level = "no_drift"
        if psi > psi_thresholds["high_drift"]:
            drift_level = "high_drift"
        elif psi > psi_thresholds["moderate_drift"]:
            drift_level = "moderate_drift"
        
        return {
            "feature": feature_name,
            "psi": float(psi),
            "drift_level": drift_level,
            "is_drift": psi > psi_thresholds["moderate_drift"]
        }
    
    def detect_mmd(self, reference_data, current_data, feature_name):
        """最大均值差异检测"""
        # 使用高斯核计算MMD
        ref_data = reference_data.reshape(-1, 1)
        cur_data = current_data.reshape(-1, 1)
        
        # 计算核矩阵
        gamma = 1.0 / (ref_data.var() + 1e-8)
        
        # 计算MMD统计量
        n_ref = len(ref_data)
        n_cur = len(cur_data)
        
        # 核函数
        def rbf_kernel(x, y, gamma):
            return np.exp(-gamma * np.sum((x - y)**2))
        
        # 计算MMD（简化版本）
        mmd = 0
        for i in range(min(100, n_ref)):
            for j in range(min(100, n_cur)):
                mmd += rbf_kernel(ref_data[i], cur_data[j], gamma)
        
        mmd = mmd / (100 * 100) if n_ref >= 100 and n_cur >= 100 else 0
        
        return {
            "feature": feature_name,
            "mmd": float(mmd),
            "is_drift": mmd > 0.5  # 阈值需要根据实际情况调整
        }
    
    def detect_all(self, current_data, features=None):
        """执行所有漂移检测"""
        if features is None:
            features = self.reference_data.columns
        
        results = []
        for feature in features:
            # KS检验
            ks_result = self.detect_ks_test(current_data, feature)
            results.append(ks_result)
            
            # PSI检测
            psi_result = self.detect_psi(
                self.reference_data[feature].values,
                current_data[feature].values,
                feature
            )
            results.append(psi_result)
            
            # MMD检测
            mmd_result = self.detect_mmd(
                self.reference_data[feature].values,
                current_data[feature].values,
                feature
            )
            results.append(mmd_result)
        
        return results
```

### 3.2 概念漂移检测
```python
# I:\AI-Ready\ai\scripts\concept_drift_detection.py
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from collections import deque
import warnings
warnings.filterwarnings('ignore')

class ConceptDriftDetector:
    def __init__(self, window_size=100, drift_threshold=0.15):
        self.window_size = window_size
        self.drift_threshold = drift_threshold
        self.window_data = deque(maxlen=window_size)
        self.window_labels = deque(maxlen=window_size)
        self.drift_alerts = []
    
    def add_data(self, features, true_label, predicted_label):
        """添加新数据"""
        self.window_data.append(features)
        self.window_labels.append(1 if true_label == predicted_label else 0)
    
    def detect_error_rate(self):
        """基于错误率的漂移检测"""
        if len(self.window_labels) < self.window_size:
            return None
        
        # 计算错误率
        error_rate = 1 - np.mean(self.window_labels)
        
        # 简单阈值检测
        if error_rate > self.drift_threshold:
            return {
                "detector": "error_rate",
                "error_rate": error_rate,
                "threshold": self.drift_threshold,
                "is_drift": True,
                "window_size": len(self.window_labels)
            }
        
        return {
            "detector": "error_rate",
            "error_rate": error_rate,
            "threshold": self.drift_threshold,
            "is_drift": False,
            "window_size": len(self.window_labels)
        }
    
    def detect_adaptive_window(self):
        """自适应窗口漂移检测"""
        if len(self.window_data) < 100:
            return None
        
        # 将窗口分为两个子窗口
        split_point = len(self.window_data) // 2
        window1 = list(self.window_data)[:split_point]
        window2 = list(self.window_data)[split_point:]
        
        labels1 = list(self.window_labels)[:split_point]
        labels2 = list(self.window_labels)[split_point:]
        
        # 训练两个分类器
        clf1 = RandomForestClassifier(n_estimators=10, random_state=42)
        clf2 = RandomForestClassifier(n_estimators=10, random_state=42)
        
        # 训练并在对方数据上测试
        clf1.fit(window1, labels1)
        clf2.fit(window2, labels2)
        
        acc1_on2 = accuracy_score(labels2, clf1.predict(window2))
        acc2_on1 = accuracy_score(labels1, clf2.predict(window1))
        
        # 计算漂移分数
        drift_score = 1 - (acc1_on2 + acc2_on1) / 2
        
        return {
            "detector": "adaptive_window",
            "drift_score": drift_score,
            "acc1_on2": acc1_on2,
            "acc2_on1": acc2_on1,
            "is_drift": drift_score > 0.2,
            "window_size": len(self.window_data)
        }
    
    def detect_ddm(self, warning_level=2.0, drift_level=3.0):
        """DDM（Drift Detection Method）检测"""
        if len(self.window_labels) < 30:
            return None
        
        # 计算错误率
        n = len(self.window_labels)
        p = np.mean(self.window_labels)  # 正确率
        error_rate = 1 - p
        
        # 计算标准差
        std = np.sqrt(p * (1 - p) / n)
        
        # 计算控制界限
        warning_bound = error_rate + warning_level * std
        drift_bound = error_rate + drift_level * std
        
        # 当前错误率
        current_errors = list(self.window_labels)[-min(30, n):]
        current_error_rate = 1 - np.mean(current_errors)
        
        return {
            "detector": "DDM",
            "current_error_rate": current_error_rate,
            "overall_error_rate": error_rate,
            "warning_bound": warning_bound,
            "drift_bound": drift_bound,
            "is_warning": current_error_rate > warning_bound,
            "is_drift": current_error_rate > drift_bound
        }
    
    def detect_all(self, features=None, true_label=None, predicted_label=None):
        """执行所有概念漂移检测"""
        if features is not None and true_label is not None and predicted_label is not None:
            self.add_data(features, true_label, predicted_label)
        
        results = []
        
        # 错误率检测
        error_result = self.detect_error_rate()
        if error_result:
            results.append(error_result)
        
        # 自适应窗口检测
        adaptive_result = self.detect_adaptive_window()
        if adaptive_result:
            results.append(adaptive_result)
        
        # DDM检测
        ddm_result = self.detect_ddm()
        if ddm_result:
            results.append(ddm_result)
        
        return results
```

## 4. 模型性能退化告警规则

### 4.1 告警规则设计
| 告警级别 | 触发条件 | 响应时间 | 处理措施 |
|----------|----------|----------|----------|
| 信息 | 单指标轻微波动 | 24小时内 | 记录日志，持续观察 |
| 警告 | 多指标异常或单指标中度异常 | 4小时内 | 通知负责人，启动调查 |
| 严重 | 关键指标严重异常或服务中断 | 1小时内 | 紧急通知，启动应急预案 |
| 紧急 | 系统完全不可用或数据丢失 | 15分钟内 | 全员通知，立即修复 |

### 4.2 告警规则实现
```python
# I:\AI-Ready\ai\scripts\alert_rules.py
from datetime import datetime, timedelta
from enum import Enum
import json

class AlertLevel(Enum):
    INFO = "info"
    WARNING = "warning"
    CRITICAL = "critical"
    EMERGENCY = "emergency"

class AlertRule:
    def __init__(self, rule_id, name, condition, level, cooldown_minutes=30):
        self.rule_id = rule_id
        self.name = name
        self.condition = condition  # 函数，返回True触发告警
        self.level = level
        self.cooldown_minutes = cooldown_minutes
        self.last_triggered = None
    
    def should_trigger(self, metrics):
        """检查是否应该触发告警"""
        # 检查冷却时间
        if self.last_triggered:
            cooldown_end = self.last_triggered + timedelta(minutes=self.cooldown_minutes)
            if datetime.now() < cooldown_end:
                return False
        
        # 检查条件
        try:
            if self.condition(metrics):
                self.last_triggered = datetime.now()
                return True
        except Exception as e:
            print(f"告警规则检查失败: {e}")
        
        return False

class AlertManager:
    def __init__(self):
        self.rules = []
        self.alerts_history = []
        self.setup_default_rules()
    
    def setup_default_rules(self):
        """设置默认告警规则"""
        
        # 规则1: 准确率下降
        def accuracy_rule(metrics):
            if 'accuracy' in metrics:
                return metrics['accuracy'] < 0.85
            return False
        
        self.rules.append(AlertRule(
            rule_id="accuracy_low",
            name="模型准确率过低",
            condition=accuracy_rule,
            level=AlertLevel.CRITICAL
        ))
        
        # 规则2: 推理延迟过高
        def latency_rule(metrics):
            if 'p95_latency' in metrics:
                return metrics['p95_latency'] > 500  # 毫秒
            return False
        
        self.rules.append(AlertRule(
            rule_id="latency_high",
            name="推理延迟过高",
            condition=latency_rule,
            level=AlertLevel.WARNING
        ))
        
        # 规则3: 错误率过高
        def error_rate_rule(metrics):
            if 'error_rate' in metrics:
                return metrics['error_rate'] > 0.05
            return False
        
        self.rules.append(AlertRule(
            rule_id="error_rate_high",
            name="错误率过高",
            condition=error_rate_rule,
            level=AlertLevel.CRITICAL
        ))
        
        # 规则4: 数据漂移检测
        def data_drift_rule(metrics):
            if 'data_drift_score' in metrics:
                return metrics['data_drift_score'] > 0.3
            return False
        
        self.rules.append(AlertRule(
            rule_id="data_drift",
            name="数据分布漂移",
            condition=data_drift_rule,
            level=AlertLevel.WARNING
        ))
        
        # 规则5: 概念漂移检测
        def concept_drift_rule(metrics):
            if 'concept_drift_score' in metrics:
                return metrics['concept_drift_score'] > 0.25
            return False
        
        self.rules.append(AlertRule(
            rule_id="concept_drift",
            name="概念漂移检测",
            condition=concept_drift_rule,
            level=AlertLevel.CRITICAL
        ))
        
        # 规则6: 资源使用过高
        def resource_rule(metrics):
            if 'cpu_usage' in metrics and 'memory_usage' in metrics:
                return metrics['cpu_usage'] > 0.8 or metrics['memory_usage'] > 0.85
            return False
        
        self.rules.append(AlertRule(
            rule_id="resource_high",
            name="资源使用率过高",
            condition=resource_rule,
            level=AlertLevel.WARNING
        ))
        
        # 规则7: 服务不可用
        def availability_rule(metrics):
            if 'availability' in metrics:
                return metrics['availability'] < 0.99
            return False
        
        self.rules.append(AlertRule(
            rule_id="availability_low",
            name="服务可用性低",
            condition=availability_rule,
            level=AlertLevel.EMERGENCY
        ))
    
    def check_metrics(self, metrics):
        """检查指标并触发告警"""
        triggered_alerts = []
        
        for rule in self.rules:
            if rule.should_trigger(metrics):
                alert = {
                    "rule_id": rule.rule_id,
                    "name": rule.name,
                    "level": rule.level.value,
                    "timestamp": datetime.now().isoformat(),
                    "metrics": metrics
                }
                
                triggered_alerts.append(alert)
                self.alerts_history.append(alert)
        
        return triggered_alerts
    
    def get_recent_alerts(self, hours=24):
        """获取最近N小时的告警"""
        cutoff_time = datetime.now() - timedelta(hours=hours)
        
        recent_alerts = []
        for alert in reversed(self.alerts_history):
            alert_time = datetime.fromisoformat(alert['timestamp'])
            if alert_time >= cutoff_time:
                recent_alerts.append(alert)
            else:
                break
        
        return recent_alerts
    
    def save_alerts(self, filepath):
        """保存告警历史"""
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(self.alerts_history, f, indent=2, default=str)
    
    def load_alerts(self, filepath):
        """加载告警历史"""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                self.alerts_history = json.load(f)
        except FileNotFoundError:
            self.alerts_history = []
```

## 5. 评估系统实现

### 5.1 模型准确性评估模块
```python
# I:\AI-Ready\ai\scripts\accuracy_evaluation.py
import numpy as np
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    roc_auc_score, mean_absolute_error, mean_squared_error,
    r2_score, confusion_matrix, classification_report
)
from sklearn.model_selection import cross_val_score
import pandas as pd
from datetime import datetime

class AccuracyEvaluator:
    def __init__(self, model_name, model_type="classification"):
        self.model_name = model_name
        self.model_type = model_type
        self.evaluation_history = []
    
    def evaluate_classification(self, y_true, y_pred, y_prob=None, labels=None):
        """评估分类模型"""
        results = {
            "model_name": self.model_name,
            "model_type": self.model_type,
            "evaluation_time": datetime.now().isoformat(),
            "metrics": {}
        }
        
        # 基础指标
        results["metrics"]["accuracy"] = accuracy_score(y_true, y_pred)
        results["metrics"]["precision"] = precision_score(y_true, y_pred, average='weighted', zero_division=0)
        results["metrics"]["recall"] = recall_score(y_true, y_pred, average='weighted', zero_division=0)
        results["metrics"]["f1_score"] = f1_score(y_true, y_pred, average='weighted', zero_division=0)
        
        # 多分类指标
        if len(np.unique(y_true)) > 2:
            results["metrics"]["macro_precision"] = precision_score(y_true, y_pred, average='macro', zero_division=0)
            results["metrics"]["macro_recall"] = recall_score(y_true, y_pred, average='macro', zero_division=0)
            results["metrics"]["macro_f1"] = f1_score(y_true, y_pred, average='macro', zero_division=0)
        
        # ROC-AUC（如果有概率预测）
        if y_prob is not None:
            try:
                if len(np.unique(y_true)) == 2:
                    results["metrics"]["roc_auc"] = roc_auc_score(y_true, y_prob[:, 1])
                else:
                    results["metrics"]["roc_auc_ovr"] = roc_auc_score(y_true, y_prob, multi_class='ovr', average='weighted')
            except Exception as e:
                results["metrics"]["roc_auc_error"] = str(e)
        
        # 混淆矩阵
        cm = confusion_matrix(y_true, y_pred)
        results["metrics"]["confusion_matrix"] = cm.tolist()
        
        # 分类报告
        if labels is not None:
            report = classification_report(y_true, y_pred, target_names=labels, output_dict=True, zero_division=0)
            results["metrics"]["classification_report"] = report
        
        self.evaluation_history.append(results)
        return results
    
    def evaluate_regression(self, y_true, y_pred):
        """评估回归模型"""
        results = {
            "model_name": self.model_name,
            "model_type": "regression",
            "evaluation_time": datetime.now().isoformat(),
            "metrics": {}
        }
        
        # 回归指标
        results["metrics"]["mae"] = mean_absolute_error(y_true, y_pred)
        results["metrics"]["mse"] = mean_squared_error(y_true, y_pred)
        results["metrics"]["rmse"] = np.sqrt(mean_squared_error(y_true, y_pred))
        results["metrics"]["r2"] = r2_score(y_true, y_pred)
        
        # 误差分析
        errors = y_pred - y_true
        results["metrics"]["mean_error"] = np.mean(errors)
        results["metrics"]["std_error"] = np.std(errors)
        results["metrics"]["max_absolute_error"] = np.max(np.abs(errors))
        
        # 百分比误差
        percentage_errors = 100 * np.abs(errors) / (np.abs(y_true) + 1e-8)
        results["metrics"]["mean_absolute_percentage_error"] = np.mean(percentage_errors)
        
        self.evaluation_history.append(results)
        return results
    
    def cross_validation_evaluation(self, X, y, model, cv=5):
        """交叉验证评估"""
        cv_results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "cross_validation": {}
        }
        
        if self.model_type == "classification":
            # 分类交叉验证
            scoring_metrics = ['accuracy', 'precision_weighted', 'recall_weighted', 'f1_weighted']
        else:
            # 回归交叉验证
            scoring_metrics = ['neg_mean_absolute_error', 'neg_mean_squared_error', 'r2']
        
        cv_scores = {}
        for metric in scoring_metrics:
            scores = cross_val_score(model, X, y, cv=cv, scoring=metric)
            cv_scores[metric] = {
                "mean": np.mean(scores),
                "std": np.std(scores),
                "scores": scores.tolist()
            }
        
        cv_results["cross_validation"] = cv_scores
        self.evaluation_history.append(cv_results)
        return cv_results
    
    def get_accuracy_trend(self, days=30):
        """获取准确率趋势"""
        if not self.evaluation_history:
            return []
        
        # 筛选最近N天的评估结果
        cutoff_date = datetime.now() - timedelta(days=days)
        
        trend_data = []
        for eval_result in self.evaluation_history:
            eval_time = datetime.fromisoformat(eval_result["evaluation_time"])
            if eval_time >= cutoff_date and "metrics" in eval_result:
                if "accuracy" in eval_result["metrics"]:
                    trend_data.append({
                        "date": eval_time.date().isoformat(),
                        "accuracy": eval_result["metrics"]["accuracy"]
                    })
        
        return trend_data
    
    def save_evaluation_results(self, filepath):
        """保存评估结果"""
        df = pd.DataFrame(self.evaluation_history)
        df.to_json(filepath, orient='records', indent=2)
```

### 5.2 模型稳定性评估模块
```python
# I:\AI-Ready\ai\scripts\stability_evaluation.py
import numpy as np
from scipy import stats
import pandas as pd
from datetime import datetime, timedelta

class StabilityEvaluator:
    def __init__(self, model_name):
        self.model_name = model_name
        self.stability_metrics = []
        self.performance_history = []
    
    def add_performance_record(self, timestamp, metrics):
        """添加性能记录"""
        record = {
            "timestamp": timestamp,
            "metrics": metrics
        }
        self.performance_history.append(record)
    
    def evaluate_temporal_stability(self, metric_name, window_days=7):
        """评估时间稳定性"""
        if len(self.performance_history) < 2:
            return None
        
        # 获取指定时间窗口的数据
        cutoff_time = datetime.now() - timedelta(days=window_days)
        recent_records = [
            r for r in self.performance_history 
            if datetime.fromisoformat(r["timestamp"]) >= cutoff_time
        ]
        
        if len(recent_records) < 2:
            return None
        
        # 提取指标值
        metric_values = []
        timestamps = []
        for record in recent_records:
            if metric_name in record["metrics"]:
                metric_values.append(record["metrics"][metric_name])
                timestamps.append(datetime.fromisoformat(record["timestamp"]))
        
        if len(metric_values) < 2:
            return None
        
        # 计算稳定性指标
        values_array = np.array(metric_values)
        
        # 1. 变异系数（CV）
        mean_value = np.mean(values_array)
        std_value = np.std(values_array)
        cv = std_value / mean_value if mean_value != 0 else 0
        
        # 2. 趋势分析（线性回归斜率）
        time_numeric = np.array([t.timestamp() for t in timestamps])
        slope, intercept, r_value, p_value, std_err = stats.linregress(time_numeric, values_array)
        
        # 3. 自相关
        if len(values_array) > 10:
            autocorr = np.corrcoef(values_array[:-1], values_array[1:])[0, 1]
        else:
            autocorr = 0
        
        # 4. 异常点检测
        z_scores = np.abs(stats.zscore(values_array))
        outlier_count = np.sum(z_scores > 3)
        outlier_percentage = outlier_count / len(values_array) * 100
        
        results = {
            "model_name": self.model_name,
            "metric_name": metric_name,
            "evaluation_time": datetime.now().isoformat(),
            "window_days": window_days,
            "stability_metrics": {
                "mean": float(mean_value),
                "std": float(std_value),
                "cv": float(cv),
                "trend_slope": float(slope),
                "trend_r_squared": float(r_value**2),
                "trend_p_value": float(p_value),
                "autocorrelation": float(autocorr),
                "outlier_count": int(outlier_count),
                "outlier_percentage": float(outlier_percentage)
            },
            "stability_assessment": self.assess_stability(cv, slope, outlier_percentage)
        }
        
        self.stability_metrics.append(results)
        return results
    
    def assess_stability(self, cv, slope, outlier_percentage):
        """评估稳定性等级"""
        assessment = {
            "overall_stability": "stable",
            "issues": []
        }
        
        # CV评估
        if cv > 0.3:
            assessment["issues"].append("高变异系数")
            assessment["overall_stability"] = "unstable"
        elif cv > 0.15:
            assessment["issues"].append("中等变异系数")
            if assessment["overall_stability"] == "stable":
                assessment["overall_stability"] = "warning"
        
        # 趋势评估
        if abs(slope) > 0.01:  # 每天变化超过1%
            assessment["issues"].append("明显趋势")
            if assessment["overall_stability"] == "stable":
                assessment["overall_stability"] = "warning"
        
        # 异常点评估
        if outlier_percentage > 10:
            assessment["issues"].append("高异常点比例")
            assessment["overall_stability"] = "unstable"
        elif outlier_percentage > 5:
            assessment["issues"].append("中等异常点比例")
            if assessment["overall_stability"] == "stable":
                assessment["overall_stability"] = "warning"
        
        return assessment
    
    def evaluate_prediction_stability(self, predictions_history):
        """评估预测稳定性"""
        if len(predictions_history) < 2:
            return None
        
        # 计算预测一致性
        consistency_matrix = []
        for i in range(len(predictions_history)):
            row = []
            for j in range(len(predictions_history)):
                if i == j:
                    row.append(1.0)
                else:
                    # 计算预测相似度
                    if isinstance(predictions_history[i], np.ndarray) and isinstance(predictions_history[j], np.ndarray):
                        similarity = np.mean(predictions_history[i] == predictions_history[j])
                    else:
                        similarity = np.mean(np.array(predictions_history[i]) == np.array(predictions_history[j]))
                    row.append(similarity)
            consistency_matrix.append(row)
        
        consistency_matrix = np.array(consistency_matrix)
        
        # 计算平均一致性
        mask = ~np.eye(len(consistency_matrix), dtype=bool)
        avg_consistency = np.mean(consistency_matrix[mask])
        std_consistency = np.std(consistency_matrix[mask])
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "prediction_stability": {
                "average_consistency": float(avg_consistency),
                "consistency_std": float(std_consistency),
                "min_consistency": float(np.min(consistency_matrix[mask])),
                "max_consistency": float(np.max(consistency_matrix[mask])),
                "consistency_matrix": consistency_matrix.tolist()
            }
        }
        
        return results
    
    def evaluate_robustness(self, test_cases):
        """评估模型鲁棒性"""
        if not test_cases:
            return None
        
        robustness_results = []
        for case in test_cases:
            case_result = {
                "case_id": case.get("id", "unknown"),
                "case_type": case.get("type", "unknown"),
                "metrics": {}
            }
            
            # 这里需要根据具体测试用例进行评估
            # 例如：噪声测试、对抗样本测试、边缘案例测试等
            
            robustness_results.append(case_result)
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "robustness_evaluation": robustness_results
        }
        
        return results
```

## 6. 系统集成与部署

### 6.1 监控系统集成
```yaml
# I:\AI-Ready\ai\config\monitoring-config.yaml
prometheus:
  scrape_interval: 15s
  evaluation_interval: 15s
  
  scrape_configs:
    - job_name: 'ai-model-metrics'
      static_configs:
        - targets: ['localhost:8000']
      metrics_path: '/metrics'
      params:
        format: ['prometheus']
    
    - job_name: 'ai-model-performance'
      static_configs:
        - targets: ['localhost:8001']
      metrics_path: '/performance-metrics'
    
    - job_name: 'ai-model-drift'
      static_configs:
        - targets: ['localhost:8002']
      metrics_path: '/drift-metrics'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

  alert_rules:
    - alert: ModelAccuracyLow
      expr: ai_model_accuracy < 0.85
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "模型准确率过低"
        description: "模型 {{ $labels.model_name }} 准确率已降至 {{ $value }}"
    
    - alert: ModelLatencyHigh
      expr: ai_model_p95_latency > 500
      for: 2m
      labels:
        severity: warning
      annotations:
        summary: "模型推理延迟过高"
        description: "模型 {{ $labels.model_name }} P95延迟为 {{ $value }}ms"
    
    - alert: DataDriftDetected
      expr: ai_data_drift_score > 0.3
      for: 10m
      labels:
        severity: warning
      annotations:
        summary: "检测到数据分布漂移"
        description: "模型 {{ $labels.model_name }} 数据漂移得分为 {{ $value }}"
```

### 6.2 Grafana仪表板配置
```json
# I:\AI-Ready\ai\config\grafana-dashboard.json
{
  "dashboard": {
    "title": "AI模型质量监控仪表板",
    "panels": [
      {
        "title": "模型准确率",
        "type":