"""
QL361 数据源适配器

从来肯云商（ql361.com）同步业务单据数据。

用法:
```python
from sync_engine.adapters.sources.ql361 import QL361SourceAdapter

source = QL361SourceAdapter(
    username="手机号",
    password="密码",
    report_names={"601": "SaleOutBound"},  # 用户需自行获取
)
await source.authenticate()
```
"""

from .adapter import QL361SourceAdapter

__all__ = ["QL361SourceAdapter"]
