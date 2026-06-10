"""
核心同步框架 - 源系统无关的通用同步引擎

设计原则:
1. **SourceAdapter** - 统一的数据源读取接口（任何系统实现此接口即可接入）
2. **TargetAdapter** - 统一的目标写入接口（支持各类数据库/API）
3. **SyncPipeline** - 同步编排器（全量/增量、分页、错误处理）
4. **DataMapper** - 数据映射转换器

接入新系统只需两步:
   1. 实现 SourceAdapter（读取源数据）
   2. 实现 TargetAdapter（写入目标）
"""

from .source import SourceAdapter
from .target import TargetAdapter
from .pipeline import SyncPipeline, SyncResult
from .mapper import DataMapper, FieldMapping

__all__ = [
    "SourceAdapter",
    "TargetAdapter",
    "SyncPipeline",
    "SyncResult",
    "DataMapper",
    "FieldMapping",
]
