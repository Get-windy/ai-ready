# 集成测试配置文件
import pytest
import sys
import os

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from tests.mocks.mock_services import (
    mock_db, mock_auth, mock_permission, 
    mock_erp, mock_crm, mock_notification
)


@pytest.fixture(scope="session")
def integration_config():
    """集成测试配置"""
    return {
        "test_tenant_id": 1,
        "test_user_id": 1,
        "timeout": 30
    }


@pytest.fixture(scope="function")
def clean_mocks():
    """每个测试前清理Mock数据"""
    mock_db._init_tables()
    mock_auth.tokens = {}
    mock_permission.user_roles = {}
    mock_erp.products = {}
    mock_erp.stock = {}
    mock_crm.customers = {}
    mock_crm.leads = {}
    mock_crm.opportunities = {}
    yield


@pytest.fixture(scope="function")
def sample_user():
    """测试用户数据"""
    return {
        "id": 1,
        "username": "test_user",
        "email": "test@example.com",
        "tenant_id": 1,
        "dept_id": 1
    }


@pytest.fixture(scope="function")
def sample_product():
    """测试产品数据"""
    return {
        "id": 1,
        "code": "PROD001",
        "name": "测试产品",
        "price": 100.00,
        "stock": 100
    }


@pytest.fixture(scope="function")
def sample_customer():
    """测试客户数据"""
    return {
        "id": 1,
        "name": "测试客户",
        "level": "A",
        "phone": "13800138000"
    }