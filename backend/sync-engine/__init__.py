"""
同步引擎 - 通用数据同步框架

设计原则:
1. **SourceAdapter** - 统一的数据源读取接口
2. **TargetAdapter** - 统一的目标写入接口
3. **SyncPipeline** - 同步编排器（全量/增量、分页、错误处理）
4. **DataMapper** - 数据映射转换器

用法:
```python
from sync_engine.core import SyncPipeline, DataMapper
from sync_engine.adapters.sources.ql361 import QL361SourceAdapter
from sync_engine.adapters.targets.ai_ready import AiReadyTargetAdapter

source = QL361SourceAdapter(config)
target = AiReadyTargetAdapter(db_url)
pipeline = SyncPipeline(source, target)
await pipeline.run_full_sync()
```
"""

__version__ = "0.1.0"
