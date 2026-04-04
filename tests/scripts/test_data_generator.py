#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready测试数据生成器
支持基础数据/边界数据/异常数据三种类型
"""

import argparse
import random
import string
from datetime import datetime, timedelta

# 基础数据
FIRST_NAMES = ["张", "王", "李", "刘", "陈", "杨", "黄", "赵", "吴", "周"]
LAST_NAMES = ["伟", "芳", "娜", "静", "强", "磊", "军", "洋", "勇", "艳"]
COMPANIES = ["科技", "网络", "信息", "智能", "软件", "系统", "平台", "服务"]

def generate_username(prefix="test", length=6):
    """生成随机用户名"""
    chars = string.ascii_lowercase + string.digits
    return f"{prefix}_{ ''.join(random.choices(chars, k=length)) }"

def generate_email(username, domain="ai-ready.local"):
    """生成邮箱"""
    return f"{username}@{domain}"

def generate_password():
    """生成强密码"""
    upper = random.choice(string.ascii_uppercase)
    lower = ''.join(random.choices(string.ascii_lowercase, k=5))
    digit = random.choice(string.digits)
    special = random.choice "!@#$%^&*"
    return upper + lower + digit + special + ''.join(random.choices(string.punctuation, k=3))

def generate_phone():
    """生成手机号"""
    prefixes = ["138", "139", "158", "159", "188", "189"]
    return random.choice(prefixes) + ''.join(random.choices(string.digits, k=8))

def generate_user(user_type="normal"):
    """生成单个用户数据"""
    username = generate_username()
    email = generate_email(username)
    
    user = {
        "username": username,
        "email": email,
        "password": generate_password(),
        "phone": generate_phone(),
        "real_name": random.choice(FIRST_NAMES) + random.choice(LAST_NAMES),
        "user_type": user_type,
        "is_active": True,
        "created_at": datetime.now().isoformat(),
        "company": f"{random.choice(COMPANIES)}有限公司"
    }
    
    return user

def generate_customers(count=10, user_ids=None):
    """生成客户数据"""
    customers = []
    
    for i in range(count):
        if user_ids and i < len(user_ids):
            user_id = user_ids[i]
        else:
            user_id = f"user_{i}"
        
        customer = {
            "customer_id": f"CUST_{i+1:04d}",
            "customer_name": f"{random.choice(COMPANIES)}客户_{i+1}",
            "contact_person": random.choice(FIRST_NAMES) + random.choice(LAST_NAMES),
            "contact_phone": generate_phone(),
            "email": generate_email(f"customer_{i+1}"),
            "address": f"{random.randint(100, 999)}号{random.choice([' streets', ' Avenue', ' Road'])}",
            "customer_level": random.choice(["A", "B", "C", "D"]),
            "status": random.choice(["active", "inactive", "pending"]),
            "created_at": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
            "user_id": user_id
        }
        customers.append(customer)
    
    return customers

def generate_products(count=10):
    """生成产品数据"""
    products = []
    product_types = ["产品A", "产品B", "产品C", "产品D", "产品E"]
    specs = ["标准版", "专业版", "企业版", "旗舰版"]
    
    for i in range(count):
        product = {
            "product_id": f"PROD_{i+1:04d}",
            "product_name": f"{random.choice(product_types)}_{i+1}",
            "product_type": random.choice(product_types),
            "spec": random.choice(specs),
            "price": round(random.uniform(100, 10000), 2),
            "stock": random.randint(0, 1000),
            "status": random.choice(["active", "inactive", "discontinued"]),
            "created_at": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
            "description": f"{random.choice(product_types)}{random.choice(specs)}产品描述"
        }
        products.append(product)
    
    return products

def generate_orders(count=10, customer_ids=None, product_ids=None):
    """生成订单数据"""
    orders = []
    
    for i in range(count):
        if customer_ids and i < len(customer_ids):
            customer_id = customer_ids[i]
        else:
            customer_id = f"CUST_{i+1:04d}"
        
        if product_ids and i < len(product_ids):
            product_id = product_ids[i]
        else:
            product_id = f"PROD_{i+1:04d}"
        
        order = {
            "order_id": f"ORD_{i+1:06d}",
            "customer_id": customer_id,
            "product_id": product_id,
            "quantity": random.randint(1, 100),
            "unit_price": round(random.uniform(100, 1000), 2),
            "total_amount": round(random.uniform(100, 100000), 2),
            "status": random.choice(["pending", "paid", "shipped", "completed", "cancelled"]),
            "order_date": (datetime.now() - timedelta(days=random.randint(1, 30))).isoformat(),
            "delivery_date": (datetime.now() + timedelta(days=random.randint(1, 7))).isoformat(),
            "payment_method": random.choice(["alipay", "wechat", "bank", "credit"])
        }
        orders.append(order)
    
    return orders

def generate_agents(count=5):
    """生成Agent数据"""
    agents = []
    agent_types = ["customer_service", "sales", "hr", "finance", "technical"]
    capabilities = ["chat", "email", "phone", "ticket", "knowledge_base"]
    
    for i in range(count):
        agent = {
            "agent_id": f"AGENT_{i+1:04d}",
            "agent_name": f"AI_Assistant_{i+1}",
            "agent_type": random.choice(agent_types),
            "capabilities": random.sample(capabilities, random.randint(2, 5)),
            "description": f"{random.choice(agent_types)}客服AI助手",
            "is_active": True,
            "created_at": datetime.now().isoformat(),
            "config": {
                "response_time": random.randint(1, 10),
                "max_concurrent": random.randint(1, 100)
            }
        }
        agents.append(agent)
    
    return agents

def generate_boundary_data():
    """生成边界数据"""
    return {
        "min_username": "a",
        "max_username": "a" * 30,
        "min_password": "Aa1!",
        "max_password": "A" * 100,
        "min_phone": "1" * 11,
        "max_phone": "1" * 11,
        "empty_string": "",
        "null_value": None,
        "special_chars": "!@#$%^&*()_+-=[]{}|;':",./<>?",
        "unicode_chars": "你好世界测试",
        "number_min": -2147483648,
        "number_max": 2147483647,
        "decimal_min": 0.000001,
        "decimal_max": 999999999.99
    }

def generate_negative_data():
    """生成异常数据"""
    return {
        "invalid_email": "invalid_email",
        "invalid_phone": "12345",
        "negative_number": -100,
        "special_chars_password": "' OR '1'='1",
        "sql_injection": "'; DROP TABLE users; --",
        "xss_script": "<script>alert('xss')</script>",
        "over_length": "a" * 10000,
        "null_field": None
    }

def main():
    parser = argparse.ArgumentParser(description='AI-Ready测试数据生成器')
    parser.add_argument('--type', default='normal', 
                       choices=['normal', 'boundary', 'negative', 'all'],
                       help='数据类型: normal(正常)/boundary(边界)/negative(异常)/all(全部)')
    parser.add_argument('--count', type=int, default=10, help='生成数据数量')
    parser.add_argument('--output', help='输出文件路径')
    
    args = parser.parse_args()
    
    # 生成数据
    data = {}
    
    if args.type in ['normal', 'all']:
        # 生成基础数据
        users = [generate_user() for _ in range(args.count)]
        customers = generate_customers(args.count, [u['email'] for u in users])
        products = generate_products(args.count)
        orders = generate_orders(args.count, 
                                [c['customer_id'] for c in customers],
                                [p['product_id'] for p in products])
        agents = generate_agents()
        
        data['users'] = users
        data['customers'] = customers
        data['products'] = products
        data['orders'] = orders
        data['agents'] = agents
    
    if args.type in ['boundary', 'all']:
        data['boundary'] = generate_boundary_data()
    
    if args.type in ['negative', 'all']:
        data['negative'] = generate_negative_data()
    
    # 输出数据
    import json
    output_str = json.dumps(data, indent=2, ensure_ascii=False)
    
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(output_str)
        print(f"测试数据已保存到: {args.output}")
    else:
        print(output_str)

if __name__ == '__main__':
    main()
