#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
CRM模块单元测试 - 本地环境
测试客户、线索、商机等核心功能
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mocks.mock_services import mock_crm, mock_db
from unit.local_test_config import TEST_CRM_DATA


@pytest.mark.unit
class TestCustomerModel:
    """客户模型测试"""
    
    def test_customer_creation(self):
        """测试客户创建"""
        customer = {
            'name': '测试公司',
            'type': '企业',
            'level': 'A',
            'phone': '010-12345678',
            'email': 'test@company.com'
        }
        
        result = mock_crm.add_customer(customer)
        
        assert result['id'] > 0
        assert result['name'] == '测试公司'
        assert result['create_time'] is not None
    
    def test_customer_query(self):
        """测试客户查询"""
        customer = mock_crm.add_customer({'name': '查询公司', 'type': '企业'})
        
        result = mock_crm.get_customer(customer['id'])
        
        assert result is not None
        assert result['name'] == '查询公司'
    
    def test_customer_not_found(self):
        """测试客户不存在"""
        result = mock_crm.get_customer(99999)
        
        assert result is None
    
    def test_customer_value_calculation(self):
        """测试客户价值计算"""
        customer = {
            'score': 70,
            'orders_count': 5,
            'avg_order_value': 1000
        }
        
        mock_crm.customers[1] = customer
        value = mock_crm.calculate_customer_value(1)
        
        expected = 70 * 0.4 + 5 * 0.3 + 1000 * 0.3
        assert value == round(expected, 2)
    
    def test_customer_level(self):
        """测试客户等级"""
        assert mock_crm.get_customer_level(85) == 'A'
        assert mock_crm.get_customer_level(70) == 'B'
        assert mock_crm.get_customer_level(50) == 'C'
        assert mock_crm.get_customer_level(20) == 'D'


@pytest.mark.unit
class TestLeadModel:
    """线索模型测试"""
    
    def test_lead_creation(self):
        """测试线索创建"""
        lead_data = {
            'customer_id': 1,
            'source': '网站',
            'status': '新线索',
            'name': '产品咨询',
            'expected_amount': 50000
        }
        
        mock_crm.leads[1] = lead_data
        
        assert mock_crm.leads[1]['status'] == '新线索'
    
    def test_lead_conversion(self):
        """测试线索转化"""
        mock_crm.leads[1] = {
            'customer_id': 1,
            'status': '跟进中',
            'name': '测试线索'
        }
        
        result = mock_crm.convert_lead(1)
        
        assert result['success'] == True
        assert result['lead']['status'] == '已转化'
        assert 'convert_time' in result['lead']
    
    def test_lead_conversion_not_found(self):
        """测试线索不存在转化"""
        result = mock_crm.convert_lead(99999)
        
        assert result['success'] == False
        assert '不存在' in result['message']
    
    def test_lead_status_transition(self):
        """测试线索状态转换"""
        def validate_lead_status(current_status, target_status):
            valid_transitions = {
                '新线索': ['跟进中', '已放弃'],
                '跟进中': ['已转化', '已放弃'],
                '已转化': [],
                '已放弃': []
            }
            return target_status in valid_transitions.get(current_status, [])
        
        assert validate_lead_status('新线索', '跟进中') == True
        assert validate_lead_status('新线索', '已转化') == False
        assert validate_lead_status('跟进中', '已转化') == True
    
    def test_conversion_rate(self):
        """测试转化率计算"""
        def calculate_conversion_rate(leads, converted):
            if leads == 0:
                return 0.0
            return round(converted / leads * 100, 2)
        
        rate = calculate_conversion_rate(100, 25)
        assert rate == 25.0
        
        rate = calculate_conversion_rate(0, 0)
        assert rate == 0.0


@pytest.mark.unit
class TestOpportunityModel:
    """商机模型测试"""
    
    def test_opportunity_creation(self):
        """测试商机创建"""
        opportunity = {
            'customer_id': 1,
            'name': '采购项目',
            'amount': 100000,
            'stage': '需求确认',
            'win_rate': 30
        }
        
        mock_crm.opportunities[1] = opportunity
        
        assert mock_crm.opportunities[1]['amount'] == 100000
    
    def test_expected_revenue(self):
        """测试期望成交金额"""
        def calculate_probabilistic_revenue(amount, win_rate):
            return round(amount * win_rate / 100, 2)
        
        revenue = calculate_probabilistic_revenue(100000, 30)
        assert revenue == 30000.0
        
        revenue = calculate_probabilistic_revenue(50000, 90)
        assert revenue == 45000.0
    
    def test_pipeline_stage(self):
        """测试销售阶段"""
        def get_pipeline_stage(stage_name):
            stages = {
                '潜在客户': 'identify',
                '需求确认': 'qualify',
                '方案提供': 'propose',
                '谈判阶段': 'negotiate',
                '成交阶段': 'close'
            }
            return stages.get(stage_name, 'unknown')
        
        assert get_pipeline_stage('需求确认') == 'qualify'
        assert get_pipeline_stage('成交阶段') == 'close'
        assert get_pipeline_stage('未知阶段') == 'unknown'
    
    def test_pipeline_analysis(self):
        """测试销售管道分析"""
        def analyze_pipeline(opportunities):
            stages = {}
            total_amount = 0
            
            for opp in opportunities:
                stage = opp.get('stage', 'unknown')
                amount = opp.get('amount', 0)
                
                if stage not in stages:
                    stages[stage] = {'count': 0, 'amount': 0}
                
                stages[stage]['count'] += 1
                stages[stage]['amount'] += amount
                total_amount += amount
            
            return {
                'stages': stages,
                'total_amount': total_amount,
                'total_opportunities': len(opportunities)
            }
        
        opportunities = [
            {'stage': '需求确认', 'amount': 10000},
            {'stage': '方案提供', 'amount': 20000},
            {'stage': '需求确认', 'amount': 15000}
        ]
        
        result = analyze_pipeline(opportunities)
        
        assert result['total_opportunities'] == 3
        assert result['total_amount'] == 45000
        assert result['stages']['需求确认']['count'] == 2


@pytest.mark.unit
class TestActivityModel:
    """活动模型测试"""
    
    def test_activity_scoring(self):
        """测试活动评分"""
        def calculate_activity_score(events):
            scores = {
                'view': 1,
                'click': 3,
                'download': 5,
                'register': 10,
                'purchase': 20
            }
            
            total_score = 0
            for event in events:
                event_type = event.get('type')
                count = event.get('count', 1)
                total_score += scores.get(event_type, 0) * count
            
            return total_score
        
        events = [
            {'type': 'view', 'count': 5},
            {'type': 'click', 'count': 2},
            {'type': 'register', 'count': 1}
        ]
        
        score = calculate_activity_score(events)
        assert score == 5 * 1 + 2 * 3 + 1 * 10
    
    def test_activity_duration(self):
        """测试活动时长"""
        activity = {
            'type': 'visit',
            'duration': 60,
            'description': '上门拜访'
        }
        
        mock_crm.activities[1] = activity
        
        assert mock_crm.activities[1]['duration'] == 60
    
    def test_campaign_effectiveness(self):
        """测试活动效果"""
        def calculate_roi(invest, revenue):
            if invest == 0:
                return 0.0
            return round((revenue - invest) / invest * 100, 2)
        
        roi = calculate_roi(10000, 35000)
        assert roi == 250.0
        
        def get_campaign_status(open_rate, click_rate, conversion_rate):
            if open_rate >= 30 and click_rate >= 5 and conversion_rate >= 2:
                return 'excellent'
            elif open_rate >= 20 and click_rate >= 3 and conversion_rate >= 1:
                return 'good'
            elif open_rate >= 10:
                return 'fair'
            else:
                return 'poor'
        
        assert get_campaign_status(35, 8, 3) == 'excellent'
        assert get_campaign_status(15, 2, 0.5) == 'fair'


@pytest.mark.unit
class TestCRMReport:
    """CRM报表测试"""
    
    def test_customer_retention_rate(self):
        """测试客户留存率"""
        def calculate_retention_rate(start_count, end_count, new_customers):
            if start_count == 0:
                return 0.0
            
            retained = end_count - new_customers
            if retained < 0:
                retained = 0
            
            return round(retained / start_count * 100, 2)
        
        rate = calculate_retention_rate(100, 90, 20)
        assert rate == 70.0
    
    def test_customer_journey(self):
        """测试客户旅程"""
        class CustomerJourney:
            def __init__(self):
                self.steps = []
            
            def add_step(self, step_name, description):
                self.steps.append({
                    'name': step_name,
                    'description': description,
                    'completed': False
                })
            
            def complete_step(self, step_index):
                if 0 <= step_index < len(self.steps):
                    self.steps[step_index]['completed'] = True
            
            def get_progress(self):
                if not self.steps:
                    return 0.0
                completed = sum(1 for s in self.steps if s['completed'])
                return round(completed / len(self.steps) * 100, 2)
        
        journey = CustomerJourney()
        journey.add_step('awareness', '知晓')
        journey.add_step('consideration', '考虑')
        journey.add_step('decision', '决策')
        journey.add_step('action', '行动')
        
        journey.complete_step(0)
        journey.complete_step(1)
        
        assert journey.get_progress() == 50.0


@pytest.mark.unit
class TestCRMValidation:
    """CRM数据验证测试"""
    
    def test_customer_type_validation(self):
        """测试客户类型验证"""
        def validate_customer_type(type_name):
            valid_types = ['企业', '个体', '个人']
            return type_name in valid_types
        
        assert validate_customer_type('企业') == True
        assert validate_customer_type('个人') == True
        assert validate_customer_type('其他') == False
    
    def test_customer_level_validation(self):
        """测试客户等级验证"""
        def validate_customer_level(level):
            valid_levels = ['A', 'B', 'C', 'D']
            return level in valid_levels
        
        assert validate_customer_level('A') == True
        assert validate_customer_level('E') == False
    
    def test_lead_source_validation(self):
        """测试线索来源验证"""
        def validate_lead_source(source):
            valid_sources = ['网站', '电话', '线下', '推荐', '广告', '其他']
            return source in valid_sources
        
        assert validate_lead_source('网站') == True
        assert validate_lead_source('电话') == True
        assert validate_lead_source('未知') == False
    
    def test_amount_validation(self):
        """测试金额验证"""
        def validate_amount(amount):
            if amount is None:
                return False, "金额不能为空"
            if amount < 0:
                return False, "金额不能为负"
            if amount > 100000000:
                return False, "金额超出范围"
            return True, "OK"
        
        assert validate_amount(10000)[0] == True
        assert validate_amount(-100)[0] == False
        assert validate_amount(None)[0] == False


@pytest.mark.unit
class TestCRMIntegration:
    """CRM集成测试"""
    
    def test_lead_to_customer_conversion(self):
        """测试线索转客户流程"""
        # 创建线索
        mock_crm.leads[1] = {
            'customer_id': 1,
            'status': '跟进中',
            'name': '测试线索',
            'expected_amount': 50000
        }
        
        # 转化线索
        result = mock_crm.convert_lead(1)
        
        assert result['success'] == True
        
        # 创建商机
        mock_crm.opportunities[1] = {
            'customer_id': 1,
            'amount': 50000,
            'stage': '需求确认',
            'win_rate': 50
        }
        
        assert mock_crm.opportunities[1]['customer_id'] == 1
    
    def test_customer_full_lifecycle(self):
        """测试客户完整生命周期"""
        # 1. 创建客户
        customer = mock_crm.add_customer({
            'name': '全流程客户',
            'type': '企业',
            'level': 'C'
        })
        
        # 2. 创建线索
        mock_crm.leads[1] = {'customer_id': customer['id'], 'status': '新线索'}
        
        # 3. 线索跟进
        mock_crm.leads[1]['status'] = '跟进中'
        
        # 4. 线索转化
        mock_crm.convert_lead(1)
        
        # 5. 创建商机
        mock_crm.opportunities[1] = {'customer_id': customer['id'], 'stage': '需求确认'}
        
        # 6. 商机推进
        mock_crm.opportunities[1]['stage'] = '成交阶段'
        
        # 7. 客户等级提升
        mock_crm.customers[customer['id']]['level'] = 'A'
        
        # 验证最终状态
        assert mock_crm.customers[customer['id']]['level'] == 'A'
        assert mock_crm.leads[1]['status'] == '已转化'
        assert mock_crm.opportunities[1]['stage'] == '成交阶段'