#!/usr/bin/env python3
"""
库存管理模块功能测试
Sprint 27+1 测试环境专项
测试内容：库存查询、预警、调拨、盘点、调整功能
"""

import pytest
import requests
import json
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any

# 测试配置
BASE_URL = "http://localhost:8080"
API_PREFIX = "/api/erp/stock"
TEST_TIMEOUT = 30

# 测试数据
TEST_WAREHOUSE_ID = "WH001"
TEST_PRODUCT_ID = "PROD001"
TEST_PRODUCT_SKU = "SKU001"

class TestInventoryQuery:
    """库存查询功能测试"""
    
    def test_inventory_overview_query(self):
        """INV-QUERY-001: 库存总览查询测试"""
        url = f"{BASE_URL}{API_PREFIX}/inventory/overview"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert "code" in data and data["code"] == 200
        assert "data" in data
        assert "totalSkuCount" in data["data"]
        assert "totalStockQuantity" in data["data"]
        assert "lowStockCount" in data["data"]
        print(f"✅ 库存总览查询成功: {data['data']}")
    
    def test_product_inventory_detail_query(self):
        """INV-QUERY-002: 商品库存明细查询测试"""
        url = f"{BASE_URL}{API_PREFIX}/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "productId" in data["data"]
        assert "sku" in data["data"]
        assert "availableQuantity" in data["data"]
        print(f"✅ 商品库存明细查询成功: {data['data']}")
    
    def test_warehouse_inventory_distribution_query(self):
        """INV-QUERY-003: 仓库库存分布查询测试"""
        url = f"{BASE_URL}{API_PREFIX}/inventory/warehouse/{TEST_WAREHOUSE_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "warehouseId" in data["data"]
        assert "products" in data["data"]
        print(f"✅ 仓库库存分布查询成功: 仓库{TEST_WAREHOUSE_ID}有{len(data['data']['products'])}种商品")
    
    def test_inventory_alert_query(self):
        """INV-QUERY-004: 库存预警查询测试"""
        url = f"{BASE_URL}{API_PREFIX}/inventory/alerts"
        params = {"alertLevel": "warning", "page": 1, "size": 20}
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "list" in data["data"]
        print(f"✅ 库存预警查询成功: 发现{len(data['data']['list'])}条预警记录")


class TestInventoryInbound:
    """入库管理功能测试"""
    
    def test_purchase_inbound(self):
        """INV-IN-001: 采购入库测试"""
        url = f"{BASE_URL}{API_PREFIX}/inbound/purchase"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "purchaseOrderNo": "PO20260426001",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 100,
                    "batchNo": "BATCH001",
                    "expiryDate": (datetime.now() + timedelta(days=365)).strftime("%Y-%m-%d")
                }
            ],
            "operator": "TEST_USER",
            "remark": "采购入库测试"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "inboundId" in data["data"]
        print(f"✅ 采购入库成功: 入库单号 {data['data']['inboundId']}")
    
    def test_production_inbound(self):
        """INV-IN-002: 生产入库测试"""
        url = f"{BASE_URL}{API_PREFIX}/inbound/production"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "productionOrderNo": "MO20260426001",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 50,
                    "batchNo": "PROD_BATCH001",
                    "qualityStatus": "qualified"
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 生产入库成功: 入库单号 {data['data']['inboundId']}")
    
    def test_return_inbound(self):
        """INV-IN-003: 退货入库测试"""
        url = f"{BASE_URL}{API_PREFIX}/inbound/return"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "returnOrderNo": "RO20260426001",
            "originalOrderNo": "SO20260425001",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 5,
                    "returnReason": "质量问题",
                    "qualityStatus": "unqualified"
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 退货入库成功: 入库单号 {data['data']['inboundId']}")


class TestInventoryOutbound:
    """出库管理功能测试"""
    
    def test_sales_outbound(self):
        """INV-OUT-001: 销售出库测试"""
        url = f"{BASE_URL}{API_PREFIX}/outbound/sales"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "salesOrderNo": "SO20260426001",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 10,
                    "priority": "normal"
                }
            ],
            "operator": "TEST_USER",
            "shippingAddress": "测试地址"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "outboundId" in data["data"]
        print(f"✅ 销售出库成功: 出库单号 {data['data']['outboundId']}")
    
    def test_production_outbound(self):
        """INV-OUT-002: 生产出库测试"""
        url = f"{BASE_URL}{API_PREFIX}/outbound/production"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "productionOrderNo": "MO20260426002",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 20,
                    "usage": "生产领料"
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 生产出库成功: 出库单号 {data['data']['outboundId']}")
    
    def test_transfer_outbound(self):
        """INV-OUT-003: 调拨出库测试"""
        url = f"{BASE_URL}{API_PREFIX}/outbound/transfer"
        payload = {
            "fromWarehouseId": TEST_WAREHOUSE_ID,
            "toWarehouseId": "WH002",
            "transferOrderNo": "TO20260426001",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 30
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 调拨出库成功: 出库单号 {data['data']['outboundId']}")


class TestInventoryCheck:
    """库存盘点功能测试"""
    
    def test_check_order_create(self):
        """INV-CHECK-001: 盘点单创建测试"""
        url = f"{BASE_URL}{API_PREFIX}/check"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "checkType": "full",
            "checkScope": "all_products",
            "plannedDate": datetime.now().strftime("%Y-%m-%d"),
            "operator": "TEST_USER",
            "remark": "月度全盘"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "checkId" in data["data"]
        self.check_id = data["data"]["checkId"]
        print(f"✅ 盘点单创建成功: 盘点单号 {self.check_id}")
        return self.check_id
    
    def test_check_data_entry(self):
        """INV-CHECK-002: 盘点数据录入测试"""
        check_id = self.test_check_order_create()
        url = f"{BASE_URL}{API_PREFIX}/check/{check_id}/entry"
        payload = {
            "entries": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "systemQuantity": 100,
                    "actualQuantity": 98,
                    "difference": -2,
                    "reason": "损耗"
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 盘点数据录入成功: 录入{len(payload['entries'])}条记录")
    
    def test_check_difference_handle(self):
        """INV-CHECK-003: 盘点差异处理测试"""
        url = f"{BASE_URL}{API_PREFIX}/check/differences"
        params = {"warehouseId": TEST_WAREHOUSE_ID, "status": "pending"}
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        print(f"✅ 盘点差异查询成功: 发现{len(data['data'])}条待处理差异")
    
    def test_check_adjustment(self):
        """INV-CHECK-004: 盘点盈亏调整测试"""
        url = f"{BASE_URL}{API_PREFIX}/check/adjust"
        payload = {
            "adjustments": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "adjustQuantity": 2,
                    "adjustType": "profit",
                    "reason": "盘点盈余"
                }
            ],
            "operator": "TEST_USER",
            "approvedBy": "MANAGER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 盘点盈亏调整成功")


class TestInventoryTransfer:
    """库存调拨功能测试"""
    
    def test_transfer_order_create(self):
        """INV-TRANS-001: 调拨单创建测试"""
        url = f"{BASE_URL}{API_PREFIX}/transfer"
        payload = {
            "fromWarehouseId": TEST_WAREHOUSE_ID,
            "toWarehouseId": "WH002",
            "transferType": "direct",
            "items": [
                {
                    "productId": TEST_PRODUCT_ID,
                    "sku": TEST_PRODUCT_SKU,
                    "quantity": 50,
                    "remark": "库存调拨测试"
                }
            ],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "transferId" in data["data"]
        self.transfer_id = data["data"]["transferId"]
        print(f"✅ 调拨单创建成功: 调拨单号 {self.transfer_id}")
        return self.transfer_id
    
    def test_transfer_outbound_confirm(self):
        """INV-TRANS-002: 调拨出库确认测试"""
        transfer_id = self.test_transfer_order_create()
        url = f"{BASE_URL}{API_PREFIX}/transfer/{transfer_id}/outbound"
        payload = {
            "operator": "TEST_USER",
            "outboundTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 调拨出库确认成功")
    
    def test_transfer_inbound_confirm(self):
        """INV-TRANS-003: 调拨入库确认测试"""
        url = f"{BASE_URL}{API_PREFIX}/transfer/inbound"
        payload = {
            "transferId": "TRANS001",
            "operator": "TEST_USER",
            "inboundTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "actualQuantity": 50
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 调拨入库确认成功")
    
    def test_transfer_status_tracking(self):
        """INV-TRANS-004: 调拨状态跟踪测试"""
        url = f"{BASE_URL}{API_PREFIX}/transfer/status"
        params = {"transferId": "TRANS001"}
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "status" in data["data"]
        assert "statusHistory" in data["data"]
        print(f"✅ 调拨状态跟踪成功: 当前状态 {data['data']['status']}")


class TestInventoryAlert:
    """库存预警功能测试"""
    
    def test_alert_threshold_config(self):
        """测试库存预警阈值配置"""
        url = f"{BASE_URL}{API_PREFIX}/alert/config"
        payload = {
            "productId": TEST_PRODUCT_ID,
            "sku": TEST_PRODUCT_SKU,
            "minStock": 10,
            "maxStock": 1000,
            "reorderPoint": 20,
            "reorderQuantity": 50,
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        print(f"✅ 库存预警阈值配置成功")
    
    def test_alert_notification(self):
        """测试库存预警通知"""
        url = f"{BASE_URL}{API_PREFIX}/alert/notifications"
        params = {"status": "unread", "page": 1, "size": 10}
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        print(f"✅ 库存预警通知查询成功: {len(data['data'].get('list', []))}条未读通知")


class TestInventoryIntegration:
    """库存集成测试 - 与订单、采购模块集成"""
    
    def test_inventory_order_integration(self):
        """测试库存与订单模块集成"""
        # 1. 查询当前库存
        url = f"{BASE_URL}{API_PREFIX}/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        initial_stock = response.json()["data"]["availableQuantity"]
        print(f"初始库存: {initial_stock}")
        
        # 2. 模拟订单扣减库存（通过出库）
        url = f"{BASE_URL}{API_PREFIX}/outbound/sales"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "salesOrderNo": "SO_TEST_001",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 5}],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        
        # 3. 验证库存已扣减
        url = f"{BASE_URL}{API_PREFIX}/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        current_stock = response.json()["data"]["availableQuantity"]
        
        assert current_stock == initial_stock - 5, f"库存扣减失败: 期望{initial_stock-5}, 实际{current_stock}"
        print(f"✅ 库存与订单集成测试成功: 库存从{initial_stock}扣减到{current_stock}")
    
    def test_inventory_purchase_integration(self):
        """测试库存与采购模块集成"""
        # 1. 查询当前库存
        url = f"{BASE_URL}{API_PREFIX}/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        initial_stock = response.json()["data"]["availableQuantity"]
        
        # 2. 模拟采购入库
        url = f"{BASE_URL}{API_PREFIX}/inbound/purchase"
        payload = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "purchaseOrderNo": "PO_TEST_001",
            "items": [{"productId": TEST_PRODUCT_ID, "sku": TEST_PRODUCT_SKU, "quantity": 20}],
            "operator": "TEST_USER"
        }
        response = requests.post(url, json=payload, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        
        # 3. 验证库存已增加
        url = f"{BASE_URL}{API_PREFIX}/inventory/product/{TEST_PRODUCT_ID}"
        response = requests.get(url, timeout=TEST_TIMEOUT)
        assert response.status_code == 200
        current_stock = response.json()["data"]["availableQuantity"]
        
        assert current_stock == initial_stock + 20, f"库存增加失败: 期望{initial_stock+20}, 实际{current_stock}"
        print(f"✅ 库存与采购集成测试成功: 库存从{initial_stock}增加到{current_stock}")


class TestInventoryReport:
    """库存报表统计功能测试"""
    
    def test_inventory_summary_report(self):
        """测试库存汇总报表"""
        url = f"{BASE_URL}{API_PREFIX}/report/summary"
        params = {"warehouseId": TEST_WAREHOUSE_ID, "date": datetime.now().strftime("%Y-%m-%d")}
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "totalSku" in data["data"]
        assert "totalQuantity" in data["data"]
        assert "totalValue" in data["data"]
        print(f"✅ 库存汇总报表查询成功: {data['data']}")
    
    def test_inventory_inout_report(self):
        """测试库存出入库报表"""
        url = f"{BASE_URL}{API_PREFIX}/report/inout"
        params = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "startDate": (datetime.now() - timedelta(days=7)).strftime("%Y-%m-%d"),
            "endDate": datetime.now().strftime("%Y-%m-%d")
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        assert "inbound" in data["data"]
        assert "outbound" in data["data"]
        print(f"✅ 库存出入库报表查询成功")
    
    def test_inventory_turnover_report(self):
        """测试库存周转率报表"""
        url = f"{BASE_URL}{API_PREFIX}/report/turnover"
        params = {
            "warehouseId": TEST_WAREHOUSE_ID,
            "period": "month",
            "year": datetime.now().year,
            "month": datetime.now().month
        }
        response = requests.get(url, params=params, timeout=TEST_TIMEOUT)
        
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "data" in data
        print(f"✅ 库存周转率报表查询成功")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])