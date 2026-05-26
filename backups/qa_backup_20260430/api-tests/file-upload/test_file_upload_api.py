"""
文件上传/下载API自动化测试脚本
为Sprint 27+1测试环境配置文件上传/下载接口测试
"""

import sys
import os
import time
import json
import io
import mimetypes
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path

# 添加父目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config
from test_utils import logger, api_client, data_generator, validator


class FileUploadAPITests:
    """文件上传/下载API测试类"""
    
    def __init__(self):
        self.test_results = []
        self.uploaded_files = []  # 跟踪上传的文件，用于清理
        self.test_files_dir = os.path.join(os.path.dirname(__file__), "test-files")
        self._create_test_files()
    
    def _create_test_files(self):
        """创建测试文件"""
        os.makedirs(self.test_files_dir, exist_ok=True)
        
        # 创建文本文件
        text_file_path = os.path.join(self.test_files_dir, "test.txt")
        with open(text_file_path, 'w', encoding='utf-8') as f:
            f.write("这是一个测试文本文件。\n" * 10)
            f.write(f"生成时间: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
        
        # 创建CSV文件
        csv_file_path = os.path.join(self.test_files_dir, "test.csv")
        with open(csv_file_path, 'w', encoding='utf-8') as f:
            f.write("id,name,email,age\n")
            for i in range(10):
                f.write(f"{i+1},User{i+1},user{i+1}@example.com,{20 + i}\n")
        
        # 创建JSON文件
        json_file_path = os.path.join(self.test_files_dir, "test.json")
        with open(json_file_path, 'w', encoding='utf-8') as f:
            json.dump({
                "test_data": {
                    "string": "测试字符串",
                    "number": 123.45,
                    "boolean": True,
                    "array": [1, 2, 3, 4, 5],
                    "object": {"key": "value"}
                },
                "timestamp": time.time(),
                "description": "测试JSON文件"
            }, f, indent=2, ensure_ascii=False)
        
        # 创建大文件（用于测试文件大小限制）
        large_file_path = os.path.join(self.test_files_dir, "large_test.dat")
        with open(large_file_path, 'wb') as f:
            # 创建5MB的文件
            f.write(b"0" * 5 * 1024 * 1024)
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有文件上传/下载测试"""
        logger.log_test_start("文件上传/下载 API 完整测试套件")
        
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
        
        # 运行文件上传/下载测试
        tests = [
            self.test_file_upload_basic,
            self.test_multiple_file_upload,
            self.test_file_upload_with_metadata,
            self.test_file_size_limits,
            self.test_file_type_restrictions,
            self.test_file_download,
            self.test_file_delete,
            self.test_file_list,
            self.test_upload_performance,
            self.test_upload_error_handling,
        ]
        
        for test_func in tests:
            result = test_func()
            test_suite["tests"].append(result)
        
        # 清理测试文件
        self.cleanup_uploaded_files()
        
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
        
        logger.log_test_pass("文件上传/下载 API 测试套件完成", 
                           f"通过: {test_suite['summary']['passed']}, "
                           f"失败: {test_suite['summary']['failed']}, "
                           f"警告: {test_suite['summary']['warnings']}")
        
        return test_suite
    
    def test_file_upload_basic(self) -> Dict[str, Any]:
        """测试基本文件上传"""
        test_name = "基本文件上传测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0,
            "subtests": []
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            subtests = []
            
            # 测试上传不同类型的文件
            test_files = [
                {"path": os.path.join(self.test_files_dir, "test.txt"), "type": "text/plain"},
                {"path": os.path.join(self.test_files_dir, "test.csv"), "type": "text/csv"},
                {"path": os.path.join(self.test_files_dir, "test.json"), "type": "application/json"},
            ]
            
            for test_file in test_files:
                if not os.path.exists(test_file["path"]):
                    subtests.append({
                        "name": f"上传 {os.path.basename(test_file['path'])}",
                        "status": "failed",
                        "response_time": 0,
                        "details": "测试文件不存在"
                    })
                    continue
                
                file_size = os.path.getsize(test_file["path"])
                file_name = os.path.basename(test_file["path"])
                
                try:
                    start_time = time.time()
                    
                    # 准备文件上传
                    with open(test_file["path"], 'rb') as f:
                        files = {
                            'file': (file_name, f, test_file["type"])
                        }
                        
                        # 发送上传请求
                        response = api_client.session.post(
                            endpoint,
                            files=files,
                            timeout=config.test_execution["timeout_seconds"]
                        )
                    
                    response_time = (time.time() - start_time) * 1000
                    
                    if validator.validate_response_status(response, 200):
                        upload_data = response.json()
                        file_id = upload_data.get("id") or upload_data.get("fileId")
                        
                        if file_id:
                            self.uploaded_files.append(file_id)
                            subtests.append({
                                "name": f"上传 {file_name}",
                                "status": "passed",
                                "response_time": response_time,
                                "details": f"文件ID: {file_id}, 大小: {file_size}字节",
                                "file_id": file_id
                            })
                        else:
                            subtests.append({
                                "name": f"上传 {file_name}",
                                "status": "warning",
                                "response_time": response_time,
                                "details": "响应中未找到文件ID"
                            })
                    else:
                        subtests.append({
                            "name": f"上传 {file_name}",
                            "status": "failed",
                            "response_time": response_time,
                            "details": f"HTTP状态码: {response.status_code}"
                        })
                
                except Exception as e:
                    subtests.append({
                        "name": f"上传 {file_name}",
                        "status": "failed",
                        "response_time": 0,
                        "details": f"上传异常: {str(e)}"
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
            if subtests:
                result["response_time"] = sum(st["response_time"] for st in subtests) / len(subtests)
            
            if result["status"] == "passed":
                logger.log_test_pass(test_name, result["details"])
            else:
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"基本文件上传测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def test_multiple_file_upload(self) -> Dict[str, Any]:
        """测试多文件上传"""
        test_name = "多文件上传测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            
            # 准备多个文件
            files = []
            for i in range(3):
                file_content = f"测试文件内容 {i+1}\n" * 10
                files.append(
                    (f'test_file_{i+1}.txt', io.BytesIO(file_content.encode('utf-8')), 'text/plain')
                )
            
            # 构建多文件上传请求
            file_dict = {}
            for i, (filename, fileobj, content_type) in enumerate(files):
                file_dict[f'files[{i}]'] = (filename, fileobj, content_type)
            
            start_time = time.time()
            
            # 发送多文件上传请求
            response = api_client.session.post(
                endpoint,
                files=file_dict,
                timeout=config.test_execution["timeout_seconds"]
            )
            
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                upload_data = response.json()
                
                if isinstance(upload_data, list):
                    # 多个文件的响应
                    uploaded_count = len(upload_data)
                    file_ids = [f.get("id") for f in upload_data if f.get("id")]
                    
                    for file_id in file_ids:
                        if file_id:
                            self.uploaded_files.append(file_id)
                    
                    result["status"] = "passed"
                    result["details"] = f"成功上传 {uploaded_count} 个文件"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "多文件上传响应格式不正确"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"多文件上传HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"多文件上传测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_upload_with_metadata(self) -> Dict[str, Any]:
        """测试带元数据的文件上传"""
        test_name = "带元数据文件上传测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            
            # 创建测试文件
            test_content = "带元数据测试文件内容\n" * 5
            file_obj = io.BytesIO(test_content.encode('utf-8'))
            
            # 准备元数据
            metadata = {
                "description": "测试文件描述",
                "tags": ["test", "automation", "api"],
                "category": "test-files",
                "created_by": "api-tester",
                "priority": "normal"
            }
            
            start_time = time.time()
            
            # 发送带元数据的文件上传请求
            files = {
                'file': ('metadata_test.txt', file_obj, 'text/plain')
            }
            
            data = {
                'metadata': json.dumps(metadata)
            }
            
            response = api_client.session.post(
                endpoint,
                files=files,
                data=data,
                timeout=config.test_execution["timeout_seconds"]
            )
            
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                upload_data = response.json()
                file_id = upload_data.get("id")
                
                if file_id:
                    self.uploaded_files.append(file_id)
                    result["status"] = "passed"
                    result["details"] = f"带元数据文件上传成功，文件ID: {file_id}"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "响应中未找到文件ID"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"带元数据文件上传HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"带元数据文件上传测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_size_limits(self) -> Dict[str, Any]:
        """测试文件大小限制"""
        test_name = "文件大小限制测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0,
            "subtests": []
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            max_size_mb = config.file_upload_config["max_file_size_mb"]
            max_size_bytes = max_size_mb * 1024 * 1024
            
            subtests = []
            
            # 测试1: 上传小文件（应该成功）
            small_file_content = b"X" * 1024  # 1KB
            small_file = io.BytesIO(small_file_content)
            
            start_time = time.time()
            files = {'file': ('small_test.txt', small_file, 'text/plain')}
            response = api_client.session.post(endpoint, files=files)
            small_file_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                subtests.append({
                    "name": "小文件上传",
                    "status": "passed",
                    "response_time": small_file_time,
                    "details": "1KB文件上传成功"
                })
            else:
                subtests.append({
                    "name": "小文件上传",
                    "status": "warning",
                    "response_time": small_file_time,
                    "details": f"小文件上传失败，状态码: {response.status_code}"
                })
            
            # 测试2: 上传大文件（可能失败）
            large_file_path = os.path.join(self.test_files_dir, "large_test.dat")
            if os.path.exists(large_file_path):
                file_size = os.path.getsize(large_file_path)
                
                start_time = time.time()
                with open(large_file_path, 'rb') as f:
                    files = {'file': ('large_test.dat', f, 'application/octet-stream')}
                    response = api_client.session.post(endpoint, files=files)
                large_file_time = (time.time() - start_time) * 1000
                
                if validator.validate_response_status(response, 413) or validator.validate_response_status(response, 400):
                    subtests.append({
                        "name": "大文件上传",
                        "status": "passed",
                        "response_time": large_file_time,
                        "details": f"大文件({file_size}字节)上传被正确拒绝"
                    })
                elif validator.validate_response_status(response, 200):
                    subtests.append({
                        "name": "大文件上传",
                        "status": "warning",
                        "response_time": large_file_time,
                        "details": f"大文件({file_size}字节)上传成功，可能未设置大小限制"
                    })
                else:
                    subtests.append({
                        "name": "大文件上传",
                        "status": "warning",
                        "response_time": large_file_time,
                        "details": f"大文件上传状态码: {response.status_code}"
                    })
            
            # 评估整体测试结果
            passed_count = sum(1 for st in subtests if st["status"] == "passed")
            
            if passed_count == len(subtests):
                result["status"] = "passed"
            elif passed_count > 0:
                result["status"] = "warning"
            
            result["details"] = f"文件大小限制测试完成"
            result["subtests"] = subtests
            if subtests:
                result["response_time"] = sum(st["response_time"] for st in subtests) / len(subtests)
            
            if result["status"] == "passed":
                logger.log_test_pass(test_name, result["details"])
            else:
                logger.log_test_warning(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"文件大小限制测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_type_restrictions(self) -> Dict[str, Any]:
        """测试文件类型限制"""
        test_name = "文件类型限制测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            allowed_formats = config.file_upload_config["allowed_formats"]
            
            # 测试上传不允许的文件类型
            disallowed_content = b"恶意文件内容"
            disallowed_file = io.BytesIO(disallowed_content)
            
            start_time = time.time()
            files = {'file': ('test.exe', disallowed_file, 'application/x-msdownload')}
            response = api_client.session.post(endpoint, files=files)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 415) or validator.validate_response_status(response, 400):
                result["status"] = "passed"
                result["details"] = "文件类型限制有效，不允许的文件被拒绝"
                logger.log_test_pass(test_name, result["details"])
            elif validator.validate_response_status(response, 200):
                result["details"] = "文件类型限制可能未生效，.exe文件被接受"
                logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"文件类型测试状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"文件类型限制测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_download(self) -> Dict[str, Any]:
        """测试文件下载"""
        test_name = "文件下载测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 首先需要上传一个文件用于下载测试
            if not self.uploaded_files:
                result["details"] = "没有已上传的文件可用于下载测试"
                logger.log_test_warning(test_name, result["details"])
                return result
            
            # 使用第一个上传的文件进行下载测试
            file_id = self.uploaded_files[0]
            
            # 假设下载端点为 /api/v1/files/{id}/download
            download_endpoint = f"{config.base_url}/api/{config.api_version}/files/{file_id}/download"
            
            start_time = time.time()
            response = api_client.get(download_endpoint)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                # 检查响应头
                content_type = response.headers.get('Content-Type', '')
                content_disposition = response.headers.get('Content-Disposition', '')
                
                if 'application/octet-stream' in content_type or 'attachment' in content_disposition:
                    result["status"] = "passed"
                    result["details"] = f"文件下载成功，大小: {len(response.content)}字节"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = f"下载响应头异常: Content-Type={content_type}"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"文件下载HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"文件下载测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_delete(self) -> Dict[str, Any]:
        """测试文件删除"""
        test_name = "文件删除测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 首先需要上传一个文件用于删除测试
            if not self.uploaded_files:
                result["details"] = "没有已上传的文件可用于删除测试"
                logger.log_test_warning(test_name, result["details"])
                return result
            
            # 使用最后一个上传的文件进行删除测试
            file_id = self.uploaded_files[-1]
            
            # 假设删除端点为 /api/v1/files/{id}
            delete_endpoint = f"{config.base_url}/api/{config.api_version}/files/{file_id}"
            
            start_time = time.time()
            response = api_client.delete(delete_endpoint)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200) or validator.validate_response_status(response, 204):
                result["status"] = "passed"
                result["details"] = f"文件删除成功，文件ID: {file_id}"
                logger.log_test_pass(test_name, result["details"])
                
                # 从已上传文件列表中移除
                if file_id in self.uploaded_files:
                    self.uploaded_files.remove(file_id)
            else:
                result["details"] = f"文件删除HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"文件删除测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_file_list(self) -> Dict[str, Any]:
        """测试文件列表获取"""
        test_name = "文件列表获取测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0
        }
        
        try:
            # 假设文件列表端点为 /api/v1/files
            list_endpoint = f"{config.base_url}/api/{config.api_version}/files"
            
            start_time = time.time()
            response = api_client.get(list_endpoint)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 200):
                files_data = response.json()
                
                if isinstance(files_data, list):
                    result["status"] = "passed"
                    result["details"] = f"成功获取文件列表，共 {len(files_data)} 个文件"
                    logger.log_test_pass(test_name, result["details"])
                else:
                    result["details"] = "文件列表响应格式不正确"
                    logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"文件列表获取HTTP状态码: {response.status_code}"
                logger.log_test_warning(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"文件列表获取测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_upload_performance(self) -> Dict[str, Any]:
        """测试上传性能"""
        test_name = "文件上传性能测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "warning",
            "details": "",
            "response_time": 0,
            "measurements": []
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            threshold = config.performance_thresholds["response_time_p95"]
            
            # 进行多次上传测量性能
            measurements = []
            for i in range(3):  # 3次性能测试
                file_content = f"性能测试文件 {i+1}\n" * 100
                file_obj = io.BytesIO(file_content.encode('utf-8'))
                
                start_time = time.time()
                files = {'file': (f'perf_test_{i+1}.txt', file_obj, 'text/plain')}
                response = api_client.session.post(endpoint, files=files)
                response_time = (time.time() - start_time) * 1000
                
                measurements.append({
                    "iteration": i + 1,
                    "response_time_ms": round(response_time, 2),
                    "status_code": response.status_code,
                    "file_size": len(file_content)
                })
                
                if response_time > threshold:
                    result["status"] = "warning"
                    logger.log_test_warning(test_name, f"第{i+1}次响应时间: {response_time:.2f}ms > {threshold}ms")
                
                # 如果上传成功，记录文件ID
                if validator.validate_response_status(response, 200):
                    upload_data = response.json()
                    file_id = upload_data.get("id")
                    if file_id:
                        self.uploaded_files.append(file_id)
            
            # 计算统计信息
            response_times = [m["response_time_ms"] for m in measurements]
            avg_time = sum(response_times) / len(response_times)
            max_time = max(response_times)
            
            result["details"] = f"平均上传时间: {avg_time:.2f}ms, 最大: {max_time:.2f}ms"
            result["measurements"] = measurements
            result["response_time"] = avg_time
            
            if result["status"] == "warning":
                logger.log_test_warning(test_name, result["details"])
            else:
                result["status"] = "passed"
                logger.log_test_pass(test_name, result["details"])
            
        except Exception as e:
            result["details"] = f"文件上传性能测试异常: {str(e)}"
            logger.log_test_warning(test_name, str(e))
        
        return result
    
    def test_upload_error_handling(self) -> Dict[str, Any]:
        """测试上传错误处理"""
        test_name = "文件上传错误处理测试"
        logger.log_test_start(test_name)
        
        result = {
            "name": test_name,
            "status": "failed",
            "details": "",
            "response_time": 0
        }
        
        try:
            endpoint = config.get_file_upload_endpoint()
            
            # 测试空文件上传
            empty_file = io.BytesIO(b"")
            
            start_time = time.time()
            files = {'file': ('empty.txt', empty_file, 'text/plain')}
            response = api_client.session.post(endpoint, files=files)
            response_time = (time.time() - start_time) * 1000
            
            if validator.validate_response_status(response, 400):
                result["status"] = "passed"
                result["details"] = "空文件上传被正确拒绝"
                logger.log_test_pass(test_name, result["details"])
            elif validator.validate_response_status(response, 200):
                result["details"] = "空文件上传被接受，可能未验证文件大小"
                logger.log_test_warning(test_name, result["details"])
            else:
                result["details"] = f"空文件上传状态码: {response.status_code}"
                logger.log_test_fail(test_name, result["details"])
            
            result["response_time"] = response_time
            
        except Exception as e:
            result["details"] = f"文件上传错误处理测试异常: {str(e)}"
            logger.log_test_fail(test_name, str(e))
        
        return result
    
    def cleanup_uploaded_files(self):
        """清理上传的测试文件"""
        logger.log_test_start("清理上传的测试文件")
        
        cleanup_count = 0
        for file_id in self.uploaded_files[:]:  # 使用副本遍历
            try:
                delete_endpoint = f"{config.base_url}/api/{config.api_version}/files/{file_id}"
                response = api_client.delete(delete_endpoint)
                
                if validator.validate_response_status(response, 200) or validator.validate_response_status(response, 204):
                    self.uploaded_files.remove(file_id)
                    cleanup_count += 1
            except:
                pass  # 忽略清理错误
        
        logger.log_test_pass("清理上传的测试文件", f"清理了 {cleanup_count} 个文件")


def save_test_report(results: Dict[str, Any], filename: str = None):
    """保存测试报告"""
    if filename is None:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        filename = f"file_upload_api_test_report_{timestamp}.json"
    
    report_path = os.path.join(config.reports_dir, filename)
    
    try:
        os.makedirs(config.reports_dir, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.log_test_pass("保存文件上传测试报告", f"报告已保存到: {report_path}")
        return report_path
    except Exception as e:
        logger.log_test_fail("保存文件上传测试报告", str(e))
        return None


def main():
    """主函数"""
    print("=" * 60)
    print("🔧 Sprint 27+1 - 文件上传/下载API自动化测试")
    print("=" * 60)
    print(f"上传端点: {config.get_file_upload_endpoint()}")
    print(f"最大文件大小: {config.file_upload_config['max_file_size_mb']}MB")
    print(f"允许的文件格式: {', '.join(config.file_upload_config['allowed_formats'])}")
    print(f"开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 创建测试实例并运行测试
    tester = FileUploadAPITests()
    results = tester.run_all_tests()
    
    # 保存JSON报告
    json_report_path = save_test_report(results)
    if json_report_path:
        print(f"📊 JSON报告已保存: {json_report_path}")
    
    # 输出总结
    print()
    print("=" * 60)
    print("📋 文件上传/下载测试总结")
    print("=" * 60)
    print(f"总测试数: {results['summary']['total']}")
    print(f"通过: {results['summary']['passed']} ✅")
    print(f"失败: {results['summary']['failed']} ❌")
    print(f"警告: {results['summary']['warnings']} ⚠️")
    print(f"总耗时: {results['duration']:.2f} 秒")
    
    if results['summary']['failed'] == 0:
        print("🎉 文件上传/下载API测试完成！")
        return 0
    else:
        print("⚠️  文件上传/下载API测试存在失败的测试")
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)