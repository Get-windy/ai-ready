# 智企连·AI-Ready 文档索引

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 快速导航

| 文档类型 | 说明 | 跳转 |
|----------|------|------|
| 开发规范 | 代码规范、Git 规范、PR 审核规范 | [开发规范](#1-开发规范) |
| API 文档 | 接口规范、错误码规范 | [API 文档](#2-api-文档) |
| 开发指南 | 环境搭建、新人入门、FAQ | [开发指南](#3-开发指南) |
| 架构文档 | 系统架构、高可用设计 | [架构文档](#4-架构文档) |
| 数据库文档 | PostgreSQL 配置、表结构 | [数据库文档](#5-数据库文档) |

---

## 1. 开发规范

### 1.1 代码规范

| 文档 | 说明 | 路径 |
|------|------|------|
| Java 代码规范 | Java 后端编码规范 | [standards/JAVA_CODE_STANDARDS.md](standards/JAVA_CODE_STANDARDS.md) |
| 前端代码规范 | Vue3 + TypeScript 编码规范 | [standards/FRONTEND_CODE_STANDARDS.md](standards/FRONTEND_CODE_STANDARDS.md) |

### 1.2 版本控制规范

| 文档 | 说明 | 路径 |
|------|------|------|
| Git 提交规范 | 分支管理、Commit Message 规范 | [standards/GIT_COMMIT_STANDARDS.md](standards/GIT_COMMIT_STANDARDS.md) |
| PR 审核规范 | 代码审核流程、审核要点 | [standards/PR_REVIEW_STANDARDS.md](standards/PR_REVIEW_STANDARDS.md) |

---

## 2. API 文档

### 2.1 接口规范

| 文档 | 说明 | 路径 |
|------|------|------|
| API 接口规范 | RESTful 设计、请求响应格式 | [api/API_STANDARDS.md](api/API_STANDARDS.md) |
| 错误码规范 | 业务错误码定义 | [api/ERROR_CODES.md](api/ERROR_CODES.md) |

### 2.2 在线文档

- **开发环境**: http://localhost:8080/doc.html
- **测试环境**: https://api-test.aiready.cn/doc.html

---

## 3. 开发指南

### 3.1 环境搭建

| 文档 | 说明 | 路径 |
|------|------|------|
| 环境搭建指南 | JDK、Maven、Node.js 安装配置 | [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) |

### 3.2 入门指南

| 文档 | 说明 | 路径 |
|------|------|------|
| 新人入门指南 | 5 天快速上手计划 | [guide/ONBOARDING_GUIDE.md](guide/ONBOARDING_GUIDE.md) |
| 常见问题 FAQ | 开发常见问题解答 | [guide/FAQ.md](guide/FAQ.md) |

---

## 4. 架构文档

| 文档 | 说明 | 路径 |
|------|------|------|
| 高可用架构设计 | 系统高可用方案 | [HIGH_AVAILABILITY_ARCHITECTURE.md](HIGH_AVAILABILITY_ARCHITECTURE.md) |
| 部署指南 | Docker/K8s 部署方案 | [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) |

---

## 5. 数据库文档

| 文档 | 说明 | 路径 |
|------|------|------|
| PostgreSQL 配置 | 数据库安装配置 | [database/postgresql-config.md](database/postgresql-config.md) |
| 开发环境配置 | 本地开发数据库设置 | [database/development-setup.md](database/development-setup.md) |
| 基础表结构 | 初始化 SQL 脚本 | [database/init-base-tables.sql](database/init-base-tables.sql) |

---

## 6. 学习资料

| 文档 | 说明 | 路径 |
|------|------|------|
| 项目规划摘要 | 项目整体规划 | [learning/项目规划摘要.md](learning/) |
| 学习笔记 | 团队学习记录 | [learning/](learning/) |

---

## 7. 文档维护

### 7.1 文档更新流程

1. 创建或更新文档
2. 更新本文档索引
3. 提交 PR 审核
4. 合并到主分支

### 7.2 文档命名规范

- 使用大写字母和下划线：`DOCUMENT_NAME.md`
- 目录使用小写字母：`directory-name/`
- 版本号格式：`v1.0`

### 7.3 文档模板

文档模板位于项目根目录的 `templates/` 文件夹（如已创建）。

---

## 8. 联系方式

- **项目群组**: 智企连·AI-Ready 项目开发群
- **技术支持**: support@aiedge.cn

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer