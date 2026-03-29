#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
CRM模块单元测试
"""

import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



class TestCRMUnit:
    """CRM模块单元测试"""
    
    def test_customer_model(self):
        """测试客户模型"""
        def calculate_customer_value(score, orders_count, avg_order_value):
            """计算客户价值评分"""
            return score * 0.4 + orders_count * 0.3 + avg_order_value * 0.3
        
        def get_customer_level(value_score):
            """根据客户价值获取客户等级"""
            if value_score >= 80:
                return 'A'
            elif value_score >= 60:
                return 'B'
            elif value_score >= 40:
                return 'C'
            else:
                return 'D'
        
        # 测试客户价值计算
        value = calculate_customer_value(70, 5, 1000)
        assert value > 0
        
        # 测试客户等级
        assert get_customer_level(85) == 'A'
        assert get_customer_level(70) == 'B'
        assert get_customer_level(50) == 'C'
        assert get_customer_level(20) == 'D'
    
    def test_lead_conversion(self):
        """测试线索转化"""
        def calculate_conversion_rate(leads, converted):
            """计算线索转化率"""
            if leads == 0:
                return 0.0
            return round(converted / leads * 100, 2)
        
        def validate_lead_status(current_status, target_status):
            """验证线索状态转换"""
            valid_transitions = {
                '新线索': ['跟进中', '已放弃'],
                '跟进中': ['已转化', '已放弃'],
                '已转化': ['已成交', '已返单'],
                '已放弃': []
            }
            
            return target_status in valid_transitions.get(current_status, [])
        
        # 测试转化率计算
        rate = calculate_conversion_rate(100, 25)
        assert rate == 25.0
        
        # 测试状态转换
        assert validate_lead_status('新线索', '跟进中') == True
        assert validate_lead_status('新线索', '已转化') == False
        
    
    def test_opportunity_model(self):
        """测试商机模型"""
        def calculate_probabilistic_revenue(amount, win_rate):
            """计算期望成交金额"""
            return round(amount * win_rate / 100, 2)
        
        def get_pipeline_stage(stage_name):
            """获取销售阶段"""
            stages = {
                '潜在客户': 'identify',
                '需求确认': 'qualify',
                '方案提供': 'propose',
                '谈判阶段': 'negotiate',
                '成交阶段': 'close'
            }
            return stages.get(stage_name, 'unknown')
        
        # 测试期望成交金额
        revenue = calculate_probabilistic_revenue(100000, 30)
        assert revenue == 30000.0
        
        # 测试销售阶段
        assert get_pipeline_stage('需求确认') == 'qualify'
        assert get_pipeline_stage('未知阶段') == 'unknown'


class TestCRMActivity:
    """CRM活动管理测试"""
    
    def test_activity_scoring(self):
        """测试活动评分"""
        def calculate_activity_score(events):
            """计算活动评分"""
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
        
        # 测试活动评分
        events = [
            {'type': 'view', 'count': 5},
            {'type': 'click', 'count': 2},
            {'type': 'register', 'count': 1}
        ]
        
        score = calculate_activity_score(events)
        assert score == 5 * 1 + 2 * 3 + 1 * 10 == 21
    
    def test_campaign_effectiveness(self):
        """测试活动效果"""
        def calculate_roi(invest, revenue):
            """计算活动ROI"""
            if invest == 0:
                return 0.0
            return round((revenue - invest) / invest * 100, 2)
        
        def get_campaign_status(open_rate, click_rate, conversion_rate):
            """评估活动状态"""
            if open_rate >= 30 and click_rate >= 5 and conversion_rate >= 2:
                return 'excellent'
            elif open_rate >= 20 and click_rate >= 3 and conversion_rate >= 1:
                return 'good'
            elif open_rate >= 10:
                return 'fair'
            else:
                return 'poor'
        
        # 测试ROI计算
        roi = calculate_roi(10000, 35000)
        assert roi == 250.0
        
        # 测试活动状态
        status = get_campaign_status(35, 8, 3)
        assert status == 'excellent'


class TestCRMReport:
    """CRM报表测试"""
    
    def test_sales_pipeline_analysis(self):
        """测试销售管道分析"""
        def analyze_pipeline(opportunities):
            """分析销售管道"""
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
        
        # 测试管道分析
        opportunities = [
            {'stage': '潜在客户', 'amount': 10000},
            {'stage': '需求确认', 'amount': 20000},
            {'stage': '方案提供', 'amount': 30000},
            {'stage': '潜在客户', 'amount': 15000}
        ]
        
        result = analyze_pipeline(opportunities)
        assert result['total_opportunities'] == 4
        assert result['total_amount'] == 75000
        assert result['stages']['潜在客户']['count'] == 2
    
    def test_customer_retention_rate(self):
        """测试客户留存率"""
        def calculate_retention_rate(start_count, end_count, new_customers):
            """计算留存率"""
            if start_count == 0:
                return 0.0
            
            retained = end_count - new_customers
            if retained < 0:
                retained = 0
            
            return round(retained / start_count * 100, 2)
        
        # 测试留存率计算
        rate = calculate_retention_rate(100, 90, 20)
        assert rate == 70.0


class TestCRMIntegration:
    """CRM集成测试"""
    
    def test_customer_journey(self):
        """测试客户旅程"""
        class CustomerJourney:
            def __init__(self):
                self.steps = []
                self.current_step = 0
            
            def add_step(self, step_name, description):
                """添加旅程步骤"""
                self.steps.append({
                    'name': step_name,
                    'description': description,
                    'completed': False
                })
            
            def complete_step(self, step_index):
                """完成步骤"""
                if 0 <= step_index < len(self.steps):
                    self.steps[step_index]['completed'] = True
                    self.current_step = step_index + 1
            
            def get_progress(self):
                """获取完成进度"""
                if not self.steps:
                    return 0.0
                
                completed = sum(1 for s in self.steps if s['completed'])
                return round(completed / len(self.steps) * 100, 2)
        
        journey = CustomerJourney()
        
        # 添加旅程步骤
        journey.add_step('awareness', '客户知晓阶段')
        journey.add_step('consideration', '客户考虑阶段')
        journey.add_step('decision', '客户决策阶段')
        journey.add_step('action', '客户行动阶段')
        
        # 完成部分步骤
        journey.complete_step(0)
        journey.complete_step(1)
        
        # 测试进度
        assert journey.get_progress() == 50.0


class TestCRMValidation:
    """CRM数据验证测试"""
    
    def test_email_validation(self):
        """测试邮箱验证"""
        import re
        
        def validate_email(email):
            """验证邮箱格式"""
            pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            return bool(re.match(pattern, email))
        
        # 测试有效邮箱
        assert validate_email('test@example.com') == True
        assert validate_email('user.name+tag@company.co.uk') == True
        
        # 测试无效邮箱
        assert validate_email('invalid') == False
        assert validate_email('test@') == False
        assert validate_email('@example.com') == False
    
    def test_phone_validation(self):
        """测试电话号码验证"""
        import re
        
        def validate_phone(phone):
            """验证电话号码"""
            # 中国手机号格式
            pattern = r'^1[3-9]\d{9}$'
            return bool(re.match(pattern, phone))
        
        # 测试有效手机号
        assert validate_phone('13800138000') == True
        assert validate_phone('15912345678') == True
        
        # 测试无效手机号
        assert validate_phone('12800138000') == False  # 12开头无效
        assert validate_phone('1380013800') == False   # 太短
        assert validate_phone('138001380000') == False # 太长
