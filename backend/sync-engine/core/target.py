"""
目标适配器 - 抽象基类

负责将同步的数据写入目标系统（数据库、API 等）。
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Tuple


class TargetAdapter(ABC):
    """目标适配器抽象基类"""

    @property
    @abstractmethod
    def name(self) -> str:
        """目标系统名称"""
        ...

    @abstractmethod
    async def init_storage(self) -> None:
        """初始化存储（建表等）"""
        ...

    @abstractmethod
    async def upsert_records(
        self,
        bill_type: str,
        records: List[Dict[str, Any]],
        source_name: str,
        sync_id: int,
    ) -> Tuple[int, int, List[str]]:
        """
        批量写入/更新数据

        Args:
            bill_type: 单据类型
            records: 数据记录列表
            source_name: 数据源名称
            sync_id: 同步日志 ID

        Returns:
            (成功数, 失败数, 错误消息列表)
        """
        ...

    @abstractmethod
    async def get_last_sync_time(
        self,
        source_name: str,
        bill_type: str,
    ) -> Optional[str]:
        """
        获取指定数据源/单据类型的最后同步时间

        Returns:
            ISO 格式时间字符串，或 None
        """
        ...

    @abstractmethod
    async def create_sync_log(
        self,
        source_name: str,
        bill_type: str,
        bill_type_name: str,
        sync_type: str,
    ) -> int:
        """
        创建同步日志

        Returns:
            日志 ID
        """
        ...

    @abstractmethod
    async def complete_sync_log(
        self,
        log_id: int,
        status: str,
        total: int = 0,
        success: int = 0,
        failed: int = 0,
        error: str = "",
    ) -> None:
        """完成同步日志"""
        ...

    async def close(self) -> None:
        """关闭连接"""
        pass
