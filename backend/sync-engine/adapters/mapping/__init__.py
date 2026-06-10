"""
数据映射模块

定义 ql361 数据源到 AI-Ready 目标表的字段映射规则。
"""
from .ql361_mapping import BILL_TYPE_TABLE_MAPPING, map_ql361_to_ai_ready, transform_value
