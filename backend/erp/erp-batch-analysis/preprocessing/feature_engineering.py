"""
批次数据特征工程模块
提供特征选择、特征构造、特征变换等功能
"""

import pandas as pd
import numpy as np
from typing import Optional, Dict, Any, List, Tuple, Union
import logging
from dataclasses import dataclass
from sklearn.feature_selection import (
    SelectKBest, f_regression, f_classif, mutual_info_regression, mutual_info_classif,
    RFE, SelectFromModel
)
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.preprocessing import PolynomialFeatures
from sklearn.decomposition import PCA
from scipy import stats

logger = logging.getLogger(__name__)


@dataclass
class FeatureEngineeringConfig:
    """特征工程配置"""
    target_column: Optional[str] = None
    problem_type: str = "regression"  # regression, classification
    feature_selection_method: str = "rf_importance"  # kbest, rfe, rf_importance
    num_features_to_select: int = 20
    polynomial_degree: int = 2
    pca_components: Optional[int] = None
    interaction_features: bool = True
    statistical_features: bool = True
    temporal_features: bool = True
    

class FeatureEngineer:
    """特征工程师"""
    
    def __init__(self, config: Optional[FeatureEngineeringConfig] = None):
        """
        初始化特征工程师
        
        Args:
            config: 特征工程配置
        """
        self.config = config or FeatureEngineeringConfig()
        self._feature_importance: Optional[pd.DataFrame] = None
        self._selected_features: List[str] = []
        self._engineered_features: List[str] = []
        self._pca_model: Optional[PCA] = None
        
    def engineer_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        执行特征工程
        
        Args:
            df: 输入数据
            
        Returns:
            包含新特征的数据框
        """
        logger.info(f"开始特征工程，原始特征数: {df.shape[1]}")
        
        df_engineered = df.copy()
        
        # 1. 基础特征构造
        df_engineered = self._create_basic_features(df_engineered)
        
        # 2. 统计特征
        if self.config.statistical_features:
            df_engineered = self._create_statistical_features(df_engineered)
            
        # 3. 时间特征（如果存在时间列）
        if self.config.temporal_features:
            df_engineered = self._create_temporal_features(df_engineered)
            
        # 4. 交互特征
        if self.config.interaction_features:
            df_engineered = self._create_interaction_features(df_engineered)
            
        # 5. 多项式特征
        if self.config.polynomial_degree > 1:
            df_engineered = self._create_polynomial_features(df_engineered)
            
        # 6. 特征选择
        if self.config.target_column and self.config.target_column in df_engineered.columns:
            df_engineered = self._select_features(df_engineered)
            
        # 7. PCA降维（可选）
        if self.config.pca_components:
            df_engineered = self._apply_pca(df_engineered)
            
        logger.info(f"特征工程完成，最终特征数: {df_engineered.shape[1]}")
        
        return df_engineered
    
    def _create_basic_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """创建基础特征"""
        df_processed = df.copy()
        
        # 对数值列创建对数变换
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        for column in numeric_columns:
            if df[column].min() > 0:  # 对数变换要求值大于0
                log_col_name = f"{column}_log"
                df_processed[log_col_name] = np.log1p(df[column])
                self._engineered_features.append(log_col_name)
                logger.debug(f"创建对数特征: {log_col_name}")
                
            # 创建平方特征
            square_col_name = f"{column}_square"
            df_processed[square_col_name] = df[column] ** 2
            self._engineered_features.append(square_col_name)
            logger.debug(f"创建平方特征: {square_col_name}")
            
            # 创建平方根特征
            if df[column].min() >= 0:  # 平方根要求非负
                sqrt_col_name = f"{column}_sqrt"
                df_processed[sqrt_col_name] = np.sqrt(df[column])
                self._engineered_features.append(sqrt_col_name)
                logger.debug(f"创建平方根特征: {sqrt_col_name}")
                
        return df_processed
    
    def _create_statistical_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """创建统计特征"""
        df_processed = df.copy()
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        
        if len(numeric_columns) == 0:
            return df_processed
            
        # 滑动窗口统计特征
        window_sizes = [5, 10, 20]
        
        for column in numeric_columns:
            for window in window_sizes:
                if len(df) >= window:
                    # 移动平均值
                    rolling_mean = df[column].rolling(window=window, min_periods=1).mean()
                    mean_col_name = f"{column}_rolling_mean_{window}"
                    df_processed[mean_col_name] = rolling_mean
                    self._engineered_features.append(mean_col_name)
                    
                    # 移动标准差
                    rolling_std = df[column].rolling(window=window, min_periods=1).std()
                    std_col_name = f"{column}_rolling_std_{window}"
                    df_processed[std_col_name] = rolling_std
                    self._engineered_features.append(std_col_name)
                    
                    # 移动最大值
                    rolling_max = df[column].rolling(window=window, min_periods=1).max()
                    max_col_name = f"{column}_rolling_max_{window}"
                    df_processed[max_col_name] = rolling_max
                    self._engineered_features.append(max_col_name)
                    
                    # 移动最小值
                    rolling_min = df[column].rolling(window=window, min_periods=1).min()
                    min_col_name = f"{column}_rolling_min_{window}"
                    df_processed[min_col_name] = rolling_min
                    self._engineered_features.append(min_col_name)
                    
        logger.debug(f"创建统计特征: {len(self._engineered_features)} 个新特征")
        return df_processed
    
    def _create_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """创建时间特征"""
        df_processed = df.copy()
        
        # 查找日期时间列
        datetime_columns = df.select_dtypes(include=['datetime64']).columns
        
        for column in datetime_columns:
            # 提取时间组件
            df_processed[f"{column}_year"] = df[column].dt.year
            df_processed[f"{column}_month"] = df[column].dt.month
            df_processed[f"{column}_day"] = df[column].dt.day
            df_processed[f"{column}_hour"] = df[column].dt.hour
            df_processed[f"{column}_minute"] = df[column].dt.minute
            df_processed[f"{column}_dayofweek"] = df[column].dt.dayofweek
            df_processed[f"{column}_quarter"] = df[column].dt.quarter
            df_processed[f"{column}_is_weekend"] = df[column].dt.dayofweek >= 5
            
            self._engineered_features.extend([
                f"{column}_year", f"{column}_month", f"{column}_day",
                f"{column}_hour", f"{column}_minute", f"{column}_dayofweek",
                f"{column}_quarter", f"{column}_is_weekend"
            ])
            
        logger.debug(f"创建时间特征: {len(datetime_columns) * 8} 个新特征")
        return df_processed
    
    def _create_interaction_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """创建交互特征"""
        df_processed = df.copy()
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        
        if len(numeric_columns) < 2:
            return df_processed
            
        # 创建重要的数值列交互特征
        top_columns = numeric_columns[:min(5, len(numeric_columns))]
        
        for i in range(len(top_columns)):
            for j in range(i+1, len(top_columns)):
                col1 = top_columns[i]
                col2 = top_columns[j]
                
                # 乘法交互
                interaction_col = f"{col1}_x_{col2}"
                df_processed[interaction_col] = df[col1] * df[col2]
                self._engineered_features.append(interaction_col)
                
                # 除法交互（避免除零）
                if df[col2].min() > 0:
                    ratio_col = f"{col1}_div_{col2}"
                    df_processed[ratio_col] = df[col1] / df[col2]
                    self._engineered_features.append(ratio_col)
                    
        logger.debug(f"创建交互特征: {len(self._engineered_features) - len(df_processed.columns) + len(df.columns)} 个新特征")
        return df_processed
    
    def _create_polynomial_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """创建多项式特征"""
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        
        if len(numeric_columns) == 0:
            return df
            
        # 选择最重要的数值列进行多项式变换
        top_numeric_columns = numeric_columns[:min(5, len(numeric_columns))]
        
        poly = PolynomialFeatures(
            degree=self.config.polynomial_degree,
            include_bias=False,
            interaction_only=False
        )
        
        numeric_data = df[top_numeric_columns].fillna(0)
        poly_features = poly.fit_transform(numeric_data)
        
        # 创建多项式特征列名
        feature_names = poly.get_feature_names_out(top_numeric_columns)
        
        # 将多项式特征添加到数据框
        df_poly = pd.DataFrame(
            poly_features, 
            columns=feature_names,
            index=df.index
        )
        
        # 合并原始数据和新特征
        df_processed = pd.concat([df, df_poly], axis=1)
        
        # 记录新特征
        self._engineered_features.extend(feature_names)
        
        logger.debug(f"创建多项式特征: {len(feature_names)} 个新特征")
        return df_processed
    
    def _select_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """特征选择"""
        if self.config.target_column not in df.columns:
            logger.warning(f"目标列 '{self.config.target_column}' 不存在，跳过特征选择")
            return df
            
        X = df.drop(columns=[self.config.target_column])
        y = df[self.config.target_column]
        
        numeric_columns = X.select_dtypes(include=[np.number]).columns
        X_numeric = X[numeric_columns].fillna(0)
        
        if self.config.feature_selection_method == "kbest":
            selector = self._select_kbest_features(X_numeric, y)
        elif self.config.feature_selection_method == "rfe":
            selector = self._select_rfe_features(X_numeric, y)
        elif self.config.feature_selection_method == "rf_importance":
            selector = self._select_rf_importance_features(X_numeric, y)
        else:
            logger.warning(f"不支持的特征选择方法: {self.config.feature_selection_method}")
            return df
            
        # 获取选择的特征
        selected_mask = selector.get_support()
        selected_features = numeric_columns[selected_mask].tolist()
        
        # 保留选择的特征和非数值特征
        other_columns = X.drop(columns=numeric_columns).columns.tolist()
        all_selected_features = selected_features + other_columns
        
        self._selected_features = all_selected_features
        self._feature_importance = pd.DataFrame({
            "feature": numeric_columns,
            "importance": selector.scores_ if hasattr(selector, 'scores_') else [0] * len(numeric_columns),
            "selected": selected_mask
        }).sort_values("importance", ascending=False)
        
        logger.info(f"特征选择完成，选择 {len(all_selected_features)} 个特征")
        
        # 返回包含目标列的选择特征数据框
        result_df = pd.concat([
            df[all_selected_features],
            df[[self.config.target_column]]
        ], axis=1)
        
        return result_df
    
    def _select_kbest_features(self, X: pd.DataFrame, y: pd.Series) -> SelectKBest:
        """使用SelectKBest进行特征选择"""
        if self.config.problem_type == "regression":
            scorer = f_regression
        else:
            scorer = f_classif
            
        selector = SelectKBest(
            score_func=scorer,
            k=min(self.config.num_features_to_select, X.shape[1])
        )
        selector.fit(X, y)
        return selector
    
    def _select_rfe_features(self, X: pd.DataFrame, y: pd.Series) -> RFE:
        """使用递归特征消除进行特征选择"""
        if self.config.problem_type == "regression":
            estimator = RandomForestRegressor(n_estimators=50, random_state=42)
        else:
            estimator = RandomForestClassifier(n_estimators=50, random_state=42)
            
        selector = RFE(
            estimator=estimator,
            n_features_to_select=min(self.config.num_features_to_select, X.shape[1]),
            step=1
        )
        selector.fit(X, y)
        return selector
    
    def _select_rf_importance_features(self, X: pd.DataFrame, y: pd.Series) -> SelectFromModel:
        """使用随机森林重要性进行特征选择"""
        if self.config.problem_type == "regression":
            estimator = RandomForestRegressor(n_estimators=100, random_state=42)
        else:
            estimator = RandomForestClassifier(n_estimators=100, random_state=42)
            
        estimator.fit(X, y)
        
        selector = SelectFromModel(
            estimator=estimator,
            threshold=f"{(100 - self.config.num_features_to_select * 5)}th",  # 自适应阈值
            max_features=self.config.num_features_to_select
        )
        selector.fit(X, y)
        return selector
    
    def _apply_pca(self, df: pd.DataFrame) -> pd.DataFrame:
        """应用PCA降维"""
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        
        if len(numeric_columns) == 0:
            return df
            
        X_numeric = df[numeric_columns].fillna(0)
        n_components = min(self.config.pca_components, len(numeric_columns))
        
        self._pca_model = PCA(n_components=n_components, random_state=42)
        pca_features = self._pca_model.fit_transform(X_numeric)
        
        # 创建PCA特征列名
        pca_column_names = [f"pca_component_{i+1}" for i in range(n_components)]
        df_pca = pd.DataFrame(
            pca_features,
            columns=pca_column_names,
            index=df.index
        )
        
        # 计算解释方差比
        explained_variance = self._pca_model.explained_variance_ratio_
        cumulative_variance = explained_variance.cumsum()
        
        logger.info(f"PCA降维完成，保留 {n_components} 个主成分，累计解释方差: {cumulative_variance[-1]:.3f}")
        
        # 保留PCA特征和其他非数值特征
        other_columns = df.drop(columns=numeric_columns).columns
        result_df = pd.concat([df[other_columns], df_pca], axis=1)
        
        self._engineered_features.extend(pca_column_names)
        
        return result_df
    
    def get_feature_importance(self) -> Optional[pd.DataFrame]:
        """获取特征重要性"""
        return self._feature_importance.copy() if self._feature_importance is not None else None
    
    def get_selected_features(self) -> List[str]:
        """获取选择的特征"""
        return self._selected_features.copy()
    
    def get_engineered_features(self) -> List[str]:
        """获取构造的特征"""
        return self._engineered_features.copy()
    
    def get_pca_model(self) -> Optional[PCA]:
        """获取PCA模型"""
        return self._pca_model