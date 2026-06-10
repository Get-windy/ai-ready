"""
全局配置管理

使用 pydantic-settings 从环境变量 /.env 文件加载配置。
所有配置项均有默认值，可通过 .env 文件覆盖。
"""
from __future__ import annotations

from pathlib import Path
from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用全局配置"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # ── PostgreSQL 目标数据库 ──
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    DB_USER: str = "postgres"
    DB_PASSWORD: str = ""
    DB_NAME: str = "devdb"

    @property
    def db_url(self) -> str:
        """SQLAlchemy 异步连接串"""
        return (
            f"postgresql+asyncpg://{self.DB_USER}:{self.DB_PASSWORD}"
            f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
        )

    @property
    def db_url_sync(self) -> str:
        """SQLAlchemy 同步连接串（用于表结构创建等）"""
        return (
            f"postgresql://{self.DB_USER}:{self.DB_PASSWORD}"
            f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
        )

    # ── 同步引擎通用配置 ──
    PAGE_SIZE: int = 100
    PAGE_DELAY: float = 0.5  # 翻页间隔（秒）
    MAX_RETRIES: int = 3
    RETRY_DELAY: float = 2.0

    # ── 调度配置 ──
    SCHEDULE_FULL_SYNC_CRON: Optional[str] = "0 3 * * 0"  # 每周日凌晨3点全量
    SCHEDULE_INCR_SYNC_CRON: Optional[str] = "*/30 * * * *"  # 每30分钟增量

    # ── 日志配置 ──
    LOG_LEVEL: str = "INFO"
    LOG_DIR: str = "logs"

    # ── 数据源通用配置 ──
    SOURCE_NAME: str = "default"
    SOURCE_BASE_URL: str = "https://www.ql361.com"
    SOURCE_USERNAME: str = ""
    SOURCE_PASSWORD: str = ""


    @property
    def project_root(self) -> Path:
        """项目根目录"""
        return Path(__file__).resolve().parent.parent


# 全局单例
settings = Settings()  # type: ignore[call-arg]
