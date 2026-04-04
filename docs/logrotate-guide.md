# AI-Ready 日志轮转配置文档

**项目**: 智企连·AI-Ready  
**日期**: 2026-04-04

---

## 一、日志轮转规则

| 日志类型 | 轮转周期 | 保留数量 | 压缩 |
|----------|----------|----------|------|
| 应用日志 | 每日 | 30天 | gzip |
| 错误日志 | 每日 | 60天 | gzip |
| GC日志 | 每周 | 4周 | gzip |

## 二、部署

```bash
# 安装配置
cp config/logrotate.conf /etc/logrotate.d/ai-ready

# 测试配置
logrotate -d /etc/logrotate.d/ai-ready

# 手动触发
logrotate -f /etc/logrotate.d/ai-ready
```

## 三、日志归档

归档路径: `/var/log/ai-ready/archive/`

---

**文档生成**: devops-engineer
