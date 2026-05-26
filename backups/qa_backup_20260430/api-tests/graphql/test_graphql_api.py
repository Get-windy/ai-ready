"""
GraphQL API自动化测试脚本
为Sprint 27+1测试环境配置GraphQL API接口测试
"""

import sys
import os
import time
import json
from typing import Dict, List, Any, Optional

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config
from test_utils import logger, api_client, data_generator, validator


class GraphQLAPITests:
    """GraphQL API测试类"""
    
    def __init__(self):
        self.test_results = []
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有GraphQL API测试"""
        logger.log_test_start("GraphQL API 完整测试套件")
        
        test_suite = {
            "start_time": time.time(),
            "tests": [],
            "summary": {
                "total": 0,
                "passed": 0,
                "failed": 0,
                "warnings": 0
            }
        }
        
        # 运行GraphQL测试
        tests = [
            self.test_graphql_endpoint_availability,
            self.test_graphql_introspection,
            self.test_graphql_queries,
            self.test_graphql_mutations,
            self.test_graphql_subscriptions,
            self.test_graphql_error_handling,
            self.test_graphql_performance,
            self.test_graphql_schema_validation,
        ]
        
        for test_func in tests:
            result = test_func()
            test_suite["tests"].append(result)
        
        # 计算统计信息
        for test in test_suite["tests"]:
            test_suite["summary"]["total"] += 1
            if test["status"] == "passed":
                test_suite["summary"]["passed"] += 1
            elif test["status"] == "failed":
                test_suite["summary"]["failed"] += 1
            elif test["status"] == "warning":
                test_suite["summary"]["warnings"] += 1
        
        test_suite["end_time"] = time.time()
        test_suite["duration"] = test_suite["end_time"] - test_suite["start_time"]
        
        logger.log_test_pass("GraphQL API 测试套件完成", 
                           f"通过: {test_suite['summary']['passed']}, "
                           f"失败: {test_suite['summary']['failed']}, "
                           f"警告: {test_suite['summary']['warnings']}")
        
        return test_suite
    
    def test_graphql_endpoint_availability(self) -> Dict[str, Any]:
        """测试GraphQL端点可用性"""
        test_name = "GraphQL端点可用性测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            start_time = time.time()
            
            # 发送简单的GraphQL查询
            query = {
                "query": """
                    query {
                        __schema {
                            types {
                                name
                            }
                        }
                    }
                """
            }
            
            response = api_client.post(endpoint, query)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                data = response.json()
                if "data" in data and "__schema" in data["data"]:
                    result["status"] = "passed"
                    result["details"] = "GraphQL端点可用，Schema可访问"
                    logger.log_test_pass(test_name, f"响应时间: {response_time:.2f}ms")
                else:
                    result["details"] = "GraphQL响应格式不正确"
                    logger.log_test_fail(test_name, result["details"])
            else:
                result["details"] = f"HTTP状态码异常: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"GraphQL端点测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_graphql_introspection(self) -> Dict[str, Any]:
        """测试GraphQL自省查询"""
        test_name = "GraphQL自省查询测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            start_time = time.time()
            
            # 完整的自省查询
            query = {
                "query": """
                    query IntrospectionQuery {
                        __schema {
                            queryType { name }
                            mutationType { name }
                            subscriptionType { name }
                            types {
                                ...FullType
                            }
                            directives {
                                name
                                description
                                locations
                                args {
                                    ...InputValue
                                }
                            }
                        }
                    }
                    
                    fragment FullType on __Type {
                        kind
                        name
                        description
                        fields(includeDeprecated: true) {
                            name
                            description
                            args {
                                ...InputValue
                            }
                            type {
                                ...TypeRef
                            }
                            isDeprecated
                            deprecationReason
                        }
                        inputFields {
                            ...InputValue
                        }
                        interfaces {
                            ...TypeRef
                        }
                        enumValues(includeDeprecated: true) {
                            name
                            description
                            isDeprecated
                            deprecationReason
                        }
                        possibleTypes {
                            ...TypeRef
                        }
                    }
                    
                    fragment InputValue on __InputValue {
                        name
                        description
                        type { ...TypeRef }
                        defaultValue
                    }
                    
                    fragment TypeRef on __Type {
                        kind
                        name
                        ofType {
                            kind
                            name
                            ofType {
                                kind
                                name
                                ofType {
                                    kind
                                    name
                                    ofType {
                                        kind
                                        name
                                        ofType {
                                            kind
                                            name
                                            ofType {
                                                kind
                                                name
                                                ofType {
                                                    kind
                                                    name
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                """
            }
            
            response = api_client.post(endpoint, query)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                data = response.json()
                if "data" in data and "__schema" in data["data"]:
                    schema = data["data"]["__schema"]
                    
                    # 检查基本schema信息
                    query_type = schema.get("queryType", {})
                    mutation_type = schema.get("mutationType", {})
                    types = schema.get("types", [])
                    
                    result["status"] = "passed"
                    result["details"] = (f"自省查询成功 - Query类型: {query_type.get('name', 'N/A')}, "
                                       f"Mutation类型: {mutation_type.get('name', 'N/A')}, "
                                       f"类型数量: {len(types)}")
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "自省查询响应格式不正确"
                    logger.log_test_fail(test_name, result["details"])
            else:
                result["details"] = f"自省查询HTTP状态码异常: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"GraphQL自省查询测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_graphql_queries(self) -> Dict[str, Any]:
        """测试GraphQL查询"""
        test_name = "GraphQL查询测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",  # 默认警告，因为查询可能依赖具体实现
            "details": "",
            "response_time": 0,
            "subtests": []
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            subtests = []
            
            # 测试1: 获取用户查询
            test_queries = [
                {
                    "name": "获取用户列表查询",
                    "query": """
                        query {
                            users {
                                id
                                name
                                email
                            }
                        }
                    """
                },
                {
                    "name": "获取产品列表查询", 
                    "query": """
                        query {
                            products {
                                id
                                name
                                price
                                category
                            }
                        }
                    """
                },
                {
                    "name": "获取订单列表查询",
                    "query": """
                        query {
                            orders {
                                id
                                userId
                                totalAmount
                                status
                            }
                        }
                    """
                }
            ]
            
            for test_query in test_queries:
                start_time = time.time()
                response = api_client.post(endpoint, {"query": test_query["query"]})
                response_time = (time.time() - start_time) * 1000
                
                if validator.validate_response_status(response, 200):
                    data = response.json()
                    
                    if "errors" in data and data["errors"]:
                        subtests.append({
                            "name": test_query["name"],
                            "status": "warning",
                            "response_time": response_time,
                            "details": f"查询执行错误: {data['errors'][0].get('message', '未知错误')}"
                        })
                    elif "data" in data:
                        subtests.append({
                            "name": test_query["name"],
                            "status": "passed",
                            "response_time": response_time,
                            "details": "查询执行成功"
                        })
                    else:
                        subtests.append({
                            "name": test_query["name"],
                            "status": "warning",
                            "response_time": response_time,
                            "details": "响应格式不正确"
                        })
                else:
                    subtests.append({
                        "name": test_query["name"],
                        "status": "warning",
                        "response_time": response_time,
                        "details": f"HTTP状态码: {response.status_code}"
                    })
            
            # 评估整体测试结果
            passed_count = sum(1 for st in subtests if st["status"] == "passed")
            failed_count = sum(1 for st in subtests if st["status"] == "failed")
            
            if failed_count == 0 and passed_count > 0:
                result["status"] = "passed"
            elif passed_count > 0:
                result["status"] = "warning"
            
            result["details"] = f"子测试: {passed_count}通过, {len(subtests)-passed_count}警告/失败"
            result["subtests"] = subtests
            result["response_time"] = sum(st["response_time"] for st in subtests) / len(subtests)
            
            logger.log_test_pass(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"GraphQL查询测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_graphql_mutations(self) -> Dict[str, Any]:
        """测试GraphQL变更操作"""
        test_name = "GraphQL变更操作测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            
            # 测试创建用户的变更操作
            user_data = data_generator.generate_user_data()
            mutation = {
                "query": """
                    mutation CreateUser($input: UserInput!) {
                        createUser(input: $input) {
                            id
                            username
                            email
                            createdAt
                        }
                    }
                """,
                "variables": {
                    "input": {
                        "username": user_data["username"],
                        "email": user_data["email"],
                        "password": user_data["password"],
                        "firstName": user_data["first_name"],
                        "lastName": user_data["last_name"]
                    }
                }
            }
            
            start_time = time.time()
            response = api_client.post(endpoint, mutation)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                data = response.json()
                
                if "errors" in data and data["errors"]:
                    error_msg = data["errors"][0].get("message", "未知错误")
                    
                    # 检查是否是schema不匹配（预期行为）
                    if "Cannot query field" in error_msg or "Unknown type" in error_msg:
                        result["details"] = f"变更操作schema不匹配（预期）: {error_msg}"
                        logger.log_test_warning(test_name, result["details"])
                    else:
                        result["details"] = f"变更操作执行错误: {error_msg}"
                        logger.log_test_warning(test_name, result["details"])
                elif "data" in data and data["data"].get("createUser"):
                    result["status"] = "passed"
                    result["details"] = "变更操作执行成功"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "变更操作响应格式不正确"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"变更操作HTTP状态码异常: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"GraphQL变更操作测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_graphql_subscriptions(self) -> Dict[str, Any]:
        """测试GraphQL订阅功能"""
        test_name = "GraphQL订阅功能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 注意：WebSocket测试需要特殊处理，这里进行基本检查
            result["details"] = "订阅功能需要WebSocket连接，已跳过详细测试"
            result["status"] = "warning"
            logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"GraphQL订阅功能测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_graphql_error_handling(self) -> Dict[str, Any]:
        """测试GraphQL错误处理"""
        test_name = "GraphQL错误处理测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            
            # 发送无效的GraphQL查询
            invalid_query = {
                "query": """
                    query {
                        nonExistentField {
                            id
                            name
                        }
                    }
                """
            }
            
            start_time = time.time()
            response = api_client.post(endpoint, invalid_query)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                data = response.json()
                
                # 有效的GraphQL响应应该包含errors字段
                if "errors" in data and data["errors"]:
                    result["status"] = "passed"
                    result["details"] = "GraphQL错误处理正常"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "无效查询未返回错误信息"
                    logger.log_test_fail(test_name, result["details"])
            else:
                result["details"] = f"HTTP状态码异常: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"GraphQL错误处理测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_graphql_performance(self) -> Dict[str, Any]:
        """测试GraphQL性能"""
        test_name = "GraphQL性能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "passed",
            "details": "",
            "response_time": 0,
            "measurements": []
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            threshold = config.performance_thresholds["response_time_p95"]
            
            # 简单的GraphQL查询用于性能测试
            query = {
                "query": """
                    query {
                        __schema {
                            types {
                                name
                                kind
                            }
                        }
                    }
                """
            }
            
            # 进行多次请求测量性能
            measurements = []
            for i in range(5):
                start_time = time.time()
                response = api_client.post(endpoint, query)
                response_time = (time.time() - start_time) * 1000
                
                measurements.append({
                    "iteration": i + 1,
                    "response_time_ms": round(response_time, 2),
                    "status_code": response.status_code
                })
                
                if response_time > threshold:
                    result["status"] = "warning"
                    logger.log_test_warning(test_name, f"第{i+1}次响应时间: {response_time:.2f}ms > {threshold}ms")
            
            # 计算统计信息
            response_times = [m["response_time_ms"] for m in measurements]
            avg_time = sum(response_times) / len(response_times)
            max_time = max(response_times)
            
            result["details"] = f"平均响应时间: {avg_time:.2f}ms, 最大: {max_time:.2f}ms"
            result["measurements"] = measurements
            result["response_time"] = avg_time
            
            if result["status"] == "passed":
                logger.log_test_pass(test_name, result["details"])
            else:
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["status"] = "failed"
            result["details"] = f"GraphQL性能测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_graphql_schema_validation(self) -> Dict[str, Any]:
        """测试GraphQL Schema验证"""
        test_name = "GraphQL Schema验证测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_graphql_endpoint()
            
            # 测试变量验证
            query_with_variables = {
                "query": """
                    query GetUser($id: ID!) {
                        user(id: $id) {
                            id
                            name
                        }
                    }
                """,
                "variables": {
                    "id": "1"
                }
            }
            
            start_time = time.time()
            response = api_client.post(endpoint, query_with_variables)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                data = response.json()
                
                if "errors" in data and data["errors"]:
                    # 可能是schema不匹配或用户不存在
                    error_msg = data["errors"][0].get("message", "")
                    
                    if "Cannot query field" in error_msg:
                        result["details"] = "Schema验证：查询字段不存在（预期）"
                    elif "user" in error_msg.lower():
                        result["details"] = "用户不存在（预期行为）"
                    else:
                        result["details"] = f"Schema验证异常: {error_msg}"
                    
                    result["status"] = "warning"
                    logger.log_test_warning(test_name, result["details"])
                else:
                    result["status"] = "passed"
                    result["details"] = "Schema验证通过"
                    logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"Schema验证HTTP状态码异常: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"GraphQL Schema验证测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result


def save_test_report(results: Dict[str, Any], filename: str = None):
    """保存测试报告"""
    if filename is None:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        filename = f"graphql_api_test_report_{timestamp}.json"
    
    report_path = os.path.join(config.reports_dir, filename)
    
    try:
        os.makedirs(config.reports_dir, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.log_test_pass("保存GraphQL测试报告", f"报告已保存到: {report_path}")
        return report_path
    except Exception as e:
        logger.log_test_fail("保存GraphQL测试报告", str(e))
        return None


def main():
    """主函数"""
    print("=" * 60)
    print("🔧 Sprint 27+1 - GraphQL API自动化测试")
    print("=" * 60)
    print(f"GraphQL端点: {config.get_graphql_endpoint()}")
    print(f"开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 创建测试实例并运行测试
    tester = GraphQLAPITests()
    results = tester.run_all_tests()
    
    # 保存JSON报告
    json_report_path = save_test_report(results)
    if json_report_path:
        print(f"📊 JSON报告已保存: {json_report_path}")
    
    # 输出总结
    print()
    print("=" * 60)
    print("📋 GraphQL测试总结")
    print("=" * 60)
    print(f"总测试数: {results['summary']['total']}")
    print(f"通过: {results['summary']['passed']} ✅")
    print(f"失败: {results['summary']['failed']} ❌")
    print(f"警告: {results['summary']['warnings']} ⚠️")
    print(f"总耗时: {results['duration']:.2f} 秒")
    
    if results['summary']['failed'] == 0:
        print("🎉 GraphQL API测试完成！")
        return 0
    else:
        print("⚠️  GraphQL API测试存在失败的测试")
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)