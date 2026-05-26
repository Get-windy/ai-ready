# Sprint 27+1 - 测试环境API接口自动化测试框架

## 📋 概述

为Sprint 27+1测试环境配置的完整API接口自动化测试框架，支持：
- RESTful API测试
- GraphQL API测试  
- WebSocket API测试
- 文件上传/下载API测试

## 🏗️ 项目结构

```
qa/api-tests/
├── config.py                    # 测试配置
├── test_utils.py                # 测试工具类
├── run_all_api_tests.py         # 主测试运行脚本
├── README.md                    # 本文档
│
├── restful/                     # RESTful API测试
│   └── test_restful_api.py      # RESTful API测试脚本
│
├── graphql/                     # GraphQL API测试
│   └── test_graphql_api.py      # GraphQL API测试脚本
│
├── websocket/                   # WebSocket API测试
│   └── test_websocket_api.py    # WebSocket API测试脚本
│
├── file-upload/                 # 文件上传/下载测试
│   └── test_file_upload_api.py  # 文件上传/下载测试脚本
│
├── test-data/                   # 测试数据目录
│   └── (自动生成的测试文件)
│
└── reports/                     # 测试报告目录
    └── (自动生成的测试报告)
```

## 🚀 快速开始

### 1. 安装依赖

```bash
# 基础依赖
pip install requests

# WebSocket测试依赖（可选）
pip install websockets

# 如果使用虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows
pip install -r requirements.txt
```

### 2. 配置测试环境

编辑 `config.py` 文件，配置测试环境参数：

```python
# 主要配置项
base_url = "http://localhost:8080"  # 测试环境URL
api_version = "v1"                  # API版本

# 认证配置
auth_config = {
    "username": "test_user",
    "password": "test_password_123",
}

# 性能阈值
performance_thresholds = {
    "response_time_p95": 500,  # P95响应时间阈值（毫秒）
    "success_rate": 99,        # 成功率阈值（百分比）
}
```

### 3. 运行测试

#### 运行所有测试
```bash
python run_all_api_tests.py
```

#### 运行特定测试
```bash
# RESTful API测试
python restful/test_restful_api.py

# GraphQL API测试
python graphql/test_graphql_api.py

# WebSocket API测试
python websocket/test_websocket_api.py

# 文件上传/下载测试
python file-upload/test_file_upload_api.py
```

## 🔧 测试功能

### RESTful API测试
- ✅ 身份认证测试
- ✅ 健康检查端点测试
- ✅ 用户/产品/订单CRUD测试
- ✅ 错误处理测试
- ✅ 性能测试
- ✅ 分页功能测试
- ✅ 搜索过滤测试
- ✅ 数据验证测试
- ✅ 并发请求测试

### GraphQL API测试
- ✅ GraphQL端点可用性测试
- ✅ 自省查询测试
- ✅ GraphQL查询测试
- ✅ GraphQL变更操作测试
- ✅ GraphQL订阅功能测试
- ✅ GraphQL错误处理测试
- ✅ GraphQL性能测试
- ✅ GraphQL Schema验证测试

### WebSocket API测试
- ✅ WebSocket连接测试
- ✅ WebSocket消息交换测试
- ✅ WebSocket重连测试
- ✅ 多WebSocket客户端测试
- ✅ WebSocket错误处理测试
- ✅ WebSocket性能测试
- ✅ WebSocket二进制消息测试
- ✅ WebSocket认证测试

### 文件上传/下载测试
- ✅ 基本文件上传测试
- ✅ 多文件上传测试
- ✅ 带元数据文件上传测试
- ✅ 文件大小限制测试
- ✅ 文件类型限制测试
- ✅ 文件下载测试
- ✅ 文件删除测试
- ✅ 文件列表获取测试
- ✅ 上传性能测试
- ✅ 上传错误处理测试

## 📊 测试报告

测试完成后，报告会自动生成在 `reports/` 目录：

### 报告文件
- `api_tests_overall_report_YYYYMMDD_HHMMSS.json` - 总体JSON报告
- `api_tests_summary_YYYYMMDD_HHMMSS.txt` - 文本总结报告
- `restful_api_test_report_YYYYMMDD_HHMMSS.json` - RESTful测试报告
- `graphql_api_test_report_YYYYMMDD_HHMMSS.json` - GraphQL测试报告
- `websocket_api_test_report_YYYYMMDD_HHMMSS.json` - WebSocket测试报告
- `file_upload_api_test_report_YYYYMMDD_HHMMSS.json` - 文件上传测试报告

### 报告内容
- 测试统计（通过/失败/警告数量）
- 各模块详细结果
- 性能指标（响应时间、成功率）
- 错误信息和堆栈跟踪
- 测试执行时间

## ⚙️ 配置说明

### 性能阈值配置
```python
performance_thresholds = {
    "response_time_p95": 500,    # P95响应时间不超过500ms
    "success_rate": 99,          # 成功率不低于99%
    "concurrent_users": 10,      # 并发用户数
    "duration_seconds": 60,      # 测试持续时间
}
```

### 文件上传配置
```python
file_upload_config = {
    "max_file_size_mb": 10,      # 最大文件大小10MB
    "allowed_formats": [".jpg", ".png", ".pdf", ".txt", ".csv"],
    "upload_endpoint": "/api/{version}/files/upload",
}
```

### 测试执行配置
```python
test_execution = {
    "retry_attempts": 3,         # 重试次数
    "retry_delay_seconds": 2,    # 重试延迟
    "timeout_seconds": 30,       # 请求超时时间
    "verify_ssl": False,         # 是否验证SSL证书
}
```

## 🔍 测试用例设计

### 测试数据生成
框架自动生成测试数据：
- 随机用户数据
- 随机产品数据  
- 随机订单数据
- 随机文件数据

### 测试验证
- 响应状态码验证
- 响应时间验证
- JSON Schema验证
- 错误响应验证
- 分页响应验证

## 🛠️ 扩展和定制

### 添加新的测试用例
1. 在相应的测试类中添加新的测试方法
2. 遵循现有的测试方法命名规范
3. 使用提供的测试工具类（TestLogger, APIClient等）
4. 更新测试套件运行方法

### 自定义测试数据
1. 在 `test_utils.py` 的 `TestDataGenerator` 类中添加新的数据生成方法
2. 在 `config.py` 的 `generate_test_data()` 函数中添加自定义数据

### 集成到CI/CD
```yaml
# GitLab CI示例
api-tests:
  stage: test
  script:
    - pip install requests websockets
    - python qa/api-tests/run_all_api_tests.py
  artifacts:
    paths:
      - qa/api-tests/reports/
    when: always
```

## 🐛 故障排除

### 常见问题

1. **连接被拒绝**
   - 检查测试环境URL配置
   - 确保测试服务正在运行
   - 检查防火墙和网络设置

2. **认证失败**
   - 检查用户名和密码配置
   - 验证认证端点是否正确
   - 检查认证令牌的有效期

3. **测试超时**
   - 增加 `timeout_seconds` 配置
   - 检查网络延迟
   - 优化测试用例，减少不必要的等待

4. **依赖包缺失**
   - 运行 `pip install -r requirements.txt`
   - 检查Python版本兼容性

### 调试模式
设置环境变量启用详细日志：
```bash
export API_TEST_DEBUG=1
python run_all_api_tests.py
```

## 📈 性能监控

### 监控指标
- API响应时间（P50, P95, P99）
- 请求成功率
- 并发处理能力
- 错误率
- 资源使用率（CPU、内存）

### 性能基准
- RESTful API: P95响应时间 ≤ 500ms
- GraphQL API: P95响应时间 ≤ 800ms  
- WebSocket: 连接建立时间 ≤ 1000ms
- 文件上传: 10MB文件上传时间 ≤ 5000ms

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如有问题或建议，请：
1. 查看 [FAQ](#故障排除) 部分
2. 创建Issue报告问题
3. 联系项目维护者

---

**最后更新**: 2026-04-29  
**版本**: 1.0.0  
**状态**: ✅ 生产就绪