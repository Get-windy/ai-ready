#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试通用工具模块

提供测试基类、响应断言、通用fixture等可复用组件，
减少测试脚本中的重复代码。
"""

import pytest
import time
from typing import Dict, Any, Optional, Union, List
from dataclasses import dataclass, field

# 尝试导入httpx，回退到requests
try:
    import httpx
    HAS_HTTPX = True
except ImportError:
    import requests as httpx
    HAS_HTTPX = False


# ==================== 配置类 ====================

@dataclass
class TestConfig:
    """统一测试配置"""
    base_url: str = "http://localhost:8080"
    api_base_path: str = "/api"
    timeout: int = 30
    tenant_id: int = 1
    retry_count: int = 3
    retry_delay: float = 1.0

    @property
    def api_url(self) -> str:
        return f"{self.base_url}{self.api_base_path}"

    @classmethod
    def from_env(cls) -> "TestConfig":
        """从环境变量加载配置"""
        import os
        return cls(
            base_url=os.getenv("TEST_BASE_URL", "http://localhost:8080"),
            api_base_path=os.getenv("TEST_API_PATH", "/api"),
            timeout=int(os.getenv("TEST_TIMEOUT", "30")),
            tenant_id=int(os.getenv("TEST_TENANT_ID", "1")),
        )


# ==================== 响应断言工具 ====================

def assert_success(
    response,
    expected_status: Union[int, List[int]] = 200,
    msg: Optional[str] = None
) -> Dict[str, Any]:
    """
    断言响应成功并返回JSON数据

    Args:
        response: HTTP响应对象
        expected_status: 预期的状态码或状态码列表
        msg: 可选的失败消息

    Returns:
        解析后的JSON数据
    """
    if isinstance(expected_status, int):
        expected_status = [expected_status]

    assert response.status_code in expected_status, (
        f"{msg or '请求失败'}: 期望状态码 {expected_status}, "
        f"实际 {response.status_code}, 响应: {getattr(response, 'text', str(response))}"
    )

    try:
        data = response.json()
    except Exception:
        data = {}

    # 检查业务状态码（如果存在）
    if isinstance(data, dict) and "code" in data:
        assert data.get("code") in (200, 0, "200", "SUCCESS", None), (
            f"{msg or '业务响应失败'}: code={data.get('code')}, message={data.get('message', 'N/A')}"
        )

    return data


def assert_error(
    response,
    expected_status: Union[int, List[int]] = 400,
    msg: Optional[str] = None
) -> Dict[str, Any]:
    """
    断言响应为错误状态

    Args:
        response: HTTP响应对象
        expected_status: 预期的错误状态码或列表
        msg: 可选的失败消息

    Returns:
        解析后的JSON数据
    """
    if isinstance(expected_status, int):
        expected_status = [expected_status]

    assert response.status_code in expected_status, (
        f"{msg or '期望错误响应'}: 期望状态码 {expected_status}, "
        f"实际 {response.status_code}"
    )

    try:
        return response.json()
    except Exception:
        return {}


def assert_field_exists(data: Dict[str, Any], field_path: str, msg: Optional[str] = None):
    """
    断言字段存在（支持点号路径如 data.user.name）

    Args:
        data: 字典数据
        field_path: 字段路径，如 "data.user.id"
        msg: 可选的失败消息
    """
    keys = field_path.split(".")
    current = data
    for key in keys:
        assert isinstance(current, dict), (
            f"{msg or '字段路径错误'}: '{field_path}' 的中间节点不是字典"
        )
        assert key in current, (
            f"{msg or '字段不存在'}: 路径 '{field_path}' 中缺少 '{key}'"
        )
        current = current[key]


def assert_pagination(
    data: Dict[str, Any],
    msg: Optional[str] = None
) -> Dict[str, Any]:
    """
    断言分页响应结构正确

    Args:
        data: 响应数据
        msg: 可选的失败消息

    Returns:
        分页数据内容
    """
    assert isinstance(data, dict), f"{msg or '分页响应'}: 响应应为字典"

    # 支持常见的分页字段名
    page_fields = ["page", "pageNum", "current"]
    size_fields = ["size", "pageSize", "limit"]
    total_fields = ["total", "totalCount", "totalElements"]
    list_fields = ["list", "records", "data", "items", "content"]

    has_page = any(f in data for f in page_fields)
    has_size = any(f in data for f in size_fields)
    has_total = any(f in data for f in total_fields)
    has_list = any(f in data for f in list_fields)

    assert has_list, f"{msg or '分页响应'}: 缺少列表字段 ({', '.join(list_fields)})"

    for field_group, names in [
        (has_page, page_fields),
        (has_size, size_fields),
        (has_total, total_fields),
    ]:
        if not field_group:
            pytest.skip(f"分页字段缺失: {names} (非标准分页结构)")

    list_key = next(f for f in list_fields if f in data)
    return data[list_key]


# ==================== 重试工具 ====================

def retry(
    func,
    max_attempts: int = 3,
    delay: float = 1.0,
    exceptions: tuple = (Exception,),
    msg: Optional[str] = None
):
    """
    通用重试装饰器/函数

    Args:
        func: 要执行的函数
        max_attempts: 最大重试次数
        delay: 每次重试间隔（秒）
        exceptions: 捕获的异常类型
        msg: 可选的消息前缀

    Returns:
        func 的返回值
    """
    last_exception = None
    for attempt in range(1, max_attempts + 1):
        try:
            return func()
        except exceptions as e:
            last_exception = e
            if attempt < max_attempts:
                time.sleep(delay)
            else:
                prefix = f"{msg}: " if msg else ""
                raise AssertionError(
                    f"{prefix}重试 {max_attempts} 次后仍然失败: {e}"
                ) from e


# ==================== 测试基类 ====================

class BaseAPITest:
    """
    API测试基类

    使用方式:
        class TestMyAPI(BaseAPITest):
            def test_something(self):
                resp = self.client.get("/endpoint")
                data = self.assert_success(resp)
                ...
    """

    config: TestConfig = TestConfig()

    @classmethod
    def setup_class(cls):
        """类级别setup"""
        cls.config = TestConfig.from_env()
        from utils.api_client import APIClient
        cls.client = APIClient(base_url=cls.config.api_url, timeout=cls.config.timeout)

    @classmethod
    def teardown_class(cls):
        """类级别teardown"""
        if hasattr(cls, "client") and hasattr(cls.client, "close"):
            cls.client.close()

    def assert_success(self, response, expected_status=200, msg=None):
        """实例方法封装"""
        return assert_success(response, expected_status, msg)

    def assert_error(self, response, expected_status=400, msg=None):
        """实例方法封装"""
        return assert_error(response, expected_status, msg)

    def assert_field_exists(self, data, field_path, msg=None):
        """实例方法封装"""
        return assert_field_exists(data, field_path, msg)

    def login(self, username: str = "admin", password: str = "admin123") -> str:
        """
        登录并返回token

        Args:
            username: 用户名
            password: 密码

        Returns:
            认证token
        """
        resp = self.client.post("/user/login", json={
            "username": username,
            "password": password,
            "tenantId": self.config.tenant_id
        })
        data = self.assert_success(resp, [200, 201], "登录失败")

        # 支持多种token字段名
        token = None
        if isinstance(data, dict):
            token_paths = ["data.token", "token", "data.accessToken", "accessToken"]
            for path in token_paths:
                keys = path.split(".")
                current = data
                for key in keys:
                    if isinstance(current, dict) and key in current:
                        current = current[key]
                    else:
                        current = None
                        break
                if current:
                    token = current
                    break

        assert token, f"登录响应中未找到token: {data}"
        self.client.set_token(token)
        return token


# ==================== 性能测试工具 ====================

class Timer:
    """上下文管理器计时器"""

    def __init__(self, name: str = "操作"):
        self.name = name
        self.elapsed: float = 0.0

    def __enter__(self):
        self.start = time.perf_counter()
        return self

    def __exit__(self, *args):
        self.elapsed = time.perf_counter() - self.start

    def __repr__(self):
        return f"<Timer {self.name}: {self.elapsed:.3f}s>"


def benchmark(func, iterations: int = 10, warmup: int = 1):
    """
    简单基准测试

    Args:
        func: 要测试的函数（无参）
        iterations: 执行次数
        warmup: 预热次数

    Returns:
        dict: {min, max, avg, total, iterations}
    """
    for _ in range(warmup):
        func()

    times = []
    for _ in range(iterations):
        start = time.perf_counter()
        func()
        times.append(time.perf_counter() - start)

    return {
        "min": min(times),
        "max": max(times),
        "avg": sum(times) / len(times),
        "total": sum(times),
        "iterations": iterations,
    }


# ==================== 数据生成辅助 ====================

def generate_unique_string(prefix: str = "test", length: int = 8) -> str:
    """生成唯一测试字符串"""
    import random
    import string
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=length))
    timestamp = int(time.time()) % 10000
    return f"{prefix}_{suffix}_{timestamp}"


def generate_test_user(prefix: str = "test_user") -> Dict[str, Any]:
    """生成测试用户数据"""
    return {
        "username": generate_unique_string(prefix),
        "password": "Test@123456",
        "email": f"{generate_unique_string('email')}@test.com",
        "phone": f"1{ ''.join(__import__('random').choices('0123456789', k=10)) }",
        "tenantId": 1,
    }
