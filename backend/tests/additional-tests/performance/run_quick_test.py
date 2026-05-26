#!/usr/bin/env python3
"""
Quick Performance Test - ASCII only version
Avoids Unicode encoding issues on Windows
"""

import time
import json
import random
import statistics
import threading
import concurrent.futures
from datetime import datetime
import requests

class QuickPerformanceTest:
    """Quick performance test class"""
    
    def __init__(self, base_url="http://localhost:8080", timeout=30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'QuickPerformanceTest/1.0'
        })
        self.results = {}
        
    def test_endpoint(self, name, method, endpoint, data=None, iterations=10):
        """Test a single endpoint"""
        url = f"{self.base_url}{endpoint}"
        response_times = []
        successes = 0
        
        print(f"Testing {name} ({iterations} requests)...")
        
        for i in range(iterations):
            start = time.time()
            try:
                if method == 'GET':
                    resp = self.session.get(url, timeout=self.timeout)
                else:
                    resp = self.session.post(url, json=data, timeout=self.timeout)
                
                elapsed = (time.time() - start) * 1000
                response_times.append(elapsed)
                
                if resp.status_code == 200:
                    successes += 1
                    print(f"  [{i+1}/{iterations}] OK - {elapsed:.1f}ms")
                else:
                    print(f"  [{i+1}/{iterations}] FAIL - Status {resp.status_code}")
                    
            except Exception as e:
                elapsed = (time.time() - start) * 1000
                response_times.append(elapsed)
                print(f"  [{i+1}/{iterations}] ERROR - {str(e)[:50]}")
        
        if response_times:
            avg_time = statistics.mean(response_times)
            min_time = min(response_times)
            max_time = max(response_times)
            success_rate = (successes / iterations) * 100
            
            self.results[name] = {
                'avg_ms': round(avg_time, 2),
                'min_ms': round(min_time, 2),
                'max_ms': round(max_time, 2),
                'success_rate': round(success_rate, 1),
                'total_requests': iterations
            }
            
            print(f"  Summary: Avg={avg_time:.1f}ms, Min={min_time:.1f}ms, Max={max_time:.1f}ms, Success={success_rate:.1f}%")
        else:
            print(f"  No successful requests")
            
    def run_all_tests(self):
        """Run all endpoint tests"""
        print("=" * 60)
        print("AI-Ready Sprint 27+1 Quick Performance Test")
        print("=" * 60)
        print(f"Target: {self.base_url}")
        print(f"Time: {datetime.now().isoformat()}")
        print("-" * 60)
        
        # Test 1: User Login
        self.test_endpoint(
            "User Login",
            "POST",
            "/api/v1/users/login",
            {"username": "test_user", "password": "test_pass"},
            iterations=10
        )
        print()
        
        # Test 2: User Register
        self.test_endpoint(
            "User Register",
            "POST",
            "/api/v1/users/register",
            {"username": f"user_{random.randint(1000,9999)}", "password": "pass123", "email": "test@test.com"},
            iterations=10
        )
        print()
        
        # Test 3: Order Create
        self.test_endpoint(
            "Order Create",
            "POST",
            "/api/v1/orders",
            {"user_id": 123, "items": [{"product_id": 1, "quantity": 2}]},
            iterations=10
        )
        print()
        
        # Test 4: Order Query
        self.test_endpoint(
            "Order Query",
            "GET",
            "/api/v1/orders/12345",
            iterations=10
        )
        print()
        
        # Test 5: AI Predict
        self.test_endpoint(
            "AI Predict",
            "POST",
            "/api/v1/ai/predict",
            {"model_name": "test_model", "input_data": {"text": "test"}},
            iterations=10
        )
        print()
        
        # Print summary
        print("=" * 60)
        print("TEST SUMMARY")
        print("=" * 60)
        
        for name, result in self.results.items():
            status = "PASS" if result['success_rate'] >= 90 else "WARN" if result['success_rate'] >= 50 else "FAIL"
            print(f"{name:20s} | {status:4s} | Avg: {result['avg_ms']:6.1f}ms | Success: {result['success_rate']:5.1f}%")
        
        print("-" * 60)
        
        # Save results
        report = {
            'timestamp': datetime.now().isoformat(),
            'target': self.base_url,
            'results': self.results
        }
        
        report_file = f"quick_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2)
        print(f"Report saved: {report_file}")
        
        return self.results

if __name__ == "__main__":
    tester = QuickPerformanceTest()
    tester.run_all_tests()
