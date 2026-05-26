#!/usr/bin/env python3
"""
AI-Ready自动化测试框架 - 安装脚本

这个脚本用于将测试框架安装为Python包，
便于在其他项目中作为依赖使用。
"""

import os
import sys
from pathlib import Path
from setuptools import setup, find_packages

# 读取项目根目录
project_root = Path(__file__).parent

# 读取README.md
readme_path = project_root / "README.md"
if readme_path.exists():
    with open(readme_path, "r", encoding="utf-8") as f:
        long_description = f.read()
else:
    long_description = "AI-Ready企业级ERP系统自动化测试框架"

# 读取requirements.txt
requirements_path = project_root / "requirements.txt"
if requirements_path.exists():
    with open(requirements_path, "r", encoding="utf-8") as f:
        requirements = [
            line.strip() 
            for line in f 
            if line.strip() and not line.startswith("#")
        ]
else:
    requirements = []

# 读取开发依赖
dev_requirements_path = project_root / "requirements-dev.txt"
if dev_requirements_path.exists():
    with open(dev_requirements_path, "r", encoding="utf-8") as f:
        dev_requirements = [
            line.strip()
            for line in f
            if line.strip() and not line.startswith("#")
        ]
else:
    dev_requirements = []

# 包信息
setup(
    # 基本信息
    name="ai-ready-test-framework",
    version="2.0.0",
    author="AI-Ready QA Team",
    author_email="qa@ai-ready.local",
    description="AI-Ready企业级ERP系统自动化测试框架",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/ai-ready/test-framework",
    
    # 包发现
    packages=find_packages(include=["optimized_framework", "tests"]),
    include_package_data=True,
    package_data={
        "optimized_framework": [
            "config/*.yaml",
            "config/environments/*.yaml",
            "core/*.py",
        ],
        "tests": [
            "*",
            "unit/*",
            "integration/*",
            "e2e/*",
            "performance/*",
            "api/*",
            "ui/*",
            "data/*",
            "fixtures/*",
            "reports/*",
        ],
    },
    
    # 依赖
    install_requires=requirements,
    extras_require={
        "dev": dev_requirements,
        "api": [
            "requests>=2.28.0",
            "httpx>=0.24.0",
            "jsonschema>=4.17.0",
        ],
        "ui": [
            "selenium>=4.8.0",
            "playwright>=1.35.0",
            "pytest-playwright>=0.4.0",
        ],
        "performance": [
            "locust>=2.15.0",
            "pytest-benchmark>=4.0.0",
        ],
        "database": [
            "psycopg2-binary>=2.9.0",
            "redis>=4.5.0",
            "sqlalchemy>=2.0.0",
        ],
    },
    
    # 入口点
    entry_points={
        "console_scripts": [
            "ai-ready-test=optimized_framework.cli:main",
            "run-tests=optimized_framework.runner:main",
        ],
        "pytest11": [
            "ai_ready_framework=optimized_framework.pytest_plugin",
        ],
    },
    
    # 分类器
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Intended Audience :: Quality Engineers",
        "Topic :: Software Development :: Testing",
        "Topic :: Software Development :: Quality Assurance",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Operating System :: OS Independent",
        "Framework :: Pytest",
    ],
    
    # 其他元数据
    python_requires=">=3.8",
    license="MIT",
    keywords=[
        "testing",
        "automation",
        "framework",
        "pytest",
        "qa",
        "test-automation",
        "api-testing",
        "ui-testing",
        "performance-testing",
    ],
    
    # 项目URLs
    project_urls={
        "Documentation": "https://docs.ai-ready.local/testing",
        "Source": "https://github.com/ai-ready/test-framework",
        "Tracker": "https://github.com/ai-ready/test-framework/issues",
        "Changelog": "https://github.com/ai-ready/test-framework/releases",
    },
    
    # Zip安全设置
    zip_safe=False,
    
    # 脚本
    scripts=[
        "scripts/run_all_tests.sh",
        "scripts/setup_test_env.py",
        "scripts/generate_test_report.py",
    ],
)

if __name__ == "__main__":
    # 验证安装环境
    python_version = sys.version_info[:2]
    if python_version < (3, 8):
        print(f"错误: 需要Python 3.8或更高版本，当前版本: {sys.version}")
        sys.exit(1)
    
    print("AI-Ready自动化测试框架安装包配置完成")
    print(f"- 包名: ai-ready-test-framework")
    print(f"- 版本: 2.0.0")
    print(f"- Python要求: >=3.8")
    print(f"- 依赖包数: {len(requirements)}")
    print(f"- 开发依赖包数: {len(dev_requirements)}")