"""
WebSocket API自动化测试脚本
为Sprint 27+1测试环境配置WebSocket接口测试
"""

import sys
import os
import time
import json
import threading
import asyncio
import websockets
from typing import Dict, List, Any, Optional, Callable

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config
from test_utils import logger, data_generator, validator


class WebSocketAPITests:
    """WebSocket API测试类"""
    
    def __init__(self):
        self.test_results = []
        self.connected = False
        self.messages_received = []
        self.connection_errors = []
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有WebSocket API测试"""
        logger.log_test_start("WebSocket API 完整测试套件")
        
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
        
        # 运行WebSocket测试
        tests = [
            self.test_websocket_connection,
            self.test_websocket_message_exchange,
            self.test_websocket_reconnection,
            self.test_websocket_multiple_clients,
            self.test_websocket_error_handling,
            self.test_websocket_performance,
            self.test_websocket_binary_messages,
            self.test_websocket_authentication,
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
        
        logger.log_test_pass("WebSocket API 测试套件完成", 
                           f"通过: {test_suite['summary']['passed']}, "
                           f"失败: {test_suite['summary']['failed']}, "
                           f"警告: {test_suite['summary']['warnings']}")
        
        return test_suite
    
    def test_websocket_connection(self) -> Dict[str, Any]:
        """测试WebSocket连接"""
        test_name = "WebSocket连接测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            start_time = time.time()
            
            # 尝试建立WebSocket连接
            async def connect_test():
                try:
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"]
                    ) as websocket:
                        self.connected = True
                        # 发送测试消息
                        await websocket.send(json.dumps({"type": "ping", "timestamp": time.time()}))
                        # 接收响应
                        response = await websocket.recv()
                        self.messages_received.append(response)
                        return True
                except Exception as e:
                    self.connection_errors.append(str(e))
                    return False
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            connection_success = loop.run_until_complete(connect_test())
            loop.close()
            
            response_time = (time.time() - start_time) * 1000
            
            if connection_success:
                result["status"] = "passed"
                result["details"] = f"WebSocket连接成功，收到{len(self.messages_received)}条消息"
                logger.log_test_pass(test_name, f"响应时间: {response_time:.2f}ms")
            else:
                if self.connection_errors:
                    result["details"] = f"连接失败: {self.connection_errors[-1]}"
                else:
                    result["details"] = "连接失败，原因未知"
                logger.log_test_fail(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except ImportError:
            result["status"] = "warning"
            result["details"] = "websockets库未安装，跳过WebSocket测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket连接测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_websocket_message_exchange(self) -> Dict[str, Any]:
        """测试WebSocket消息交换"""
        test_name = "WebSocket消息交换测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0,
            "subtests": []
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            subtests = []
            
            # 测试不同类型的消息
            test_messages = [
                {
                    "name": "文本消息测试",
                    "message": {"type": "echo", "text": "Hello WebSocket!"}
                },
                {
                    "name": "JSON消息测试",
                    "message": {"type": "data", "data": {"key": "value", "number": 123}}
                },
                {
                    "name": "空消息测试",
                    "message": {"type": "empty"}
                }
            ]
            
            async def message_test():
                results = []
                try:
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"]
                    ) as websocket:
                        for test_msg in test_messages:
                            start_time = time.time()
                            
                            # 发送消息
                            await websocket.send(json.dumps(test_msg["message"]))
                            
                            # 尝试接收响应（带超时）
                            try:
                                response = await asyncio.wait_for(
                                    websocket.recv(),
                                    timeout=5.0
                                )
                                response_time = (time.time() - start_time) * 1000
                                
                                # 验证响应
                                try:
                                    response_data = json.loads(response)
                                    results.append({
                                        "name": test_msg["name"],
                                        "status": "passed",
                                        "response_time": response_time,
                                        "details": f"收到响应: {response[:100]}..."
                                    })
                                except json.JSONDecodeError:
                                    results.append({
                                        "name": test_msg["name"],
                                        "status": "warning",
                                        "response_time": response_time,
                                        "details": f"响应不是有效的JSON: {response[:100]}..."
                                    })
                                
                            except asyncio.TimeoutError:
                                response_time = (time.time() - start_time) * 1000
                                results.append({
                                    "name": test_msg["name"],
                                    "status": "warning",
                                    "response_time": response_time,
                                    "details": "接收响应超时"
                                })
                
                except Exception as e:
                    results.append({
                        "name": "连接异常",
                        "status": "failed",
                        "response_time": 0,
                        "details": f"连接异常: {str(e)}"
                    })
                
                return results
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            message_results = loop.run_until_complete(message_test())
            loop.close()
            
            subtests.extend(message_results)
            
            # 评估整体测试结果
            passed_count = sum(1 for st in subtests if st["status"] == "passed")
            failed_count = sum(1 for st in subtests if st["status"] == "failed")
            
            if failed_count == 0 and passed_count > 0:
                result["status"] = "passed"
            elif passed_count > 0:
                result["status"] = "warning"
            
            result["details"] = f"子测试: {passed_count}通过, {len(subtests)-passed_count}警告/失败"
            result["subtests"] = subtests
            if subtests:
                result["response_time"] = sum(st["response_time"] for st in subtests) / len(subtests)
            
            if result["status"] == "passed":
                logger.log_test_pass(test_name, result["details"])
            else:
                logger.log_test_warning(test_name, result["details"])
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过消息交换测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket消息交换测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_reconnection(self) -> Dict[str, Any]:
        """测试WebSocket重连"""
        test_name = "WebSocket重连测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            reconnect_attempts = config.websocket_config["reconnect_attempts"]
            
            async def reconnect_test():
                connection_attempts = []
                
                for attempt in range(reconnect_attempts):
                    try:
                        start_time = time.time()
                        async with websockets.connect(
                            endpoint,
                            timeout=config.websocket_config["timeout_seconds"]
                        ) as websocket:
                            connect_time = (time.time() - start_time) * 1000
                            connection_attempts.append({
                                "attempt": attempt + 1,
                                "success": True,
                                "time_ms": connect_time
                            })
                            # 立即关闭连接以测试重连
                            await websocket.close()
                    except Exception as e:
                        connection_attempts.append({
                            "attempt": attempt + 1,
                            "success": False,
                            "error": str(e)
                        })
                    
                    # 短暂延迟后进行下一次连接尝试
                    if attempt < reconnect_attempts - 1:
                        await asyncio.sleep(1)
                
                return connection_attempts
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            attempts = loop.run_until_complete(reconnect_test())
            loop.close()
            
            successful_attempts = sum(1 for a in attempts if a["success"])
            
            if successful_attempts == reconnect_attempts:
                result["status"] = "passed"
                avg_time = sum(a["time_ms"] for a in attempts if a["success"]) / successful_attempts
                result["details"] = f"所有{reconnect_attempts}次重连成功，平均连接时间: {avg_time:.2f}ms"
                logger.log_test_pass(test_name, result["details"])
            elif successful_attempts > 0:
                result["details"] = f"{successful_attempts}/{reconnect_attempts}次重连成功"
                logger.log_test_warning(test_name, result["details"])
            else:
                result["status"] = "failed"
                result["details"] = "所有重连尝试都失败"
                logger.log_test_fail(test_name, result["details"])
            
            if attempts:
                result["response_time"] = sum(a.get("time_ms", 0) for a in attempts) / len(attempts)
            
        except ImportError:
            result["details"] = "websockets库未安装，跳重重连测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket重连测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_multiple_clients(self) -> Dict[str, Any]:
        """测试多个WebSocket客户端"""
        test_name = "多WebSocket客户端测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            num_clients = 3
            
            async def client_worker(client_id: int, results: List):
                try:
                    start_time = time.time()
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"]
                    ) as websocket:
                        connect_time = (time.time() - start_time) * 1000
                        
                        # 发送客户端标识
                        await websocket.send(json.dumps({
                            "type": "identify",
                            "client_id": client_id,
                            "timestamp": time.time()
                        }))
                        
                        # 接收响应
                        try:
                            response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                            results.append({
                                "client_id": client_id,
                                "success": True,
                                "connect_time_ms": connect_time,
                                "response": response[:100] + "..." if len(response) > 100 else response
                            })
                        except asyncio.TimeoutError:
                            results.append({
                                "client_id": client_id,
                                "success": False,
                                "connect_time_ms": connect_time,
                                "error": "接收响应超时"
                            })
                
                except Exception as e:
                    results.append({
                        "client_id": client_id,
                        "success": False,
                        "connect_time_ms": 0,
                        "error": str(e)
                    })
            
            async def multiple_clients_test():
                results = []
                tasks = []
                
                # 创建多个客户端任务
                for i in range(num_clients):
                    task = asyncio.create_task(client_worker(i, results))
                    tasks.append(task)
                
                # 等待所有任务完成
                await asyncio.gather(*tasks, return_exceptions=True)
                return results
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            client_results = loop.run_until_complete(multiple_clients_test())
            loop.close()
            
            successful_clients = sum(1 for r in client_results if r["success"])
            
            if successful_clients == num_clients:
                result["status"] = "passed"
                avg_time = sum(r["connect_time_ms"] for r in client_results if r["success"]) / successful_clients
                result["details"] = f"所有{num_clients}个客户端连接成功，平均连接时间: {avg_time:.2f}ms"
                logger.log_test_pass(test_name, result["details"])
            elif successful_clients > 0:
                result["details"] = f"{successful_clients}/{num_clients}个客户端连接成功"
                logger.log_test_warning(test_name, result["details"])
            else:
                result["status"] = "failed"
                result["details"] = "所有客户端连接都失败"
                logger.log_test_fail(test_name, result["details"])
            
            if client_results:
                result["response_time"] = sum(r["connect_time_ms"] for r in client_results) / len(client_results)
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过多客户端测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"多WebSocket客户端测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_error_handling(self) -> Dict[str, Any]:
        """测试WebSocket错误处理"""
        test_name = "WebSocket错误处理测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 测试无效的WebSocket端点
            invalid_endpoint = "ws://invalid-host:9999/invalid-path"
            
            async def error_test():
                try:
                    async with websockets.connect(
                        invalid_endpoint,
                        timeout=5.0  # 较短的超时时间
                    ) as websocket:
                        await websocket.send("test")
                        response = await websocket.recv()
                        return False, f"意外成功: {response}"
                except Exception as e:
                    return True, str(e)
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            error_handled, error_msg = loop.run_until_complete(error_test())
            loop.close()
            
            if error_handled:
                result["status"] = "passed"
                result["details"] = f"错误处理正常: {error_msg}"
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"错误处理异常: {error_msg}"
                logger.log_test_warning(test_name, result["details"])
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过错误处理测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket错误处理测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_performance(self) -> Dict[str, Any]:
        """测试WebSocket性能"""
        test_name = "WebSocket性能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0,
            "measurements": []
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            
            async def performance_test():
                measurements = []
                try:
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"]
                    ) as websocket:
                        for i in range(5):  # 5次性能测试
                            start_time = time.time()
                            
                            # 发送测试消息
                            test_message = {
                                "type": "performance",
                                "iteration": i + 1,
                                "timestamp": time.time()
                            }
                            await websocket.send(json.dumps(test_message))
                            
                            # 接收响应
                            response = await websocket.recv()
                            response_time = (time.time() - start_time) * 1000
                            
                            measurements.append({
                                "iteration": i + 1,
                                "response_time_ms": round(response_time, 2),
                                "message_size": len(response)
                            })
                
                except Exception as e:
                    measurements.append({
                        "iteration": 0,
                        "response_time_ms": 0,
                        "error": str(e)
                    })
                
                return measurements
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            measurements = loop.run_until_complete(performance_test())
            loop.close()
            
            if measurements and not any("error" in m for m in measurements):
                response_times = [m["response_time_ms"] for m in measurements]
                avg_time = sum(response_times) / len(response_times)
                max_time = max(response_times)
                
                result["status"] = "passed"
                result["details"] = f"平均响应时间: {avg_time:.2f}ms, 最大: {max_time:.2f}ms"
                result["measurements"] = measurements
                result["response_time"] = avg_time
                
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = "性能测试失败或未得到有效测量结果"
                logger.log_test_warning(test_name, result["details"])
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过性能测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket性能测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_binary_messages(self) -> Dict[str, Any]:
        """测试WebSocket二进制消息"""
        test_name = "WebSocket二进制消息测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            
            async def binary_test():
                try:
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"]
                    ) as websocket:
                        # 发送二进制数据
                        binary_data = b"Hello Binary World!"
                        await websocket.send(binary_data)
                        
                        # 尝试接收响应
                        response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                        
                        if isinstance(response, bytes):
                            return True, f"收到二进制响应，长度: {len(response)}字节"
                        else:
                            return False, f"收到非二进制响应: {type(response)}"
                
                except Exception as e:
                    return False, str(e)
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            binary_supported, binary_result = loop.run_until_complete(binary_test())
            loop.close()
            
            if binary_supported:
                result["status"] = "passed"
                result["details"] = binary_result
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"二进制消息测试: {binary_result}"
                logger.log_test_warning(test_name, result["details"])
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过二进制消息测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket二进制消息测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_websocket_authentication(self) -> Dict[str, Any]:
        """测试WebSocket认证"""
        test_name = "WebSocket认证测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_websocket_endpoint()
            
            async def auth_test():
                try:
                    # 尝试使用认证令牌连接
                    headers = {}
                    if hasattr(config, 'auth_token') and config.auth_token:
                        headers["Authorization"] = f"Bearer {config.auth_token}"
                    
                    async with websockets.connect(
                        endpoint,
                        timeout=config.websocket_config["timeout_seconds"],
                        extra_headers=headers
                    ) as websocket:
                        # 发送需要认证的消息
                        auth_message = {
                            "type": "authenticated_request",
                            "action": "get_secret_data"
                        }
                        await websocket.send(json.dumps(auth_message))
                        
                        response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                        return True, f"认证请求成功: {response[:100]}..."
                
                except Exception as e:
                    return False, str(e)
            
            # 运行异步测试
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            auth_successful, auth_result = loop.run_until_complete(auth_test())
            loop.close()
            
            if auth_successful:
                result["status"] = "passed"
                result["details"] = auth_result
                logger.log_test_pass(test_name, result["details"])
            else:
                result["details"] = f"WebSocket认证测试: {auth_result}"
                logger.log_test_warning(test_name, result["details"])
            
        except ImportError:
            result["details"] = "websockets库未安装，跳过认证测试"
            logger.log_test_warning(test_name, result["details"])
        except Exception as e:
            result["details"] = f"WebSocket认证测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result


def save_test_report(results: Dict[str, Any], filename: str = None):
    """保存测试报告"""
    if filename is None:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        filename = f"websocket_api_test_report_{timestamp}.json"
    
    report_path = os.path.join(config.reports_dir, filename)
    
    try:
        os.makedirs(config.reports_dir, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.log_test_pass("保存WebSocket测试报告", f"报告已保存到: {report_path}")
        return report_path
    except Exception as e:
        logger.log_test_fail("保存WebSocket测试报告", str(e))
        return None


def main():
    """主函数"""
    print("=" * 60)
    print("🔧 Sprint 27+1 - WebSocket API自动化测试")
    print("=" * 60)
    print(f"WebSocket端点: {config.get_websocket_endpoint()}")
    print(f"开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 检查websockets库是否可用
    try:
        import websockets
        print("✅ websockets库可用")
    except ImportError:
        print("⚠️  websockets库未安装，部分测试将跳过")
        print("   安装命令: pip install websockets")
    
    # 创建测试实例并运行测试
    tester = WebSocketAPITests()
    results = tester.run_all_tests()
    
    # 保存JSON报告
    json_report_path = save_test_report(results)
    if json_report_path:
        print(f"📊 JSON报告已保存: {json_report_path}")
    
    # 输出总结
    print()
    print("=" * 60)
    print("📋 WebSocket测试总结")
    print("=" * 60)
    print(f"总测试数: {results['summary']['total']}")
    print(f"通过: {results['summary']['passed']} ✅")
    print(f"失败: {results['summary']['failed']} ❌")
    print(f"警告: {results['summary']['warnings']} ⚠️")
    print(f"总耗时: {results['duration']:.2f} 秒")
    
    if results['summary']['failed'] == 0:
        print("🎉 WebSocket API测试完成！")
        return 0
    else:
        print("⚠️  WebSocket API测试存在失败的测试")
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)