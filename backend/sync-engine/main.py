#!/usr/bin/env python
"""
同步引擎主入口

启动同步引擎，支持以下运行模式:
1. **daemon**: 以守护进程模式运行，按调度计划自动执行所有租户的同步（默认）
2. **full**: 立即对所有租户执行一次全量同步后退出
3. **incremental**: 立即对所有租户执行一次增量同步后退出
4. **once**: 使用环境变量中的单个租户配置执行一次同步（传统模式）

用法:
    python main.py                       # 守护进程模式
    python main.py --mode full           # 对所有租户全量同步
    python main.py --mode incremental    # 对所有租户增量同步
    python main.py --mode once           # 使用 SOURCE_USERNAME/PASSWORD 单次同步
"""
from __future__ import annotations

import argparse
import asyncio
import os
import signal
import sys
from typing import List, Optional

from loguru import logger

from config import settings
from core.pipeline import SyncPipeline
from logger import setup_logger


async def run_once_legacy(
    bill_types: Optional[List[str]],
) -> None:
    """
    传统单租户模式：使用环境变量中的 SOURCE_USERNAME/PASSWORD 执行同步

    仅在没有 sync_data_source 表或需要测试单个配置时使用。
    """
    from adapters.sources.ql361 import QL361SourceAdapter
    from adapters.targets.ai_ready import AiReadyTargetAdapter

    username = settings.SOURCE_USERNAME
    password = settings.SOURCE_PASSWORD

    if not username or not password:
        print(
            "错误: 请设置 SOURCE_USERNAME 和 SOURCE_PASSWORD 环境变量\n"
            "或创建 .env 文件"
        )
        sys.exit(1)

    source = QL361SourceAdapter(
        username=username,
        password=password,
        base_url=settings.SOURCE_BASE_URL,
    )
    target = AiReadyTargetAdapter(db_url=settings.db_url)

    pipeline = SyncPipeline(
        source=source,
        target=target,
        page_size=settings.PAGE_SIZE,
        page_delay=settings.PAGE_DELAY,
        max_retries=settings.MAX_RETRIES,
        retry_delay=settings.RETRY_DELAY,
    )

    try:
        await source.authenticate()
        results = await pipeline.run_incremental_sync(bill_types)
        total_success = sum(r.success_count for r in results.values())
        total_failed = sum(r.failed_count for r in results.values())
        logger.info("同步完成: 成功={}, 失败={}", total_success, total_failed)
    finally:
        await source.close()
        await target.close()


async def run_multi_tenant(mode: str) -> None:
    """多租户模式：从数据库读取所有启用配置，逐租户同步"""
    from core.orchestrator import TenantSyncOrchestrator

    orchestrator = TenantSyncOrchestrator(
        page_size=settings.PAGE_SIZE,
        page_delay=settings.PAGE_DELAY,
        max_retries=settings.MAX_RETRIES,
        retry_delay=settings.RETRY_DELAY,
    )

    if mode == "full":
        await orchestrator.run_full_sync()
    else:
        await orchestrator.run_incremental_sync()


async def run_daemon() -> None:
    """守护进程模式：按调度计划自动同步所有租户"""
    from scheduler import SyncScheduler

    scheduler = SyncScheduler()
    stop_event = asyncio.Event()

    def _handle_stop() -> None:
        logger.info("收到停止信号，正在关闭...")
        scheduler.stop()
        stop_event.set()

    if os.name != "nt":
        loop = asyncio.get_event_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            loop.add_signal_handler(sig, _handle_stop)

    # 启动 HTTP API 服务器
    from api_server import run_api_server
    api_task = asyncio.create_task(run_api_server())

    try:
        scheduler.start()
        logger.info("同步引擎已启动（守护进程模式）")

        if os.name == "nt":
            try:
                await stop_event.wait()
            except KeyboardInterrupt:
                _handle_stop()
        else:
            await stop_event.wait()

    finally:
        api_task.cancel()
        scheduler.stop()
        from core.db import close_engine
        await close_engine()
        logger.info("同步引擎已关闭")


def _parse_args() -> argparse.Namespace:
    """解析命令行参数"""
    parser = argparse.ArgumentParser(
        description="通用数据同步引擎 - AI-Ready",
    )
    parser.add_argument(
        "--mode",
        choices=["daemon", "full", "incremental", "once"],
        default="daemon",
        help="运行模式（默认: daemon）",
    )
    parser.add_argument(
        "--bill-types",
        nargs="*",
        help="要同步的单据类型代码（仅 once 模式有效）",
    )
    return parser.parse_args()


async def main() -> None:
    """主函数"""
    setup_logger()
    args = _parse_args()

    logger.info("同步引擎启动 [mode={}]", args.mode)

    if args.mode == "daemon":
        await run_daemon()
    elif args.mode == "once":
        await run_once_legacy(args.bill_types)
    else:
        await run_multi_tenant(args.mode)


if __name__ == "__main__":
    asyncio.run(main())
