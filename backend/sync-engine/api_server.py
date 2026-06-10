"""
同步引擎 HTTP API

供 Java Spring Boot 后端调用的管理接口。
提供健康检查、触发同步、测试连接等功能。

默认监听 127.0.0.1:9800，Java 后端通过此端口通信。
"""
from __future__ import annotations

import json
from typing import Any, Dict

from aiohttp import web
from loguru import logger

from config import settings


async def health_check(request: web.Request) -> web.Response:
    """健康检查"""
    return web.json_response({
        "status": "ok",
        "version": "0.1.0",
        "mode": "daemon",
    })


async def trigger_sync(request: web.Request) -> web.Response:
    """
    手动触发同步

    POST /api/sync/trigger
    Body: {"config_id": 1, "sync_type": "incremental"}
    """
    try:
        body = await request.json()
    except Exception:
        return web.json_response(
            {"success": False, "message": "请求体必须是 JSON"},
            status=400,
        )

    config_id = body.get("config_id")
    sync_type = body.get("sync_type", "incremental")

    if not config_id:
        return web.json_response(
            {"success": False, "message": "缺少 config_id"},
            status=400,
        )

    logger.info("HTTP 触发同步: config_id={}, type={}", config_id, sync_type)

    # 将任务交给后台执行
    # TODO: 通过 asyncio.create_task 或消息队列异步执行
    # 目前记录日志返回，实际同步由调度器下一次轮询执行
    return web.json_response({
        "success": True,
        "message": "同步请求已接收，将在下一次调度周期执行",
        "config_id": config_id,
        "sync_type": sync_type,
    })


async def test_connection(request: web.Request) -> web.Response:
    """
    测试外部系统连接

    POST /api/sync/test-connection
    Body: {"source_type": "ql361", "username": "...", "password": "...", "base_url": "..."}
    """
    try:
        body = await request.json()
    except Exception:
        return web.json_response(
            {"success": False, "message": "请求体必须是 JSON"},
            status=400,
        )

    source_type = body.get("source_type", "ql361")
    username = body.get("username", "")
    password = body.get("password", "")
    base_url = body.get("base_url", "https://www.ql361.com")

    if not username or not password:
        return web.json_response(
            {"success": False, "message": "缺少 username 或 password"},
            status=400,
        )

    import time
    start = time.monotonic()

    try:
        if source_type == "ql361":
            from adapters.sources.ql361.auth import QLAuthManager
            from adapters.sources.ql361.client import QLAPIClient

            auth = QLAuthManager(
                username=username,
                password=password,
                base_url=base_url,
                timeout=15.0,
            )
            await auth.login()

            # 验证 API 可达性
            client = QLAPIClient(auth)
            try:
                status = await client.get_login_status()
                api_ok = bool(status.get("user"))
                company = status.get("user", {}).get("companyname", "")
            except Exception as api_e:
                api_ok = False
                company = str(api_e)

            latency = int((time.monotonic() - start) * 1000)
            subdomain = auth.subdomain

            await auth.close()

            return web.json_response({
                "connected": api_ok,
                "message": f"连接成功（子域名: {subdomain}，公司: {company}）" if api_ok else f"登录成功但 API 访问异常: {company}",
                "latency": latency,
                "subdomain": subdomain,
            })
        else:
            return web.json_response({
                "connected": False,
                "message": f"不支持的来源类型: {source_type}",
            })

    except Exception as e:
        latency = int((time.monotonic() - start) * 1000)
        logger.warning("测试连接失败: {}", e)
        return web.json_response({
            "connected": False,
            "message": f"连接失败: {e}",
            "latency": latency,
        })


async def get_report_names(request: web.Request) -> web.Response:
    """
    测试各单据类型的 API 可访问性

    POST /api/sync/report-names
    Body: {"source_type": "ql361", "username": "...", "password": "..."}
    """
    try:
        body = await request.json()
    except Exception:
        return web.json_response(
            {"success": False, "message": "请求体必须是 JSON"},
            status=400,
        )

    username = body.get("username", "")
    password = body.get("password", "")

    if not username or not password:
        return web.json_response(
            {"success": False, "message": "缺少 username 或 password"},
            status=400,
        )

    try:
        from adapters.sources.ql361.auth import QLAuthManager
        from adapters.sources.ql361.client import QLAPIClient

        auth = QLAuthManager(username=username, password=password, timeout=15.0)
        await auth.login()

        client = QLAPIClient(auth)

        # 测试各单据类型的可访问性
        bill_types = {
            601: "销售出库单",
            604: "销售订单",
            504: "采购订单",
            801: "收款单",
        }

        results = {}
        for bt, name in bill_types.items():
            try:
                data = await client.get_bill_list(bt, page=0, page_size=1)
                results[name] = {
                    "billtype": bt,
                    "accessible": True,
                    "total_rows": data.get("total", 0),
                }
            except Exception as e:
                results[name] = {
                    "billtype": bt,
                    "accessible": False,
                    "error": str(e),
                }

        # 获取用户信息
        login_status = await client.get_login_status()
        user = login_status.get("user", {})
        server_time = login_status.get("server_time", "")

        await auth.close()

        return web.json_response({
            "success": True,
            "bill_types": results,
            "user_info": {
                "company": user.get("companyname"),
                "username": user.get("username"),
                "version": user.get("productversion"),
                "expire_days": user.get("expireday"),
            },
            "server_time": server_time,
            "api_endpoint": f"{auth.subdomain}.ql361.com/v_{auth.subdomain}/api.cc",
        })

    except Exception as e:
        logger.warning("探测 reportname 失败: {}", e)
        return web.json_response({
            "success": False,
            "message": f"探测失败: {e}",
        })


def create_app() -> web.Application:
    """创建 aiohttp 应用"""
    app = web.Application()

    app.router.add_get("/health", health_check)
    app.router.add_post("/api/sync/trigger", trigger_sync)
    app.router.add_post("/api/sync/test-connection", test_connection)
    app.router.add_post("/api/sync/report-names", get_report_names)

    return app


async def run_api_server(host: str = "127.0.0.1", port: int = 9800) -> None:
    """启动 HTTP API 服务器"""
    app = create_app()
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, host, port)
    await site.start()
    logger.info("HTTP API 服务器已启动: http://{}:{}/health", host, port)
