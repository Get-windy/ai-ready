"""
批次管理AI智能分析 - 趋势预测模块
提供批次数据的趋势预测、置信度评估和模型优化功能
"""

from .arima_forecaster import ARIMAForecaster
from .lstm_forecaster import LSTMForecaster
from .ensemble_forecaster import EnsembleForecaster
from .forecast_evaluator import ForecastEvaluator

__all__ = [
    "ARIMAForecaster",
    "LSTMForecaster",
    "EnsembleForecaster",
    "ForecastEvaluator",
]

__version__ = "1.0.0"