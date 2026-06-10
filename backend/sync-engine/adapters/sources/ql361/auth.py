"""
QL361 认证模块

处理来肯云商的登录认证流程:
1. 账号密码登录获取 token/sign（新版 JSON 响应）
2. 子域名认证（访问 desktop.html 带 token_/verify_ 参数）
3. 管理认证令牌

新 API 端点: 22stable.ql361.com/v_22stable/api.cc
认证方式: URL 查询参数 &s=xxx&tok=xxx
"""
from __future__ import annotations

import json
import time
from typing import Dict, Optional, Tuple

import httpx
from loguru import logger


class QLAuthError(Exception):
    """认证相关错误"""


class QLAuthManager:
    """
    来肯云商认证管理器

    维护登录会话，自动处理认证和令牌刷新。

    Args:
        base_url: 主站地址（默认 https://www.ql361.com）
        username: 登录账号
        password: 登录密码
        timeout: HTTP 请求超时秒数

    Examples:
        >>> auth = QLAuthManager(username="xxx", password="yyy")
        >>> await auth.login()
        >>> auth.get_request_params()
        {"s": "...", "tok": "...", "pagetype": "crmoa", "from": "allinoneclient", "platform": "pc"}
    """

    def __init__(
        self,
        username: str,
        password: str,
        base_url: str = "https://www.ql361.com",
        timeout: float = 30.0,
    ):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.timeout = timeout

        self._client: Optional[httpx.AsyncClient] = None
        self._tok: Optional[str] = None
        self._s: Optional[str] = None
        self._subdomain: Optional[str] = None
        self._version_path: Optional[str] = None
        self._last_login: float = 0.0

    @property
    def client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(self.timeout),
                follow_redirects=True,
                headers={
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/120.0.0.0 Safari/537.36"
                    ),
                },
            )
        return self._client

    async def login(self, force: bool = False) -> None:
        """
        执行完整登录流程

        Args:
            force: 是否强制重新登录（忽略缓存）

        Raises:
            QLAuthError: 登录失败
        """
        if not force and self._tok and self._s and (time.time() - self._last_login < 600):
            logger.debug("使用缓存的登录会话")
            return

        logger.info("开始登录 [{}]", self.username)

        try:
            # Step 1: 账号密码登录获取 token/sign
            tok, sign, subdomain, version_path = await self._step1_login()

            self._tok = tok
            self._s = sign
            self._subdomain = subdomain
            self._version_path = version_path

            # Step 2: 子域名认证（访问 desktop.html 带 URL 参数）
            await self._step2_subdomain_auth(tok, sign)

            self._last_login = time.time()
            logger.info("登录成功 [{}]", self.subdomain)

        except Exception as e:
            raise QLAuthError(f"登录失败: {e}") from e

    async def _step1_login(self) -> Tuple[str, str, str, str]:
        """登录获取 token/sign 和子域名信息"""
        url = f"{self.base_url}/account/Auth"
        data = {
            "username": self.username,
            "password": self.password,
            "rememberMe": "true",
        }
        headers = {
            "X-Requested-With": "XMLHttpRequest",
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        }

        response = await self.client.post(url, data=data, headers=headers)
        body = response.text.strip()

        # 新版 JSON 响应
        try:
            resp = json.loads(body)
        except json.JSONDecodeError:
            raise QLAuthError(f"登录返回非 JSON: {body[:200]}")

        if not resp.get("success"):
            raise QLAuthError(f"登录失败: {body[:200]}")

        token = resp.get("token", "")
        sign = resp.get("sign", "")
        company = resp.get("companydata", {})

        # 子域名来自 releaseversion
        release_version = company.get("releaseversion", "")
        if not release_version:
            raise QLAuthError("无法获取 releaseversion")

        subdomain = release_version  # e.g. "22stable"
        version_path = f"v_{release_version}"  # e.g. "v_22stable"

        if not token or not sign:
            raise QLAuthError("登录后无法获取 token/sign")

        return token, sign, subdomain, version_path

    async def _step2_subdomain_auth(self, token: str, sign: str) -> None:
        """
        子域名认证

        访问子域的 desktop.html 并传递 token_/verify_ 参数，
        确保 WAF cookie (acw_tc) 设置到子域名。
        """
        if not self._subdomain:
            return

        auth_url = (
            f"https://{self._subdomain}.ql361.com/desktop.html"
            f"?token_={token}&verify_={sign}#/"
        )
        try:
            await self.client.get(auth_url)
            logger.debug("子域名认证完成")
        except Exception as e:
            logger.warning("子域名认证请求失败（不影响 API 调用）: {}", e)

    @property
    def api_base(self) -> str:
        """API 基地址（含子域名和版本路径）"""
        if not self._subdomain:
            raise QLAuthError("尚未登录，无法获取 API 地址")
        return f"https://{self._subdomain}.ql361.com/{self._version_path}"

    @property
    def subdomain(self) -> str:
        return self._subdomain or "?"

    def get_api_url(self, method: str) -> str:
        """
        构造完整的 API 请求 URL

        Args:
            method: API 方法名，如 'cc.erp.bll.bill.billbllmanager.getdatalist'
        """
        if not self._subdomain:
            raise QLAuthError("尚未登录")

        # URL: https://{sub}.ql361.com/v_22stable/api.cc?___method={method}&pagetype=...
        return (
            f"{self.api_base}/api.cc"
            f"?___method={method}"
            f"&pagetype=crmoa"
            f"&from=allinoneclient"
            f"&platform=pc"
        )

    def get_request_params(self) -> Dict[str, str]:
        """
        获取需要在 API 请求 URL 中携带的认证参数
        """
        params: Dict[str, str] = {}
        if self._s:
            params["s"] = self._s
        if self._tok:
            params["tok"] = self._tok
        return params

    async def ensure_logged_in(self) -> None:
        """
        确保会话有效，必要时重新登录

        API 认证通过 URL 参数传递，无需频繁重新登录。
        仅在 tok 或 sign 缺失时重新获取。
        """
        if not self._tok or not self._s:
            logger.info("认证信息缺失，重新登录")
            await self.login(force=True)
        elif time.time() - self._last_login > 1800:
            logger.info("会话可能已过期（超过 30 分钟），重新登录")
            await self.login(force=True)

    async def close(self) -> None:
        """关闭 HTTP 客户端"""
        if self._client:
            await self._client.aclose()
            self._client = None
