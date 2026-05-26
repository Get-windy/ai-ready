"""
AI-Ready自动化测试框架 - 测试用例包

此包包含所有自动化测试用例，按测试类型和功能模块组织。

目录结构：
- unit/           # 单元测试
- integration/    # 集成测试
- e2e/           # 端到端测试
- performance/    # 性能测试
- api/           # API测试
- ui/            # UI测试
- data/          # 测试数据
- fixtures/      # 测试夹具
- reports/       # 测试报告
"""

__version__ = "1.0.0"
__author__ = "AI-Ready QA Team"

# 导出常用的测试工具和类
from optimized_framework.core.base_test import BaseTest

__all__ = [
    "BaseTest",
]