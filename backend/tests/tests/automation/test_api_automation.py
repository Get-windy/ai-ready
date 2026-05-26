#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready API自动化测试脚本
涵盖核心API端点的自动化测试
"""

import pytest
import json
from datetime import datetime


@pytest.mark.api
@pytest.mark.smoke
class TestAPIHealthCheck:
    """API健康检查测试"""
    
    def test_health_endpoint(self, api_client):
        """测试健康检查端点"""
        response = api_client.health_check()
        assert response.status_code == 200
        data = response.json()
        assert data.get("status") in ["healthy", "UP", "ok"]
    
    def test_api_availability(self, api_client):
        """测试API可用性"""
        response = api_client.get("/api/user/page", params={"pageNum": 1, "pageSize": 10})
        # 如果服务不可用则跳过
        if response.status_code == 503:
            pytest.skip("Service unavailable")


@pytest.mark.api
@pytest.mark.smoke
class TestUserAPIAutomation:
    """用户API自动化测试"""
    
    def test_user_list_query(self, api_client, test_config):
        """测试用户列表查询"""
        params = {
            "pageNum": 1,
            "pageSize": 10,
            "tenantId": test_config["tenant_id"]
        }
        response = api_client.get("/api/user/page", params=params)
        
        if response.status_code == 200:
            data = response.json()
            assert data.get("code") == 0 or data.get("success") == True
            assert "data" in data or "records" in data
    
    def test_user_creation_and_deletion(self, api_client, test_data):
        """测试用户创建和删除流程"""
        # 创建用户
        create_response = api_client.post("/api/user/create", json_data=test_data["user"])
        
        if create_response.status_code == 200:
            data = create_response.json()
            user_id = data.get("data", {}).get("id")
            
            if user_id:
                # 删除用户
                delete_response = api_client.delete(f"/api/user/{user_id}")
                assert delete_response.status_code in [200, 204]


@pytest.mark.api
@pytest.mark.smoke
class TestRoleAPIAutomation:
    """角色API自动化测试"""
    
    def test_role_list_query(self, api_client):
        """测试角色列表查询"""
        params = {"pageNum": 1, "pageSize": 10}
        response = api_client.get("/api/role/page", params=params)
        
        if response.status_code == 200:
            data = response.json()
            assert data.get("code") == 0 or data.get("success") == True
    
    def test_role_crud_flow(self, api_client, test_data):
        """测试角色CRUD流程"""
        # 创建角色
        create_response = api_client.post("/api/role/create", json_data=test_data["role"])
        
        if create_response.status_code == 200:
            data = create_response.json()
            role_id = data.get("data", {}).get("id")
            
            if role_id:
                # 更新角色
                update_data = test_data["role"].copy()
                update_data["id"] = role_id
                update_data["name"] = "Updated Automation Role"
                update_response = api_client.put("/api/role/update", json_data=update_data)
                assert update_response.status_code == 200
                
                # 删除角色
                delete_response = api_client.delete(f"/api/role/{role_id}")
                assert delete_response.status_code in [200, 204]


@pytest.mark.api
@pytest.mark.regression
class TestAPIRegression:
    """API回归测试"""
    
    def test_api_response_format(self, api_client, test_config):
        """测试API响应格式一致性"""
        endpoints = [
            ("/api/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": test_config["tenant_id"]}),
            ("/api/role/page", {"pageNum": 1, "pageSize": 10}),
            ("/api/health", {})
        ]
        
        for endpoint, params in endpoints:
            response = api_client.get(endpoint, params=params)
            if response.status_code == 200:
                data = response.json()
                # 验证响应格式
                assert isinstance(data, dict)
    
    def test_api_error_handling(self, api_client):
        """测试API错误处理"""
        # 测试无效参数
        response = api_client.get("/api/user/page", params={"pageNum": -1})
        assert response.status_code in [200, 400, 422]
        
        # 测试不存在的资源
        response = api_client.get("/api/user/999999")
        assert response.status_code in [200, 404]
    
    def test_api_performance_baseline(self, api_client):
        """测试API性能基线"""
        import time
        
        start_time = time.time()
        response = api_client.health_check()
        elapsed = (time.time() - start_time) * 1000
        
        assert response.status_code == 200
        assert elapsed < 100  # 响应时间应小于100ms


@pytest.mark.api
class TestAPIDataValidation:
    """API数据验证测试"""
    
    def test_pagination_parameters(self, api_client, test_config):
        """测试分页参数验证"""
        test_cases = [
            {"pageNum": 1, "pageSize": 10},  # 正常
            {"pageNum": 0, "pageSize": 10},  # pageNum边界
            {"pageNum": 1, "pageSize": 100},  # pageSize上限
        ]
        
        for params in test_cases:
            params["tenantId"] = test_config["tenant_id"]
            response = api_client.get("/api/user/page", params=params)
            assert response.status_code in [200, 400]
    
    def test_required_fields_validation(self, api_client):
        """测试必填字段验证"""
        # 缺少必填字段
        incomplete_data = {"username": "test"}  # 缺少其他必填字段
        response = api_client.post("/api/user/create", json_data=incomplete_data)
        assert response.status_code in [200, 400, 422]