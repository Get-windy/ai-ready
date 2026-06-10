"""
AiReady 目标数据库模型

定义 sync_ 前缀的数据表，与现有系统表隔离。
所有表使用 SQLAlchemy 2.0 声明式映射。

表结构说明:
- `sync_journal`: 同步执行日志
- `sync_records`: 通用数据存储表（JSONB 存储原始数据）
- 每张业务表通过 bill_type 区分不同单据类型

设计原则:
- 所有表名以 sync_ 开头，避免与现有系统表冲突
- 使用 JSONB 存储灵活的数据结构，兼容不同数据源
- 每条记录记录来源（source_name, source_id），支持 upsert
- 关联 sync_id 追踪数据来源批次
"""
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, Optional

from sqlalchemy import (
    BigInteger,
    Column,
    DateTime,
    Index,
    Integer,
    String,
    Text,
    func,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class SyncBase(DeclarativeBase):
    """声明式基类"""


class SyncJournal(SyncBase):
    """同步执行日志"""

    __tablename__ = "sync_journal"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    source_name: Mapped[str] = mapped_column(String(100), nullable=False, comment="数据源名称")
    bill_type: Mapped[str] = mapped_column(String(50), nullable=False, comment="单据类型代码")
    bill_type_name: Mapped[str] = mapped_column(String(200), nullable=False, comment="单据类型名称")
    sync_type: Mapped[str] = mapped_column(String(20), nullable=False, comment="同步类型: full/incremental")
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="running", comment="状态: running/success/failed/partial")
    total_records: Mapped[int] = mapped_column(Integer, nullable=False, default=0, comment="总记录数")
    success_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0, comment="成功数")
    failed_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0, comment="失败数")
    error_message: Mapped[Optional[str]] = mapped_column(Text, nullable=True, comment="错误消息")
    started_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="开始时间"
    )
    finished_at: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True), nullable=True, comment="完成时间"
    )

    __table_args__ = (
        Index("idx_journal_source_type", "source_name", "bill_type", "started_at"),
    )


class SyncRecord(SyncBase):
    """通用数据存储表"""

    __tablename__ = "sync_records"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    source_name: Mapped[str] = mapped_column(String(100), nullable=False, comment="数据源名称")
    source_id: Mapped[str] = mapped_column(String(200), nullable=False, comment="源系统记录 ID")
    bill_type: Mapped[str] = mapped_column(String(50), nullable=False, comment="单据类型代码")
    sync_id: Mapped[int] = mapped_column(BigInteger, nullable=False, comment="同步日志 ID")
    data: Mapped[Dict[str, Any]] = mapped_column(JSONB, nullable=False, comment="原始数据（JSON）")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    __table_args__ = (
        # 每个数据源的每条记录最多保留一条（用于 upsert）
        Index("idx_record_source_uid", "source_name", "source_id", unique=True),
        Index("idx_record_bill_type", "bill_type"),
        Index("idx_record_sync_id", "sync_id"),
    )


# ── 以下是可选的具体字段映射表 ──
# 如果需要对某些关键字段建索引或做 SQL 级查询，
# 可在此添加显式列映射的 ORM 模型。
# 当前版本为保持通用性，仅使用 JSONB + sync_records 存储。
