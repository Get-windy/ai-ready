"""
数据源适配器 - 抽象基类

所有数据源（ql361、其他 ERP、第三方 API）都实现此接口。
核心框架通过此接口统一读取数据，无需关心源系统细节。

实现一个新的数据源:
```python
class MyErpSource(SourceAdapter):
    @property
    def name(self) -> str: return "my_erp"

    async def authenticate(self) -> None: ...
    async def fetch_page(self, bill_type: str, page: int, page_size: int,
                         last_sync: datetime | None) -> dict: ...
```
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime
from typing import Any, Dict, List, Optional


class SourceAdapter(ABC):
    """数据源适配器抽象基类"""

    @property
    @abstractmethod
    def name(self) -> str:
        """数据源名称（唯一标识）"""
        ...

    @abstractmethod
    async def authenticate(self) -> None:
        """认证/登录到源系统"""
        ...

    @abstractmethod
    async def fetch_page(
        self,
        bill_type: str,
        page: int = 0,
        page_size: int = 100,
        last_sync: Optional[datetime] = None,
    ) -> Dict[str, Any]:
        """
        从源系统拉取一页数据

        Args:
            bill_type: 单据类型标识（由具体源系统定义）
            page: 页码
            page_size: 每页条数
            last_sync: 增量同步的截止时间

        Returns:
            包含数据的字典，格式:
            {
                "records": [...],      # 数据记录列表
                "total": 0,            # 总条数（可选）
                "has_more": False,     # 是否有下一页
            }
        """
        ...

    @abstractmethod
    def get_supported_bill_types(self) -> Dict[str, str]:
        """
        获取支持的单据类型

        Returns:
            {bill_type_id: bill_type_name} 字典
            例如: {"sale_order": "销售出库单", "purchase_order": "采购订单"}
        """
        ...

    async def get_bill_detail(
        self,
        bill_type: str,
        bill_id: str,
    ) -> Optional[Dict[str, Any]]:
        """
        获取单个单据详情（可选）

        Args:
            bill_type: 单据类型
            bill_id: 单据 ID
        """
        return None

    async def close(self) -> None:
        """关闭连接，释放资源"""
        pass
