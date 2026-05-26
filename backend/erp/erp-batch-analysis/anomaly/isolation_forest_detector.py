"""
孤立森林异常检测器
基于孤立森林算法实现批次数据异常检测
"""

import numpy as np
import pandas as pd
from typing import Optional, Dict, Any, List, Tuple, Union
import logging
from dataclasses import dataclass
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import joblib
import json
from datetime import datetime

logger = logging.getLogger(__name__)


@dataclass
class IsolationForestConfig:
    """孤立森林配置"""
    contamination: float = 0.1  # 异常值比例估计
    n_estimators: int = 100  # 基估计器数量
    max_samples: Union[str, int] = "auto"  # 每个基估计器的样本数
    max_features: float = 1.0  # 每个基估计器的特征数
    bootstrap: bool = False  # 是否使用bootstrap采样
    random_state: int = 42  # 随机种子
    n_jobs: int = -1  # 并行作业数
    

class IsolationForestDetector:
    """孤立森林异常检测器"""
    
    def __init__(self, config: Optional[IsolationForestConfig] = None):
        """
        初始化孤立森林异常检测器
        
        Args:
            config: 配置参数
        """
        self.config = config or IsolationForestConfig()
        self.model: Optional[IsolationForest] = None
        self.scaler: Optional[StandardScaler] = None
        self.feature_columns: Optional[List[str]] = None
        self._detection_stats: Dict[str, Any] = {}
        
    def fit(self, X: Union[pd.DataFrame, np.ndarray], 
            feature_columns: Optional[List[str]] = None) -> 'IsolationForestDetector':
        """
        训练异常检测模型
        
        Args:
            X: 训练数据
            feature_columns: 特征列名（如果X是DataFrame）
            
        Returns:
            训练好的检测器实例
        """
        logger.info("开始训练孤立森林异常检测模型")
        
        # 保存特征列名
        if isinstance(X, pd.DataFrame):
            self.feature_columns = feature_columns or X.columns.tolist()
            X_data = X.values
        else:
            X_data = X
            if feature_columns:
                self.feature_columns = feature_columns
            else:
                self.feature_columns = [f"feature_{i}" for i in range(X_data.shape[1])]
        
        # 数据标准化
        self.scaler = StandardScaler()
        X_scaled = self.scaler.fit_transform(X_data)
        
        # 训练孤立森林模型
        self.model = IsolationForest(
            contamination=self.config.contamination,
            n_estimators=self.config.n_estimators,
            max_samples=self.config.max_samples,
            max_features=self.config.max_features,
            bootstrap=self.config.bootstrap,
            random_state=self.config.random_state,
            n_jobs=self.config.n_jobs,
            verbose=0
        )
        
        self.model.fit(X_scaled)
        
        # 计算训练集上的异常分数
        train_anomaly_scores = self.model.decision_function(X_scaled)
        train_predictions = self.model.predict(X_scaled)
        
        # 统计训练结果
        n_anomalies = (train_predictions == -1).sum()
        n_samples = len(X_data)
        anomaly_ratio = n_anomalies / n_samples
        
        self._detection_stats.update({
            "trained_at": datetime.now().isoformat(),
            "n_samples": n_samples,
            "n_features": X_data.shape[1],
            "n_anomalies_detected": int(n_anomalies),
            "anomaly_ratio": float(anomaly_ratio),
            "contamination_setting": self.config.contamination,
            "model_params": {
                "n_estimators": self.config.n_estimators,
                "max_samples": self.config.max_samples,
                "max_features": self.config.max_features,
                "bootstrap": self.config.bootstrap
            }
        })
        
        logger.info(f"模型训练完成，检测到 {n_anomalies}/{n_samples} 个异常样本 (比例: {anomaly_ratio:.2%})")
        
        return self
    
    def predict(self, X: Union[pd.DataFrame, np.ndarray]) -> np.ndarray:
        """
        预测数据点的异常标签
        
        Args:
            X: 待检测数据
            
        Returns:
            异常标签数组：1表示正常，-1表示异常
        """
        if self.model is None or self.scaler is None:
            raise ValueError("模型尚未训练，请先调用fit方法")
            
        # 提取数据
        if isinstance(X, pd.DataFrame):
            X_data = X.values
        else:
            X_data = X
            
        # 数据标准化
        X_scaled = self.scaler.transform(X_data)
        
        # 预测异常标签
        predictions = self.model.predict(X_scaled)
        
        logger.debug(f"异常检测完成，预测了 {len(predictions)} 个样本")
        
        return predictions
    
    def decision_function(self, X: Union[pd.DataFrame, np.ndarray]) -> np.ndarray:
        """
        计算数据点的异常分数
        
        Args:
            X: 待检测数据
            
        Returns:
            异常分数数组（分数越低越可能是异常）
        """
        if self.model is None or self.scaler is None:
            raise ValueError("模型尚未训练，请先调用fit方法")
            
        # 提取数据
        if isinstance(X, pd.DataFrame):
            X_data = X.values
        else:
            X_data = X
            
        # 数据标准化
        X_scaled = self.scaler.transform(X_data)
        
        # 计算异常分数
        anomaly_scores = self.model.decision_function(X_scaled)
        
        # 归一化到[0, 1]范围（1表示最异常）
        min_score = anomaly_scores.min()
        max_score = anomaly_scores.max()
        if max_score > min_score:
            normalized_scores = (max_score - anomaly_scores) / (max_score - min_score)
        else:
            normalized_scores = np.zeros_like(anomaly_scores)
            
        return normalized_scores
    
    def detect_anomalies(self, X: Union[pd.DataFrame, np.ndarray], 
                        threshold: Optional[float] = None) -> Dict[str, Any]:
        """
        检测异常并返回详细结果
        
        Args:
            X: 待检测数据
            threshold: 异常分数阈值（如果为None，使用模型默认阈值）
            
        Returns:
            包含检测结果的字典
        """
        if isinstance(X, pd.DataFrame):
            X_data = X.values
            index = X.index
            feature_data = X
        else:
            X_data = X
            index = np.arange(len(X))
            feature_data = None
            
        # 获取异常分数和预测
        anomaly_scores = self.decision_function(X_data)
        predictions = self.predict(X_data)
        
        # 应用自定义阈值（可选）
        if threshold is not None:
            custom_predictions = np.where(anomaly_scores > threshold, -1, 1)
        else:
            custom_predictions = predictions
            
        # 统计结果
        n_samples = len(X_data)
        n_anomalies_model = (predictions == -1).sum()
        n_anomalies_custom = (custom_predictions == -1).sum() if threshold is not None else n_anomalies_model
        
        # 创建结果DataFrame
        results = pd.DataFrame({
            "is_anomaly_model": predictions == -1,
            "is_anomaly_custom": custom_predictions == -1,
            "anomaly_score": anomaly_scores,
            "prediction_confidence": np.abs(anomaly_scores - 0.5) * 2  # 置信度[0,1]
        }, index=index)
        
        # 如果有特征数据，添加特征贡献分析
        if feature_data is not None and self.feature_columns:
            feature_contributions = self._analyze_feature_contributions(X_data)
            for i, feature in enumerate(self.feature_columns):
                if i < len(feature_contributions):
                    results[f"{feature}_contribution"] = feature_contributions[i]
                    
        # 更新统计信息
        detection_stats = {
            "detected_at": datetime.now().isoformat(),
            "n_samples": int(n_samples),
            "n_anomalies_model": int(n_anomalies_model),
            "n_anomalies_custom": int(n_anomalies_custom),
            "anomaly_ratio_model": float(n_anomalies_model / n_samples),
            "anomaly_ratio_custom": float(n_anomalies_custom / n_samples) if threshold is not None else None,
            "threshold_used": threshold,
            "avg_anomaly_score": float(anomaly_scores.mean()),
            "std_anomaly_score": float(anomaly_scores.std()),
            "max_anomaly_score": float(anomaly_scores.max()),
            "min_anomaly_score": float(anomaly_scores.min())
        }
        
        self._detection_stats["last_detection"] = detection_stats
        
        logger.info(f"异常检测完成: {n_anomalies_model}/{n_samples} 个异常 (模型阈值), "
                   f"{n_anomalies_custom}/{n_samples} 个异常 (自定义阈值)")
        
        return {
            "results": results,
            "stats": detection_stats,
            "feature_columns": self.feature_columns,
            "model_contamination": self.config.contamination,
            "threshold_applied": threshold
        }
    
    def _analyze_feature_contributions(self, X: np.ndarray) -> np.ndarray:
        """
        分析特征对异常检测的贡献
        
        Args:
            X: 数据
            
        Returns:
            特征贡献度数组
        """
        if self.model is None:
            return np.zeros(X.shape[1])
            
        # 使用特征重要性估计（基于树的分裂）
        try:
            # 获取特征重要性
            importances = np.zeros(X.shape[1])
            
            for tree in self.model.estimators_:
                # 计算每个特征在树中的分裂次数
                tree_importances = np.zeros(X.shape[1])
                
                # 遍历树的每个节点
                for i in range(tree.tree_.node_count):
                    if tree.tree_.children_left[i] != tree.tree_.children_right[i]:  # 分裂节点
                        feature = tree.tree_.feature[i]
                        if feature >= 0 and feature < X.shape[1]:
                            tree_importances[feature] += 1
                            
                importances += tree_importances
                
            # 归一化重要性
            if importances.sum() > 0:
                importances = importances / importances.sum()
                
            return importances
            
        except Exception as e:
            logger.warning(f"特征贡献分析失败: {str(e)}")
            return np.zeros(X.shape[1])
    
    def evaluate_on_labeled_data(self, X: Union[pd.DataFrame, np.ndarray], 
                                y_true: np.ndarray) -> Dict[str, float]:
        """
        在有标签数据上评估模型性能
        
        Args:
            X: 特征数据
            y_true: 真实标签（1表示正常，-1表示异常）
            
        Returns:
            性能指标字典
        """
        from sklearn.metrics import (
            accuracy_score, precision_score, recall_score, f1_score,
            roc_auc_score, confusion_matrix
        )
        
        # 预测异常标签
        y_pred = self.predict(X)
        
        # 将标签转换为二进制（0=正常，1=异常）
        y_true_binary = (y_true == -1).astype(int)
        y_pred_binary = (y_pred == -1).astype(int)
        
        # 计算指标
        metrics = {
            "accuracy": accuracy_score(y_true_binary, y_pred_binary),
            "precision": precision_score(y_true_binary, y_pred_binary, zero_division=0),
            "recall": recall_score(y_true_binary, y_pred_binary, zero_division=0),
            "f1_score": f1_score(y_true_binary, y_pred_binary, zero_division=0),
            "n_true_anomalies": int(y_true_binary.sum()),
            "n_pred_anomalies": int(y_pred_binary.sum())
        }
        
        # 计算ROC AUC（需要异常分数）
        try:
            anomaly_scores = self.decision_function(X)
            metrics["roc_auc"] = roc_auc_score(y_true_binary, anomaly_scores)
        except Exception as e:
            logger.warning(f"无法计算ROC AUC: {str(e)}")
            metrics["roc_auc"] = None
            
        # 计算混淆矩阵
        cm = confusion_matrix(y_true_binary, y_pred_binary)
        if cm.shape == (2, 2):
            tn, fp, fn, tp = cm.ravel()
            metrics.update({
                "true_negatives": int(tn),
                "false_positives": int(fp),
                "false_negatives": int(fn),
                "true_positives": int(tp)
            })
            
        logger.info(f"模型评估完成: 准确率={metrics['accuracy']:.3f}, "
                   f"F1分数={metrics['f1_score']:.3f}, "
                   f"ROC AUC={metrics.get('roc_auc', 'N/A')}")
        
        return metrics
    
    def save_model(self, filepath: str) -> None:
        """
        保存模型到文件
        
        Args:
            filepath: 文件路径
        """
        if self.model is None:
            raise ValueError("模型尚未训练，无法保存")
            
        model_data = {
            "model": self.model,
            "scaler": self.scaler,
            "feature_columns": self.feature_columns,
            "config": self.config,
            "detection_stats": self._detection_stats,
            "saved_at": datetime.now().isoformat()
        }
        
        joblib.dump(model_data, filepath)
        logger.info(f"模型已保存到: {filepath}")
    
    @classmethod
    def load_model(cls, filepath: str) -> 'IsolationForestDetector':
        """
        从文件加载模型
        
        Args:
            filepath: 文件路径
            
        Returns:
            加载的检测器实例
        """
        model_data = joblib.load(filepath)
        
        detector = cls(config=model_data["config"])
        detector.model = model_data["model"]
        detector.scaler = model_data["scaler"]
        detector.feature_columns = model_data["feature_columns"]
        detector._detection_stats = model_data["detection_stats"]
        
        logger.info(f"模型已从 {filepath} 加载")
        return detector
    
    def get_detection_stats(self) -> Dict[str, Any]:
        """获取检测统计信息"""
        return self._detection_stats.copy()
    
    def get_feature_columns(self) -> Optional[List[str]]:
        """获取特征列名"""
        return self.feature_columns.copy() if self.feature_columns else None