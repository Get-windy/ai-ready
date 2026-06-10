"""
同步调度器

基于 APScheduler，支持定时全量同步和增量同步。
从数据库读取所有启用的租户配置，逐租户执行同步。
"""
from __future__ import annotations

import asyncio
from typing import Any, Dict, List, Optional

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from loguru import logger

from config import settings
from core.orchestrator import TenantSyncOrchestrator


class SyncScheduler:
    """
    多租户同步调度器

    按调度计划自动执行所有启用了同步配置的租户的数据同步。

    Examples:
        >>> scheduler = SyncScheduler()
        >>> scheduler.start()
    """

    def __init__(self):
        self._orchestrator = TenantSyncOrchestrator(
            page_size=settings.PAGE_SIZE,
            page_delay=settings.PAGE_DELAY,
            max_retries=settings.MAX_RETRIES,
            retry_delay=settings.RETRY_DELAY,
        )
        self._scheduler = AsyncIOScheduler()
        self._running_full = False
        self._running_incr = False

    def start(self) -> None:
        """启动调度器，注册定时任务"""
        logger.info("启动同步调度器")

        # 全量同步任务
        if settings.SCHEDULE_FULL_SYNC_CRON:
            self._scheduler.add_job(
                self._run_full_sync,
                CronTrigger.from_crontab(settings.SCHEDULE_FULL_SYNC_CRON),
                id="full_sync",
                name="全量同步",
                misfire_grace_time=300,
            )
            logger.info("全量同步定时: {}", settings.SCHEDULE_FULL_SYNC_CRON)

        # 增量同步任务
        if settings.SCHEDULE_INCR_SYNC_CRON:
            self._scheduler.add_job(
                self._run_incremental_sync,
                CronTrigger.from_crontab(settings.SCHEDULE_INCR_SYNC_CRON),
                id="incremental_sync",
                name="增量同步",
                misfire_grace_time=120,
            )
            logger.info("增量同步定时: {}", settings.SCHEDULE_INCR_SYNC_CRON)

        self._scheduler.start()
        logger.info("调度器已启动")

    async def _run_full_sync(self) -> None:
        """执行全量同步（防止并发执行）"""
        if self._running_full:
            logger.warning("全量同步正在执行中，跳过本次调度")
            return

        self._running_full = True
        try:
            logger.info("=== 定时全量同步开始 ===")
            await self._orchestrator.run_full_sync()
            logger.info("=== 定时全量同步结束 ===")
        except Exception as e:
            logger.error("全量同步异常: {}", e)
        finally:
            self._running_full = False

    async def _run_incremental_sync(self) -> None:
        """执行增量同步（防止并发执行）"""
        if self._running_incr:
            logger.warning("增量同步正在执行中，跳过本次调度")
            return

        self._running_incr = True
        try:
            logger.info("=== 定时增量同步开始 ===")
            await self._orchestrator.run_incremental_sync()
            logger.info("=== 定时增量同步结束 ===")
        except Exception as e:
            logger.error("增量同步异常: {}", e)
        finally:
            self._running_incr = False

    async def run_once_full(self) -> Dict[str, Any]:
        """手动执行一次全量同步"""
        return await self._orchestrator.run_full_sync()

    async def run_once_incremental(self) -> Dict[str, Any]:
        """手动执行一次增量同步"""
        return await self._orchestrator.run_incremental_sync()

    def stop(self) -> None:
        """停止调度器"""
        if self._scheduler.running:
            self._scheduler.shutdown(wait=False)
            logger.info("调度器已停止")
