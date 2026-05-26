#!/usr/bin/env python3
"""
Inventory Management Module Integration Test
Task: task_1777075907084_pqhnur1ib
"""

import requests
import json
import time
from datetime import datetime
from typing import Dict, List, Optional
import sys

# Test Configuration
BASE_URL = "http://localhost:8080/api/v1"
TEST_RESULTS = []

class InventoryIntegrationTest:
    """Inventory Management Integration Test Class"""
    
    def __init__(self):
        self.session = requests.Session()
        self.test_data = {}
        self.passed = 0
        self.failed = 0
        self.skipped = 0
        
    def log_test(self, test_name: str, status: str, message: str, details: dict = None):
        """Record test result"""
        result = {
            "test_name": test_name,
            "status": status,
            "message": message,
            "timestamp": datetime.now().isoformat(),
            "details": details or {}
        }
        TEST_RESULTS.append(result)
        if status == "PASS":
            self.passed += 1
            print(f"[PASS] {test_name}: {message}")
        elif status == "FAIL":
            self.failed += 1
            print(f"[FAIL] {test_name}: {message}")
        else:
            self.skipped += 1
            print(f"[SKIP] {test_name}: {message}")
        return result
    
    def test_inventory_query(self):
        """TC-INV-001: Inventory Query Test"""
        test_name = "TC-INV-001-InventoryQuery"
        try:
            response = self.session.get(f"{BASE_URL}/inventory/list", params={"pageNum": 1, "pageSize": 10}, timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200:
                    self.log_test(test_name, "PASS", "Inventory query API responded successfully", {
                        "response_time_ms": round(response.elapsed.total_seconds() * 1000, 2),
                        "data_count": len(data.get("data", {}).get("list", []))
                    })
                else:
                    self.log_test(test_name, "FAIL", f"API returned error: {data.get('message')}")
            else:
                self.log_test(test_name, "FAIL", f"HTTP status code error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server, please ensure service is running")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_inventory_detail(self):
        """TC-INV-002: Inventory Detail Query Test"""
        test_name = "TC-INV-002-InventoryDetail"
        try:
            list_response = self.session.get(f"{BASE_URL}/inventory/list", params={"pageNum": 1, "pageSize": 1}, timeout=10)
            if list_response.status_code == 200:
                list_data = list_response.json()
                if list_data.get("code") == 200 and list_data.get("data", {}).get("list"):
                    inventory_id = list_data["data"]["list"][0].get("id")
                    detail_response = self.session.get(f"{BASE_URL}/inventory/{inventory_id}", timeout=10)
                    if detail_response.status_code == 200:
                        detail_data = detail_response.json()
                        if detail_data.get("code") == 200:
                            self.log_test(test_name, "PASS", "Inventory detail query successful", {
                                "inventory_id": inventory_id,
                                "response_time_ms": round(detail_response.elapsed.total_seconds() * 1000, 2)
                            })
                        else:
                            self.log_test(test_name, "FAIL", f"Detail query returned error: {detail_data.get('message')}")
                    else:
                        self.log_test(test_name, "FAIL", f"Detail query HTTP status error: {detail_response.status_code}")
                else:
                    self.log_test(test_name, "SKIP", "No inventory data available for testing")
            else:
                self.log_test(test_name, "FAIL", f"List query failed: {list_response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_stock_in(self):
        """TC-INV-003: Stock In Operation Test"""
        test_name = "TC-INV-003-StockIn"
        try:
            stock_in_data = {
                "warehouseId": "WH001",
                "operationType": "PURCHASE_IN",
                "businessNo": f"PO{datetime.now().strftime('%Y%m%d%H%M%S')}",
                "supplierId": "SUP001",
                "items": [
                    {
                        "productId": "PROD001",
                        "quantity": "100",
                        "unitPrice": "50.00",
                        "batchNo": f"BATCH{datetime.now().strftime('%Y%m%d')}001",
                        "remark": "Integration test stock in"
                    }
                ]
            }
            
            response = self.session.post(f"{BASE_URL}/inventory/in", json=stock_in_data, timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200:
                    record_no = data.get("data")
                    self.log_test(test_name, "PASS", "Stock in operation successful", {
                        "record_no": record_no,
                        "response_time_ms": round(response.elapsed.total_seconds() * 1000, 2)
                    })
                    self.test_data["stock_in_record_no"] = record_no
                else:
                    self.log_test(test_name, "FAIL", f"Stock in returned error: {data.get('message')}")
            else:
                self.log_test(test_name, "FAIL", f"Stock in HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_stock_out(self):
        """TC-INV-004: Stock Out Operation Test"""
        test_name = "TC-INV-004-StockOut"
        try:
            stock_out_data = {
                "warehouseId": "WH001",
                "operationType": "SALE_OUT",
                "businessNo": f"SO{datetime.now().strftime('%Y%m%d%H%M%S')}",
                "customerId": "CUST001",
                "items": [
                    {
                        "productId": "PROD001",
                        "quantity": "10",
                        "unitPrice": "80.00",
                        "batchNo": f"BATCH{datetime.now().strftime('%Y%m%d')}001",
                        "remark": "Integration test stock out"
                    }
                ]
            }
            
            response = self.session.post(f"{BASE_URL}/inventory/out", json=stock_out_data, timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200:
                    record_no = data.get("data")
                    self.log_test(test_name, "PASS", "Stock out operation successful", {
                        "record_no": record_no,
                        "response_time_ms": round(response.elapsed.total_seconds() * 1000, 2)
                    })
                    self.test_data["stock_out_record_no"] = record_no
                else:
                    self.log_test(test_name, "FAIL", f"Stock out returned error: {data.get('message')}")
            else:
                self.log_test(test_name, "FAIL", f"Stock out HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_stock_lock(self):
        """TC-INV-005: Stock Lock Test"""
        test_name = "TC-INV-005-StockLock"
        try:
            response = self.session.get(f"{BASE_URL}/inventory/product/PROD001/warehouse/WH001", timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200 and data.get("data"):
                    inventory_id = data["data"].get("id")
                    lock_response = self.session.post(
                        f"{BASE_URL}/inventory/{inventory_id}/lock",
                        params={"warehouseId": "WH001", "quantity": "5"},
                        timeout=10
                    )
                    if lock_response.status_code == 200:
                        lock_data = lock_response.json()
                        if lock_data.get("code") == 200 and lock_data.get("data") == True:
                            self.log_test(test_name, "PASS", "Stock lock successful", {
                                "inventory_id": inventory_id,
                                "locked_quantity": 5
                            })
                        else:
                            self.log_test(test_name, "FAIL", f"Stock lock failed: {lock_data.get('message')}")
                    else:
                        self.log_test(test_name, "FAIL", f"Lock HTTP status error: {lock_response.status_code}")
                else:
                    self.log_test(test_name, "SKIP", "Product inventory not found")
            else:
                self.log_test(test_name, "FAIL", f"Query HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_stock_unlock(self):
        """TC-INV-006: Stock Unlock Test"""
        test_name = "TC-INV-006-StockUnlock"
        try:
            response = self.session.get(f"{BASE_URL}/inventory/product/PROD001/warehouse/WH001", timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200 and data.get("data"):
                    inventory_id = data["data"].get("id")
                    unlock_response = self.session.post(
                        f"{BASE_URL}/inventory/{inventory_id}/unlock",
                        params={"warehouseId": "WH001", "quantity": "5"},
                        timeout=10
                    )
                    if unlock_response.status_code == 200:
                        unlock_data = unlock_response.json()
                        if unlock_data.get("code") == 200 and unlock_data.get("data") == True:
                            self.log_test(test_name, "PASS", "Stock unlock successful", {
                                "inventory_id": inventory_id,
                                "unlocked_quantity": 5
                            })
                        else:
                            self.log_test(test_name, "FAIL", f"Stock unlock failed: {unlock_data.get('message')}")
                    else:
                        self.log_test(test_name, "FAIL", f"Unlock HTTP status error: {unlock_response.status_code}")
                else:
                    self.log_test(test_name, "SKIP", "Product inventory not found")
            else:
                self.log_test(test_name, "FAIL", f"Query HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_warning_inventory(self):
        """TC-INV-007: Warning Inventory Query Test"""
        test_name = "TC-INV-007-WarningInventory"
        try:
            response = self.session.get(f"{BASE_URL}/inventory/warning", params={"pageNum": 1, "pageSize": 10}, timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200:
                    self.log_test(test_name, "PASS", "Warning inventory query successful", {
                        "response_time_ms": round(response.elapsed.total_seconds() * 1000, 2),
                        "warning_count": len(data.get("data", {}).get("list", []))
                    })
                else:
                    self.log_test(test_name, "FAIL", f"Warning query returned error: {data.get('message')}")
            else:
                self.log_test(test_name, "FAIL", f"Warning query HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_inventory_statistics(self):
        """TC-INV-008: Inventory Statistics Test"""
        test_name = "TC-INV-008-InventoryStatistics"
        try:
            response = self.session.get(f"{BASE_URL}/inventory/statistics", timeout=10)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 200:
                    stats = data.get("data", {})
                    self.log_test(test_name, "PASS", "Inventory statistics query successful", {
                        "response_time_ms": round(response.elapsed.total_seconds() * 1000, 2),
                        "total_sku": stats.get("totalSku"),
                        "total_stock": stats.get("totalStock"),
                        "warning_count": stats.get("warningCount")
                    })
                else:
                    self.log_test(test_name, "FAIL", f"Statistics query returned error: {data.get('message')}")
            else:
                self.log_test(test_name, "FAIL", f"Statistics query HTTP status error: {response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def test_cross_module_integration(self):
        """TC-INV-009: Cross-Module Integration Test (Inventory + Order)"""
        test_name = "TC-INV-009-CrossModuleIntegration"
        try:
            # This test verifies inventory-order integration
            # Check if inventory is properly deducted when order is created
            
            # Step 1: Query initial inventory
            inv_response = self.session.get(f"{BASE_URL}/inventory/product/PROD001/warehouse/WH001", timeout=10)
            if inv_response.status_code == 200:
                inv_data = inv_response.json()
                if inv_data.get("code") == 200 and inv_data.get("data"):
                    initial_stock = inv_data["data"].get("availableStock", 0)
                    
                    # Step 2: Create stock out (simulating order fulfillment)
                    stock_out_data = {
                        "warehouseId": "WH001",
                        "operationType": "SALE_OUT",
                        "businessNo": f"ORD{datetime.now().strftime('%Y%m%d%H%M%S')}",
                        "customerId": "CUST001",
                        "items": [
                            {
                                "productId": "PROD001",
                                "quantity": "5",
                                "unitPrice": "80.00",
                                "remark": "Cross-module integration test"
                            }
                        ]
                    }
                    
                    out_response = self.session.post(f"{BASE_URL}/inventory/out", json=stock_out_data, timeout=10)
                    if out_response.status_code == 200:
                        out_data = out_response.json()
                        if out_data.get("code") == 200:
                            # Step 3: Verify inventory deduction
                            inv_response2 = self.session.get(f"{BASE_URL}/inventory/product/PROD001/warehouse/WH001", timeout=10)
                            if inv_response2.status_code == 200:
                                inv_data2 = inv_response2.json()
                                if inv_data2.get("code") == 200 and inv_data2.get("data"):
                                    final_stock = inv_data2["data"].get("availableStock", 0)
                                    expected_stock = float(initial_stock) - 5
                                    
                                    if abs(float(final_stock) - expected_stock) < 0.01:
                                        self.log_test(test_name, "PASS", "Cross-module integration successful", {
                                            "initial_stock": initial_stock,
                                            "deducted_quantity": 5,
                                            "final_stock": final_stock
                                        })
                                    else:
                                        self.log_test(test_name, "FAIL", f"Stock deduction mismatch: expected {expected_stock}, got {final_stock}")
                                else:
                                    self.log_test(test_name, "FAIL", "Failed to query final inventory")
                            else:
                                self.log_test(test_name, "FAIL", f"Final inventory query HTTP error: {inv_response2.status_code}")
                        else:
                            self.log_test(test_name, "FAIL", f"Stock out failed: {out_data.get('message')}")
                    else:
                        self.log_test(test_name, "FAIL", f"Stock out HTTP error: {out_response.status_code}")
                else:
                    self.log_test(test_name, "SKIP", "Product inventory not found")
            else:
                self.log_test(test_name, "FAIL", f"Initial inventory query HTTP error: {inv_response.status_code}")
        except requests.exceptions.ConnectionError:
            self.log_test(test_name, "FAIL", "Cannot connect to test server")
        except Exception as e:
            self.log_test(test_name, "FAIL", f"Test execution exception: {str(e)}")
    
    def run_all_tests(self):
        """Run all integration tests"""
        print("=" * 60)
        print("Inventory Management Module Integration Test")
        print("=" * 60)
        print(f"Test Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"Base URL: {BASE_URL}")
        print("-" * 60)
        
        # Run all tests
        self.test_inventory_query()
        self.test_inventory_detail()
        self.test_stock_in()
        self.test_stock_out()
        self.test_stock_lock()
        self.test_stock_unlock()
        self.test_warning_inventory()
        self.test_inventory_statistics()
        self.test_cross_module_integration()
        
        # Print summary
        print("-" * 60)
        print("Test Summary")
        print("-" * 60)
        print(f"Total Tests: {self.passed + self.failed + self.skipped}")
        print(f"Passed: {self.passed}")
        print(f"Failed: {self.failed}")
        print(f"Skipped: {self.skipped}")
        print(f"Pass Rate: {round(self.passed / (self.passed + self.failed + self.skipped) * 100, 2)}%")
        print(f"Test End Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        
        return {
            "total": self.passed + self.failed + self.skipped,
            "passed": self.passed,
            "failed": self.failed,
            "skipped": self.skipped,
            "pass_rate": round(self.passed / (self.passed + self.failed + self.skipped) * 100, 2) if (self.passed + self.failed + self.skipped) > 0 else 0
        }

def save_test_report(summary: dict, output_file: str):
    """Save test report to file"""
    report = {
        "test_task": "task_1777075907084_pqhnur1ib",
        "test_name": "Inventory Management Module Integration Test",
        "test_date": datetime.now().isoformat(),
        "summary": summary,
        "results": TEST_RESULTS
    }
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    
    print(f"\nTest report saved to: {output_file}")

if __name__ == "__main__":
    tester = InventoryIntegrationTest()
    summary = tester.run_all_tests()
    
    # Save report
    report_file = f"inventory_integration_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    save_test_report(summary, report_file)
    
    # Exit with appropriate code
    sys.exit(0 if summary["failed"] == 0 else 1)
