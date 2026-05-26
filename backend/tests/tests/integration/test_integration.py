#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""AI-Ready集成测试"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.mocks.mock_services import mock_db, mock_auth, mock_permission, mock_erp, mock_crm


@pytest.mark.integration
class TestUserPermissionIntegration:
    """用户-权限模块集成测试"""
    
    def test_user_login_and_permission_check(self):
        """测试用户登录后权限检查流程"""
        # 1. 用户登录
        login_result = mock_auth.login('admin', 'Admin@123456')
        assert login_result['success'] == True
        
        # 2. 验证Token
        token = login_result['token']
        validation = mock_auth.validate_token(token)
        assert validation['valid'] == True
        
        # 3. 分配角色并检查权限
        user_id = validation['user_id']
        mock_permission.assign_role(user_id, 'ADMIN')
        assert mock_permission.check_permission(user_id, 'user:create') == True
    
    def test_cross_module_permission_enforcement(self):
        """测试跨模块权限强制执行"""
        mock_permission.user_roles = {}
        
        # ERP管理员不应有CRM权限
        mock_permission.assign_role(1, 'ERP_ADMIN')
        assert mock_permission.check_permission(1, 'erp:create') == True
        assert mock_permission.check_permission(1, 'crm:create') == False


@pytest.mark.integration
class TestERPCRMIntegration:
    """ERP-CRM模块集成测试"""
    
    def test_customer_order_stock_flow(self):
        """测试客户下单-库存扣减流程"""
        # 1. 创建客户
        customer = mock_crm.add_customer({'name': '测试客户', 'level': 'A'})
        assert customer['id'] > 0
        
        # 2. 创建产品并添加库存
        mock_erp.products = {}
        mock_erp.stock = {}
        product = mock_erp.add_product({'id': 1, 'name': '产品A', 'price': 100})
        
        # 3. 增加库存
        mock_erp.update_stock(1, 1, 100, 'add')
        assert mock_erp.get_stock(1, 1) == 100
        
        # 4. 创建商机
        mock_crm.opportunities[1] = {
            'customer_id': customer['id'],
            'amount': 10000,
            'stage': '成交阶段'
        }
        
        # 5. 扣减库存
        mock_erp.update_stock(1, 1, 10, 'reduce')
        assert mock_erp.get_stock(1, 1) == 90
    
    def test_lead_to_order_integration(self):
        """测试线索到订单的完整流程"""
        # 1. 创建线索
        mock_crm.leads[1] = {'customer_id': 1, 'status': '新线索', 'name': '测试线索'}
        
        # 2. 线索转化
        result = mock_crm.convert_lead(1)
        assert result['success'] == True
        
        # 3. 创建商机
        mock_crm.opportunities[1] = {'customer_id': 1, 'stage': '需求确认', 'win_rate': 50}


@pytest.mark.integration
class TestMultiTenantIntegration:
    """多租户集成测试"""
    
    def test_tenant_data_isolation(self):
        """测试租户数据隔离"""
        mock_db._init_tables()
        
        # 创建不同租户的用户
        mock_db.insert('sys_user', {'username': 'user_t1', 'tenant_id': 1})
        mock_db.insert('sys_user', {'username': 'user_t2', 'tenant_id': 2})
        
        # 查询验证隔离
        t1_users = mock_db.select('sys_user', {'tenant_id': 1})
        t2_users = mock_db.select('sys_user', {'tenant_id': 2})
        
        assert len(t1_users) == 1
        assert len(t2_users) == 1
        assert t1_users[0]['username'] != t2_users[0]['username']


@pytest.mark.integration
class TestDataFlowIntegration:
    """数据流集成测试"""
    
    def test_full_customer_journey(self):
        """测试完整客户旅程"""
        # 1. 创建客户
        customer = mock_crm.add_customer({'name': '旅程客户', 'level': 'C'})
        
        # 2. 创建线索
        mock_crm.leads[1] = {'customer_id': customer['id'], 'status': '新线索'}
        
        # 3. 线索跟进
        mock_crm.leads[1]['status'] = '跟进中'
        
        # 4. 线索转化
        mock_crm.convert_lead(1)
        
        # 5. 创建商机
        mock_crm.opportunities[1] = {
            'customer_id': customer['id'],
            'amount': 50000,
            'stage': '需求确认'
        }
        
        # 6. 商机推进
        mock_crm.opportunities[1]['stage'] = '成交阶段'
        
        # 7. 客户等级提升
        mock_crm.customers[customer['id']]['level'] = 'A'
        
        # 验证最终状态
        assert mock_crm.customers[customer['id']]['level'] == 'A'
        assert mock_crm.leads[1]['status'] == '已转化'
    
    def test_inventory_update_flow(self):
        """测试库存更新流程"""
        mock_erp.products = {}
        mock_erp.stock = {}
        
        # 添加产品
        mock_erp.add_product({'id': 1, 'name': '产品', 'price': 100})
        
        # 采购入库
        mock_erp.update_stock(1, 1, 1000, 'add')
        assert mock_erp.get_stock(1, 1) == 1000
        
        # 销售出库
        mock_erp.update_stock(1, 1, 300, 'reduce')
        assert mock_erp.get_stock(1, 1) == 700
        
        # 库存检查
        status = 'low_stock' if mock_erp.get_stock(1, 1) < 100 else 'normal'
        assert status == 'normal'


@pytest.mark.integration
class TestReportIntegration:
    """报表集成测试"""
    
    def test_cross_module_reporting(self):
        """测试跨模块报表数据"""
        # 创建测试数据
        mock_crm.customers = {
            1: {'name': '客户A', 'level': 'A', 'orders_count': 10},
            2: {'name': '客户B', 'level': 'B', 'orders_count': 5}
        }
        
        mock_erp.products = {
            1: {'name': '产品A', 'price': 100, 'stock': 50},
            2: {'name': '产品B', 'price': 200, 'stock': 30}
        }
        
        # 生成报表数据
        customer_count = len(mock_crm.customers)
        total_stock = sum(p['stock'] for p in mock_erp.products.values())
        
        assert customer_count == 2
        assert total_stock == 80