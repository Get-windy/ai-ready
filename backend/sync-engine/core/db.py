"""
数据库工具

共享的数据库引擎和会话管理。
用于读取租户同步配置和管理同步记录。
"""
from __future__ import annotations

from typing import AsyncGenerator

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from config import settings

_engine = None
_session_factory = None


def get_engine():
    """获取或创建异步数据库引擎"""
    global _engine
    if _engine is None:
        _engine = create_async_engine(
            settings.db_url,
            echo=False,
            pool_size=5,
            max_overflow=10,
        )
    return _engine


def get_session_factory():
    """获取或创建会话工厂"""
    global _session_factory
    if _session_factory is None:
        _session_factory = async_sessionmaker(
            get_engine(), class_=AsyncSession, expire_on_commit=False,
        )
    return _session_factory


async def get_session() -> AsyncGenerator[AsyncSession, None]:
    """获取数据库会话（上下文管理器用法）"""
    factory = get_session_factory()
    async with factory() as session:
        yield session


async def get_tenant_configs() -> list[dict]:
    """
    从数据库读取所有启用的租户同步配置

    Returns:
        [{
            "id": 1,
            "tenant_id": 1,
            "source_type": "ql361",
            "source_username": "xxx",
            "source_password": "xxx",
            "base_url": "https://www.ql361.com",
            "sync_mode": "incremental",
            "sync_cron": "*/30 * * * *",
            "bill_types": ["601", "604", "504", "801"],
            "heartbeat_interval": 300,
        }, ...]
    """
    async with get_session_factory()() as session:
        result = await session.execute(
            text("""
                SELECT
                    id, tenant_id, source_type,
                    source_username, source_password,
                    base_url, sync_mode, sync_cron,
                    heartbeat_interval, bill_types
                FROM sync_data_source
                WHERE status = 1 AND deleted = 0
                ORDER BY tenant_id
            """)
        )
        rows = result.fetchall()

    configs = []
    for row in rows:
        import json
        bill_types = []
        if row.bill_types:
            try:
                bill_types = json.loads(row.bill_types)
            except (json.JSONDecodeError, TypeError):
                bill_types = ["601", "604", "504", "801"]

        configs.append({
            "id": row.id,
            "tenant_id": row.tenant_id,
            "source_type": row.source_type,
            "source_username": row.source_username,
            "source_password": row.source_password,
            "base_url": row.base_url or "https://www.ql361.com",
            "sync_mode": row.sync_mode or "incremental",
            "sync_cron": row.sync_cron or "*/30 * * * *",
            "heartbeat_interval": row.heartbeat_interval or 300,
            "bill_types": bill_types,
        })

    return configs


async def close_engine() -> None:
    """关闭数据库引擎"""
    global _engine
    if _engine:
        await _engine.dispose()
        _engine = None
