"""
QL361 API 客户端

封装对 22stable.ql361.com 后端 API 的 HTTP 调用。

API 端点: 22stable.ql361.com/v_22stable/api.cc
认证方式: URL 查询参数 &s=xxx&tok=xxx
数据格式: POST __postdata=JSON
"""
from __future__ import annotations

import asyncio
import json
import time
from datetime import datetime
from typing import Any, Dict, List, Optional

import httpx
from loguru import logger

from .auth import QLAuthManager, QLAuthError


class RateLimiter:
    """
    简单令牌桶速率限制器

    限制每秒最大请求数，防止 API 被封。

    Args:
        max_per_second: 每秒最大请求数
    """

    def __init__(self, max_per_second: float = 2.0):
        self.max_per_second = max_per_second
        self._tokens = max_per_second
        self._last_refill = time.monotonic()
        self._lock = asyncio.Lock()

    async def acquire(self) -> None:
        """获取一个令牌，如不足则等待"""
        async with self._lock:
            now = time.monotonic()
            elapsed = now - self._last_refill
            self._tokens = min(
                self.max_per_second,
                self._tokens + elapsed * self.max_per_second,
            )
            self._last_refill = now

            if self._tokens < 1:
                wait = (1 - self._tokens) / self.max_per_second
                await asyncio.sleep(wait)
                self._tokens = 0
                self._last_refill = time.monotonic()
            else:
                self._tokens -= 1


class QLAPIClient:
    """
    QL361 API 客户端

    封装对 ql361.com 的所有 API 调用。
    自动处理认证、请求频率、重试。

    Args:
        auth: 认证管理器
        max_requests_per_second: API 请求频率限制
        request_timeout: 单次请求超时秒数

    Examples:
        >>> client = QLAPIClient(ql_auth)
        >>> data = await client.get_bill_list(601, page=0, page_size=100)
    """

    def __init__(
        self,
        auth: QLAuthManager,
        max_requests_per_second: float = 2.0,
        request_timeout: float = 60.0,
    ):
        self.auth = auth
        self.rate_limiter = RateLimiter(max_per_second=max_requests_per_second)
        self.timeout = request_timeout

    async def call(
        self,
        method: str,
        postdata: Optional[Dict[str, Any]] = None,
    ) -> Any:
        """
        调用 QL361 API

        Args:
            method: API 方法名
            postdata: POST 数据（将被 JSON 序列化后放入 __postdata 字段）

        Returns:
            API 返回数据（完整响应体）

        Raises:
            QLAuthError: 认证相关错误
            httpx.HTTPError: HTTP 请求错误
        """
        await self.auth.ensure_logged_in()
        await self.rate_limiter.acquire()

        # 构造 URL: 方法 + 固定参数 + 认证参数
        url = self.auth.get_api_url(method)
        auth_params = self.auth.get_request_params()
        if auth_params:
            url += "&" + "&".join(f"{k}={v}" for k, v in auth_params.items())

        headers = {
            "Referer": f"{self.auth.api_base}/",
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
        }

        # POST 数据包装为 __postdata
        body = {
            "__postdata": json.dumps(postdata or {}, ensure_ascii=False),
        }

        logger.debug("API 请求: {} postdata={}", method, _summarize_params(postdata))

        for attempt in range(3):
            try:
                response = await self.auth.client.post(
                    url,
                    data=body,
                    headers=headers,
                    timeout=self.timeout,
                )
                response.raise_for_status()

                result = response.json()

                # 检查业务错误
                if result.get("type") == "error":
                    msg = result.get("msg", "未知错误")
                    # 某些错误需要重新登录
                    if "登录" in msg or "认证" in msg:
                        logger.warning("认证错误，尝试重新登录: {}", msg)
                        await self.auth.login(force=True)
                        continue
                    raise QLApiError(f"API 错误 [{method}]: {msg}")

                logger.debug("API 响应: {} type={}", method, result.get("type"))
                return result

            except httpx.HTTPStatusError as e:
                if e.response.status_code == 401:
                    logger.warning("HTTP 401，尝试重新登录")
                    await self.auth.login(force=True)
                    continue
                raise

            except httpx.TimeoutException:
                if attempt < 2:
                    wait = (attempt + 1) * 3
                    logger.warning("API 超时，{}/3 重试 ({}s)", attempt + 1, wait)
                    await asyncio.sleep(wait)
                    continue
                raise

    async def get_bill_list(
        self,
        billtype: int,
        page: int = 0,
        page_size: int = 100,
        begindate: Optional[datetime] = None,
        enddate: Optional[datetime] = None,
    ) -> Dict[str, Any]:
        """
        获取单据列表（替代旧的 get_data_list）

        Args:
            billtype: 单据类型代码（如 601）
            page: 页码（0-based）
            page_size: 每页条数
            begindate: 开始时间（增量同步）
            enddate: 结束时间（增量同步）

        Returns:
            {
                "records": [...],    # 数据行
                "total": N,          # 总记录数
                "has_more": bool     # 是否有更多数据
            }
        """
        conditionparam: Dict[str, Any] = {
            "billtype": billtype,
        }

        if begindate:
            conditionparam["begindate"] = begindate.strftime("%Y-%m-%d %H:%M:%S")
        if enddate:
            conditionparam["enddate"] = enddate.strftime("%Y-%m-%d %H:%M:%S")

        postdata = {
            "reportparam": {
                "pagerow": page_size,
                "pagenumber": page,
            },
            "conditionparam": conditionparam,
        }

        result = await self.call(
            "cc.erp.bll.bill.billbllmanager.getdatalist",
            postdata,
        )

        data = result.get("data", {})
        datasource = data.get("datasource", []) or []
        rowcount = int(data.get("rowcount", 0) or 0)

        return {
            "records": datasource,
            "total": rowcount,
            "has_more": (page + 1) * page_size < rowcount,
        }

    async def get_bill_detail(
        self,
        bill_type: int,
        bill_id: int,
    ) -> Optional[Dict[str, Any]]:
        """
        获取单个单据详情

        Args:
            bill_type: 单据类型代码（如 601）
            bill_id: 单据 ID

        Returns:
            单据详情数据，None 表示未找到
        """
        postdata = {
            "billtype": bill_type,
            "billid": bill_id,
        }

        result = await self.call(
            "cc.erp.bll.bill.billbllmanager.getfulldata",
            postdata,
        )

        data = result.get("data")
        if not data:
            return None

        # 解包：header + body0.datasource
        return {
            "header": data.get("header"),
            "body": data.get("body0", {}).get("datasource", []),
            "billtype": data.get("billtype"),
        }

    async def get_bill_subtypes(self) -> List[Dict[str, Any]]:
        """
        获取所有单据子类型列表（用于发现可用单据类型）

        Returns:
            单据子类型列表
        """
        result = await self.call(
            "cc.erp.bll.bas.basebillsubtype.getlist",
        )
        if isinstance(result, dict):
            data = result.get("data", {})
            return data.get("datasource", data.get("rows", []))
        if isinstance(result, list):
            return result
        return []

    async def get_login_status(self) -> Dict[str, Any]:
        """
        获取登录状态信息（含菜单树、用户信息等）
        """
        result = await self.call(
            "CC.ERP.BLL.API.MessageServer.GetLoginStatusForDesktop",
        )
        return result.get("data", {})


class QLApiError(Exception):
    """QL361 API 业务错误"""


def _summarize_params(params: Optional[Dict[str, Any]]) -> str:
    """精简显示参数（避免日志过长）"""
    if not params:
        return "{}"
    keys = list(params.keys())
    return f"{{{', '.join(keys[:5])}}}"
