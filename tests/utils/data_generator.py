#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试数据生成器 - 生成各种测试数据
"""

from faker import Faker
import random


class DataGenerator:
    """测试数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        """
        初始化数据生成器
        
        Args:
            locale: 区域设置,默认中文
        """
        self.fake = Faker(locale)
    
    def generate_user(self) -> dict:
        """
        生成用户测试数据
        
        Returns:
            用户数据字典
        """
        username = f'testuser_{self.fake.password(length=8, special_chars=False)}'
        password = f'Test@{self.fake.password(length=8)}'
        
        return {
            'username': username,
            'email': self.fake.email(),
            'phone': self.fake.phone_number(),
            'password': password,
            'confirmPassword': password,
            'realName': self.fake.name(),
            'departmentId': random.randint(1, 100),
            'position': self.fake.job()
        }
    
    def generate_order(self, product_count: int = 1) -> dict:
        """
        生成订单测试数据
        
        Args:
            product_count: 产品数量
        
        Returns:
            订单数据字典
        """
        return {
            'productIds': [random.randint(1, 100) for _ in range(product_count)],
            'quantity': random.randint(1, 10),
            'note': self.fake.sentence(nb_words=5),
            'totalAmount': round(random.uniform(100, 10000), 2)
        }
    
    def generate_product(self) -> dict:
        """
        生成产品测试数据
        
        Returns:
            产品数据字典
        """
        return {
            'name': f'{self.fake.word()}\u7269',
            'code': f'PROD_{self.fake.uuid4()[:8].upper()}',
            'category': random.choice(['电子', '家居', '服装', '食品']),
            'price': round(random.uniform(10, 1000), 2),
            'stock': random.randint(0, 1000),
            'description': self.fake.text(max_nb_chars=200)
        }
    
    def generate_customer(self) -> dict:
        """
        生成客户测试数据
        
        Returns:
            客户数据字典
        """
        return {
            'name': self.fake.company(),
            'type': random.choice(['企业', '个体', '个人']),
            'level': random.choice(['A', 'B', 'C', 'D']),
            'phone': self.fake.phone_number(),
            'email': self.fake.email(),
            'address': self.fake.address(),
            'contactPerson': self.fake.name(),
            'contactPhone': self.fake.phone_number()
        }
    
    def generate_lead(self) -> dict:
        """
        生成线索测试数据
        
        Returns:
            线索数据字典
        """
        return {
            'customerId': random.randint(1, 1000),
            'source': random.choice(['网站', '电话', '线下', '推荐']),
            'status': random.choice(['新线索', '跟进中', '已转化', '已放弃']),
            'name': self.fake.sentence(nb_words=3),
            'description': self.fake.text(max_nb_chars=100),
            'nextFollowUp': self.fake.date_time_between(start_date='now', end_date='+30d')
        }
    
    def generate_random_data(self, data_type: str) -> dict:
        """
        根据类型生成随机测试数据
        
        Args:
            data_type: 数据类型('user', 'order', 'product', 'customer', 'lead')
        
        Returns:
            随机数据字典
        """
        generators = {
            'user': self.generate_user,
            'order': self.generate_order,
            'product': self.generate_product,
            'customer': self.generate_customer,
            'lead': self.generate_lead
        }
        
        generator = generators.get(data_type)
        if generator:
            return generator()
        else:
            raise ValueError(f"Unsupported data type: {data_type}")