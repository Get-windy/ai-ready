"""
多租户同步编排器

从数据库读取所有启用的租户同步配置，为每个租户创建
SourceAdapter + TargetAdapter，逐租户执行同步。

数据隔离策略：每条同步记录关联 tenant_id。
"""
from __future__ import annotations

import asyncio
from typing import Any, Dict, List, Optional

from loguru import logger

from core.db import get_tenant_configs
from core.pipeline import SyncPipeline, SyncResult


class TenantSyncOrchestrator:
    """
    多租户同步编排器

    从数据库读取所有启用的租户同步配置，为每个配置创建适配器并执行同步。

    Args:
        page_size: 每页记录数
        page_delay: 翻页间隔秒数
        max_retries: 每页最大重试次数
        retry_delay: 重试间隔秒数

    Examples:
        >>> orchestrator = TenantSyncOrchestrator()
        >>> results = await orchestrator.run_incremental_sync()
    """

    def __init__(
        self,
        page_size: int = 100,
        page_delay: float = 0.5,
        max_retries: int = 3,
        retry_delay: float = 2.0,
    ):
        self.page_size = page_size
        self.page_delay = page_delay
        self.max_retries = max_retries
        self.retry_delay = retry_delay

    async def run_incremental_sync(
        self,
        tenant_ids: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """
        对所有（或指定）租户执行增量同步

        Args:
            tenant_ids: 租户ID列表，None表示所有

        Returns:
            {source_type: {bill_type: SyncResult}}
        """
        return await self._run_sync(incremental=True, tenant_ids=tenant_ids)

    async def run_full_sync(
        self,
        tenant_ids: Optional[List[int]] = None,
    ) -> Dict[str, Any]:
        """
        对所有（或指定）租户执行全量同步

        Args:
            tenant_ids: 租户ID列表，None表示所有

        Returns:
            {source_type: {bill_type: SyncResult}}
        """
        return await self._run_sync(incremental=False, tenant_ids=tenant_ids)

    async def _run_sync(
        self,
        incremental: bool,
        tenant_ids: Optional[List[int]],
    ) -> Dict[str, Any]:
        """通用同步逻辑"""
        sync_type = "incremental" if incremental else "full"
        logger.info("多租户{}同步开始", sync_type)

        configs = await get_tenant_configs()

        if not configs:
            logger.warning("没有启用的租户同步配置")
            return {}

        # 过滤指定租户
        if tenant_ids:
            configs = [c for c in configs if c["tenant_id"] in tenant_ids]

        if not configs:
            logger.warning("没有匹配的租户配置")
            return {}

        all_results: Dict[str, Any] = {}

        for config in configs:
            tenant_id = config["tenant_id"]
            source_type = config["source_type"]
            logger.info(
                "开始同步 租户={}, 来源={}, 账号={}",
                tenant_id, source_type, config["source_username"],
            )

            try:
                results = await self._sync_tenant(config, incremental)
                all_results[f"tenant_{tenant_id}_{source_type}"] = results
            except Exception as e:
                logger.error(
                    "租户 {} 同步失败: {}", tenant_id, e
                )

        logger.info("多租户{}同步完成: {} 个配置", sync_type, len(configs))
        return all_results

    async def _sync_tenant(
        self,
        config: dict,
        incremental: bool,
    ) -> Dict[str, SyncResult]:
        """执行单个租户的同步"""
        source_type = config["source_type"]

        if source_type == "ql361":
            return await self._sync_ql361(config, incremental)
        else:
            logger.warning("不支持的来源类型: {}", source_type)
            return {}

    async def _sync_ql361(
        self,
        config: dict,
        incremental: bool,
    ) -> Dict[str, SyncResult]:
        """执行 QL361 同步"""
        from adapters.sources.ql361 import QL361SourceAdapter
        from adapters.targets.ai_ready import AiReadyTargetAdapter
        from config import settings

        tenant_id = config["tenant_id"]

        source = QL361SourceAdapter(
            username=config["source_username"],
            password=config["source_password"],
            base_url=config["base_url"],
        )

        target = AiReadyTargetAdapter(
            db_url=settings.db_url,
        )

        pipeline = SyncPipeline(
            source=source,
            target=target,
            page_size=self.page_size,
            page_delay=self.page_delay,
            max_retries=self.max_retries,
            retry_delay=self.retry_delay,
        )

        try:
            await source.authenticate()

            bill_types = config.get("bill_types")
            if incremental:
                results = await pipeline.run_incremental_sync(bill_types)
            else:
                results = await pipeline.run_full_sync(bill_types)

            return results

        finally:
            await source.close()
            await target.close()
