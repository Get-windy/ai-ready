"""
供应商协同门户API客户端
提供统一的接口调用、认证管理、错误处理和日志记录功能
"""
import time
import json
import logging
from typing import Any, Dict, List, Optional, Union
from dataclasses import dataclass
from pathlib import Path

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import yaml
import structlog

# 配置结构化日志
structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer()
    ]
)
logger = structlog.get_logger()


@dataclass
class APIConfig:
    """API配置数据类"""
    base_url: str
    timeout: int = 30
    verify_ssl: bool = False
    max_retries: int = 3
    backoff_factor: float = 1.0
    token: Optional[str] = None


class APIClient:
    """API客户端类"""
    
    def __init__(self, config_path: Optional[Path] = None):
        """
        初始化API客户端
        
        Args:
            config_path: 配置文件路径，如果为None则使用默认配置
        """
        if config_path:
            self.config = self._load_config(config_path)
        else:
            self.config = APIConfig(base_url="http://localhost:8080/api/supplier")
        
        # 创建带重试机制的会话
        self.session = self._create_session()
        
        # 认证信息
        self.token = None
        self.supplier_id = None
        
    def _load_config(self, config_path: Path) -> APIConfig:
        """加载配置文件"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                config_data = yaml.safe_load(f)
            
            test_env = config_data.get('test_environment', {})
            return APIConfig(
                base_url=test_env.get('base_url', 'http://localhost:8080/api/supplier'),
                timeout=test_env.get('timeout', 30),
                verify_ssl=test_env.get('verify_ssl', False),
                max_retries=3,
                backoff_factor=1.0
            )
        except Exception as e:
            logger.error("加载配置文件失败", config_path=str(config_path), error=str(e))
            return APIConfig(base_url="http://localhost:8080/api/supplier")
    
    def _create_session(self) -> requests.Session:
        """创建带重试机制的HTTP会话"""
        session = requests.Session()
        
        # 配置重试策略
        retry_strategy = Retry(
            total=self.config.max_retries,
            backoff_factor=self.config.backoff_factor,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "POST", "PUT", "DELETE", "PATCH"]
        )
        
        # 配置HTTP适配器
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        # 设置默认请求头
        session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'User-Agent': 'SupplierPortalTest/1.0.0'
        })
        
        return session
    
    def set_auth_token(self, token: str):
        """设置认证令牌"""
        self.token = token
        self.session.headers.update({'Authorization': f'Bearer {token}'})
        logger.info("认证令牌已设置")
    
    def clear_auth(self):
        """清除认证信息"""
        self.token = None
        self.supplier_id = None
        self.session.headers.pop('Authorization', None)
        logger.info("认证信息已清除")
    
    def _make_request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        """
        发送HTTP请求
        
        Args:
            method: HTTP方法 (GET, POST, PUT, DELETE, PATCH)
            endpoint: API端点路径
            **kwargs: 传递给requests.request的其他参数
        
        Returns:
            requests.Response对象
        """
        url = f"{self.config.base_url.rstrip('/')}/{endpoint.lstrip('/')}"
        
        # 记录请求信息
        logger.info("发送HTTP请求", 
                   method=method, 
                   url=url,
                   params=kwargs.get('params'),
                   data=kwargs.get('data'))
        
        try:
            response = self.session.request(
                method=method,
                url=url,
                timeout=self.config.timeout,
                verify=self.config.verify_ssl,
                **kwargs
            )
            
            # 记录响应信息
            logger.info("收到HTTP响应",
                       status_code=response.status_code,
                       response_time_ms=response.elapsed.total_seconds() * 1000,
                       content_length=len(response.content))
            
            return response
            
        except requests.exceptions.Timeout:
            logger.error("请求超时", method=method, url=url, timeout=self.config.timeout)
            raise
        except requests.exceptions.ConnectionError:
            logger.error("连接错误", method=method, url=url)
            raise
        except Exception as e:
            logger.error("请求异常", method=method, url=url, error=str(e))
            raise
    
    def _handle_response(self, response: requests.Response) -> Dict[str, Any]:
        """处理响应，返回JSON数据"""
        try:
            response.raise_for_status()
            
            if response.status_code == 204:  # No Content
                return {"message": "操作成功"}
            
            return response.json()
        except requests.exceptions.HTTPError as e:
            logger.error("HTTP错误", 
                        status_code=response.status_code,
                        response_text=response.text[:500])
            raise
        except json.JSONDecodeError:
            logger.error("JSON解析错误", response_text=response.text[:500])
            raise
    
    # 供应商信息管理接口
    def supplier_register(self, supplier_data: Dict[str, Any]) -> Dict[str, Any]:
        """供应商注册"""
        endpoint = "suppliers/register"
        response = self._make_request('POST', endpoint, json=supplier_data)
        return self._handle_response(response)
    
    def supplier_login(self, credentials: Dict[str, str]) -> Dict[str, Any]:
        """供应商登录"""
        endpoint = "suppliers/login"
        response = self._make_request('POST', endpoint, json=credentials)
        result = self._handle_response(response)
        if 'token' in result:
            self.set_auth_token(result['token'])
            self.supplier_id = result.get('supplier_id')
        return result
    
    def get_supplier_info(self, supplier_id: Optional[str] = None) -> Dict[str, Any]:
        """获取供应商信息"""
        supplier_id = supplier_id or self.supplier_id
        if not supplier_id:
            raise ValueError("供应商ID不能为空")
        
        endpoint = f"suppliers/{supplier_id}"
        response = self._make_request('GET', endpoint)
        return self._handle_response(response)
    
    def update_supplier_info(self, supplier_id: str, update_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新供应商信息"""
        endpoint = f"suppliers/{supplier_id}"
        response = self._make_request('PUT', endpoint, json=update_data)
        return self._handle_response(response)
    
    def search_suppliers(self, filters: Dict[str, Any]) -> Dict[str, Any]:
        """搜索供应商"""
        endpoint = "suppliers/search"
        response = self._make_request('POST', endpoint, json=filters)
        return self._handle_response(response)
    
    # 合同管理接口
    def create_contract(self, contract_data: Dict[str, Any]) -> Dict[str, Any]:
        """创建合同"""
        endpoint = "contracts"
        response = self._make_request('POST', endpoint, json=contract_data)
        return self._handle_response(response)
    
    def get_contract(self, contract_id: str) -> Dict[str, Any]:
        """获取合同详情"""
        endpoint = f"contracts/{contract_id}"
        response = self._make_request('GET', endpoint)
        return self._handle_response(response)
    
    def update_contract(self, contract_id: str, update_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新合同"""
        endpoint = f"contracts/{contract_id}"
        response = self._make_request('PUT', endpoint, json=update_data)
        return self._handle_response(response)
    
    def list_contracts(self, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """获取合同列表"""
        endpoint = "contracts"
        response = self._make_request('GET', endpoint, params=params or {})
        return self._handle_response(response)
    
    def approve_contract(self, contract_id: str, approval_data: Dict[str, Any]) -> Dict[str, Any]:
        """审批合同"""
        endpoint = f"contracts/{contract_id}/approve"
        response = self._make_request('POST', endpoint, json=approval_data)
        return self._handle_response(response)
    
    def reject_contract(self, contract_id: str, rejection_data: Dict[str, Any]) -> Dict[str, Any]:
        """驳回合同"""
        endpoint = f"contracts/{contract_id}/reject"
        response = self._make_request('POST', endpoint, json=rejection_data)
        return self._handle_response(response)
    
    # 订单协同接口
    def publish_order(self, order_data: Dict[str, Any]) -> Dict[str, Any]:
        """发布订单"""
        endpoint = "orders/publish"
        response = self._make_request('POST', endpoint, json=order_data)
        return self._handle_response(response)
    
    def accept_order(self, order_id: str, acceptance_data: Dict[str, Any]) -> Dict[str, Any]:
        """接受订单"""
        endpoint = f"orders/{order_id}/accept"
        response = self._make_request('POST', endpoint, json=acceptance_data)
        return self._handle_response(response)
    
    def reject_order(self, order_id: str, rejection_data: Dict[str, Any]) -> Dict[str, Any]:
        """拒绝订单"""
        endpoint = f"orders/{order_id}/reject"
        response = self._make_request('POST', endpoint, json=rejection_data)
        return self._handle_response(response)
    
    def update_order(self, order_id: str, update_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新订单"""
        endpoint = f"orders/{order_id}"
        response = self._make_request('PUT', endpoint, json=update_data)
        return self._handle_response(response)
    
    def track_order(self, order_id: str) -> Dict[str, Any]:
        """跟踪订单状态"""
        endpoint = f"orders/{order_id}/track"
        response = self._make_request('GET', endpoint)
        return self._handle_response(response)
    
    def cancel_order(self, order_id: str, cancel_data: Dict[str, Any]) -> Dict[str, Any]:
        """取消订单"""
        endpoint = f"orders/{order_id}/cancel"
        response = self._make_request('POST', endpoint, json=cancel_data)
        return self._handle_response(response)
    
    # 健康检查
    def health_check(self) -> bool:
        """健康检查"""
        try:
            endpoint = "health"
            response = self._make_request('GET', endpoint)
            return response.status_code == 200
        except Exception:
            return False
    
    # 性能测试辅助方法
    def benchmark_request(self, method: str, endpoint: str, iterations: int = 10, **kwargs) -> Dict[str, Any]:
        """
        基准测试：多次请求并统计性能
        
        Args:
            method: HTTP方法
            endpoint: API端点
            iterations: 迭代次数
            **kwargs: 请求参数
        
        Returns:
            性能统计信息
        """
        response_times = []
        status_codes = []
        
        for i in range(iterations):
            start_time = time.time()
            try:
                response = self._make_request(method, endpoint, **kwargs)
                status_codes.append(response.status_code)
            except Exception as e:
                status_codes.append(0)  # 0表示请求失败
            finally:
                response_time = (time.time() - start_time) * 1000  # 转换为毫秒
                response_times.append(response_time)
            
            time.sleep(0.1)  # 避免请求过于密集
        
        # 计算统计信息
        successful_requests = sum(1 for code in status_codes if 200 <= code < 300)
        success_rate = (successful_requests / iterations) * 100
        
        return {
            "iterations": iterations,
            "successful_requests": successful_requests,
            "success_rate": success_rate,
            "avg_response_time": sum(response_times) / len(response_times),
            "min_response_time": min(response_times),
            "max_response_time": max(response_times),
            "response_times": response_times,
            "status_codes": status_codes
        }