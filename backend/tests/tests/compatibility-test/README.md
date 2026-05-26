# 企智连兼容性测试项目

## 项目概述

本项目包含企智连系统的兼容性测试方案、测试矩阵和测试脚本。

## 目录结构

```
compatibility-test/
├── COMPATIBILITY_TEST_PLAN.md    # 兼容性测试方案
├── README.md                      # 项目说明
└── matrix/                        # 测试矩阵
    ├── browser_compatibility_matrix.csv
    └── database_compatibility_matrix.csv
```

## 测试覆盖范围

### 1. 浏览器兼容性
- Chrome 120+ (P0)
- Firefox 120+ (P0)
- Edge 120+ (P0)
- Safari 17+ (P1)

### 2. 移动端兼容性
- iOS 16+ (P0)
- Android 12+ (P0)

### 3. 数据库兼容性
- PostgreSQL 14/15/16 (P0/P1)
- MySQL 5.7/8.0 (P0/P1)

### 4. 操作系统兼容性
- Windows 10/11 (P0)
- macOS 13/14 (P0)
- Ubuntu 22.04 (P2)

## 测试工具

| 工具 | 用途 |
|------|------|
| Selenium WebDriver | 浏览器自动化测试 |
| Selenium Grid | 多浏览器并行测试 |
| Appium | 移动端自动化测试 |
| BrowserStack | 云端真机测试(可选) |

## 快速开始

### 运行浏览器测试

```bash
# Chrome测试
mvn test -Dbrowser=chrome -Dplatform=windows

# Firefox测试
mvn test -Dbrowser=firefox -Dplatform=windows

# Edge测试
mvn test -Dbrowser=edge -Dplatform=windows

# 所有浏览器
mvn test -Pbrowser-all
```

### 运行移动端测试

```bash
# Android测试
mvn test -Dplatform=android -Ddevice="Samsung S24"

# iOS测试
mvn test -Dplatform=ios -Ddevice="iPhone 15 Pro"
```

### 运行数据库测试

```bash
# PostgreSQL测试
mvn test -Ddatabase=postgresql -Dversion=16

# MySQL测试
mvn test -Ddatabase=mysql -Dversion=8.0
```

## 测试矩阵更新

每次测试完成后，更新对应矩阵文件：

1. `browser_compatibility_matrix.csv` - 浏览器测试结果
2. `database_compatibility_matrix.csv` - 数据库测试结果

## 缺陷管理

发现缺陷时，创建缺陷记录：

```
标题: [浏览器/系统] 具体问题描述
环境: Chrome 120 + Windows 11
优先级: P0/P1/P2
步骤:
1. xxx
2. xxx
预期: xxx
实际: xxx
附件: 截图/日志
```

## 维护说明

- 定期更新浏览器版本支持
- 新设备发布时更新测试矩阵
- 数据库版本升级时补充测试

---

**负责人**: QA Lead  
**创建日期**: 2026-04-14