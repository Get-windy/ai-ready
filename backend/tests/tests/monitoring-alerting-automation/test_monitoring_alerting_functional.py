"""
监控告警模块功能自动化测试
Sprint 28 - 测试用例自动化实现
"""

import pytest
import requests
import json
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional

# 测试配置
BASE_URL = "http://localhost:8080/api/v1"
TEST_USER = {"username": "test_admin", "password": "test_pass123"}


class TestAlertRuleEngine:
    """告警规则引擎功能测试"""
    
    @pytest.fixture
    def auth_headers(self):
        """获取认证头"""
        resp = requests.post(f"{BASE_URL}/auth/login", json=TEST_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    @pytest.fixture
    def sample_rule(self):
        """示例告警规则"""
        return {
            "name": f"测试规则_{int(time.time())}",
            "description": "自动化测试用规则",
            "metric": "cpu_usage",
            "condition": "gt",
            "threshold": 80,
            "duration": 60,
            "severity": "warning",
            "enabled": True,
            "notifications": ["email", "webhook"]
        }
    
    def test_create_alert_rule(self, auth_headers, sample_rule):
        """TC-MA-001: 创建告警规则"""
        response = requests.post(
            f"{BASE_URL}/alerting/rules",
            headers=auth_headers,
            json=sample_rule
        )
        assert response.status_code == 201
        data = response.json()
        assert data["name"] == sample_rule["name"]
        assert data["enabled"] == True
        assert "id" in data
        # 清理
        requests.delete(f"{BASE_URL}/alerting/rules/{data['id']}", headers=auth_headers)
    
    def test_edit_alert_rule(self, auth_headers, sample_rule):
        """TC-MA-002: 编辑告警规则"""
        # 先创建
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=sample_rule)
        rule_id = resp.json()["id"]
        
        # 修改
        update_data = {"threshold": 90, "description": "已修改的描述"}
        response = requests.put(
            f"{BASE_URL}/alerting/rules/{rule_id}",
            headers=auth_headers,
            json=update_data
        )
        assert response.status_code == 200
        data = response.json()
        assert data["threshold"] == 90
        assert data["description"] == "已修改的描述"
        
        # 清理
        requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_delete_alert_rule(self, auth_headers, sample_rule):
        """TC-MA-003: 删除告警规则"""
        # 创建
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=sample_rule)
        rule_id = resp.json()["id"]
        
        # 删除
        response = requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
        assert response.status_code == 204
        
        # 验证已删除
        get_resp = requests.get(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
        assert get_resp.status_code == 404
    
    def test_toggle_alert_rule(self, auth_headers, sample_rule):
        """TC-MA-004: 启用/禁用告警规则"""
        # 创建
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=sample_rule)
        rule_id = resp.json()["id"]
        
        # 禁用
        resp_disable = requests.patch(
            f"{BASE_URL}/alerting/rules/{rule_id}/disable",
            headers=auth_headers
        )
        assert resp_disable.status_code == 200
        assert resp_disable.json()["enabled"] == False
        
        # 启用
        resp_enable = requests.patch(
            f"{BASE_URL}/alerting/rules/{rule_id}/enable",
            headers=auth_headers
        )
        assert resp_enable.status_code == 200
        assert resp_enable.json()["enabled"] == True
        
        # 清理
        requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_alert_trigger(self, auth_headers):
        """TC-MA-005: 告警规则触发验证"""
        # 创建低阈值规则确保触发
        rule = {
            "name": f"触发测试_{int(time.time())}",
            "metric": "test_metric",
            "condition": "gt",
            "threshold": 1,
            "duration": 5,
            "severity": "critical",
            "enabled": True
        }
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
        rule_id = resp.json()["id"]
        
        # 模拟指标数据
        metric_data = {"metric": "test_metric", "value": 50, "timestamp": int(time.time())}
        requests.post(f"{BASE_URL}/metrics/push", headers=auth_headers, json=metric_data)
        
        # 等待检测周期
        time.sleep(6)
        
        # 检查告警
        alerts_resp = requests.get(
            f"{BASE_URL}/alerting/alerts?rule_id={rule_id}",
            headers=auth_headers
        )
        assert alerts_resp.status_code == 200
        alerts = alerts_resp.json().get("alerts", [])
        assert len(alerts) > 0
        
        # 清理
        requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_alert_recovery(self, auth_headers):
        """TC-MA-006: 告警恢复验证"""
        # 创建规则并触发告警
        rule = {
            "name": f"恢复测试_{int(time.time())}",
            "metric": "recovery_test_metric",
            "condition": "gt",
            "threshold": 10,
            "duration": 5,
            "severity": "warning",
            "enabled": True
        }
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
        rule_id = resp.json()["id"]
        
        # 触发告警
        requests.post(f"{BASE_URL}/metrics/push", headers=auth_headers, 
                     json={"metric": "recovery_test_metric", "value": 50})
        time.sleep(6)
        
        # 恢复正常值
        requests.post(f"{BASE_URL}/metrics/push", headers=auth_headers,
                     json={"metric": "recovery_test_metric", "value": 5})
        time.sleep(6)
        
        # 检查恢复状态
        alerts_resp = requests.get(f"{BASE_URL}/alerting/alerts?rule_id={rule_id}", headers=auth_headers)
        alerts = alerts_resp.json().get("alerts", [])
        if alerts:
            assert alerts[0].get("status") in ["resolved", "recovered"]
        
        # 清理
        requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)


class TestMetricCollection:
    """指标采集功能测试"""
    
    @pytest.fixture
    def auth_headers(self):
        """获取认证头"""
        resp = requests.post(f"{BASE_URL}/auth/login", json=TEST_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    def test_metric_collection(self, auth_headers):
        """TC-MA-007: 指标数据采集"""
        metrics = [
            {"metric": "cpu_usage", "value": 45.5, "timestamp": int(time.time())},
            {"metric": "memory_usage", "value": 60.2, "timestamp": int(time.time())},
            {"metric": "disk_usage", "value": 75.0, "timestamp": int(time.time())}
        ]
        
        for metric in metrics:
            resp = requests.post(f"{BASE_URL}/metrics/push", headers=auth_headers, json=metric)
            assert resp.status_code == 202
        
        # 验证数据存储
        query_resp = requests.get(
            f"{BASE_URL}/metrics/query?metric=cpu_usage&from={int(time.time())-60}",
            headers=auth_headers
        )
        assert query_resp.status_code == 200
        data = query_resp.json()
        assert len(data.get("data_points", [])) > 0
    
    def test_collection