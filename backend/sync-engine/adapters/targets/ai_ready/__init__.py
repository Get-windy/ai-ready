"""
AiReady 目标适配器

将同步数据写入 AI-Ready 系统的 PostgreSQL 数据库。

用法:
```python
from sync_engine.adapters.targets.ai_ready import AiReadyTargetAdapter

target = AiReadyTargetAdapter("postgresql+asyncpg://user:pass@localhost/devdb")
await target.init_storage()
```
"""

from .adapter import AiReadyTargetAdapter

__all__ = ["AiReadyTargetAdapter"]
