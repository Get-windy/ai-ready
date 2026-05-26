"""
批次数据清洗模块
提供批次数据的自动化清洗和标准化功能
"""

import pandas as pd
import numpy as np
from typing import Optional, Dict, Any, List, Tuple
import logging
from datetime import datetime
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class CleaningConfig:
    """数据清洗配置"""
    missing_value_strategy: str = "mean"  # mean, median, mode, zero, drop
    outlier_detection_method: str = "iqr"  # iqr, zscore, isolation_forest
    outlier_threshold: float = 1.5
    datetime_format: str = "%Y-%m-%d %H:%M:%S"
    numeric_columns: Optional[List[str]] = None
    categorical_columns: Optional[List[str]] = None
    datetime_columns: Optional[List[str]] = None
    

class BatchDataCleaner:
    """批次数据清洗器"""
    
    def __init__(self, config: Optional[CleaningConfig] = None):
        """
        初始化数据清洗器
        
        Args:
            config: 数据清洗配置
        """
        self.config = config or CleaningConfig()
        self._cleaning_stats: Dict[str, Any] = {}
        
    def clean_batch_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        清洗批次数据
        
        Args:
            df: 原始批次数据
            
        Returns:
            清洗后的数据
        """
        logger.info(f"开始清洗批次数据，数据形状: {df.shape}")
        self._cleaning_stats = {"original_shape": df.shape}
        
        # 1. 处理缺失值
        df_cleaned = self._handle_missing_values(df)
        
        # 2. 处理异常值
        df_cleaned = self._handle_outliers(df_cleaned)
        
        # 3. 数据类型转换
        df_cleaned = self._convert_data_types(df_cleaned)
        
        # 4. 重复数据处理
        df_cleaned = self._handle_duplicates(df_cleaned)
        
        # 5. 数据标准化
        df_cleaned = self._standardize_data(df_cleaned)
        
        self._cleaning_stats["cleaned_shape"] = df_cleaned.shape
        logger.info(f"数据清洗完成，清洗后形状: {df_cleaned.shape}")
        
        return df_cleaned
    
    def _handle_missing_values(self, df: pd.DataFrame) -> pd.DataFrame:
        """处理缺失值"""
        missing_counts = df.isnull().sum()
        total_rows = len(df)
        
        logger.info(f"缺失值统计: {missing_counts[missing_counts > 0].to_dict()}")
        self._cleaning_stats["missing_counts"] = missing_counts.to_dict()
        
        df_processed = df.copy()
        
        for column in df.columns:
            if df[column].isnull().any():
                missing_percentage = df[column].isnull().sum() / total_rows
                
                if missing_percentage > 0.5:  # 缺失超过50%，删除列
                    logger.warning(f"列 '{column}' 缺失值超过50%，删除该列")
                    df_processed = df_processed.drop(column, axis=1)
                    continue
                    
                if self.config.missing_value_strategy == "mean":
                    if pd.api.types.is_numeric_dtype(df[column]):
                        fill_value = df[column].mean()
                        df_processed[column] = df[column].fillna(fill_value)
                elif self.config.missing_value_strategy == "median":
                    if pd.api.types.is_numeric_dtype(df[column]):
                        fill_value = df[column].median()
                        df_processed[column] = df[column].fillna(fill_value)
                elif self.config.missing_value_strategy == "mode":
                    fill_value = df[column].mode()[0] if not df[column].mode().empty else None
                    if fill_value is not None:
                        df_processed[column] = df[column].fillna(fill_value)
                elif self.config.missing_value_strategy == "zero":
                    if pd.api.types.is_numeric_dtype(df[column]):
                        df_processed[column] = df[column].fillna(0)
                elif self.config.missing_value_strategy == "forward":
                    df_processed[column] = df[column].ffill()
                elif self.config.missing_value_strategy == "backward":
                    df_processed[column] = df[column].bfill()
                    
        return df_processed
    
    def _handle_outliers(self, df: pd.DataFrame) -> pd.DataFrame:
        """处理异常值"""
        if self.config.outlier_detection_method == "iqr":
            return self._handle_outliers_iqr(df)
        elif self.config.outlier_detection_method == "zscore":
            return self._handle_outliers_zscore(df)
        else:
            logger.warning(f"不支持的异常值检测方法: {self.config.outlier_detection_method}")
            return df
    
    def _handle_outliers_iqr(self, df: pd.DataFrame) -> pd.DataFrame:
        """使用IQR方法处理异常值"""
        df_processed = df.copy()
        outlier_stats = {}
        
        for column in df.select_dtypes(include=[np.number]).columns:
            Q1 = df[column].quantile(0.25)
            Q3 = df[column].quantile(0.75)
            IQR = Q3 - Q1
            lower_bound = Q1 - self.config.outlier_threshold * IQR
            upper_bound = Q3 + self.config.outlier_threshold * IQR
            
            outliers = df[(df[column] < lower_bound) | (df[column] > upper_bound)]
            outlier_count = len(outliers)
            
            if outlier_count > 0:
                outlier_percentage = outlier_count / len(df) * 100
                logger.info(f"列 '{column}' 发现 {outlier_count} 个异常值 ({outlier_percentage:.2f}%)")
                
                # 使用中位数替换异常值
                median_value = df[column].median()
                df_processed.loc[outliers.index, column] = median_value
                
                outlier_stats[column] = {
                    "outlier_count": outlier_count,
                    "outlier_percentage": outlier_percentage,
                    "lower_bound": lower_bound,
                    "upper_bound": upper_bound,
                    "replacement_value": median_value
                }
        
        self._cleaning_stats["outlier_stats"] = outlier_stats
        return df_processed
    
    def _handle_outliers_zscore(self, df: pd.DataFrame) -> pd.DataFrame:
        """使用Z-score方法处理异常值"""
        from scipy import stats
        
        df_processed = df.copy()
        outlier_stats = {}
        
        for column in df.select_dtypes(include=[np.number]).columns:
            z_scores = np.abs(stats.zscore(df[column].fillna(df[column].mean())))
            outliers = z_scores > self.config.outlier_threshold
            outlier_count = outliers.sum()
            
            if outlier_count > 0:
                outlier_percentage = outlier_count / len(df) * 100
                logger.info(f"列 '{column}' 发现 {outlier_count} 个异常值 ({outlier_percentage:.2f}%)")
                
                # 使用中位数替换异常值
                median_value = df[column].median()
                df_processed.loc[outliers, column] = median_value
                
                outlier_stats[column] = {
                    "outlier_count": int(outlier_count),
                    "outlier_percentage": outlier_percentage,
                    "threshold": self.config.outlier_threshold,
                    "replacement_value": median_value
                }
        
        self._cleaning_stats["outlier_stats"] = outlier_stats
        return df_processed
    
    def _convert_data_types(self, df: pd.DataFrame) -> pd.DataFrame:
        """转换数据类型"""
        df_processed = df.copy()
        
        # 转换日期时间列
        if self.config.datetime_columns:
            for column in self.config.datetime_columns:
                if column in df_processed.columns:
                    try:
                        df_processed[column] = pd.to_datetime(
                            df_processed[column], 
                            format=self.config.datetime_format,
                            errors='coerce'
                        )
                        logger.info(f"转换列 '{column}' 为日期时间类型")
                    except Exception as e:
                        logger.warning(f"转换列 '{column}' 失败: {str(e)}")
        
        # 转换数值列
        if self.config.numeric_columns:
            for column in self.config.numeric_columns:
                if column in df_processed.columns:
                    try:
                        df_processed[column] = pd.to_numeric(
                            df_processed[column],
                            errors='coerce'
                        )
                        logger.info(f"转换列 '{column}' 为数值类型")
                    except Exception as e:
                        logger.warning(f"转换列 '{column}' 失败: {str(e)}")
        
        return df_processed
    
    def _handle_duplicates(self, df: pd.DataFrame) -> pd.DataFrame:
        """处理重复数据"""
        duplicate_count = df.duplicated().sum()
        
        if duplicate_count > 0:
            logger.info(f"发现 {duplicate_count} 个重复行，删除重复行")
            df_processed = df.drop_duplicates()
            self._cleaning_stats["duplicate_removed"] = duplicate_count
        else:
            df_processed = df.copy()
            
        return df_processed
    
    def _standardize_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """数据标准化"""
        df_processed = df.copy()
        
        # 对数值列进行标准化
        numeric_columns = df_processed.select_dtypes(include=[np.number]).columns
        
        if len(numeric_columns) > 0:
            from sklearn.preprocessing import StandardScaler
            
            scaler = StandardScaler()
            df_processed[numeric_columns] = scaler.fit_transform(df_processed[numeric_columns])
            logger.info(f"标准化数值列: {list(numeric_columns)}")
            
            self._cleaning_stats["standardized_columns"] = list(numeric_columns)
            
        return df_processed
    
    def get_cleaning_report(self) -> Dict[str, Any]:
        """获取数据清洗报告"""
        return self._cleaning_stats.copy()