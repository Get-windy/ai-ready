"""
AI-Ready 智能审批模块API测试
"""
import pytest
import requests
from utils.api_client import AIReadyAPIClient


class TestAIApproval:
    """智能审批API测试"""
    
    @pytest.fixture(scope="class")
    def api_client(self):
        """创建API客户端"""
        client = AIReadyAPIClient()
        client.login("test_user", "Test@123456")
        yield client
        client.close()
    
    @pytest.mark.ai
    @pytest.mark.approval
    def test_approval_intent_recognition(self, api_client):
        """测试审批意图识别"""
        test_cases = [
            {"input": "帮我审批这个订单", "expected_intent": "approval_order"},
            {"input": "同意这个申请", "expected_intent": "approve_request"},
            {"input": "拒绝退款", "expected_intent": "reject_refund"},
        ]
        
        for case in test_cases:
            response = api_client.ai_dialog_intent(case["input"])
            
            assert response.status_code == 200
            data = response.json()
            assert data["intent"] == case["expected_intent"]
            assert "confidence" in data
            assert data["confidence"] >= 0.7
    
    @pytest.mark.ai
    @pytest.mark.approval
    def test_approval_recommendation(self, api_client):
        """测试审批建议生成"""
        response = api_client.ai_approval_recommend({
            "request_id": "REQ-001",
            "request_type": "order_approval",
            "amount": 50000,
            "customer_level": "VIP",
            "customer_history": "good"
        })
        
        assert response.status_code == 200
        data = response.json()
        
        # 验证建议结构
        assert "recommendation" in data
        assert "confidence" in data
        assert "reasoning" in data
        assert data["confidence"] >= 0.7
        assert data["recommendation"] in ["approve", "reject", "review"]
    
    @pytest.mark.ai
    @pytest.mark.approval
    def test_batch_approval(self, api_client):
        """测试批量审批"""
        response = api_client.post('/ai/approval/batch', json={
            "request_ids": ["REQ-001", "REQ-002", "REQ-003"],
            "action": "approve",
            "ai_assist": True
        })
        
        assert response.status_code == 200
        data = response.json()
        
        assert data["processed_count"] == 3
        assert "results" in data
        assert len(data["results"]) == 3
        
        for result in data["results"]:
            assert "request_id" in result
            assert "status" in result
            assert "ai_recommendation" in result
    
    @pytest.mark.ai
    @pytest.mark.approval
    def test_approval_risk_assessment(self, api_client):
        """测试审批风险评估"""
        response = api_client.post('/ai/approval/risk-assessment', json={
            "customer_id": "CUST-001",
            "amount": 100000,
            "payment_terms": "net_30"
        })
        
        assert response.status_code == 200
        data = response.json()
        
        assert "risk_score" in data
        assert "risk_level" in data
        assert "factors" in data
        assert 0 <= data["risk_score"] <= 100
        assert data["risk_level"] in ["low", "medium", "high", "critical"]


class TestAIRecommendation:
    """智能推荐API测试"""
    
    @pytest.fixture(scope="class")
    def api_client(self):
        client = AIReadyAPIClient()
        client.login("test_user", "Test@123456")
        yield client
        client.close()
    
    @pytest.mark.ai
    @pytest.mark.recommendation
    def test_product_recommendation(self, api_client):
        """测试商品推荐"""
        response = api_client.ai_recommend_products("USER-001", limit=5)
        
        assert response.status_code == 200
        data = response.json()
        
        assert "recommendations" in data
        assert len(data["recommendations"]) <= 5
        
        for item in data["recommendations"]:
            assert "product_id" in item
            assert "score" in item
            assert "reason" in item
            assert 0 <= item["score"] <= 1
    
    @pytest.mark.ai
    @pytest.mark.recommendation
    def test_workflow_recommendation(self, api_client):
        """测试工作流推荐"""
        response = api_client.post('/ai/recommend/workflow', json={
            "current_page": "order_detail",
            "user_actions": ["view_order", "check_inventory"],
            "user_role": "sales_manager"
        })
        
        assert response.status_code == 200
        data = response.json()
        
        assert "next_actions" in data
        assert len(data["next_actions"]) > 0
        
        for action in data["next_actions"]:
            assert "action" in action
            assert "label" in action
            assert "probability" in action


class TestAISearch:
    """智能搜索API测试"""
    
    @pytest.fixture(scope="class")
    def api_client(self):
        client = AIReadyAPIClient()
        client.login("test_user", "Test@123456")
        yield client
        client.close()
    
    @pytest.mark.ai
    @pytest.mark.search
    def test_semantic_search(self, api_client):
        """测试语义搜索"""
        response = api_client.ai_search(
            query="上个月销售额最高的客户",
            filters={
                "entity_type": ["customer", "order"],
                "time_range": "last_month"
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        
        assert "results" in data
        assert "total_count" in data
        assert "query_understanding" in data
        
        # 验证查询理解
        assert "intent" in data["query_understanding"]
        assert "entities" in data["query_understanding"]
    
    @pytest.mark.ai
    @pytest.mark.search
    def test_search_suggestions(self, api_client):
        """测试搜索建议"""
        response = api_client.get('/ai/search/suggestions', params={
            "q": "订单",
            "limit": 10
        })
        
        assert response.status_code == 200
        data = response.json()
        
        assert "suggestions" in data
        assert len(data["suggestions"]) <= 10
        
        for suggestion in data["suggestions"]:
            assert "text" in suggestion
            assert "type" in suggestion
            assert suggestion["type"] in ["history", "popular", "entity"]


class TestAIDialog:
    """智能对话API测试"""
    
    @pytest.fixture(scope="class")
    def api_client(self):
        client = AIReadyAPIClient()
        client.login("test_user", "Test@123456")
        yield client
        client.close()
    
    @pytest.mark.ai
    @pytest.mark.dialog
    def test_dialog_context_understanding(self, api_client):
        """测试对话上下文理解"""
        # 第一轮对话
        response1 = api_client.post('/ai/dialog', json={
            "session_id": "test-session-001",
            "message": "查询客户张三的订单",
            "context": {}
        })
        
        assert response1.status_code == 200
        
        # 第二轮对话（引用上下文）
        response2 = api_client.post('/ai/dialog', json={
            "session_id": "test-session-001",
            "message": "显示第一个订单详情",
            "context": response1.json().get("context", {})
        })
        
        assert response2.status_code == 200
        data = response2.json()
        
        # 验证AI理解上下文
        assert "response" in data
        assert "action" in data
        assert data["action"] == "show_order_detail"
    
    @pytest.mark.ai
    @pytest.mark.dialog
    def test_dialog_multi_turn(self, api_client):
        """测试多轮对话"""
        session_id = "test-session-002"
        
        # 多轮对话测试
        turns = [
            {"message": "帮我创建一个订单", "expected_action": "create_order"},
            {"message": "客户是李四", "expected_action": "set_customer"},
            {"message": "商品是PROD-001，数量10", "expected_action": "add_item"},
            {"message": "确认提交", "expected_action": "submit_order"},
        ]
        
        context = {}
        for turn in turns:
            response = api_client.post('/ai/dialog', json={
                "session_id": session_id,
                "message": turn["message"],
                "context": context
            })
            
            assert response.status_code == 200
            data = response.json()
            
            assert data["action"] == turn["expected_action"]
            context = data.get("context", {})
