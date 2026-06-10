"""
QL361 数据源适配器

实现 SourceAdapter 接口，用于从来肯云商（ql361.com）同步数据。

支持的同步流程:
1. 登录认证（新版 JSON 登录 + 子域名认证）
2. 分页拉取单据数据（全量/增量），使用 billbllmanager.getdatalist
3. 按需获取单据详情

API 端点: 22stable.ql361.com/v_22stable/api.cc
"""
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from loguru import logger

from core.source import SourceAdapter

from .auth import QLAuthManager, QLAuthError
from .client import QLAPIClient


# 已知的单据类型映射
KNOWN_BILL_TYPES: Dict[str, str] = {
    "601": "销售出库单",
    "604": "销售订单",
    "504": "采购订单",
    "801": "收款单",
}

# billtype 代码与中文名的双向映射
BILL_TYPE_CODES: Dict[int, str] = {
    601: "销售出库单",
    604: "销售订单",
    504: "采购订单",
    801: "收款单",
}


class QL361SourceAdapter(SourceAdapter):
    """
    来肯云商数据源适配器

    Args:
        username: 登录账号
        password: 登录密码
        base_url: 主站地址
        bill_types: 要同步的单据类型列表（如 ["601", "604"]），默认全部

    Examples:
        >>> source = QL361SourceAdapter("user", "pass")
        >>> await source.authenticate()
        >>> types = source.get_supported_bill_types()
        >>> page = await source.fetch_page("601", page=0)
    """

    def __init__(
        self,
        username: str,
        password: str,
        base_url: str = "https://www.ql361.com",
        bill_types: Optional[List[str]] = None,
    ):
        self._username = username
        self._password = password
        self._base_url = base_url
        self._bill_types = bill_types or list(KNOWN_BILL_TYPES.keys())

        self._auth: Optional[QLAuthManager] = None
        self._client: Optional[QLAPIClient] = None
        self._authenticated = False

    @property
    def name(self) -> str:
        return "ql361"

    @property
    def _auth_manager(self) -> QLAuthManager:
        if self._auth is None:
            self._auth = QLAuthManager(
                username=self._username,
                password=self._password,
                base_url=self._base_url,
            )
        return self._auth

    @property
    def _api_client(self) -> QLAPIClient:
        if self._client is None:
            self._client = QLAPIClient(self._auth_manager)
        return self._client

    async def authenticate(self) -> None:
        """登录 ql361.com"""
        logger.info("QL361 数据源认证中...")
        await self._auth_manager.login()
        self._authenticated = True
        logger.info("QL361 认证成功，子域名: {}", self._auth_manager.subdomain)

    async def fetch_page(
        self,
        bill_type: str,
        page: int = 0,
        page_size: int = 100,
        last_sync: Optional[datetime] = None,
    ) -> Dict[str, Any]:
        """
        从 ql361 拉取一页数据

        使用 billbllmanager.getdatalist 查询。

        Args:
            bill_type: 单据类型代码（如 "601"）
            page: 页码（0-based）
            page_size: 每页条数
            last_sync: 增量同步的截止时间

        Returns:
            {"records": [...], "total": N, "has_more": bool}
        """
        if not self._authenticated:
            await self.authenticate()

        try:
            billtype_code = int(bill_type)
        except ValueError:
            logger.warning("无效的单据类型代码: {}", bill_type)
            return {"records": [], "total": 0, "has_more": False}

        # 增量同步：添加时间筛选
        begindate = last_sync if last_sync else None
        enddate = datetime.now() if last_sync else None

        logger.debug(
            "拉取数据: billtype={}, page={}, page_size={}, last_sync={}",
            bill_type, page, page_size, last_sync,
        )

        result = await self._api_client.get_bill_list(
            billtype=billtype_code,
            page=page,
            page_size=page_size,
            begindate=begindate,
            enddate=enddate,
        )

        return result

    def get_supported_bill_types(self) -> Dict[str, str]:
        """返回已知的单据类型"""
        result: Dict[str, str] = {}
        for bt in self._bill_types:
            if bt in KNOWN_BILL_TYPES:
                result[bt] = KNOWN_BILL_TYPES[bt]
        return result

    async def get_bill_detail(
        self,
        bill_type: str,
        bill_id: str,
    ) -> Optional[Dict[str, Any]]:
        """获取单个单据详情"""
        if not self._authenticated:
            await self.authenticate()

        try:
            bt = int(bill_type)
            bid = int(bill_id)
        except ValueError:
            logger.warning("无效的参数: bill_type={}, bill_id={}", bill_type, bill_id)
            return None

        return await self._api_client.get_bill_detail(bt, bid)

    async def close(self) -> None:
        """释放资源"""
        if self._client:
            self._client = None
        if self._auth:
            await self._auth.close()
            self._auth = None
        self._authenticated = False
