# AI-Ready 服务启停脚本使用文档

**版本**: v2.0  
**日期**: 2026-04-01  
**作者**: devops-engineer

---

## 一、脚本概述

### 1.1 功能特性

- ✅ 按依赖顺序启动服务
- ✅ 优雅关闭（先停止应用，再停止基础设施）
- ✅ 服务健康检查
- ✅ 超时控制
- ✅ 日志记录

### 1.2 服务依赖顺序

```
启动: PostgreSQL → Redis → Nacos → AI-Ready API
停止: AI-Ready API → Nacos → Redis → PostgreSQL
```

---

## 二、使用方法

### 2.1 启动服务

```bash
./ai-ready-service.sh start
```

### 2.2 停止服务

```bash
./ai-ready-service.sh stop
```

### 2.3 重启服务

```bash
./ai-ready-service.sh restart
```

### 2.4 查看状态

```bash
./ai-ready-service.sh status
```

---

## 三、配置说明

| 变量 | 默认值 | 说明 |
|------|--------|------|
| APP_HOME | /opt/ai-ready | 应用目录 |
| LOG_DIR | /var/log/ai-ready | 日志目录 |
| START_TIMEOUT | 120 | 启动超时(秒) |
| STOP_TIMEOUT | 60 | 停止超时(秒) |

---

## 四、服务端口

| 服务 | 端口 | 健康检查 |
|------|------|----------|
| PostgreSQL | 5432 | TCP连接 |
| Redis | 6379 | TCP连接 |
| Nacos | 8848 | TCP连接 |
| AI-Ready API | 8080 | /actuator/health |

---

**版本**: v2.0