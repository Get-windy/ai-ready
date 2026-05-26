"""
AI-Ready API测试客户端
提供统一的API调用接口
"""
import requests
import os
from typing import Dict, Any, Optional
from urllib.parse import urljoin


class AIReadyAPIClient:
    """AI-Ready API测试客户端"""
    
    def __init__(self, base_url: str = None):
        self.base_url = base_url or os.getenv('TEST_BASE_URL', 'http://localhost:3000/api/v1')
        self.session = requests.Session()
        self.token = None
        
    def login(self, username: str, password: str) -> Dict[str, Any]:
        """登录获取token"""
        response = self.session.post(
            f"{self.base_url}/auth/login",
            json={"username": username, "password": password}
        )
        response.raise_for_status()
        
        data = response.json()
        self.token = data.get('access_token')
        if self.token:
            self.session.headers.update({
                'Authorization': f'Bearer {self.token}'
            })
        
        return data
    
    def get(self, endpoint: str, params: Dict = None) -> requests.Response:
        """GET请求"""
        url = urljoin(self.base_url, endpoint)
        return self.session.get(url, params=params)
    
    def post(self, endpoint: str, json: Dict = None, data: Dict = None) -> requests.Response:
        """POST请求"""
        url = urljoin(self.base_url, endpoint)
        return self.session.post(url, json=json, data=data)
    
    def put(self, endpoint: str, json: Dict = None) -> requests.Response:
        """PUT请求"""
        url = urljoin(self.base_url, endpoint)
        return self.session.put(url, json=json)
    
    def delete(self, endpoint: str) -> requests.Response:
        """DELETE请求"""
        url = urljoin(self.base_url, endpoint)
        return self.session.delete(url)
    
    def ai_approval_recommend(self, request_data: Dict) -> requests.Response:
        """智能审批建议"""
        return self.post('/ai/approval/recommend', json=request_data)
    
    def ai_recommend_products(self, user_id: str, limit: int = 5) -> requests.Response:
        """智能商品推荐"""
        return self.post('/ai/recommend/products', json={
            "user_id": user_id,
            "limit": limit
        })
    
    def ai_search(self, query: str, filters: Dict = None) -> requests.Response:
        """智能搜索"""
        return self.post('/ai/search', json={
            "query": query,
            "filters": filters or {}
        })
    
    def ai_dialog_intent(self, message: str) -> requests.Response:
        """AI对话意图识别"""
        return self.post('/ai/dialog/intent', json={"message": message})
    
    def close(self):
        """关闭会话"""
        self.session.close()
