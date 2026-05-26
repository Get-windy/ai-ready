#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI模型评估脚本
用于评估AI模型的准确性、稳定性、公平性等指标
"""

import numpy as np
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    confusion_matrix, roc_auc_score, mean_absolute_error, mean_squared_error, r2_score
)
from sklearn.model_selection import cross_val_score
from datetime import datetime, timedelta
import json
import os

class AIEvaluator:
    def __init__(self, model_name="ai_model"):
        self.model_name = model_name
        self.evaluation_history = []
        self.eval_dir = "I:\\AI-Ready\\ai-monitoring\\evaluations"
        os.makedirs(self.eval_dir, exist_ok=True)
    
    def evaluate_classification(self, y_true, y_pred, y_prob=None, class_names=None):
        """评估分类模型"""
        print(f"\n=== 分类模型评估: {self.model_name} ===")
        
        # 基础指标
        accuracy = accuracy_score(y_true, y_pred)
        precision = precision_score(y_true, y_pred, average='weighted', zero_division=0)
        recall = recall_score(y_true, y_pred, average='weighted', zero_division=0)
        f1 = f1_score(y_true, y_pred, average='weighted', zero_division=0)
        
        print(f"准确率 (Accuracy): {accuracy:.4f}")
        print(f"精确率 (Precision): {precision:.4f}")
        print(f"召回率 (Recall): {recall:.4f}")
        print(f"F1分数 (F1-Score): {f1:.4f}")
        
        # 混淆矩阵
        cm = confusion_matrix(y_true, y_pred)
        print(f"\n混淆矩阵:\n{cm}")
        
        # ROC-AUC (如果有概率预测)
        if y_prob is not None and len(np.unique(y_true)) == 2:
            try:
                auc = roc_auc_score(y_true, y_prob[:, 1])
                print(f"ROC-AUC: {auc:.4f}")
            except:
                auc = None
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "evaluation_type": "classification",
            "metrics": {
                "accuracy": float(accuracy),
                "precision": float(precision),
                "recall": float(recall),
                "f1_score": float(f1),
                "confusion_matrix": cm.tolist()
            }
        }
        
        if auc:
            results["metrics"]["roc_auc"] = float(auc)
        
        self.evaluation_history.append(results)
        return results
    
    def evaluate_regression(self, y_true, y_pred):
        """评估回归模型"""
        print(f"\n=== 回归模型评估: {self.model_name} ===")
        
        mae = mean_absolute_error(y_true, y_pred)
        mse = mean_squared_error(y_true, y_pred)
        rmse = np.sqrt(mse)
        r2 = r2_score(y_true, y_pred)
        
        print(f"平均绝对误差 (MAE): {mae:.4f}")
        print(f"均方误差 (MSE): {mse:.4f}")
        print(f"均方根误差 (RMSE): {rmse:.4f}")
        print(f"R²分数 (R²): {r2:.4f}")
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "evaluation_type": "regression",
            "metrics": {
                "mae": float(mae),
                "mse": float(mse),
                "rmse": float(rmse),
                "r2": float(r2)
            }
        }
        
        self.evaluation_history.append(results)
        return results
    
    def evaluate_stability(self, y_true_list, y_pred_list):
        """评估模型稳定性"""
        print(f"\n=== 模型稳定性评估: {self.model_name} ===")
        
        # 计算多次评估的指标变化
        accuracies = []
        for i, (y_true, y_pred) in enumerate(zip(y_true_list, y_pred_list)):
            acc = accuracy_score(y_true, y_pred)
            accuracies.append(acc)
        
        mean_acc = np.mean(accuracies)
        std_acc = np.std(accuracies)
        min_acc = np.min(accuracies)
        max_acc = np.max(accuracies)
        
        print(f"平均准确率: {mean_acc:.4f}")
        print(f"准确率标准差: {std_acc:.4f}")
        print(f"最低准确率: {min_acc:.4f}")
        print(f"最高准确率: {max_acc:.4f}")
        
        stability_score = 1 - std_acc  # 越高越稳定
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "evaluation_type": "stability",
            "metrics": {
                "mean_accuracy": float(mean_acc),
                "std_accuracy": float(std_acc),
                "min_accuracy": float(min_acc),
                "max_accuracy": float(max_acc),
                "stability_score": float(stability_score)
            }
        }
        
        self.evaluation_history.append(results)
        return results
    
    def evaluate_fairness(self, y_true, y_pred, protected_attribute):
        """评估模型公平性（简化版）"""
        print(f"\n=== 模型公平性评估: {self.model_name} ===")
        
        unique_groups = np.unique(protected_attribute)
        group_metrics = {}
        
        for group in unique_groups:
            group_mask = protected_attribute == group
            y_true_group = y_true[group_mask]
            y_pred_group = y_pred[group_mask]
            
            if len(y_true_group) > 0:
                acc = accuracy_score(y_true_group, y_pred_group)
                group_metrics[str(group)] = {
                    "accuracy": float(acc),
                    "sample_size": int(len(y_true_group))
                }
        
        # 计算公平性指标（准确率差异）
        accuracies = [metrics["accuracy"] for metrics in group_metrics.values()]
        accuracy_gap = max(accuracies) - min(accuracies)
        
        print(f"各组准确率:")
        for group, metrics in group_metrics.items():
            print(f"  组 {group}: {metrics['accuracy']:.4f} (样本数: {metrics['sample_size']})")
        
        print(f"准确率差距: {accuracy_gap:.4f}")
        
        results = {
            "model_name": self.model_name,
            "evaluation_time": datetime.now().isoformat(),
            "evaluation_type": "fairness",
            "metrics": {
                "group_metrics": group_metrics,
                "accuracy_gap": float(accuracy_gap),
                "fairness_score": float(1 - accuracy_gap)
            }
        }
        
        self.evaluation_history.append(results)
        return results
    
    def get_trend_analysis(self, days=7):
        """获取评估趋势分析"""
        cutoff_date = datetime.now() - timedelta(days=days)
        
        recent_evaluations = []
        for eval_data in self.evaluation_history:
            eval_time = datetime.fromisoformat(eval_data["evaluation_time"])
            if eval_time >= cutoff_date:
                recent_evaluations.append(eval_data)
        
        if not recent_evaluations:
            return None
        
        # 分析趋势
        accuracies = []
        for eval_data in recent_evaluations:
            if "accuracy" in eval_data["metrics"]:
                accuracies.append(eval_data["metrics"]["accuracy"])
            elif "mean_accuracy" in eval_data["metrics"]:
                accuracies.append(eval_data["metrics"]["mean_accuracy"])
        
        if accuracies:
            trend_analysis = {
                "period_days": days,
                "evaluation_count": len(recent_evaluations),
                "average_accuracy": float(np.mean(accuracies)),
                "std_accuracy": float(np.std(accuracies)),
                "min_accuracy": float(np.min(accuracies)),
                "max_accuracy": float(np.max(accuracies)),
                "trend": self.calculate_trend(accuracies)
            }
            
            return trend_analysis
        
        return None
    
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
    
    def save_evaluations(self):
        """保存评估结果"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"evaluation_{self.model_name}_{timestamp}.json"
        filepath = os.path.join(self.eval_dir, filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump({
                "model_name": self.model_name,
                "evaluations": self.evaluation_history,
                "trend_analysis": self.get_trend_analysis()
            }, f, indent=2, ensure_ascii=False)
        
        print(f"\n评估结果已保存到: {filepath}")
        return filepath

def demo_evaluation():
    """演示评估功能"""
    print("=== AI模型评估演示 ===")
    
    evaluator = AIEvaluator("demo_model")
    
    # 生成示例分类数据
    np.random.seed(42)
    y_true = np.random.randint(0, 3, 100)
    y_pred = y_true.copy()
    errors = np.random.choice(100, 15, replace=False)
    y_pred[errors] = np.random.randint(0, 3, 15)
    
    # 评估分类模型
    evaluator.evaluate_classification(y_true, y_pred)
    
    # 评估稳定性
    print("\n=== 稳定性评估演示 ===")
    y_true_list = [y_true] * 5
    y_pred_list = [y_pred]
    
    for _ in range(4):
        y_pred_temp = y_true.copy()
        temp_errors = np.random.choice(100, np.random.randint(10, 20), replace=False)
        y_pred_temp[temp_errors] = np.random.randint(0, 3, len(temp_errors))
        y_pred_list.append(y_pred_temp)
    
    evaluator.evaluate_stability(y_true_list, y_pred_list)
    
    # 评估公平性
    print("\n=== 公平性评估演示 ===")
    protected_attr = np.random.choice(['A', 'B', 'C'], 100)
    evaluator.evaluate_fairness(y_true, y_pred, protected_attr)
    
    # 趋势分析
    trend = evaluator.get_trend_analysis(days=1)
    if trend:
        print(f"\n=== 趋势分析 ===")
        print(f"评估次数: {trend['evaluation_count']}")
        print(f"平均准确率: {trend['average_accuracy']:.4f}")
        print(f"准确率标准差: {trend['std_accuracy']:.4f}")
        print(f"趋势: {trend['trend']}")
    
    # 保存结果
    evaluator.save_evaluations()
    
    print("\n=== 评估完成 ===")
    return evaluator

if __name__ == "__main__":
    demo_evaluation()