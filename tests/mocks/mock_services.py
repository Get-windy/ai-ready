#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready Mock服务 - 模拟外部依赖，用于本地测试
"""

from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import hashlib
import json
import uuid


class MockDatabase:
    """Mock数据库服务"""
    
    def __init__(self):
        self.tables = {}
        self._init_tables()
    
    def _init_tables(self):
        """初始化表结构"""
        self.tables = {
            'sys_user': [],
            'sys_role': [],
            'sys_menu': [],
            'sys_tenant': [],
            'erp_account': [],
            'erp_product': [],
            'erp_warehouse': [],
            'crm_customer': [],
            'crm_lead': [],
            'crm_opportunity': [],
            'crm_activity': []
        }
    
    def insert(self, table: str, data: Dict) -> Dict:
        """插入数据"""
        if table not in self.tables:
            raise ValueError(f"Table {table} not exists")
        
        data['id'] = len(self.tables[table]) + 1
        data['create_time'] = datetime.now()
        data['update_time'] = datetime.now()
        data['deleted'] = 0
        self.tables[table].append(data)
        return data
    
    def select(self, table: str, conditions: Dict = None) -> List[Dict]:
        """查询数据"""
        if table not in self.tables:
            return []
        
        results = self.tables[table]
        if conditions:
            for key, value in conditions.items():
                results = [r for r in results if r.get(key) == value]
        
        return [r for r in results if r.get('deleted', 0) == 0]
    
    def update(self, table: str, id: int, data: Dict) -> Optional[Dict]:
        """更新数据"""
        if table not in self.tables:
            return None
        
        for item in self.tables[table]:
            if item.get('id') == id:
                item.update(data)
                item['update_time'] = datetime.now()
                return item
        return None
    
    def delete(self, table: str, id: int) -> bool:
        """软删除数据"""
        result = self.update(table, id, {'deleted': 1})
        return result is not None
    
    def count(self, table: str, conditions: Dict = None) -> int:
        """计数"""
        return len(self.select(table, conditions))


class MockAuthService:
    """Mock认证服务"""
    
    def __init__(self):
        self.tokens = {}
        self.users = {}
    
    def login(self, username: str, password: str) -> Dict:
        """模拟登录"""
        # 验证密码格式
        if not password or len(password) < 6:
            return {'success': False, 'message': '密码格式错误'}
        
        # 生成token
        token = hashlib.sha256(f"{username}:{password}:{datetime.now()}".encode()).hexdigest()
        user_id = hash(username) % 10000 + 1
        
        self.tokens[token] = {
            'user_id': user_id,
            'username': username,
            'expires_at': datetime.now() + timedelta(hours=24)
        }
        
        return {
            'success': True,
            'token': token,
            'user_id': user_id,
            'expires_in': 86400
        }
    
    def validate_token(self, token: str) -> Dict:
        """验证token"""
        if token not in self.tokens:
            return {'valid': False, 'message': 'Token不存在'}
        
        token_info = self.tokens[token]
        if token_info['expires_at'] < datetime.now():
            return {'valid': False, 'message': 'Token已过期'}
        
        return {'valid': True, 'user_id': token_info['user_id']}
    
    def logout(self, token: str) -> bool:
        """注销"""
        if token in self.tokens:
            del self.tokens[token]
            return True
        return False


class MockPermissionService:
    """Mock权限服务"""
    
    def __init__(self):
        self.roles = {
            'SUPER_ADMIN': {'permissions': ['*'], 'menus': [1,2,3,4,5,6,7]},
            'ADMIN': {'permissions': ['user:*', 'dept:*', 'role:read'], 'menus': [1,2,3]},
            'USER': {'permissions': ['user:read', 'user:update:self'], 'menus': [1]},
            'ERP_ADMIN': {'permissions': ['erp:*'], 'menus': [4,5]},
            'CRM_ADMIN': {'permissions': ['crm:*'], 'menus': [6,7]}
        }
        self.user_roles = {}
    
    def assign_role(self, user_id: int, role_code: str) -> bool:
        """分配角色"""
        if role_code not in self.roles:
            return False
        self.user_roles[user_id] = role_code
        return True
    
    def check_permission(self, user_id: int, permission: str) -> bool:
        """检查权限"""
        role_code = self.user_roles.get(user_id, 'USER')
        role = self.roles.get(role_code, {})
        
        permissions = role.get('permissions', [])
        if '*' in permissions:
            return True
        
        # 检查通配符权限
        for p in permissions:
            if p.endswith(':*'):
                base = p[:-2]
                if permission.startswith(base):
                    return True
            if p == permission:
                return True
        
        return False
    
    def get_menus(self, user_id: int) -> List[int]:
        """获取菜单"""
        role_code = self.user_roles.get(user_id, 'USER')
        role = self.roles.get(role_code, {})
        return role.get('menus', [])


class MockERPService:
    """Mock ERP服务"""
    
    def __init__(self):
        self.products = {}
        self.warehouses = {}
        self.accounts = {}
        self.stock = {}  # warehouse_id -> product_id -> quantity
    
    def add_product(self, product: Dict) -> Dict:
        """添加产品"""
        product_id = len(self.products) + 1
        product['id'] = product_id
        self.products[product_id] = product
        return product
    
    def get_product(self, product_id: int) -> Optional[Dict]:
        """获取产品"""
        return self.products.get(product_id)
    
    def update_stock(self, warehouse_id: int, product_id: int, quantity: int, operation: str = 'add') -> int:
        """更新库存"""
        key = (warehouse_id, product_id)
        
        if key not in self.stock:
            self.stock[key] = 0
        
        if operation == 'add':
            self.stock[key] += quantity
        elif operation == 'reduce':
            if self.stock[key] < quantity:
                raise ValueError("库存不足")
            self.stock[key] -= quantity
        
        return self.stock[key]
    
    def get_stock(self, warehouse_id: int, product_id: int) -> int:
        """获取库存"""
        return self.stock.get((warehouse_id, product_id), 0)
    
    def calculate_account_balance(self) -> Dict:
        """计算账户余额"""
        assets = sum(a['balance'] for a in self.accounts.values() if a['type'] == 'asset')
        liabilities = sum(a['balance'] for a in self.accounts.values() if a['type'] == 'liability')
        equity = sum(a['balance'] for a in self.accounts.values() if a['type'] == 'equity')
        
        return {
            'assets': assets,
            'liabilities': liabilities,
            'equity': equity,
            'balanced': abs(assets - (liabilities + equity)) < 0.01
        }


class MockCRMService:
    """Mock CRM服务"""
    
    def __init__(self):
        self.customers = {}
        self.leads = {}
        self.opportunities = {}
        self.activities = {}
    
    def add_customer(self, customer: Dict) -> Dict:
        """添加客户"""
        customer_id = len(self.customers) + 1
        customer['id'] = customer_id
        customer['create_time'] = datetime.now()
        self.customers[customer_id] = customer
        return customer
    
    def get_customer(self, customer_id: int) -> Optional[Dict]:
        """获取客户"""
        return self.customers.get(customer_id)
    
    def calculate_customer_value(self, customer_id: int) -> float:
        """计算客户价值"""
        customer = self.customers.get(customer_id)
        if not customer:
            return 0.0
        
        score = customer.get('score', 50)
        orders = customer.get('orders_count', 0)
        avg_value = customer.get('avg_order_value', 0)
        
        return round(score * 0.4 + orders * 0.3 + avg_value * 0.3, 2)
    
    def get_customer_level(self, value_score: float) -> str:
        """获取客户等级"""
        if value_score >= 80:
            return 'A'
        elif value_score >= 60:
            return 'B'
        elif value_score >= 40:
            return 'C'
        return 'D'
    
    def convert_lead(self, lead_id: int) -> Dict:
        """转化线索"""
        lead = self.leads.get(lead_id)
        if not lead:
            return {'success': False, 'message': '线索不存在'}
        
        lead['status'] = '已转化'
        lead['convert_time'] = datetime.now()
        
        return {'success': True, 'lead': lead}


class MockNotificationService:
    """Mock通知服务"""
    
    def __init__(self):
        self.notifications = []
    
    def send_email(self, to: str, subject: str, content: str) -> Dict:
        """发送邮件"""
        notification = {
            'id': len(self.notifications) + 1,
            'type': 'email',
            'to': to,
            'subject': subject,
            'content': content,
            'status': 'sent',
            'send_time': datetime.now()
        }
        self.notifications.append(notification)
        return notification
    
    def send_sms(self, phone: str, content: str) -> Dict:
        """发送短信"""
        notification = {
            'id': len(self.notifications) + 1,
            'type': 'sms',
            'to': phone,
            'content': content,
            'status': 'sent',
            'send_time': datetime.now()
        }
        self.notifications.append(notification)
        return notification


# 全局Mock实例
mock_db = MockDatabase()
mock_auth = MockAuthService()
mock_permission = MockPermissionService()
mock_erp = MockERPService()
mock_crm = MockCRMService()
mock_notification = MockNotificationService()