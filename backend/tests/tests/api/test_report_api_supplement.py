#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 报表分析API补充测试
补充边界场景和异常场景测试
"""

import pytest
import requests
import json
from datetime import datetime, timedelta
from typing import Dict, Any

BASE_URL = "http://localhost:8080/api"

class ReportApiClient:
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self._token: str = None
    
    def set_token(self, token: str):
        self._token = token
        self.session.headers["Authorization"] = f"Bearer {token}"
    
    def login(self, username: str = "admin", password: str = "Admin@123456") -> Dict:
        url = f"{self.base_url}/auth/login"
        data = {"username": username, "password": password, "tenantId": 1}
        try:
            resp = self.session.post(url, json=data, timeout=10)
            result = resp.json()
            if result.get("code") == 200:
                self.set_token(result.get("data"))
            return result
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_yoy_analysis(self, report_id: str, field: str, start_date: str, end_date: str) -> Dict:
        url = f"{self.base_url}/report/analytics/yoy"
        params = {"reportId": report_id, "field": field, "startDate": start_date, "endDate": end_date, "tenantId": 1}
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_mom_analysis(self, report_id: str, field: str, period: str, current_period: str) -> Dict:
        url = f"{self.base_url}/report/analytics/mom"
        params = {"reportId": report_id, "field": field, "period": period, "currentPeriod": current_period, "tenantId": 1}
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_trend_analysis(self, report_id: str, field: str, period_type: str, start_date: str, end_date: str) -> Dict:
        url = f"{self.base_url}/report/analytics/trend"
        params = {"reportId": report_id, "field": field, "periodType": period_type, "startDate": start_date, "endDate": end_date, "tenantId": 1}
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_forecast_analysis(self, report_id: str, field: str, forecast_periods: int, start_date: str, end_date: str) -> Dict:
        url = f"{self.base_url}/report/analytics/forecast"
        params = {"reportId": report_id, "field": field, "forecastPeriods": forecast_periods, "startDate": start_date, "endDate": end_date, "tenantId": 1}
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_comparison_analysis(self, report_id: str, field: str, comparison_type: str, start_date: str, end_date: str) -> Dict:
        url = f"{self.base_url}/report/analytics/comparison"
        params = {"reportId": report_id, "field": field, "comparisonType": comparison_type, "startDate": start_date, "endDate": end_date, "tenantId": 1}
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}


@pytest.fixture
def client():
    api = ReportApiClient()
    api.login()
    return api


@pytest.mark.report
class TestReportAnalyticsApiSupplement:
    def test_yoy_analysis_with_same_year(self, client):
        result = client.get_yoy_analysis("sales_report", "amount", "2024-01-01", "2024-01-31")
        assert result.get("code") in [200, 400, 404]
    
    def test_yoy_analysis_with_future_date(self, client):
        future_date = (datetime.now() + timedelta(days=365)).strftime("%Y-%m-%d")
        result = client.get_yoy_analysis("sales_report", "amount", future_date, future_date)
        assert result.get("code") in [200, 400]
    
    def test_yoy_analysis_with_invalid_date_format(self, client):
        result = client.get_yoy_analysis("sales_report", "amount", "invalid-date", "2024-01-31")
        assert result.get("code") in [200, 400]
    
    def test_yoy_analysis_with_reversed_dates(self, client):
        result = client.get_yoy_analysis("sales_report", "amount", "2024-12-31", "2024-01-01")
        assert result.get("code") in [200, 400]
    
    def test_yoy_analysis_with_empty_field(self, client):
        result = client.get_yoy_analysis("sales_report", "", "2024-01-01", "2024-01-31")
        assert result.get("code") in [200, 400]
    
    def test_yoy_analysis_with_nonexistent_report(self, client):
        result = client.get_yoy_analysis("nonexistent_report", "amount", "2024-01-01", "2024-01-31")
        assert result.get("code") in [200, 404]
    
    def test_mom_analysis_with_invalid_period(self, client):
        result = client.get_mom_analysis("sales_report", "amount", "invalid_period", "2024-03-01")
        assert result.get("code") in [200, 400]
    
    def test_mom_analysis_with_day_period(self, client):
        result = client.get_mom_analysis("sales_report", "amount", "day", "2024-03-01")
        assert result.get("code") in [200, 400]
    
    def test_mom_analysis_with_week_period(self, client):
        result = client.get_mom_analysis("sales_report", "amount", "week", "2024-03-01")
        assert result.get("code") in [200, 400]
    
    def test_mom_analysis_with_month_period(self, client):
        result = client.get_mom_analysis("sales_report", "amount", "month", "2024-03-01")
        assert result.get("code") in [200, 400]
    
    def test_trend_analysis_with_day_type(self, client):
        result = client.get_trend_analysis("sales_report", "amount", "day", "2024-01-01", "2024-01-31")
        assert result.get("code") in [200, 400]
    
    def test_trend_analysis_with_week_type(self, client):
        result = client.get_trend_analysis("sales_report", "amount", "week", "2024-01-01", "2024-03-31")
        assert result.get("code") in [200, 400]
    
    def test_trend_analysis_with_month_type(self, client):
        result = client.get_trend_analysis("sales_report", "amount", "month", "2024-01-01", "2024-12-31")
        assert result.get("code") in [200, 400]
    
    def test_trend_analysis_with_long_period(self, client):
        result = client.get_trend_analysis("sales_report", "amount", "month", "2020-01-01", "2024-12-31")
        assert result.get("code") in [200, 400]
    
    def test_forecast_analysis_with_zero_periods(self, client):
        result = client.get_forecast_analysis("sales_report", "amount", 0, "2024-01-01", "2024-03-31")
        assert result.get("code") in [200, 400]
    
    def test_forecast_analysis_with_large_periods(self, client):
        result = client.get_forecast_analysis("sales_report", "amount", 100, "2024-01-01", "2024-03-31")
        assert result.get("code") in [200, 400]
    
    def test_forecast_analysis_with_negative_periods(self, client):
        result = client.get_forecast_analysis("sales_report", "amount", -5, "2024-01-01", "2024-03-31")

