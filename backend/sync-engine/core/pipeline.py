"""
同步编排器

核心编排逻辑，协调 SourceAdapter 和 TargetAdapter 完成数据同步。
支持全量同步和增量同步两种模式。

工作流程:
1. 获取需要同步的单据类型列表
2. 对每个单据类型：
   a. 创建同步日志
   b. 分页拉取源数据
   c. 数据映射转换
   d. 批量写入目标
   e. 完成同步日志
3. 返回同步结果统计
"""
from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional, Set

from loguru import logger

from .source import SourceAdapter
from .target import TargetAdapter

from .mapper import DataMapper


@dataclass
class SyncResult:
    """单次同步结果"""

    bill_type: str
    bill_type_name: str
    sync_type: str  # "full" | "incremental"
    total_records: int = 0
    success_count: int = 0
    failed_count: int = 0
    page_count: int = 0
    duration_seconds: float = 0.0
    error_messages: List[str] = field(default_factory=list)
    status: str = "pending"  # pending | running | success | failed | partial


class SyncPipeline:
    """
    同步编排器

    Args:
        source: 数据源适配器
        target: 目标适配器
        mapper: 数据映射器（可选，None 时透传原始数据）
        page_size: 每页记录数
        page_delay: 翻页间隔秒数（用于控制请求频率）
        max_retries: 每页最大重试次数
        retry_delay: 重试间隔秒数

    Examples:
        >>> pipeline = SyncPipeline(source, target, mapper)
        >>> result = await pipeline.run_incremental_sync()
        >>> print(f"同步完成: {result}")
    """

    def __init__(
        self,
        source: SourceAdapter,
        target: TargetAdapter,
        mapper: Optional[DataMapper] = None,
        *,
        page_size: int = 100,
        page_delay: float = 0.5,
        max_retries: int = 3,
        retry_delay: float = 2.0,
    ):
        self.source = source
        self.target = target
        self.mapper = mapper
        self.page_size = page_size
        self.page_delay = page_delay
        self.max_retries = max_retries
        self.retry_delay = retry_delay

    async def run_full_sync(
        self,
        bill_types: Optional[List[str]] = None,
    ) -> Dict[str, SyncResult]:
        """
        全量同步指定单据类型

        Args:
            bill_types: 要同步的单据类型列表，None 表示同步所有支持的类型

        Returns:
            {bill_type: SyncResult} 字典
        """
        return await self._run_sync(bill_types, incremental=False)

    async def run_incremental_sync(
        self,
        bill_types: Optional[List[str]] = None,
    ) -> Dict[str, SyncResult]:
        """
        增量同步指定单据类型

        只同步自上次同步以来发生变化的数据。

        Args:
            bill_types: 要同步的单据类型列表，None 表示同步所有支持的类型

        Returns:
            {bill_type: SyncResult} 字典
        """
        return await self._run_sync(bill_types, incremental=True)

    async def _run_sync(
        self,
        bill_types: Optional[List[str]],
        incremental: bool,
    ) -> Dict[str, SyncResult]:
        """通用同步入口"""
        sync_type = "incremental" if incremental else "full"
        logger.info("开始{}同步", sync_type)

        types_to_sync = await self._resolve_bill_types(bill_types)
        if not types_to_sync:
            logger.warning("没有需要同步的单据类型")
            return {}

        # 确保存储已初始化
        await self.target.init_storage()

        results: Dict[str, SyncResult] = {}

        for bill_type, bill_type_name in types_to_sync.items():
            logger.info("正在同步 [{}] {} ({})", bill_type, bill_type_name, sync_type)
            result = await self._sync_one(bill_type, bill_type_name, incremental)
            results[bill_type] = result

            # 每个单据类型之间也加一点延迟
            await asyncio.sleep(self.page_delay)

        self._log_summary(results, sync_type)
        return results

    async def _resolve_bill_types(
        self,
        bill_types: Optional[List[str]],
    ) -> Dict[str, str]:
        """解析要同步的单据类型列表"""
        supported = self.source.get_supported_bill_types()

        if bill_types is None:
            return supported

        # 过滤出支持的、且在请求列表中的类型
        resolved: Dict[str, str] = {}
        for bt in bill_types:
            if bt in supported:
                resolved[bt] = supported[bt]
            else:
                logger.warning("单据类型 {} 不被数据源 {} 支持，跳过", bt, self.source.name)

        return resolved

    async def _sync_one(
        self,
        bill_type: str,
        bill_type_name: str,
        incremental: bool,
    ) -> SyncResult:
        """同步单个单据类型"""
        result = SyncResult(
            bill_type=bill_type,
            bill_type_name=bill_type_name,
            sync_type="incremental" if incremental else "full",
        )

        start_time = time.monotonic()
        sync_id: Optional[int] = None

        try:
            # 获取最后同步时间（增量模式）
            last_sync: Optional[datetime] = None
            if incremental:
                last_sync_str = await self.target.get_last_sync_time(
                    self.source.name, bill_type
                )
                if last_sync_str:
                    try:
                        last_sync = datetime.fromisoformat(last_sync_str)
                        logger.info(
                            "上次同步时间: {} ({})", last_sync_str, bill_type
                        )
                    except ValueError:
                        logger.warning("解析上次同步时间失败: {}", last_sync_str)

            # 创建同步日志
            sync_id = await self.target.create_sync_log(
                source_name=self.source.name,
                bill_type=bill_type,
                bill_type_name=bill_type_name,
                sync_type=result.sync_type,
            )

            result.status = "running"

            # 分页循环
            page = 0
            has_more = True

            while has_more:
                page += 1
                logger.debug(
                    "拉取第 {} 页 [{}]", page, bill_type
                )

                page_data = await self._fetch_page_with_retry(
                    bill_type, page, last_sync
                )

                records = page_data.get("records", [])
                total = page_data.get("total", 0)

                if not records:
                    has_more = False
                    continue

                # 数据映射转换
                mapped_records = records
                if self.mapper:
                    mapped_records, mapping_errors = self.mapper.map_bill_type(
                        bill_type, records
                    )
                    for idx, err in mapping_errors:
                        result.error_messages.append(f"第{page}页第{idx}条映射失败: {err}")

                # 写入目标（sync_records）
                success, failed, errors = await self.target.upsert_records(
                    bill_type=bill_type,
                    records=mapped_records,
                    source_name=self.source.name,
                    sync_id=sync_id,
                )

                # 写入业务表（erp_*，使用原始 ql361 数据进行映射）
                if records:
                    biz_ok, biz_fail, biz_errors = await self.target.import_to_business_tables(
                        bill_type=bill_type,
                        records=records,
                    )
                    if biz_errors:
                        result.error_messages.extend(
                            f"[业务表] {e}" for e in biz_errors[:5]
                        )

                result.total_records += total or len(records)
                result.success_count += success
                result.failed_count += failed
                result.page_count += 1

                if errors:
                    result.error_messages.extend(errors[:10])  # 最多记录10条错误

                # 判断是否有下一页
                has_more = page_data.get("has_more", len(records) >= self.page_size)

                # 翻页间隔（控制请求频率）
                if has_more:
                    await asyncio.sleep(self.page_delay)

            # 标记成功
            result.status = "success" if result.failed_count == 0 else "partial"
            await self.target.complete_sync_log(
                log_id=sync_id,
                status=result.status,
                total=result.total_records,
                success=result.success_count,
                failed=result.failed_count,
            )

        except Exception as e:
            logger.error("同步 [{}] 失败: {}", bill_type, e)
            result.status = "failed"
            result.error_messages.append(str(e))

            if sync_id is not None:
                await self.target.complete_sync_log(
                    log_id=sync_id,
                    status="failed",
                    total=result.total_records,
                    success=result.success_count,
                    failed=result.failed_count,
                    error=str(e),
                )

        finally:
            result.duration_seconds = time.monotonic() - start_time

        return result

    async def _fetch_page_with_retry(
        self,
        bill_type: str,
        page: int,
        last_sync: Optional[datetime],
    ) -> Dict[str, Any]:
        """带重试的分页数据拉取"""
        last_error: Optional[Exception] = None

        for attempt in range(1, self.max_retries + 1):
            try:
                return await self.source.fetch_page(
                    bill_type=bill_type,
                    page=page,
                    page_size=self.page_size,
                    last_sync=last_sync,
                )
            except Exception as e:
                last_error = e
                if attempt < self.max_retries:
                    wait = self.retry_delay * attempt
                    logger.warning(
                        "第 {} 页拉取失败（第{}次重试，等待{}s）: {}",
                        page, attempt, wait, e,
                    )
                    await asyncio.sleep(wait)

        raise RuntimeError(
            f"第 {page} 页拉取失败，已重试 {self.max_retries} 次: {last_error}"
        ) from last_error

    def _log_summary(
        self,
        results: Dict[str, SyncResult],
        sync_type: str,
    ) -> None:
        """输出同步汇总日志"""
        total_time = sum(r.duration_seconds for r in results.values())
        total_success = sum(r.success_count for r in results.values())
        total_failed = sum(r.failed_count for r in results.values())
        failed_types = [bt for bt, r in results.items() if r.status == "failed"]

        logger.info("=" * 50)
        logger.info("{}同步完成汇总", sync_type)
        logger.info("  耗时: {:.1f}s", total_time)
        logger.info("  成功: {} 条", total_success)
        logger.info("  失败: {} 条", total_failed)

        for bt, r in results.items():
            logger.info(
                "  [{}] {}: {} 条 (成功={}, 失败={}, {:.1f}s)",
                r.status, bt, r.total_records,
                r.success_count, r.failed_count,
                r.duration_seconds,
            )

        if failed_types:
            logger.warning("  失败类型: {}", ", ".join(failed_types))

        logger.info("=" * 50)
