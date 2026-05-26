#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试框架安装脚本

自动安装和配置测试框架依赖
"""

import subprocess
import sys
from pathlib import Path


def install_dependencies():
    """安装测试框架依赖"""
    print("正在安装测试框架依赖...")
    
    # 基础依赖
    base_packages = [
        "pytest>=7.4.0",
        "pytest-asyncio>=0.21.0",
        "pytest-cov>=4.1.0",
        "pytest-html>=3.2.0",
        "pytest-json-report>=1.5.0",
        "pytest-metadata>=3.1.0",
        "pytest-order>=1.1.0",
        "pytest-rerunfailures>=12.0",
        "pytest-timeout>=2.2.0",
        "pytest-xdist>=3.3.0",
    ]
    
    # HTTP客户端
    http_packages = [
        "httpx>=0.24.0",
        "requests>=2.31.0",
    ]
    
    # 数据生成
    data_packages = [
        "faker>=19.0.0",
        "python-dateutil>=2.8.2",
    ]
    
    # 数据库支持
    db_packages = [
        "psycopg2-binary>=2.9.9",
        "redis>=5.0.0",
        "pika>=1.3.0",
        "elasticsearch>=8.12.0",
    ]
    
    # 云存储
    cloud_packages = [
        "boto3>=1.34.0",
        "minio>=7.2.0",
    ]
    
    # 其他工具
    utils_packages = [
        "pydantic>=2.5.0",
        "python-dotenv>=1.0.0",
        "alive-progress>=3.1.0",
    ]
    
    # 代码质量
    quality_packages = [
        "black>=23.11.0",
        "flake8>=7.0.0",
        "mypy>=1.7.0",
    ]
    
    # 性能测试
    perf_packages = [
        "pytest-benchmark>=4.0.0",
    ]
    
    all_packages = (
        base_packages + 
        http_packages + 
        data_packages + 
        db_packages + 
        cloud_packages + 
        utils_packages + 
        quality_packages + 
        perf_packages
    )
    
    # 安装所有包
    for package in all_packages:
        print(f"安装 {package}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package])
    
    print("所有依赖安装完成！")


def verify_installation():
    """验证安装"""
    print("\n验证测试框架安装...")
    
    try:
        import pytest
        print(f"✓ pytest {pytest.__version__}")
    except ImportError:
        print("✗ pytest 未安装")
        return False
    
    try:
        import pytest_xdist
        print("✓ pytest-xdist 已安装")
    except ImportError:
        print("✗ pytest-xdist 未安装")
        return False
    
    try:
        import pytest_html
        print("✓ pytest-html 已安装")
    except ImportError:
        print("✗ pytest-html 未安装")
        return False
    
    try:
        import pytest_cov
        print("✓ pytest-cov 已安装")
    except ImportError:
        print("✗ pytest-cov 未安装")
        return False
    
    try:
        import httpx
        print(f"✓ httpx {httpx.__version__}")
    except ImportError:
        print("✗ httpx 未安装")
        return False
    
    print("\n所有组件验证通过！")
    return True


def create_directories():
    """创建必要的目录"""
    print("\n创建测试目录结构...")
    
    dirs = [
        "reports",
        "results",
        "fixtures",
        "data",
        "logs",
        "screenshots",
    ]
    
    for dir_name in dirs:
        Path(dir_name).mkdir(exist_ok=True)
        print(f"✓ 创建目录: {dir_name}")


def main():
    """主函数"""
    print("=" * 50)
    print("AI-Ready 测试框架安装程序")
    print("=" * 50)
    
    try:
        install_dependencies()
        create_directories()
        
        if verify_installation():
            print("\n" + "=" * 50)
            print("测试框架安装成功！")
            print("=" * 50)
            print("\n使用方法:")
            print("  python run_tests.py --help")
            print("  pytest tests/ -v")
            print("  pytest tests/ -n auto  # 并行测试")
        else:
            print("\n安装验证失败，请检查错误信息")
            sys.exit(1)
    
    except Exception as e:
        print(f"\n安装过程中出现错误: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
