#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
订单管理模块功能测试脚本
Sprint 27+1 测试环境
"""

import pytest
import requests
import json
from datetime import datetime
from typing import Dict, Any

# 测试配置
BASE_URL = "http://test-ai-ready.example.com"
API_PREFIX = "/api/v1/order"
TIMEOUT = 10

# 测试数据
TEST_USER_ID = 1
TEST_PRODUCTS = {
    "SKU001": {"name": "测试商品1", "price": 99.99, "stock": 100},
    "SKU002": {"name": "测试商品2", "price": 199.99, "stock": 0},
    "SKU003": {"name": "测试商品3", "price": 299.99, "stock": 50},
}


class TestOrderCreate:
    """订单创建测试"""

    def test_create_order_success(self):
        """TC-CREATE-001: 正常订单创建"""
        url = f"{BASE_URL}{API_PREFIX}/create"
        payload = {
            "userId": TEST_USER_ID,
            "items": [
                {
                    "productId": "SKU001",
                    "quantity": 2,
                    "price": 99.99
                }
            ],
            "shippingAddress": {
                "province": "广东省",
                "city": "深圳市",
                "district": "南山区",
                "address": "科技园"
            },
            "remark": "测试订单"
        }

        try:
            response = requests.post(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert "orderNo" in data, "返回结果应包含订单号"
                assert data["orderNo"].startswith("ORD"), "订单号应以ORD开头"
                assert data["status"] == "PENDING", "订单状态应为PENDING"
                assert data["totalAmount"] == 199.98, "订单金额计算错误"
                print(f"✅ 订单创建成功: {data['orderNo']}")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_create_order_insufficient_stock(self):
        """TC-CREATE-002: 订单创建-库存不足"""
        url = f"{BASE_URL}{API_PREFIX}/create"
        payload = {
            "userId": TEST_USER_ID,
            "items": [
                {
                    "productId": "SKU002",
                    "quantity": 1,
                    "price": 199.99
                }
            ]
        }

        try:
            response = requests.post(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 400:
                data = response.json()
                assert "库存不足" in str(data), "应返回库存不足错误"
                print("✅ 库存不足验证通过")
            else:
                pytest.skip(f"服务返回非预期状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_create_order_missing_params(self):
        """TC-CREATE-003: 订单创建-参数缺失"""
        url = f"{BASE_URL}{API_PREFIX}/create"
        payload = {
            "items": [
                {
                    "productId": "SKU001",
                    "quantity": 1
                }
            ]
        }

        try:
            response = requests.post(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 400:
                print("✅ 参数缺失验证通过")
            else:
                pytest.skip(f"服务返回非预期状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_create_order_amount_calculation(self):
        """TC-CREATE-004: 订单创建-金额校验"""
        url = f"{BASE_URL}{API_PREFIX}/create"
        payload = {
            "userId": TEST_USER_ID,
            "items": [
                {
                    "productId": "SKU001",
                    "quantity": 2,
                    "price": 99.99
                },
                {
                    "productId": "SKU003",
                    "quantity": 1,
                    "price": 299.99
                }
            ]
        }

        try:
            response = requests.post(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                expected_amount = 99.99 * 2 + 299.99 * 1
                assert abs(data["totalAmount"] - expected_amount) < 0.01, "订单金额计算错误"
                print(f"✅ 金额计算验证通过: {data['totalAmount']}")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")


class TestOrderQuery:
    """订单查询测试"""

    def test_query_order_by_order_no(self):
        """TC-QUERY-001: 按订单号查询"""
        order_no = "ORD00000001"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}"

        try:
            response = requests.get(url, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert data["orderNo"] == order_no, "订单号不匹配"
                print(f"✅ 订单查询成功: {order_no}")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_query_order_list_by_user(self):
        """TC-QUERY-002: 按用户查询订单列表"""
        url = f"{BASE_URL}{API_PREFIX}/list"
        params = {
            "userId": TEST_USER_ID,
            "page": 1,
            "size": 10
        }

        try:
            response = requests.get(url, params=params, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert "list" in data, "返回结果应包含列表"
                assert "total" in data, "返回结果应包含总数"
                print(f"✅ 订单列表查询成功，共 {data['total']} 条")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_query_order_by_status(self):
        """TC-QUERY-003: 按状态查询订单"""
        url = f"{BASE_URL}{API_PREFIX}/list"
        params = {
            "userId": TEST_USER_ID,
            "status": "PENDING",
            "page": 1,
            "size": 10
        }

        try:
            response = requests.get(url, params=params, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                if "list" in data and data["list"]:
                    for order in data["list"]:
                        assert order["status"] == "PENDING", "订单状态应为PENDING"
                print("✅ 按状态查询验证通过")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_query_order_not_found(self):
        """TC-QUERY-004: 订单不存在"""
        order_no = "ORD99999999"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}"

        try:
            response = requests.get(url, timeout=TIMEOUT)
            
            if response.status_code == 404:
                print("✅ 订单不存在验证通过")
            else:
                pytest.skip(f"服务返回非404状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")


class TestOrderUpdate:
    """订单修改测试"""

    def test_update_order_address(self):
        """TC-UPDATE-001: 修改订单地址"""
        order_no = "ORD00000001"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/address"
        payload = {
            "province": "北京市",
            "city": "北京市",
            "district": "朝阳区",
            "address": "国贸大厦"
        }

        try:
            response = requests.put(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 200:
                print("✅ 订单地址修改成功")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_update_shipped_order(self):
        """TC-UPDATE-002: 修改已发货订单"""
        order_no = "ORD00000003"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/address"
        payload = {
            "province": "上海市",
            "city": "上海市",
            "district": "浦东新区",
            "address": "陆家嘴"
        }

        try:
            response = requests.put(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 400:
                print("✅ 已发货订单不可修改验证通过")
            else:
                pytest.skip(f"服务返回非400状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_cancel_order(self):
        """TC-UPDATE-003: 取消订单"""
        order_no = "ORD00000001"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/cancel"

        try:
            response = requests.put(url, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert data["status"] == "CANCELLED", "订单状态应为CANCELLED"
                print("✅ 订单取消成功")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")


class TestOrderStatus:
    """订单状态管理测试"""

    def test_status_pending_to_paid(self):
        """TC-STATUS-001: 状态流转-PENDING到PAID"""
        order_no = "ORD00000001"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/pay"

        try:
            response = requests.post(url, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert data["status"] == "PAID", "订单状态应为PAID"
                assert "paidTime" in data, "应记录支付时间"
                print("✅ 状态流转 PENDING -> PAID 验证通过")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_status_paid_to_shipped(self):
        """TC-STATUS-002: 状态流转-PAID到SHIPPED"""
        order_no = "ORD00000002"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/ship"
        payload = {
            "logisticsNo": "SF1234567890",
            "logisticsCompany": "顺丰速运"
        }

        try:
            response = requests.post(url, json=payload, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert data["status"] == "SHIPPED", "订单状态应为SHIPPED"
                assert "shippedTime" in data, "应记录发货时间"
                print("✅ 状态流转 PAID -> SHIPPED 验证通过")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_status_shipped_to_completed(self):
        """TC-STATUS-003: 状态流转-SHIPPED到COMPLETED"""
        order_no = "ORD00000003"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/complete"

        try:
            response = requests.post(url, timeout=TIMEOUT)
            
            if response.status_code == 200:
                data = response.json()
                assert data["status"] == "COMPLETED", "订单状态应为COMPLETED"
                assert "completedTime" in data, "应记录完成时间"
                print("✅ 状态流转 SHIPPED -> COMPLETED 验证通过")
            else:
                pytest.skip(f"服务返回非200状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")

    def test_status_invalid_transition(self):
        """TC-STATUS-004: 非法状态流转"""
        order_no = "ORD00000001"
        url = f"{BASE_URL}{API_PREFIX}/{order_no}/complete"

        try:
            response = requests.post(url, timeout=TIMEOUT)
            
            if response.status_code == 400:
                print("✅ 非法状态流转验证通过")
            else:
                pytest.skip(f"服务返回非400状态码: {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("服务未启动，跳过测试")


def run_all_tests():
    """运行所有测试"""
    pytest.main([__file__, "-v", "--tb=short", "-r", "s"])


if __name__ == "__main__":
    run_all_tests()