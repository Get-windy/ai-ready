#!/bin/bash
# AI-Ready Performance Report Generator
# 性能报告生成脚本

set -e

REPORT_DIR="I:/AI-Ready/reports/performance"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
REPORT_FILE="$REPORT_DIR/performance_report_$TIMESTAMP.md"

mkdir -p "$REPORT_DIR"

echo "Generating performance report..."

cat > "$REPORT_FILE" << 'EOF'
# AI-Ready Performance Report

**生成时间**: $(date '+%Y-%m-%d %H:%M:%S')  
**报告周期**: $(date -d '1 day ago' '+%Y-%m-%d') ~ $(date '+%Y-%m-%d')

---

## 一、系统概览

### 1.1 服务状态

| 服务 | 状态 | CPU | 内存 | 运行时间 |
|-----|------|-----|------|---------|
| ai-ready-api | $(systemctl is-active ai-ready-api 2>/dev/null || echo 'N/A') | $(ps -p $(pgrep -f ai-ready-api) -o %cpu= 2>/dev/null || echo 'N/A')% | $(ps -p $(pgrep -f ai-ready-api) -o %mem= 2>/dev/null || echo 'N/A')% | $(ps -p $(pgrep -f ai-ready-api) -o etime= 2>/dev/null || echo 'N/A') |
| redis | $(systemctl is-active redis 2>/dev/null || echo 'N/A') | - | - | - |
| postgresql | $(systemctl is-active postgresql 2>/dev/null || echo 'N/A') | - | - | - |

### 1.2 数据库性能

```sql
-- 慢查询统计
SELECT count(*) as slow_query_count 
FROM pg_stat_statements 
WHERE mean_time > 1000;

