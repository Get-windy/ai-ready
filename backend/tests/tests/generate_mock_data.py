import json
import random
from datetime import datetime, timedelta
from faker import Faker

fake = Faker('zh_CN')

def generate_users(count=100):
    """生成用户Mock数据"""
    users = []
    roles = ['USER', 'ADMIN', 'MANAGER']
    statuses = ['ACTIVE', 'INACTIVE', 'LOCKED']
    
    for i in range(1, count + 1):
        user = {
            "id": i,
            "username": f"test_user_{i:03d}",
            "email": f"user{i:03d}@example.com",
            "password": "hashed_password",
            "role": random.choice(roles),
            "status": random.choice(statuses),
            "createdAt": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
            "updatedAt": datetime.now().isoformat()
        }
        users.append(user)
    
    return users

def generate_customers(count=200):
    """生成客户Mock数据"""
    customers = []
    statuses = ['ACTIVE', 'INACTIVE', 'SUSPENDED']
    
    for i in range(1, count + 1):
        customer = {
            "id": i,
            "name": fake.company(),
            "contact": fake.name(),
            "phone": fake.phone_number(),
            "email": fake.email(),
            "address": fake.address(),
            "status": random.choice(statuses),
            "createdAt": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat()
        }
        customers.append(customer)
    
    return customers

def generate_orders(count=500, user_count=100, customer_count=200):
    """生成订单Mock数据"""
    orders = []
    statuses = ['PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED']
    
    for i in range(1, count + 1):
        order = {
            "id": i,
            "orderNo": f"ORD{datetime.now().year}{i:06d}",
            "customerId": random.randint(1, customer_count),
            "userId": random.randint(1, user_count),
            "amount": round(random.uniform(100, 10000), 2),
            "status": random.choice(statuses),
            "remark": fake.text(max_nb_chars=100),
            "createdAt": (datetime.now() - timedelta(days=random.randint(1, 180))).isoformat(),
            "updatedAt": datetime.now().isoformat()
        }
        orders.append(order)
    
    return orders

def save_mock_data(data, filename):
    """保存Mock数据到JSON文件"""
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Mock数据已保存到: {filename}")

def generate_all_mock_data():
    """生成所有Mock数据"""
    print("开始生成Mock数据...")
    
    # 生成用户数据
    users = generate_users(100)
    save_mock_data(users, 'mock_users.json')
    
    # 生成客户数据
    customers = generate_customers(200)
    save_mock_data(customers, 'mock_customers.json')
    
    # 生成订单数据
    orders = generate_orders(500, 100, 200)
    save_mock_data(orders, 'mock_orders.json')
    
    # 生成汇总文件
    all_data = {
        "users": users,
        "customers": customers,
        "orders": orders,
        "generatedAt": datetime.now().isoformat(),
        "version": "1.0.0"
    }
    save_mock_data(all_data, 'mock_data.json')
    
    print("Mock数据生成完成!")
    print(f"- 用户数据: {len(users)} 条")
    print(f"- 客户数据: {len(customers)} 条")
    print(f"- 订单数据: {len(orders)} 条")

if __name__ == '__main__':
    generate_all_mock_data()
