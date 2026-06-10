"""
名称 → ID 解析器

自动查询或创建客户、供应商、仓库等参照数据。
实现"导入即自动创建"，无需手动维护参照表。
"""
from __future__ import annotations

import hashlib
from typing import Any, Dict

from loguru import logger
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


def _generate_code(name: str, prefix: str = "QL") -> str:
    """根据名称生成唯一编码"""
    if not name:
        name = "unknown"
    h = hashlib.md5(name.encode("utf-8")).hexdigest()[:8].upper()
    return f"{prefix}-{h}"


class NameResolver:
    """
    名称 → ID 解析器（异步）

    使用数据库连接查找或自动创建参照记录。会话内缓存避免重复查询。

    Examples:
        >>> async with session.begin():
        ...     r = NameResolver(session)
        ...     cid = await r.resolve_customer("某客户")
        ...     wid = await r.resolve_warehouse("成品仓库")
    """

    _ENTITY_CONFIG = {
        "customer": {
            "table": "crm_customer",
            "name_col": "customer_name",
            "code_col": "customer_code",
            "required": ["customer_name", "customer_code", "status", "tenant_id"],
            "defaults": {"status": 1, "deleted": 0},
        },
        "supplier": {
            "table": "erp_supplier",
            "name_col": "supplier_name",
            "code_col": "supplier_code",
            "required": ["supplier_name", "supplier_code", "status", "tenant_id"],
            "defaults": {"status": 1, "deleted": 0},
        },
        "warehouse": {
            "table": "erp_warehouse",
            "name_col": "warehouse_name",
            "code_col": "warehouse_code",
            "required": ["warehouse_name", "warehouse_code"],
            "defaults": {"deleted": 0},
        },
    }

    def __init__(self, session: AsyncSession, tenant_id: int = 1):
        self._session = session
        self._tenant_id = tenant_id
        # {entity_type: {name_lower: id}}
        self._cache: Dict[str, Dict[str, int]] = {}
        # 追踪新创建的记录数
        self._created: Dict[str, int] = {}

    async def resolve(self, entity_type: str, name: str) -> int:
        """解析名称到 ID，不存在则自动创建"""
        if not name or not name.strip():
            return 0

        name_key = name.strip().lower()
        cfg = self._ENTITY_CONFIG.get(entity_type)
        if not cfg:
            return 0

        # 缓存命中
        cache = self._cache.setdefault(entity_type, {})
        if name_key in cache:
            return cache[name_key]

        # 数据库查找
        table = cfg["table"]
        name_col = cfg["name_col"]
        row = await self._session.execute(
            text(
                f"SELECT id FROM {table} "
                f"WHERE LOWER({name_col}) = :name AND deleted = 0 "
                f"LIMIT 1"
            ),
            {"name": name_key},
        )
        existing = row.scalar_one_or_none()

        if existing is not None:
            cache[name_key] = existing
            return existing

        # 自动创建
        new_id = await self._auto_create(entity_type, name, cfg)
        if new_id:
            cache[name_key] = new_id
        return new_id

    async def _auto_create(
        self, entity_type: str, name: str, cfg: dict
    ) -> int:
        """自动创建参照记录"""
        table = cfg["table"]
        name_col = cfg["name_col"]
        code_col = cfg["code_col"]
        defaults = cfg.get("defaults", {})

        cols = [name_col, code_col]
        params: dict = {
            "name_val": name.strip(),
            "code_val": _generate_code(name),
        }

        for k, v in defaults.items():
            # tenant_id 可能是 int 或 varchar 类型，统一转字符串安全
            val = str(self._tenant_id) if k == "tenant_id" else v
            cols.append(k)
            params[k] = val

        try:
            result = await self._session.execute(
                text(
                    f"INSERT INTO {table} ({', '.join(cols)}) "
                    f"VALUES (:{', :'.join(params.keys())}) "
                    f"RETURNING id"
                ),
                params,
            )
            new_id = result.scalar_one()
            self._created[entity_type] = self._created.get(entity_type, 0) + 1
            logger.debug("自动创建 {}: [{}] id={}", entity_type, name, new_id)
            return new_id
        except Exception as e:
            logger.warning("创建 {} [{}] 失败: {}", entity_type, name, e)
            return 0

    # ── 便捷方法 ──

    async def resolve_customer(self, name: str) -> int:
        return await self.resolve("customer", name)

    async def resolve_supplier(self, name: str) -> int:
        return await self.resolve("supplier", name)

    async def resolve_warehouse(self, name: str) -> int:
        return await self.resolve("warehouse", name)

    @property
    def stats(self) -> Dict[str, int]:
        """解析统计"""
        return {
            **{f"{k}_resolved": len(v) for k, v in self._cache.items()},
            **{f"{k}_created": v for k, v in self._created.items()},
        }


# ── 名称列 → FK 列映射规则 ──

NAME_FK_RULES = [
    ("customer_name", "customer_id", "customer"),
    ("supplier_name", "supplier_id", "supplier"),
    ("warehouse_name", "warehouse_id", "warehouse"),
    ("from_warehouse", "from_warehouse_id", "warehouse"),
    ("to_warehouse", "to_warehouse_id", "warehouse"),
]


async def resolve_record_fks(
    data: Dict[str, Any],
    resolver: NameResolver,
    existing_cols: set[str],
) -> Dict[str, Any]:
    """
    将映射后的数据中的文本名称字段解析为 FK ID

    1. 遍历规则，找到 data 中的名称字段
    2. 如果在目标表中不存在文本列，则移除
    3. 如果目标表有对应的 FK 列，则解析名称并写入 ID

    Args:
        data: 映射后的数据字典
        resolver: 名称解析器
        existing_cols: 目标表的列名集合

    Returns:
        解析完成的数据字典
    """
    for name_col, fk_col, entity_type in NAME_FK_RULES:
        name_val = data.get(name_col)
        if not name_val:
            continue

        # 文本列在目标表中不存在 → 移除
        if name_col not in existing_cols:
            del data[name_col]

        # 目标表有 FK 列 → 解析名称
        if fk_col in existing_cols:
            id_ = await resolver.resolve(entity_type, str(name_val))
            if id_:
                data[fk_col] = id_

    return data
