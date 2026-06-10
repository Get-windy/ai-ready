"""
日志配置

基于 loguru，支持文件轮转、控制台输出、结构化日志。
"""
from __future__ import annotations

import sys
from pathlib import Path

from loguru import logger

from config import settings


def setup_logger() -> None:
    """初始化日志配置"""
    # 移除默认 handler
    logger.remove()

    log_dir = Path(settings.LOG_DIR)
    log_dir.mkdir(parents=True, exist_ok=True)

    # 控制台输出（带颜色）
    logger.add(
        sys.stderr,
        level=settings.LOG_LEVEL,
        format=(
            "<green>{time:MM-DD HH:mm:ss}</green> "
            "| <level>{level: <7}</level> "
            "| <cyan>{name}</cyan>:<cyan>{line}</cyan> "
            "| <level>{message}</level>"
        ),
        enqueue=True,
        colorize=True,
    )

    # 文件输出（轮转）
    logger.add(
        log_dir / "sync_{time:YYYY-MM-DD}.log",
        level=settings.LOG_LEVEL,
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <7} | {name}:{line} | {message}",
        rotation="100 MB",
        retention="30 days",
        compression="gz",
        enqueue=True,
    )

    # 错误日志单独文件
    logger.add(
        log_dir / "error_{time:YYYY-MM-DD}.log",
        level="ERROR",
        format="{time:YYYY-MM-DD HH:mm:ss} | {level: <7} | {name}:{line} | {message}",
        rotation="100 MB",
        retention="30 days",
        compression="gz",
        enqueue=True,
    )

    logger.info("日志初始化完成，级别: {}", settings.LOG_LEVEL)
