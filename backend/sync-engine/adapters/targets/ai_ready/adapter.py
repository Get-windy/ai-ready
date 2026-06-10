"""
AiReady 目标适配器

将同步的数据写入 AI-Ready 系统的 PostgreSQL 数据库。
使用 sync_ 前缀的表，与现有业务表完全隔离。
"""
from __future__ import annotations

import json
from datetime import date, datetime
from typing import Any, Dict, List, Optional, Tuple

from loguru import logger
from sqlalchemy import select, text, update
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from core.target import TargetAdapter

from .models import SyncBase, SyncJournal, SyncRecord


class AiReadyTargetAdapter(TargetAdapter):
    """
    AI-Ready 目标适配器

    将同步数据写入 AI-Ready 项目的 PostgreSQL 数据库。

    Args:
        db_url: PostgreSQL 异步连接串
        echo_sql: 是否打印 SQL 日志

    Examples:
        >>> target = AiReadyTargetAdapter("postgresql+asyncpg://user:pass@localhost/devdb")
        >>> await target.init_storage()
        >>> ok, fail, errors = await target.upsert_records("601", records, "ql361", 1)
    """

    def __init__(self, db_url: str, echo_sql: bool = False):
        self._engine = create_async_engine(db_url, echo=echo_sql, pool_size=5, max_overflow=10)
        self._session_factory = async_sessionmaker(
            self._engine, class_=AsyncSession, expire_on_commit=False,
        )

    @property
    def name(self) -> str:
        return "ai_ready"

    async def init_storage(self) -> None:
        """创建 sync_ 前缀的数据表"""
        async with self._engine.begin() as conn:
            await conn.run_sync(SyncBase.metadata.create_all)
        logger.info("数据表初始化完成")

    def _get_session(self) -> AsyncSession:
        return self._session_factory()

    async def upsert_records(
        self,
        bill_type: str,
        records: List[Dict[str, Any]],
        source_name: str,
        sync_id: int,
    ) -> Tuple[int, int, List[str]]:
        """
        批量写入/更新数据到 sync_records 表

        使用 PostgreSQL INSERT ... ON CONFLICT DO UPDATE 实现 upsert。
        以 (source_name, source_id) 为唯一键去重。

        Args:
            bill_type: 单据类型
            records: 数据记录列表（已映射）
            source_name: 数据源名称
            sync_id: 同步日志 ID

        Returns:
            (成功数, 失败数, 错误消息列表)
        """
        success = 0
        failed = 0
        error_messages: List[str] = []

        if not records:
            return 0, 0, []

        async with self._get_session() as session:
            for record in records:
                source_id = self._extract_source_id(record)
                if not source_id:
                    failed += 1
                    error_messages.append(f"记录缺少标识字段: {record}")
                    continue

                try:
                    # PostgreSQL upsert
                    stmt = text("""
                        INSERT INTO sync_records (source_name, source_id, bill_type, sync_id, data, created_at)
                        VALUES (:source_name, :source_id, :bill_type, :sync_id, :data, NOW())
                        ON CONFLICT (source_name, source_id)
                        DO UPDATE SET
                            data = EXCLUDED.data,
                            bill_type = EXCLUDED.bill_type,
                            sync_id = EXCLUDED.sync_id
                    """)
                    await session.execute(stmt, {
                        "source_name": source_name,
                        "source_id": source_id,
                        "bill_type": bill_type,
                        "sync_id": sync_id,
                        "data": json.dumps(self._to_json(record), ensure_ascii=False),
                    })
                    success += 1
                except Exception as e:
                    failed += 1
                    error_messages.append(f"写入失败 {source_id}: {e}")

            await session.commit()

        return success, failed, error_messages

    async def get_last_sync_time(
        self,
        source_name: str,
        bill_type: str,
    ) -> Optional[str]:
        """
        查询指定数据源/单据类型的最后成功同步时间

        Args:
            source_name: 数据源名称
            bill_type: 单据类型

        Returns:
            ISO 格式时间字符串，或 None（从未同步过）
        """
        async with self._get_session() as session:
            stmt = (
                select(SyncJournal.started_at)
                .where(
                    SyncJournal.source_name == source_name,
                    SyncJournal.bill_type == bill_type,
                    SyncJournal.status.in_(["success", "partial"]),
                )
                .order_by(SyncJournal.started_at.desc())
                .limit(1)
            )
            result = await session.execute(stmt)
            row = result.scalar_one_or_none()

            if row:
                if isinstance(row, datetime):
                    return row.isoformat()
                return str(row)
            return None

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
        async with self._get_session() as session:
            journal = SyncJournal(
                source_name=source_name,
                bill_type=bill_type,
                bill_type_name=bill_type_name,
                sync_type=sync_type,
                status="running",
            )
            session.add(journal)
            await session.commit()
            await session.refresh(journal)
            return journal.id  # type: ignore[return-value]

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
        async with self._get_session() as session:
            stmt = (
                update(SyncJournal)
                .where(SyncJournal.id == log_id)
                .values(
                    status=status,
                    total_records=total,
                    success_count=success,
                    failed_count=failed,
                    error_message=error or None,
                    finished_at=datetime.now(),
                )
            )
            await session.execute(stmt)
            await session.commit()

        logger.info(
            "同步日志完成 [{}]: {} (总={}, 成功={}, 失败={})",
            log_id, status, total, success, failed,
        )

    async def import_to_business_tables(
        self,
        bill_type: str,
        records: List[Dict[str, Any]],
    ) -> Tuple[int, int, List[str]]:
        """
        将 ql361 同步数据导入到 AI-Ready ERP 业务表 (erp_*)

        按 billcode 分组去重，使用 map_ql361_to_ai_ready() 进行字段映射，
        然后写入对应的 erp_* 业务表。

        Args:
            bill_type: 单据类型 (如 "601", "702")
            records:   ql361 原始记录列表

        Returns:
            (成功数, 失败数, 错误消息列表)
        """
        from adapters.mapping.ql361_mapping import (
            BILL_TYPE_TABLE_MAPPING,
            TABLE_FIELD_MAPPINGS,
            map_ql361_to_ai_ready,
        )

        tables = BILL_TYPE_TABLE_MAPPING.get(bill_type)
        if not tables:
            logger.debug("单据类型 {} 无业务表映射，跳过导入", bill_type)
            return 0, 0, []

        target_table = tables[0]

        # 按 billcode 分组（确保同一单据只写一条到业务表）
        groups: Dict[str, List[Dict[str, Any]]] = {}
        for record in records:
            bc = str(record.get("billcode") or record.get("billCode") or "")
            if bc not in groups:
                groups[bc] = []
            groups[bc].append(record)

        if not groups:
            return 0, 0, []

        success = 0
        failed = 0
        error_messages: List[str] = []

        async with self._get_session() as session:
            # 获取目标表的列信息（缓存）
            table_info = await session.execute(
                text(
                    "SELECT column_name, data_type FROM information_schema.columns "
                    "WHERE table_schema='public' AND table_name=:table_name"
                ),
                {"table_name": target_table},
            )
            existing_cols = {row[0]: row[1] for row in table_info.fetchall()}

            # 系统字段（自动维护，不写入）
            system_cols = {"id", "deleted", "create_time", "update_time",
                           "create_by", "update_by", "approval_status",
                           "approval_user_id", "approval_time"}

            from adapters.targets.ai_ready.resolver import (
                NameResolver,
                resolve_record_fks,
            )

            unique_keys = {
                "erp_sale_outbound": "outbound_no",
                "erp_sale_order": "order_no",
                "erp_purchase_inbound": "inbound_no",
                "erp_purchase_order": "order_no",
                "erp_purchase_return": "return_no",
                "erp_receipt": "receipt_no",
                "erp_payment": "payment_no",
                "erp_stock_transfer": "transfer_no",
            }

            resolver = NameResolver(session, tenant_id=1)
            existing_cols_set = set(existing_cols.keys())

            for billcode, group_records in groups.items():
                mapped = map_ql361_to_ai_ready(bill_type, group_records[0])
                if not mapped:
                    continue

                data = mapped["data"]

                # 步骤1：过滤 — 只保留目标表存在的列，去掉系统维护列
                filtered = {}
                for col, val in data.items():
                    if col in existing_cols and col not in system_cols:
                        db_type = existing_cols[col]
                        if db_type.startswith("timestamp"):
                            filtered[col] = _parse_timestamp(val)
                        elif db_type in ("numeric", "real", "double precision", "money"):
                            filtered[col] = _parse_numeric(val)
                        elif db_type in ("integer", "bigint", "smallint"):
                            filtered[col] = _parse_int(val)
                        else:
                            filtered[col] = val

                if not filtered:
                    continue

                # 步骤2：名称 → ID 解析（客户、供应商、仓库）
                filtered = await resolve_record_fks(filtered, resolver, existing_cols_set)

                # 步骤3：填充 NOT NULL 缺省值（0 表示"未匹配"）
                fk_defaults = {
                    "tenant_id": 1,
                    "customer_id": 0,
                    "supplier_id": 0,
                    "warehouse_id": 0,
                    "from_warehouse_id": 0,
                    "to_warehouse_id": 0,
                    "transfer_type": 1,
                    "deleted": 0,
                    "salesperson_id": 0,
                    "salesman_id": 0,
                    "approval_status": 0,
                    "payment_status": 0,
                    "delivery_status": 0,
                }
                for col_name, default_val in fk_defaults.items():
                    if col_name in existing_cols and col_name not in filtered:
                        filtered[col_name] = default_val

                # 步骤4：构建 ext_info — 存储未映射但有数据的 ql361 字段
                raw_record = group_records[0]
                mapped_ql361_fields = {
                    item[0] for item in TABLE_FIELD_MAPPINGS.get(bill_type, [])
                }
                ext_data = {}
                for k, v in raw_record.items():
                    if k not in mapped_ql361_fields:
                        # 跳过空值和零值
                        if v is not None and v != '' and v != 0 and v != 0.0:
                            ext_data[k] = _serialize_value(v)
                if ext_data and "ext_info" in existing_cols:
                    filtered["ext_info"] = json.dumps(ext_data, ensure_ascii=False)

                try:
                    cols = list(filtered.keys())
                    vals = list(filtered.values())
                    placeholders = [f":c{i}" for i in range(len(cols))]
                    param_dict = {f"c{i}": vals[i] for i in range(len(cols))}

                    unique_key = unique_keys.get(target_table)
                    if unique_key and unique_key in existing_cols:
                        # 有业务唯一键 → UPSERT
                        update_set = ", ".join(
                            f"{c} = EXCLUDED.{c}"
                            for c in cols if c != unique_key
                        )
                        stmt = text(
                            f"INSERT INTO {target_table} ({', '.join(cols)}) "
                            f"VALUES ({', '.join(placeholders)}) "
                            f"ON CONFLICT ({unique_key}) DO UPDATE SET {update_set}"
                        )
                    else:
                        # 无业务唯一键 → 直接 INSERT
                        stmt = text(
                            f"INSERT INTO {target_table} ({', '.join(cols)}) "
                            f"VALUES ({', '.join(placeholders)})"
                        )

                    await session.execute(stmt, param_dict)
                    success += 1
                except Exception as e:
                    failed += 1
                    error_messages.append(f"{billcode}: {e}")

            await session.commit()

        if failed:
            logger.warning(
                "业务表导入完成 [{}]: {} 成功, {} 失败", bill_type, success, failed
            )
        else:
            logger.debug(
                "业务表导入完成 [{}]: {} 条", bill_type, success
            )

        return success, failed, error_messages

    async def close(self) -> None:
        """关闭数据库连接"""
        await self._engine.dispose()
        logger.info("数据库连接已关闭")

    # ── 辅助方法 ──

    @staticmethod
    def _extract_source_id(record: Dict[str, Any]) -> Optional[str]:
        """从记录中提取源系统 ID"""
        for key in ("id", "Id", "ID", "billid", "billId", "saleId", "orderId", "receiptId"):
            value = record.get(key)
            if value is not None:
                return str(value)
        return None

    @staticmethod
    def _to_json(data: Any) -> Any:
        """将数据转为 JSON 兼容格式"""
        if isinstance(data, dict):
            return {k: _serialize_value(v) for k, v in data.items()}
        if isinstance(data, (list, tuple)):
            return [_serialize_value(v) for v in data]
        return data


def _serialize_value(value: Any) -> Any:
    """将 Python 对象转为 JSON 可序列化格式"""
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, (int, float, str, bool, type(None))):
        return value
    if isinstance(value, (dict, list)):
        return AiReadyTargetAdapter._to_json(value)
    return str(value)


def _parse_timestamp(value: Any) -> Optional[datetime]:
    """将字符串时间解析为 datetime 对象"""
    if value is None:
        return None
    if isinstance(value, (datetime, date)):
        return value if isinstance(value, datetime) else datetime.combine(value, datetime.min.time())
    s = str(value).strip()
    if not s:
        return None
    # 尝试常见格式
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d", "%Y/%m/%d"):
        try:
            return datetime.strptime(s[:19], fmt)
        except ValueError:
            continue
    return None


def _parse_numeric(value: Any) -> Optional[float]:
    """将值解析为浮点数"""
    if value is None:
        return None
    try:
        return float(str(value).replace(",", ""))
    except (ValueError, TypeError):
        return None


def _parse_int(value: Any) -> Optional[int]:
    """将值解析为整数"""
    if value is None:
        return None
    try:
        return int(float(str(value)))
    except (ValueError, TypeError):
        return None
