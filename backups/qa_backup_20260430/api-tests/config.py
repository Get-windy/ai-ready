"""
API测试配置文件
为Sprint 27+1测试环境配置API接口自动化测试
"""

import os
from datetime import datetime

class APITestConfig:
    """API测试配置类"""
    
    def __init__(self):
        # 基础配置
        self.base_url = "http://localhost:8080"  # 测试环境基础URL
        self.api_version = "v1"
        
        # 测试数据配置
        self.test_data_dir = os.path.join(os.path.dirname(__file__), "test-data")
        self.reports_dir = os.path.join(os.path.dirname(__file__), "reports")
        
        # 性能测试配置
        self.performance_thresholds = {
            "response_time_p95": 500,  # P95响应时间阈值（毫秒）
            "success_rate": 99,  # 成功率阈值（百分比）
            "concurrent_users": 10,  # 并发用户数
            "duration_seconds": 60,  # 测试持续时间（秒）
        }
        
        # 认证配置
        self.auth_config = {
            "username": "test_user",
            "password": "test_password_123",
            "token_expiry": 3600,  # 令牌过期时间（秒）
        }
        
        # RESTful API端点
        self.rest_endpoints = {
            "users": "/api/{version}/users",
            "orders": "/api/{version}/orders",
            "products": "/api/{version}/products",
            "auth": "/api/{version}/auth/login",
            "health": "/api/{version}/health",
        }
        
        # GraphQL配置
        self.graphql_config = {
            "endpoint": "/graphql",
            "queries": {
                "get_users": "query { users { id name email } }",
                "get_orders": "query { orders { id userId totalAmount } }",
            }
        }
        
        # WebSocket配置
        self.websocket_config = {
            "endpoint": "/ws",
            "reconnect_attempts": 3,
            "timeout_seconds": 30,
        }
        
        # 文件上传配置
        self.file_upload_config = {
            "max_file_size_mb": 10,
            "allowed_formats": [".jpg", ".png", ".pdf", ".txt", ".csv"],
            "upload_endpoint": "/api/{version}/files/upload",
        }
        
        # 测试执行配置
        self.test_execution = {
            "retry_attempts": 3,
            "retry_delay_seconds": 2,
            "timeout_seconds": 30,
            "verify_ssl": False,  # 测试环境通常不验证SSL
        }
        
        # 日志配置
        self.logging_config = {
            "level": "INFO",
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            "file": os.path.join(self.reports_dir, f"api_tests_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"),
        }
        
        # 报告配置
        self.report_config = {
            "html_report": True,
            "json_report": True,
            "junit_report": True,
            "dashboard_url": "http://localhost:3000/dashboard",  # 监控仪表板URL
        }
    
    def get_endpoint(self, endpoint_name, version=None):
        """获取完整的API端点URL"""
        if version is None:
            version = self.api_version
        
        if endpoint_name in self.rest_endpoints:
            endpoint = self.rest_endpoints[endpoint_name]
            return f"{self.base_url}{endpoint.format(version=version)}"
        
        raise ValueError(f"未知的端点: {endpoint_name}")
    
    def get_graphql_endpoint(self):
        """获取GraphQL端点URL"""
        return f"{self.base_url}{self.graphql_config['endpoint']}"
    
    def get_websocket_endpoint(self):
        """获取WebSocket端点URL"""
        return f"ws://{self.base_url.split('://')[1]}{self.websocket_config['endpoint']}"
    
    def get_file_upload_endpoint(self, version=None):
        """获取文件上传端点URL"""
        if version is None:
            version = self.api_version
        return f"{self.base_url}{self.file_upload_config['upload_endpoint'].format(version=version)}"


# 全局配置实例
config = APITestConfig()

# 测试数据生成函数
def generate_test_data():
    """生成测试数据"""
    test_data = {
        "user": {
            "username": "test_user_" + datetime.now().strftime("%Y%m%d%H%M%S"),
            "email": f"test_{datetime.now().strftime('%Y%m%d%H%M%S')}@example.com",
            "password": "TestPassword123!",
            "first_name": "Test",
            "last_name": "User",
        },
        "order": {
            "user_id": 1,
            "items": [
                {"product_id": 1, "quantity": 2, "price": 29.99},
                {"product_id": 2, "quantity": 1, "price": 49.99},
            ],
            "total_amount": 109.97,
        },
        "product": {
            "name": "Test Product",
            "description": "This is a test product for API testing",
            "price": 99.99,
            "stock_quantity": 100,
            "category": "test",
        }
    }
    return test_data


if __name__ == "__main__":
    # 测试配置
    print("API测试配置:")
    print(f"基础URL: {config.base_url}")
    print(f"RESTful端点: {config.rest_endpoints}")
    print(f"GraphQL端点: {config.get_graphql_endpoint()}")
    print(f"WebSocket端点: {config.get_websocket_endpoint()}")
    print(f"性能阈值: {config.performance_thresholds}")