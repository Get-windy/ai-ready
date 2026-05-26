"""
测试数据工厂 - 基于工厂模式生成测试数据

此模块提供测试数据生成服务，支持：
1. 随机数据生成
2. 确定性数据生成
3. 数据模板和模式
4. 数据关系和依赖
5. 数据清理和管理
"""

import random
import string
from typing import Any, Dict, List, Optional, Union, TypeVar, Generic
from datetime import datetime, timedelta
from faker import Faker
import factory
from factory import Faker as FactoryFaker
from factory.fuzzy import FuzzyChoice, FuzzyInteger, FuzzyFloat, FuzzyDate, FuzzyText
import pytest

# 创建Faker实例（支持中文）
fake = Faker('zh_CN')
Faker.seed(42)  # 设置随机种子，确保可重复性

T = TypeVar('T')


class DataFactory:
    """测试数据工厂基类"""
    
    def __init__(self, seed: Optional[int] = None):
        """初始化数据工厂
        
        Args:
            seed: 随机种子，确保测试数据的可重复性
        """
        if seed is not None:
            random.seed(seed)
            fake.seed_instance(seed)
    
    def random_string(
        self, 
        length: int = 10, 
        letters: bool = True,
        digits: bool = True,
        special_chars: bool = False
    ) -> str:
        """生成随机字符串"""
        chars = ''
        if letters:
            chars += string.ascii_letters
        if digits:
            chars += string.digits
        if special_chars:
            chars += string.punctuation
        
        if not chars:
            chars = string.ascii_letters + string.digits
        
        return ''.join(random.choice(chars) for _ in range(length))
    
    def random_email(self, domain: str = "example.com") -> str:
        """生成随机邮箱地址"""
        username = self.random_string(8, letters=True, digits=True, special_chars=False)
        return f"{username}@{domain}"
    
    def random_phone(self) -> str:
        """生成随机手机号"""
        prefix = random.choice(['130', '131', '132', '133', '134', '135', '136', '137', '138', '139',
                                '150', '151', '152', '153', '155', '156', '157', '158', '159',
                                '170', '171', '172', '173', '174', '175', '176', '177', '178',
                                '180', '181', '182', '183', '184', '185', '186', '187', '188', '189'])
        suffix = ''.join(random.choice(string.digits) for _ in range(8))
        return f"{prefix}{suffix}"
    
    def random_date(
        self, 
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None
    ) -> datetime:
        """生成随机日期时间"""
        if start_date is None:
            start_date = datetime.now() - timedelta(days=365*5)  # 5年前
        if end_date is None:
            end_date = datetime.now() + timedelta(days=365)  # 1年后
        
        time_between = end_date - start_date
        random_days = random.randrange(time_between.days)
        random_seconds = random.randrange(86400)  # 一天中的随机秒数
        
        return start_date + timedelta(days=random_days, seconds=random_seconds)
    
    def random_boolean(self) -> bool:
        """生成随机布尔值"""
        return random.choice([True, False])
    
    def random_int(self, min_value: int = 0, max_value: int = 1000) -> int:
        """生成随机整数"""
        return random.randint(min_value, max_value)
    
    def random_float(self, min_value: float = 0.0, max_value: float = 1000.0, precision: int = 2) -> float:
        """生成随机浮点数"""
        value = random.uniform(min_value, max_value)
        return round(value, precision)
    
    def random_choice(self, choices: List[Any]) -> Any:
        """从列表中随机选择"""
        return random.choice(choices)
    
    def random_list(self, item_generator, count: int = 5) -> List[Any]:
        """生成随机列表"""
        return [item_generator() for _ in range(count)]
    
    def random_dict(self, field_generators: Dict[str, callable]) -> Dict[str, Any]:
        """生成随机字典"""
        return {field: generator() for field, generator in field_generators.items()}


class UserDataFactory(DataFactory):
    """用户数据工厂"""
    
    def generate_user(
        self,
        user_id: Optional[int] = None,
        include_password: bool = True
    ) -> Dict[str, Any]:
        """生成用户数据"""
        if user_id is None:
            user_id = self.random_int(1000, 9999)
        
        user_data = {
            "id": user_id,
            "username": fake.user_name(),
            "email": fake.email(),
            "phone": self.random_phone(),
            "first_name": fake.first_name(),
            "last_name": fake.last_name(),
            "nickname": fake.name(),
            "birthdate": self.random_date().strftime("%Y-%m-%d"),
            "gender": random.choice(["male", "female", "other"]),
            "avatar": f"https://example.com/avatars/{user_id}.jpg",
            "bio": fake.text(max_nb_chars=200),
            "website": fake.url(),
            "location": fake.city(),
            "timezone": random.choice(["Asia/Shanghai", "UTC", "America/New_York"]),
            "language": random.choice(["zh_CN", "en_US", "ja_JP"]),
            "currency": random.choice(["CNY", "USD", "EUR", "JPY"]),
            "is_active": self.random_boolean(),
            "is_staff": self.random_boolean(),
            "is_superuser": self.random_boolean(),
            "date_joined": self.random_date().isoformat(),
            "last_login": self.random_date().isoformat() if self.random_boolean() else None,
            "metadata": {
                "login_count": self.random_int(0, 1000),
                "failed_login_count": self.random_int(0, 10),
                "last_ip": fake.ipv4(),
                "device_type": random.choice(["web", "mobile", "tablet", "desktop"]),
            }
        }
        
        if include_password:
            user_data["password"] = self.random_string(12, letters=True, digits=True, special_chars=True)
        
        return user_data
    
    def generate_users(self, count: int = 10) -> List[Dict[str, Any]]:
        """生成多个用户数据"""
        return [self.generate_user(1000 + i) for i in range(count)]


class ProductDataFactory(DataFactory):
    """产品数据工厂"""
    
    def generate_product(
        self,
        product_id: Optional[int] = None,
        include_variants: bool = True
    ) -> Dict[str, Any]:
        """生成产品数据"""
        if product_id is None:
            product_id = self.random_int(10000, 99999)
        
        categories = ["电子产品", "办公用品", "家居用品", "图书音像", "服装鞋帽", "食品饮料", "美妆个护", "运动户外"]
        brands = ["Apple", "华为", "小米", "联想", "戴尔", "惠普", "三星", "索尼", "佳能", "尼康"]
        
        base_price = self.random_float(10.0, 10000.0, 2)
        discount = self.random_float(0.0, 0.5, 2) if self.random_boolean() else 0.0
        
        product_data = {
            "id": product_id,
            "sku": f"SKU{product_id:06d}",
            "name": f"{random.choice(brands)} {fake.word().capitalize()} {self.random_int(1, 10)}",
            "description": fake.text(max_nb_chars=500),
            "short_description": fake.text(max_nb_chars=100),
            "category": random.choice(categories),
            "subcategory": fake.word(),
            "brand": random.choice(brands),
            "model": f"MODEL-{self.random_string(6, letters=True, digits=True)}",
            "upc": ''.join(random.choice(string.digits) for _ in range(12)),
            "ean": ''.join(random.choice(string.digits) for _ in range(13)),
            "price": base_price,
            "cost_price": base_price * self.random_float(0.3, 0.7, 2),
            "discount_price": base_price * (1 - discount) if discount > 0 else None,
            "currency": "CNY",
            "weight": self.random_float(0.1, 50.0, 2),
            "dimensions": {
                "length": self.random_float(5.0, 100.0, 1),
                "width": self.random_float(5.0, 100.0, 1),
                "height": self.random_float(5.0, 100.0, 1),
                "unit": "cm"
            },
            "stock_quantity": self.random_int(0, 1000),
            "stock_status": random.choice(["in_stock", "out_of_stock", "backorder", "discontinued"]),
            "min_order_quantity": self.random_int(1, 10),
            "max_order_quantity": self.random_int(10, 100),
            "is_active": self.random_boolean(),
            "is_featured": self.random_boolean(),
            "is_digital": self.random_boolean(),
            "requires_shipping": self.random_boolean(),
            "tax_class": random.choice(["standard", "reduced", "zero", "exempt"]),
            "tags": [fake.word() for _ in range(self.random_int(0, 5))],
            "attributes": {
                "color": random.choice(["黑色", "白色", "红色", "蓝色", "金色", "银色"]),
                "size": random.choice(["S", "M", "L", "XL", "XXL"]),
                "material": random.choice(["塑料", "金属", "木材", "玻璃", "陶瓷"]),
            },
            "images": [
                f"https://example.com/products/{product_id}/main.jpg",
                f"https://example.com/products/{product_id}/detail1.jpg",
                f"https://example.com/products/{product_id}/detail2.jpg",
            ],
            "specifications": [
                {"key": "处理器", "value": f"Intel Core i{self.random_int(5, 9)}"},
                {"key": "内存", "value": f"{self.random_int(4, 32)}GB"},
                {"key": "存储", "value": f"{self.random_int(128, 2048)}GB SSD"},
                {"key": "屏幕尺寸", "value": f"{self.random_float(10.0, 32.0, 1)}英寸"},
                {"key": "分辨率", "value": f"{self.random_int(1920, 3840)}x{self.random_int(1080, 2160)}"},
            ],
            "created_at": self.random_date().isoformat(),
            "updated_at": self.random_date().isoformat(),
        }
        
        if include_variants:
            product_data["variants"] = self.generate_product_variants(product_id)
        
        return product_data
    
    def generate_product_variants(self, product_id: int) -> List[Dict[str, Any]]:
        """生成产品变体数据"""
        variants = []
        colors = ["黑色", "白色", "红色", "蓝色", "金色"]
        sizes = ["S", "M", "L", "XL"]
        
        for i, color in enumerate(colors[:self.random_int(1, 3)]):
            for j, size in enumerate(sizes[:self.random_int(1, 2)]):
                variant_id = product_id * 100 + i * 10 + j
                variants.append({
                    "id": variant_id,
                    "sku": f"SKU{product_id:06d}-{color[:2]}-{size}",
                    "color": color,
                    "size": size,
                    "price_adjustment": self.random_float(-50.0, 50.0, 2),
                    "stock_quantity": self.random_int(0, 100),
                    "image": f"https://example.com/products/{product_id}/variant_{variant_id}.jpg",
                })
        
        return variants
    
    def generate_products(self, count: int = 20) -> List[Dict[str, Any]]:
        """生成多个产品数据"""
        return [self.generate_product(10000 + i) for i in range(count)]


class OrderDataFactory(DataFactory):
    """订单数据工厂"""
    
    def generate_order(
        self,
        order_id: Optional[int] = None,
        user_id: Optional[int] = None,
        include_items: bool = True
    ) -> Dict[str, Any]:
        """生成订单数据"""
        if order_id is None:
            order_id = self.random_int(100000, 999999)
        
        if user_id is None:
            user_id = self.random_int(1000, 1999)
        
        order_date = self.random_date()
        shipping_date = order_date + timedelta(days=self.random_int(1, 7))
        delivery_date = shipping_date + timedelta(days=self.random_int(1, 14))
        
        subtotal = self.random_float(100.0, 10000.0, 2)
        shipping_cost = self.random_float(0.0, 100.0, 2)
        tax = subtotal * self.random_float(0.05, 0.20, 2)
        discount = subtotal * self.random_float(0.0, 0.3, 2)
        total = subtotal + shipping_cost + tax - discount
        
        order_data = {
            "id": order_id,
            "order_number": f"ORD{order_id:08d}",
            "user_id": user_id,
            "status": random.choice([
                "pending", "processing", "shipped", "delivered", 
                "cancelled", "refunded", "failed", "on_hold"
            ]),
            "order_date": order_date.isoformat(),
            "shipping_date": shipping_date.isoformat() if self.random_boolean() else None,
            "delivery_date": delivery_date.isoformat() if self.random_boolean() else None,
            "payment_method": random.choice([
                "credit_card", "alipay", "wechat_pay", "bank_transfer", 
                "cash_on_delivery", "paypal", "apple_pay"
            ]),
            "payment_status": random.choice(["pending", "paid", "failed", "refunded"]),
            "transaction_id": f"TXN{self.random_string(16, letters=False, digits=True)}",
            "currency": "CNY",
            "subtotal": subtotal,
            "shipping_cost": shipping_cost,
            "tax": tax,
            "discount": discount,
            "total": total,
            "shipping_method": random.choice([
                "standard", "express", "overnight", "pickup", "international"
            ]),
            "tracking_number": f"TRK{self.random_string(12, letters=False, digits=True)}" if self.random_boolean() else None,
            "carrier": random.choice(["顺丰", "中通", "圆通", "申通", "韵达", "EMS", "DHL", "FedEx"]),
            "notes": fake.text(max_nb_chars=200) if self.random_boolean() else None,
            "customer_note": fake.text(max_nb_chars=100) if self.random_boolean() else None,
            "billing_address": self.generate_address(),
            "shipping_address": self.generate_address(),
            "metadata": {
                "ip_address": fake.ipv4(),
                "user_agent": fake.user_agent(),
                "referrer": fake.url() if self.random_boolean() else None,
                "campaign": random.choice(["google", "facebook", "email", "direct", "organic"]),
                "source": random.choice(["web", "mobile_app", "wechat", "alipay"]),
            },
            "created_at": order_date.isoformat(),
            "updated_at": self.random_date(order_date, datetime.now()).isoformat(),
        }
        
        if include_items:
            order_data["items"] = self.generate_order_items(order_id, count=self.random_int(1, 5))
        
        return order_data
    
    def generate_address(self) -> Dict[str, Any]:
        """生成地址数据"""
        return {
            "first_name": fake.first_name(),
            "last_name": fake.last_name(),
            "company": fake.company() if self.random_boolean() else None,
            "address_line1": fake.street_address(),
            "address_line2": fake.secondary_address() if self.random_boolean() else None,
            "city": fake.city(),
            "state": fake.province(),
            "postal_code": fake.postcode(),
            "country": "中国",
            "phone": self.random_phone(),
            "email": fake.email(),
        }
    
    def generate_order_items(self, order_id: int, count: int = 3) -> List[Dict[str, Any]]:
        """生成订单项数据"""
        items = []
        
        for i in range(count):
            product_price = self.random_float(10.0, 1000.0, 2)
            quantity = self.random_int(1, 10)
            discount = product_price * quantity * self.random_float(0.0, 0.2, 2)
            
            items.append({
                "id": order_id * 100 + i,
                "order_id": order_id,
                "product_id": self.random_int(10000, 19999),
                "product_name": f"产品 {self.random_string(5)}",
                "product_sku": f"SKU{self.random_int(100000, 999999)}",
                "quantity": quantity,
                "unit_price": product_price,
                "discount": discount,
                "tax": product_price * quantity * self.random_float(0.05, 0.20, 2),
                "total": (product_price * quantity) - discount,
                "variant_id": self.random_int(1, 10) if self.random_boolean() else None,
                "variant_attributes": {
                    "color": random.choice(["黑色", "白色", "红色"]),
                    "size": random.choice(["S", "M", "L"]),
